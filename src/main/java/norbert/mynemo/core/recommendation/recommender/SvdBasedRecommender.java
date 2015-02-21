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

import java.io.IOException;

import norbert.mynemo.core.recommendation.configuration.SvdBasedRecommenderConfiguration;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.recommender.svd.ALSWRFactorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.Factorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.ParallelSGDFactorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.PersistenceStrategy;
import org.apache.mahout.cf.taste.impl.recommender.svd.RatingSGDFactorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.SVDPlusPlusFactorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.SVDRecommender;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.Recommender;

/**
 * This class represents several recommender builders. The recommenders are based on the SVD method.
 * A builder is chosen via the {@link #SvdBasedRecommender(SvdBasedRecommenderConfiguration)
 * constructor}.
 */
public class SvdBasedRecommender implements RecommenderBuilder {

  private static final int DEFAULT_EPOCHS = 1;
  private static final double DEFAULT_LAMBDA = 0.065;
  private Factorizer cachedFactorizer;
  private final SvdBasedRecommenderConfiguration configuration;
  private PersistenceStrategy persistenceStrategy;

  public SvdBasedRecommender(SvdBasedRecommenderConfiguration configuration) {
    checkArgument(configuration != null, "The configuration must not be null.");
    this.configuration = configuration;
  }

  @Override
  public Recommender buildRecommender(DataModel dataModel) throws TasteException {
    if (!configuration.allowCachedFactorizationReuse()) {

      // create a new factorizer each time
      return new SVDRecommender(dataModel, createFactorizer(dataModel));

    }

    if (cachedFactorizer == null) {
      // lazy initialization
      cachedFactorizer = createFactorizer(configuration.getDataModel());
      try {
        persistenceStrategy = new RamPersistenceStrategy();
        persistenceStrategy.maybePersist(cachedFactorizer.factorize());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    return new SVDRecommender(dataModel, cachedFactorizer, persistenceStrategy);
  }

  private Factorizer createFactorizer(DataModel dataModel) throws TasteException {
    int featuresNumber = configuration.getFeatureNumber();
    int iterationsNumber = configuration.getIterationNumber();
    Factorizer result;

    switch (configuration.getType()) {
      case SVD_WITH_ALSWR_FACTORIZER:
        result = new ALSWRFactorizer(dataModel, featuresNumber, DEFAULT_LAMBDA, iterationsNumber);
        break;

      case SVD_WITH_PARALLEL_SGD_FACTORIZER:
        result =
            new ParallelSGDFactorizer(dataModel, featuresNumber, DEFAULT_LAMBDA, DEFAULT_EPOCHS);
        break;

      case SVD_WITH_RATING_SGD_FACTORIZER:
        result = new RatingSGDFactorizer(dataModel, featuresNumber, iterationsNumber);
        break;

      case SVD_WITH_SVDPLUSPLUS_FACTORIZER:
        result = new SVDPlusPlusFactorizer(dataModel, featuresNumber, iterationsNumber);
        break;

      default:
        throw new UnsupportedOperationException();
    }

    return result;
  }
}
