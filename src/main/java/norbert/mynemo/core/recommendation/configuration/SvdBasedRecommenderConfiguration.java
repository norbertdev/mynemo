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

import norbert.mynemo.core.recommendation.RecommenderType;
import norbert.mynemo.core.recommendation.recommender.SvdBasedRecommender;

/**
 * This class represents a configuration for the {@link SvdBasedRecommender}.
 */
public class SvdBasedRecommenderConfiguration implements RecommenderConfiguration {

  private final int featureNumber;
  private final int iteractionNumber;
  private final RecommenderType type;

  public SvdBasedRecommenderConfiguration(RecommenderType type, int numberOfFeatures,
      int numberOfIterations) {
    this.type = type;
    this.featureNumber = numberOfFeatures;
    this.iteractionNumber = numberOfIterations;

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
