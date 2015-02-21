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
 * This selector performs evaluations on a SVD based recommender for a user. Several numbers of
 * features and iterations are tested.
 */
class SvdRecommenderSelector {
  private static final int CMAES_MAX_ITERATIONS = 40;
  private static final int CMAES_SIGMA_FEATURE = 350;
  private static final int CMAES_SIGMA_ITERATION = 1;
  private static final int INITIAL_GUESS_FEATURES = 400;
  private static final int INITIAL_GUESS_ITERATIONS = 2;
  private static final int MAX_SVD_FEATURES = 800;
  private static final int MAX_SVD_ITERATIONS = 3;
  private static final int MIN_SVD_FEATURES = 50;
  private static final int MIN_SVD_ITERATIONS = 1;

  private final SelectorConfiguration configuration;

  public SvdRecommenderSelector(SelectorConfiguration configuration) throws TasteException {
    this.configuration = configuration;
  }

  /**
   * Evaluates the given recommender type for several configurations. The number of iterations and
   * the number of features for each evaluation is chosen via an optimizer. The number of evaluation
   * performed depends of the number of steps necessary for the evaluation results to converge. The
   * number of evaluations performed is also bounded by an internal maximum.
   *
   * <p>
   * The given recommender type must be part of the SVD based family.
   *
   * @return all evaluations done during the selection process
   */
  public Collection<RecommenderEvaluation> select(RecommenderType type, double minimumCoverage) {
    checkArgument(type.getFamily() == RecommenderFamily.SVD_BASED);

    // initialize necessary optimization data
    ConvergenceChecker<PointValuePair> checker =
        new MaxIterationChecker<PointValuePair>(CMAES_MAX_ITERATIONS);
    SvdRecommenderEvalFunction function =
        new SvdRecommenderEvalFunction(configuration, type, minimumCoverage);
    ObjectiveFunction objectiveFunction = new ObjectiveFunction(function);
    CMAESOptimizer optimizer =
        new CMAESOptimizer(CMAES_MAX_ITERATIONS, 1.0, true, 2, 0, new JDKRandomGenerator(), false,
            checker);

    MaxEval maxEval = MaxEval.unlimited();
    GoalType goalType = GoalType.MINIMIZE;
    SimpleBounds bounds =
        new SimpleBounds(new double[] {MIN_SVD_FEATURES, MIN_SVD_ITERATIONS}, new double[] {
            MAX_SVD_FEATURES, MAX_SVD_ITERATIONS});
    InitialGuess initialGuess =
        new InitialGuess(new double[] {INITIAL_GUESS_FEATURES, INITIAL_GUESS_ITERATIONS});
    CMAESOptimizer.PopulationSize populationSize = new CMAESOptimizer.PopulationSize(16);
    CMAESOptimizer.Sigma sigma =
        new CMAESOptimizer.Sigma(new double[] {CMAES_SIGMA_FEATURE, CMAES_SIGMA_ITERATION});

    // run the optimizer
    optimizer.optimize(objectiveFunction, goalType, initialGuess, populationSize, sigma, bounds,
        maxEval);

    return function.getEvaluations();
  }
}
