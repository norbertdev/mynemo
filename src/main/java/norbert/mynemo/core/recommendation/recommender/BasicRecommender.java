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

import norbert.mynemo.core.recommendation.RecommenderType;
import norbert.mynemo.core.recommendation.configuration.BasicRecommenderConfiguration;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.recommender.ItemAverageRecommender;
import org.apache.mahout.cf.taste.impl.recommender.ItemUserAverageRecommender;
import org.apache.mahout.cf.taste.impl.recommender.RandomRecommender;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.Recommender;

/**
 * This class represents several recommender builders. A builder is chosen via the
 * {@link #BasicRecommender(BasicRecommenderConfiguration) constructor}.
 */
public class BasicRecommender implements RecommenderBuilder {

  private RecommenderType recommender;

  public BasicRecommender(BasicRecommenderConfiguration configuration) {
    this.recommender = configuration.getType();
  }

  @Override
  public Recommender buildRecommender(DataModel dataModel) throws TasteException {

    switch (recommender) {
      case ITEM_AVERAGE:
        return new ItemAverageRecommender(dataModel);
      case RANDOM:
        return new RandomRecommender(dataModel);
      case USER_AVERAGE:
        return new ItemUserAverageRecommender(dataModel);
      default:
        throw new IllegalStateException();
    }
  }
}
