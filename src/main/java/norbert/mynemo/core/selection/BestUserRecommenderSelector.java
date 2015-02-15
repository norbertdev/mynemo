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

import java.util.Collection;

import norbert.mynemo.core.recommendation.RecommenderFamily;
import norbert.mynemo.core.recommendation.RecommenderType;

import org.apache.commons.math3.optim.ConvergenceChecker;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;
import org.apache.commons.math3.optim.univariate.UnivariatePointValuePair;
import org.apache.mahout.cf.taste.common.TasteException;

/**
 * This selector performs evaluations on a user-similarity based recommender for a user.
 */
class BestUserRecommenderSelector {
  private static final double ABS_FOR_STOPPING_CRITERION = 0.01;
  private static final int MAX_EVALUATIONS = 1024;
  /**
   * Maximum iterations allowed. the number of evaluations is ahead by 2 from the number of
   * iterations in the Brent optimizer.
   */
  private static final int MAX_ITERATIONS = MAX_EVALUATIONS - 2;
  private static final double REL_FOR_STOPPING_CRITERION = 0.001;

  private final SelectorConfiguration configuration;
  private final int maxNeighbors;

  public BestUserRecommenderSelector(SelectorConfiguration configuration) throws TasteException {
    this.configuration = configuration;
    maxNeighbors =
        (int) (configuration.getDataModel().getNumUsers() * configuration.getEvaluationPercentage() * configuration
            .getSpeed().getTrainingPercentage());
  }

  /**
   * Evaluates the given recommender type for several configurations. The number of neighbors for
   * each evaluation is chosen via an optimizer. The number of evaluation performed depends of the
   * number of steps necessary for the evaluation results to converge. The number of evaluations
   * performed is also bounded by an internal maximum.
   *
   * <p>
   * The given recommender type must be part of the user similarity based family.
   * </p>
   *
   * @return all evaluations done during the selection process
   */
  public Collection<RecommenderEvaluation> select(RecommenderType type, double minimumCoverage) {
    checkArgument(type.getFamily() == RecommenderFamily.USER_SIMILARITY_BASED);

    // initialize necessary optimization data
    ConvergenceChecker<UnivariatePointValuePair> checker = new MaxIterationChecker(MAX_ITERATIONS);
    UserBasedRecommenderEvaluationFunction function =
        new UserBasedRecommenderEvaluationFunction(configuration, type, minimumCoverage);
    UnivariateObjectiveFunction objectiveFunction = new UnivariateObjectiveFunction(function);
    BrentOptimizer optimizer =
        new BrentOptimizer(REL_FOR_STOPPING_CRITERION, ABS_FOR_STOPPING_CRITERION, checker);
    MaxIter maxIter = new MaxIter(MAX_ITERATIONS);
    MaxEval maxEval = new MaxEval(MAX_EVALUATIONS);
    SearchInterval searchInterval = new SearchInterval(1, maxNeighbors);
    GoalType goalType = GoalType.MINIMIZE;

    // run the optimizer
    optimizer.optimize(maxIter, maxEval, objectiveFunction, searchInterval, goalType);

    return function.getEvaluations();
  }
}
