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
package norbert.mynemo.core.selection;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import norbert.mynemo.core.evaluation.PersonnalRecommenderEvaluator;
import norbert.mynemo.core.recommendation.RecommenderFamily;
import norbert.mynemo.core.recommendation.RecommenderType;
import norbert.mynemo.core.recommendation.configuration.UserBasedRecommenderConfiguration;
import norbert.mynemo.core.recommendation.recommender.UserSimilarityRecommender;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.DataModelBuilder;
import org.apache.mahout.cf.taste.model.DataModel;

/**
 * This function renders optimizable the number of neighbors for a user-similarity based
 * recommender. Indeed, the {@link #value(double)} method takes a number of neighbors as parameter,
 * and returns the value of a metric produced by an evaluator for that number of neighbors. A list
 * of all evaluations performed are kept, and can be accessed via the {@link #getEvaluations()}
 * method.
 *
 * <p>
 * A minimum coverage is taken in account. Indeed, an evaluation with a coverage below the given
 * minimum coverage produces the value {code Double.MAX_VALUE}.
 *
 * <p>
 * Because this class implements the {@link UnivariateFunction}, it can be used by a
 * {@link org.apache.commons.math3.optim.univariate.UnivariateOptimizer UnivariateOptimizer}.
 */
class UserBasedRecommenderEvaluationFunction extends RecommenderEvalFunction implements
    UnivariateFunction, MultivariateFunction {

  private final Map<Integer, Double> cachedResults;
  private final DataModel dataModel;
  private final DataModelBuilder dataModelBuilder;
  private final double evaluationPercentage;
  private final List<RecommenderEvaluation> evaluations;
  private final PersonnalRecommenderEvaluator evaluator;
  private final boolean reuseIsAllowed;
  private final double trainingPercentage;
  private final RecommenderType type;

  /**
   * Creates a function for the given type of user-similarity based recommender.
   */
  public UserBasedRecommenderEvaluationFunction(SelectorConfiguration configuration,
      RecommenderType type, double minimumCoverage) {
    super(minimumCoverage);

    checkNotNull(configuration);
    checkArgument(type.getFamily() == RecommenderFamily.USER_SIMILARITY_BASED);

    this.type = type;

    // extract the necessary data from the configuration
    dataModel = configuration.getDataModel();
    dataModelBuilder = configuration.getDataModelBuilder();
    evaluationPercentage = configuration.getEvaluationPercentage();
    evaluator = configuration.getEvaluator();
    reuseIsAllowed = configuration.reuseIsAllowed();
    trainingPercentage = configuration.getSpeed().getTrainingPercentage();

    cachedResults = new HashMap<>();
    evaluations = new ArrayList<>();
  }

  public Collection<RecommenderEvaluation> getEvaluations() {
    return evaluations;
  }

  @Override
  public double value(double doubleNumNeighbors) {

    int numNeighbors = (int) Math.round(doubleNumNeighbors);

    if (cachedResults.containsKey(numNeighbors)) {
      return cachedResults.get(numNeighbors);
    }

    // initialize the data for the evaluation
    UserBasedRecommenderConfiguration configuration =
        new UserBasedRecommenderConfiguration(type, numNeighbors, dataModel, reuseIsAllowed);
    UserSimilarityRecommender recommenderBuilder = new UserSimilarityRecommender(configuration);

    // run the evaluation
    double result = 0;
    try {
      result =
          evaluator.evaluate(recommenderBuilder, dataModelBuilder, dataModel, trainingPercentage,
              evaluationPercentage);
    } catch (TasteException e) {
      throw new RuntimeException(e);
    }

    // save the evaluation and prepare the result
    evaluations.add(new RecommenderEvaluation(configuration, evaluator.getEvaluationReport()));

    double coverage = evaluator.getEvaluationReport().getCoverage();
    if (coverage < getMinimumCoverage()) {
      // if the minimum coverage is not reached, the return value depends on the coverage instead
      // of the number of neighbors
      result = valueFromCoverage(coverage);
    }

    cachedResults.put(numNeighbors, result);

    return result;
  }

  @Override
  public double value(double[] point) {
    return value(point[0]);
  }
}
