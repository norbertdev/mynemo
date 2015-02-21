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
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.mahout.cf.taste.common.TasteException;

/**
 * This selector performs evaluations on a user-similarity based recommender for a user.
 */
class UserRecommenderSelector {

  private final SelectorConfiguration configuration;
  private final int maxNeighbors;

  public UserRecommenderSelector(SelectorConfiguration configuration) throws TasteException {
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
   *
   * @return all evaluations done during the selection process
   */
  public Collection<RecommenderEvaluation> select(RecommenderType type, double minimumCoverage) {
    checkArgument(type.getFamily() == RecommenderFamily.USER_SIMILARITY_BASED);
    checkArgument(0 <= minimumCoverage && minimumCoverage <= 1, "The minimum coverage must be"
        + "between 0 and 1.");

    final UserBasedRecommenderEvaluationFunction function =
        new UserBasedRecommenderEvaluationFunction(configuration, type, minimumCoverage);

    // find a good initial guess: test 10%, 20%… 90% of maximum neighbors
    double secondBestInitialValue = 1;
    double bestInitialValue = 0;
    double bestEvalResult = Integer.MAX_VALUE;
    for (double factor = 0.1; factor <= 0.9; factor += 0.1) {
      double currentNumNeighbors = maxNeighbors * factor;
      double evalResult = function.value(currentNumNeighbors);
      if (evalResult < bestEvalResult) {
        bestEvalResult = evalResult;
        secondBestInitialValue = bestInitialValue;
        bestInitialValue = currentNumNeighbors;
      }
    }

    // initialize the quality parameters of the CMAES optimizer
    final double minNeighbors = 1;
    final double initialGuessNeighbors = bestInitialValue;
    final double sigmaNeighbors =
        Math.min(Math.abs(bestInitialValue - secondBestInitialValue),
            Math.min(maxNeighbors - bestInitialValue, bestInitialValue - 1));
    // not sure about that
    final int populationSize = (int) (Math.log10(maxNeighbors) / Math.log10(2));
    // inhibit the exception throw if the maximum number of evaluation is reached
    final int maxCmaesEvaluations = Integer.MAX_VALUE;
    // not sure about that
    final int maxCmaesIterations = (int) (Math.log10(maxNeighbors) / Math.log10(2));

    // initialize the other parameters
    final ConvergenceChecker<PointValuePair> checker = new MaxIterationChecker<PointValuePair>(20);
    final ObjectiveFunction objectiveFunction = new ObjectiveFunction(function);
    final CMAESOptimizer optimizer =
        new CMAESOptimizer(maxCmaesIterations, 1.0, true, 0, 0, new JDKRandomGenerator(), false,
            checker);

    final MaxEval maxEval = new MaxEval(maxCmaesEvaluations);
    final GoalType goalType = GoalType.MINIMIZE;
    final SimpleBounds bounds =
        new SimpleBounds(new double[] {minNeighbors, minNeighbors}, new double[] {maxNeighbors,
            maxNeighbors});
    final InitialGuess initialGuess =
        new InitialGuess(new double[] {initialGuessNeighbors, initialGuessNeighbors});
    final CMAESOptimizer.PopulationSize popSize =
        new CMAESOptimizer.PopulationSize((populationSize));
    final CMAESOptimizer.Sigma sigma =
        new CMAESOptimizer.Sigma(new double[] {sigmaNeighbors, sigmaNeighbors});

    // run the optimizer
    optimizer.optimize(objectiveFunction, goalType, initialGuess, popSize, sigma, bounds, maxEval);

    return function.getEvaluations();
  }
}
