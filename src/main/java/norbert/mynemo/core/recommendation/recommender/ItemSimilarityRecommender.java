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

import static com.google.common.base.Preconditions.checkArgument;
import norbert.mynemo.core.recommendation.RecommenderType;
import norbert.mynemo.core.recommendation.configuration.ItemBasedRecommenderConfiguration;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.common.Weighting;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.recommender.GenericItemBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.CachingItemSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.CityBlockSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.EuclideanDistanceSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.LogLikelihoodSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.TanimotoCoefficientSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.UncenteredCosineSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.ItemSimilarity;

/**
 * This class represents several recommender builders. The recommenders are based on similarities
 * between the items. A recommender is chosen via the
 * {@link #ItemSimilarityRecommender(ItemBasedRecommenderConfiguration) constructor}.
 */
public class ItemSimilarityRecommender implements RecommenderBuilder {
  // 200 mega
  private static final int MAXIMUM_CACHE_SIZE = (int) (200 * Math.pow(2, 30));
  private final RecommenderType selectedSimilarity;

  public ItemSimilarityRecommender(ItemBasedRecommenderConfiguration configuration) {
    checkArgument(configuration != null, "Recommender type must not be null.");

    selectedSimilarity = configuration.getType();
  }

  @Override
  public Recommender buildRecommender(DataModel dataModel) throws TasteException {
    checkArgument(dataModel != null, "Data model type must not be null.");

    ItemSimilarity similarity = null;
    switch (selectedSimilarity) {
      case ITEM_SIMILARITY_WITH_CITY_BLOCK_DISTANCE:
        similarity = new CityBlockSimilarity(dataModel);
        break;
      case ITEM_SIMILARITY_WITH_EUCLIDEAN_DISTANCE:
        similarity = new EuclideanDistanceSimilarity(dataModel, Weighting.UNWEIGHTED);
        break;
      case ITEM_SIMILARITY_WITH_LOG_LIKELIHOOD:
        similarity = new LogLikelihoodSimilarity(dataModel);
        break;
      case ITEM_SIMILARITY_WITH_PEARSON_CORRELATION:
        similarity = new PearsonCorrelationSimilarity(dataModel, Weighting.UNWEIGHTED);
        break;
      case ITEM_SIMILARITY_WITH_TANIMOTO_COEFFICIENT:
        similarity = new TanimotoCoefficientSimilarity(dataModel);
        break;
      case ITEM_SIMILARITY_WITH_UNCENTERED_COSINE:
        similarity = new UncenteredCosineSimilarity(dataModel, Weighting.UNWEIGHTED);
        break;
      case ITEM_SIMILARITY_WITH_WEIGHTED_EUCLIDEAN_DISTANCE:
        similarity = new EuclideanDistanceSimilarity(dataModel, Weighting.WEIGHTED);
        break;
      case ITEM_SIMILARITY_WITH_WEIGHTED_PEARSON_CORRELATION:
        similarity = new PearsonCorrelationSimilarity(dataModel, Weighting.WEIGHTED);
        break;
      case ITEM_SIMILARITY_WITH_WEIGHTED_UNCENTERED_COSINE:
        similarity = new UncenteredCosineSimilarity(dataModel, Weighting.WEIGHTED);
        break;
      default:
        throw new IllegalStateException();
    }

    int cacheSize = Math.min(dataModel.getNumItems() * dataModel.getNumItems(), MAXIMUM_CACHE_SIZE);
    similarity = new CachingItemSimilarity(similarity, cacheSize);

    return new GenericItemBasedRecommender(dataModel, similarity);
  }
}
