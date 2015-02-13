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

import static com.google.common.base.Preconditions.checkState;
import norbert.mynemo.core.recommendation.RecommenderType;
import norbert.mynemo.core.recommendation.recommender.SvdBasedRecommender;
import norbert.mynemo.core.recommendation.recommender.UserSimilarityRecommender;

import org.apache.mahout.cf.taste.eval.RecommenderEvaluator;
import org.apache.mahout.cf.taste.model.DataModel;

/**
 * This class represents a configuration for the {@link SvdBasedRecommender}.
 */
public class SvdBasedRecommenderConfiguration implements RecommenderConfiguration {

  private final DataModel dataModel;
  private final int featureNumber;
  private final int iteractionNumber;
  private final boolean reuseFactorizer;
  private final RecommenderType type;

  /**
   * Creates a configuration.
   *
   * <p>
   * The <code>reuseFactorization</code> parameter can improve the speed of a build recommender by
   * avoiding to recompute some values. The factorization will be based on the given data model, not
   * the one given to the builder. Thus, the caller must be careful that only slightly mismatching
   * {@link DataModel} will be given to the
   * {@link UserSimilarityRecommender#buildRecommender(DataModel)} method. Generally speaking, the
   * factorization can be reuse only when the <code>evaluationPercentage</code> given to a
   * subclasses of {@link RecommenderEvaluator} is <code>1</code>.
   * </p>
   *
   * @param type type of the recommender
   * @param numberOfFeatures number of features
   * @param numberOfIterations number of iterations
   * @param dataModel data model, only necessary if <code>reuseFactorization</code> is
   *        <code>true</code>, may be <code>null</code> otherwise
   * @param reuseFactorization if <code>true</code>, do not recompute the factorization between two
   *        creations of recommender
   */
  public SvdBasedRecommenderConfiguration(RecommenderType type, int numberOfFeatures,
      int numberOfIterations, DataModel dataModel, boolean reuseFactorization) {
    this.type = type;
    this.featureNumber = numberOfFeatures;
    this.iteractionNumber = numberOfIterations;
    this.dataModel = dataModel;
    this.reuseFactorizer = reuseFactorization;
  }

  public boolean allowCachedFactorizationReuse() {
    return reuseFactorizer;
  }

  /**
   * Returns the data model. Throws an exception if the {@link #allowCachedFactorizationReuse()}
   * method returns <code>false</code>.
   */
  public DataModel getDataModel() {
    checkState(allowCachedFactorizationReuse());
    return dataModel;
  }

  public int getFeatureNumber() {
    return featureNumber;
  }

  public int getIterationNumber() {
    return iteractionNumber;
  }

  @Override
  public RecommenderType getType() {
    return type;
  }

  @Override
  public String toString() {
    return type.toString() + " with " + featureNumber + " features and " + iteractionNumber
        + " iterations";
  }
}
