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
package norbert.mynemo.core.recommendation.configuration;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import norbert.mynemo.core.recommendation.RecommenderFamily;
import norbert.mynemo.core.recommendation.RecommenderType;
import norbert.mynemo.core.recommendation.recommender.UserSimilarityRecommender;

import org.apache.mahout.cf.taste.eval.RecommenderEvaluator;
import org.apache.mahout.cf.taste.model.DataModel;

/**
 * This class represents a configuration for the {@link UserSimilarityRecommender}.
 */
public class UserBasedRecommenderConfiguration implements RecommenderConfiguration {

  private final DataModel dataModel;
  private final int neighborNumber;
  private final boolean reuseSimilarity;
  private final RecommenderType type;

  /**
   * Creates a configuration for the {{@link UserSimilarityRecommender}.
   *
   * <p>
   * The <code>reuseSimilarity</code> parameter can improve the speed of a build recommender by
   * avoiding to recompute some values. The similarity computation will be based on the given data
   * model, not the one given to the builder. Thus, the caller must be careful that a very
   * mismatching {@link DataModel} won't be given to the
   * {@link UserSimilarityRecommender#buildRecommender(DataModel)} method. Generally speaking, the
   * similarity can be reuse only when the <code>evaluationPercentage</code> given to a subclasses
   * of {@link RecommenderEvaluator} is <code>1</code>.
   * </p>
   *
   * @param type the type of similarity
   * @param neighborNumber the maximum number of neighbors for a user
   * @param dataModel the complete dataModel, only necessary if the reuseSimilarity is
   *        <code>true</code>, may be null otherwise
   * @param reuseSimilarity if <code>true</code>, do not recompute the similarity matrix between two
   *        creations of recommender
   */
  public UserBasedRecommenderConfiguration(RecommenderType type, int neighborNumber,
      DataModel dataModel, boolean reuseSimilarity) {
    checkArgument(type.getFamily() == RecommenderFamily.USER_SIMILARITY_BASED);
    checkArgument(!reuseSimilarity || dataModel != null, "A data model must be provided if the"
        + " similarities have to be reused.");

    this.neighborNumber = neighborNumber;
    this.type = type;
    this.reuseSimilarity = reuseSimilarity;
    // keep the data model only if necessary
    this.dataModel = reuseSimilarity ? dataModel : null;
  }

  public boolean allowCachedSimilarityReuse() {
    return reuseSimilarity;
  }

  /**
   * Returns the data model. Throws an exception if the {@link #allowCachedSimilarityReuse()} method
   * returns <code>false</code>.
   */
  public DataModel getDataModel() {
    checkState(allowCachedSimilarityReuse());
    return dataModel;
  }

  public int getNeighborNumber() {
    return neighborNumber;
  }

  @Override
  public RecommenderType getType() {
    return type;
  }

  @Override
  public String toString() {
    return type.toString() + " with " + neighborNumber + " maximum neighbors";
  }
}
