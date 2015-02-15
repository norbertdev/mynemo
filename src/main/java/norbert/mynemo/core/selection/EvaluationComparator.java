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

import java.util.Comparator;

import norbert.mynemo.core.evaluation.PersonnalRecommenderEvaluator.MetricType;

/**
 * This class provides a comparison between recommender evaluations. An evaluation A is lesser than
 * another evaluation B if its metric value is lesser than the metric value of B. Evaluations with a
 * coverage less or equal to a given number are greater than those with an acceptable coverage. Two
 * evaluations with a coverage less or equal to a given number are equals.
 *
 * <p>
 * The metric to compare is selected via the constructor.
 * </p>
 */
public class EvaluationComparator implements Comparator<RecommenderEvaluation> {

  public static final MetricType DEFAULT_METRIC = MetricType.ROOT_MEAN_SQUARED_ERROR;
  public static final double DEFAULT_MINIMUM_COVERAGE = 0.05;

  private final MetricType metricToCompare;
  private final double minimumCoverage;

  public EvaluationComparator(MetricType metricToCompare, double minimumCoverage) {
    this.metricToCompare = metricToCompare;
    this.minimumCoverage = minimumCoverage;
  }

  @Override
  public int compare(RecommenderEvaluation o1, RecommenderEvaluation o2) {

    double o1Value;
    double o2Value;

    switch (metricToCompare) {
      case MEAN_ABSOLUTE_ERROR:
        o1Value = o1.getEvaluationReport().getMae();
        o2Value = o2.getEvaluationReport().getMae();
        break;

      case ROOT_MEAN_SQUARED_ERROR:
        o1Value = o1.getEvaluationReport().getRmse();
        o2Value = o2.getEvaluationReport().getRmse();
        break;

      default:
        throw new IllegalStateException();
    }

    if (Double.isNaN(o1Value) || o1.getEvaluationReport().getCoverage() <= minimumCoverage) {
      o1Value = Double.MAX_VALUE;
    }
    if (Double.isNaN(o2Value) || o2.getEvaluationReport().getCoverage() <= minimumCoverage) {
      o2Value = Double.MAX_VALUE;
    }
    double difference = o1Value - o2Value;

    if (difference < 0) {
      return -1;
    } else if (difference == 0) {
      return 0;
    } else {
      return 1;
    }
  }
}
