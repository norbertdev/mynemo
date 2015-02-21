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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Comparator;

import norbert.mynemo.core.evaluation.MetricType;

/**
 * This comparator compares evaluations of recommenders. An evaluation A is lesser than another
 * evaluation B if its metric value is lesser than the metric value of B. The metric to compare is
 * selected via the constructor.
 */
public class EvaluationComparator implements Comparator<RecommenderEvaluation> {

  private final MetricType metric;

  public EvaluationComparator(MetricType metricToCompare) {
    checkNotNull(metricToCompare);

    this.metric = metricToCompare;
  }

  @Override
  public int compare(RecommenderEvaluation o1, RecommenderEvaluation o2) {

    double o1Value = o1.getEvaluationReport().getValue(metric);
    double o2Value = o2.getEvaluationReport().getValue(metric);

    // handle the NaN value
    if (Double.isNaN(o1Value)) {
      o1Value = Double.MAX_VALUE;
    }
    if (Double.isNaN(o2Value)) {
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
