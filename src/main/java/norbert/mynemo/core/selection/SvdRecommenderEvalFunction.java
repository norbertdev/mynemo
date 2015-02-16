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

import java.util.ArrayList;
import java.util.Collection;

import norbert.mynemo.core.evaluation.PersonnalRecommenderEvaluator;
import norbert.mynemo.core.recommendation.RecommenderType;
import norbert.mynemo.core.recommendation.configuration.SvdBasedRecommenderConfiguration;
import norbert.mynemo.core.recommendation.recommender.SvdBasedRecommender;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.DataModelBuilder;
import org.apache.mahout.cf.taste.model.DataModel;

/**
 * This function renders optimizable the number of features and the number of iterations for a SVD
 * based recommender. Indeed, the {@link #value(double[])} method takes the two numbers as
 * parameter, and returns the value of a metric produced by an evaluator for these numbers. A list
 * of all evaluations performed are kept, and can be accessed via the {@link #getEvaluations()}
 * method.
 *
 * <p>
 * A minimum coverage is taken in account. Indeed, an evaluation with a coverage below the given
 * minimum coverage produces the value {code Double.MAX_VALUE}.
 * </p>
 *
 * <p>
 * Because this class implements the {@link MultivariateFunction}, it can be used by a
 * {@link org.apache.commons.math3.optim.nonlinear.scalar.MultivariateOptimizer
 * MultivariateOptimizer}.
 * </p>
 */
class SvdRecommenderEvalFunction implements MultivariateFunction {

  private final DataModel dataModel;
  private final DataModelBuilder dataModelBuilder;
  private final double evaluationPercentage;
  private final Collection<RecommenderEvaluation> evaluations;
  private final PersonnalRecommenderEvaluator evaluator;
  private final double minimumCoverage;
  private final boolean reuseIsAllowed;
  private final double trainingPercentage;
  private final RecommenderType type;

  /**
   * Creates a function for the given type of SVD based recommender.
   */
  public SvdRecommenderEvalFunction(SelectorConfiguration configuration, RecommenderType type,
      double minimumCoverage) {

    this.type = type;
    this.minimumCoverage = minimumCoverage;

    // extract the necessary data from the configuration
    dataModel = configuration.getDataModel();
    dataModelBuilder = configuration.getDataModelBuilder();
    evaluationPercentage = configuration.getEvaluationPercentage();
    evaluator = configuration.getEvaluator();
    reuseIsAllowed = configuration.reuseIsAllowed();
    trainingPercentage = configuration.getSpeed().getTrainingPercentage();

    evaluations = new ArrayList<>();
  }

  /**
   * Returns all evaluations perfomed.
   */
  public Collection<RecommenderEvaluation> getEvaluations() {
    return evaluations;
  }

  @Override
  public double value(double[] point) {
    // initialize the data for the evaluation
    int numFeatures = (int) Math.round(point[0]);
    int numIterations = (int) Math.round(point[1]);
    SvdBasedRecommenderConfiguration configuration =
        new SvdBasedRecommenderConfiguration(type, numFeatures, numIterations, dataModel,
            reuseIsAllowed);
    SvdBasedRecommender recommenderBuilder = new SvdBasedRecommender(configuration);

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

    if (evaluator.getEvaluationReport().getCoverage() < minimumCoverage) {
      // if the minimum coverage is not reached, the worst value is returned
      result = Double.MAX_VALUE;
    }

    return result;
  }
}
