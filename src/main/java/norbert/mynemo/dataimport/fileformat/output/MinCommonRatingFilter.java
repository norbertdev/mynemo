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

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import norbert.mynemo.dataimport.fileformat.MynemoRating;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * This filter writes only the ratings of a user if it have at least a minimum number of ratings in
 * common with a target user. Two ratings are common if there movies are equal.
 *
 * <p>
 * The ratings are kept in memory, they are written only when the {@link #clone()} method is called.
 */
public class MinCommonRatingFilter implements RatingWriter {

  /**
   * Returns the number of ratings in common between the two given collections.
   */
  private static int numCommonRatings(final Collection<MynemoRating> ratings1,
      final Collection<MynemoRating> ratings2) {

    final Set<String> movieSet = new HashSet<>();

    // fill the set
    for (MynemoRating rating : ratings1) {
      movieSet.add(rating.getMovie());
    }
    for (MynemoRating rating : ratings2) {
      movieSet.add(rating.getMovie());
    }

    return ratings1.size() + ratings2.size() - movieSet.size();
  }

  /** Every rating given to the 'write' method is kept in this map. */
  private final Multimap<String, MynemoRating> allRatings;
  private final int minCommonRatings;
  private final RatingWriter nextWriter;
  private final String targetUser;

  /**
   * Creates a writer that writes into the given writer. The given minimum must be at least 1.
   */
  public MinCommonRatingFilter(RatingWriter nextWriter, String targetUser, int minCommonRatings) {
    checkNotNull(nextWriter);
    checkNotNull(targetUser);
    checkArgument(1 <= minCommonRatings, "The minimum number of ratings in common must be at"
        + " least 1.");

    this.targetUser = targetUser;
    this.minCommonRatings = minCommonRatings;
    this.nextWriter = nextWriter;

    allRatings = HashMultimap.create();
  }

  @Override
  public void close() throws IOException {

    final Collection<MynemoRating> targetUserRatings = allRatings.get(targetUser);

    // write all writable ratings
    for (String user : allRatings.keySet()) {
      final Collection<MynemoRating> userRatings = allRatings.get(user);
      if (minCommonRatings <= numCommonRatings(userRatings, targetUserRatings)) {
        writeAll(userRatings);
      }
    }

    allRatings.clear();

    nextWriter.close();
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
