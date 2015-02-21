/*
 * Copyright 2015 Norbert
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package norbert.mynemo.dataimport.fileformat.output;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

import norbert.mynemo.core.recommendation.similarity.OriginalSpearmanCorrelationSimilarity;
import norbert.mynemo.dataimport.fileformat.MynemoRating;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.common.Weighting;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.model.GenericDataModel;
import org.apache.mahout.cf.taste.impl.model.GenericUserPreferenceArray;
import org.apache.mahout.cf.taste.impl.similarity.CityBlockSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.EuclideanDistanceSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.LogLikelihoodSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.SpearmanCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.TanimotoCoefficientSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.UncenteredCosineSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

import com.google.common.base.Charsets;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.MinMaxPriorityQueue;
import com.google.common.collect.Multimap;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 * This filter writes only the ratings of a given number of users. Only the nearest neighbors of the
 * given user are written.
 */
public class MaxNeighborUserFilter implements RatingWriter {
  /**
   * Comparable user such as <code>u1 < u2</code> if <code>u1</code> is more similar than
   * <code>u2</code>. Two users are equal if their similarities are equal.
   */
  private class ComparableUser implements Comparable<ComparableUser> {

    private final double similarity;
    private final String user;

    public ComparableUser(String user, double similarity) {
      this.user = user;
      this.similarity = similarity;
    }

    @Override
    public int compareTo(ComparableUser o) {
      double difference = this.similarity - o.similarity;
      if (difference < 0) {
        return 1;
      }
      if (0 < difference) {
        return -1;
      } else {
        return 0;
      }
    };

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof ComparableUser)) {
        return false;
      }

      return similarity == ((ComparableUser) o).similarity;
    }

    public String getUser() {
      return user;
    }

    @Override
    public int hashCode() {
      return new HashCodeBuilder().append(similarity).toHashCode();
    }
  }

  private static final HashFunction HASH_FUNCTION = Hashing.goodFastHash(64);

  /**
   * Returns a <code>long</code> hash from the given string.
   *
   * @param string a string
   * @return the hash of the string
   */
  private static long encode(String string) {
    return HASH_FUNCTION.newHasher().putString(string, Charsets.UTF_8).hash().asLong();
  }

  /**
   * All ratings. A key is a user, its value is the user's ratings.
   */
  private final Multimap<String, MynemoRating> allRatings;
  private final int maxUsers;
  private final RatingWriter nextWriter;
  private final UserSimilarityType similarityType;
  private final String targetUser;

  public MaxNeighborUserFilter(RatingWriter nextWriter, String user, int maxUsers,
      UserSimilarityType similarityType) {
    checkNotNull(nextWriter);
    checkNotNull(user);
    checkArgument(1 <= maxUsers, "The maximum number of users must be at least 1.");

    this.maxUsers = maxUsers;
    this.nextWriter = nextWriter;
    this.similarityType = similarityType;
    this.targetUser = user;

    allRatings = HashMultimap.create();
  }

  @Override
  public void close() throws IOException {
    try {
      HashSet<String> similarUsers = getMostSimilarUsers(createUserSimilarity());

      for (String user : allRatings.keySet()) {
        if (similarUsers.contains(user)) {
          writeAll(allRatings.get(user));
        }
      }
    } catch (TasteException e) {
      throw new RuntimeException(e);
    };

    allRatings.clear();

    nextWriter.close();
  }

  /**
   * Creates a data model from the {@link #allRatings} field.
   */
  private DataModel createDataModel() {
    FastByIDMap<PreferenceArray> map = new FastByIDMap<>(allRatings.keySet().size());

    for (String user : allRatings.keySet()) {
      long longUser = encode(user);
      List<MynemoRating> ratings = newArrayList(allRatings.get(user));

      PreferenceArray newPreferences = new GenericUserPreferenceArray(ratings.size());

      // set the user
      newPreferences.setUserID(0, longUser);

      // add its preferences
      for (int index = 0; index < newPreferences.length(); index++) {
        MynemoRating rating = ratings.get(index);

        newPreferences.setItemID(index, rating.getLongMovie());
        newPreferences.setValue(index, rating.getFloatValue());
      }

      map.put(longUser, newPreferences);
    }

    checkState(map.containsKey(encode(targetUser)), "The target user of the similarity must have"
        + " at least one rating.");

    return new GenericDataModel(map);
  }

  /**
   * Creates and returns a user similarity according to the {@link #similarityType} field.
   */
  private UserSimilarity createUserSimilarity() throws TasteException {
    DataModel dataModel = createDataModel();

    UserSimilarity result;

    switch (similarityType) {
      case CITY_BLOCK_DISTANCE:
        result = new CityBlockSimilarity(dataModel);
        break;

      case EUCLIDEAN_DISTANCE:
        result = new EuclideanDistanceSimilarity(dataModel, Weighting.UNWEIGHTED);
        break;

      case LOG_LIKELIHOOD:
        result = new LogLikelihoodSimilarity(dataModel);
        break;

      case ORIGINAL_SPEARMAN_CORRELATION:
        result = new OriginalSpearmanCorrelationSimilarity(dataModel);
        break;

      case PEARSON_CORRELATION:
        result = new PearsonCorrelationSimilarity(dataModel, Weighting.UNWEIGHTED);
        break;

      case SPEARMAN_CORRELATION:
        result = new SpearmanCorrelationSimilarity(dataModel);
        break;

      case TANIMOTO_COEFFICIENT:
        result = new TanimotoCoefficientSimilarity(dataModel);
        break;

      case UNCENTERED_COSINE:
        result = new UncenteredCosineSimilarity(dataModel, Weighting.UNWEIGHTED);
        break;

      case WEIGHTED_EUCLIDEAN_DISTANCE:
        result = new EuclideanDistanceSimilarity(dataModel, Weighting.WEIGHTED);
        break;

      case WEIGHTED_PEARSON_CORRELATION:
        result = new PearsonCorrelationSimilarity(dataModel, Weighting.WEIGHTED);
        break;

      case WEIGHTED_UNCENTERED_COSINE:
        result = new UncenteredCosineSimilarity(dataModel, Weighting.WEIGHTED);
        break;

      default:
        throw new UnsupportedOperationException();
    }

    return result;
  }

  /**
   * Returns the most similar users according to the given similarity and the {@link #allUsers}
   * field. The target user is included in the returned collection.
   */
  private HashSet<String> getMostSimilarUsers(UserSimilarity similarity) throws TasteException {
    Queue<ComparableUser> mostSimilarUsers = MinMaxPriorityQueue.maximumSize(maxUsers).create();
    // cache the value of encoding to speed up
    long longUser = encode(targetUser);

    for (String currentUser : allRatings.keySet()) {
      mostSimilarUsers.add(new ComparableUser(currentUser, similarity.userSimilarity(longUser,
          encode(currentUser))));
    }

    HashSet<String> result = new HashSet<String>();
    for (ComparableUser currentUser : mostSimilarUsers) {
      result.add(currentUser.getUser());
    }

    // ensure that the target user is included
    if (!result.contains(targetUser)) {
      // remove a user, then add the target user
      Iterator<String> iterator = result.iterator();
      iterator.next();
      iterator.remove();
      result.add(targetUser);
    }

    return result;
  }

  @Override
  public void write(MynemoRating rating) throws IOException {
    checkNotNull(rating);
    allRatings.put(rating.getUser(), rating);
  }

  /**
   * Writes the given ratings into the next writer.
   */
  private void writeAll(Collection<MynemoRating> ratings) throws IOException {
    for (MynemoRating rating : ratings) {
      nextWriter.write(rating);
    }
  }
}
