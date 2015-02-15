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
package norbert.mynemo.core.recommendation.recommender;

import norbert.mynemo.core.recommendation.configuration.UserBasedRecommenderConfiguration;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.common.Weighting;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.neighborhood.CachingUserNeighborhood;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.CachingUserSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.CityBlockSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.EuclideanDistanceSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.LogLikelihoodSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.SpearmanCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.TanimotoCoefficientSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.UncenteredCosineSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

/**
 * This class represents several recommender builders. The recommenders are based on similarities
 * between the users. A builder is chosen via the
 * {@link #UserSimilarityRecommender(UserBasedRecommenderConfiguration) constructor}.
 */
public class UserSimilarityRecommender implements RecommenderBuilder {
  // 200 mega
  private static final int MAXIMUM_CACHE_SIZE = (int) (200 * Math.pow(2, 30));

  private UserSimilarity cachedSimilarity;
  private final UserBasedRecommenderConfiguration configuration;

  /**
   * Creates a recommender builder specialized in the user-similarity based algorithms.
   *
   * <p>
   * Whereas the similarity matrix can be reuse, the neighborhood is never reused.
   * </p>
   */
  public UserSimilarityRecommender(UserBasedRecommenderConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public Recommender buildRecommender(DataModel dataModel) throws TasteException {
    UserSimilarity currentSimilarity;
    if (!configuration.allowCachedSimilarityReuse()) {
      // create a new similarity each time
      currentSimilarity = createSimilarity(dataModel);
    } else {
      if (cachedSimilarity == null) {
        // lazy initialization
        cachedSimilarity = createSimilarity(configuration.getDataModel());
      }
      currentSimilarity = cachedSimilarity;
    }

    UserNeighborhood neighborhood =
        new NearestNUserNeighborhood(configuration.getNeighborNumber(), currentSimilarity,
            dataModel);
    neighborhood = new CachingUserNeighborhood(neighborhood, dataModel);

    return new GenericUserBasedRecommender(dataModel, neighborhood, currentSimilarity);
  }

  private UserSimilarity createSimilarity(DataModel dataModel) throws TasteException {
    UserSimilarity selectedSimilarity;

    switch (configuration.getType()) {
      case USER_SIMILARITY_WITH_CITY_BLOCK_DISTANCE:
        selectedSimilarity = new CityBlockSimilarity(dataModel);
        break;

      case USER_SIMILARITY_WITH_EUCLIDEAN_DISTANCE:
        selectedSimilarity = new EuclideanDistanceSimilarity(dataModel, Weighting.UNWEIGHTED);
        break;

      case USER_SIMILARITY_WITH_LOG_LIKELIHOOD:
        selectedSimilarity = new LogLikelihoodSimilarity(dataModel);
        break;

      case USER_SIMILARITY_WITH_PEARSON_CORRELATION:
        selectedSimilarity = new PearsonCorrelationSimilarity(dataModel, Weighting.UNWEIGHTED);
        break;

      case USER_SIMILARITY_WITH_SPEARMAN_CORRELATION:
        selectedSimilarity = new SpearmanCorrelationSimilarity(dataModel);
        break;

      case USER_SIMILARITY_WITH_TANIMOTO_COEFFICIENT:
        selectedSimilarity = new TanimotoCoefficientSimilarity(dataModel);
        break;

      case USER_SIMILARITY_WITH_UNCENTERED_COSINE:
        selectedSimilarity = new UncenteredCosineSimilarity(dataModel);
        break;

      case USER_SIMILARITY_WITH_WEIGHTED_EUCLIDEAN_DISTANCE:
        selectedSimilarity = new EuclideanDistanceSimilarity(dataModel, Weighting.WEIGHTED);
        break;

      case USER_SIMILARITY_WITH_WEIGHTED_PEARSON_CORRELATION:
        selectedSimilarity = new PearsonCorrelationSimilarity(dataModel, Weighting.WEIGHTED);
        break;

      default:
        throw new IllegalStateException();
    }

    int cacheSize = Math.min(dataModel.getNumUsers() * dataModel.getNumUsers(), MAXIMUM_CACHE_SIZE);

    return new CachingUserSimilarity(selectedSimilarity, cacheSize);
  }
}
