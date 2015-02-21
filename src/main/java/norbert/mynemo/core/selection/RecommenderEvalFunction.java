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

/**
 * This function provides a convenient method to compute a value from the coverage of an evaluation.
 * This is useful when an evaluation does not reach the minimum acceptable coverage. Then, the
 * returned value must be worst than any other value that can be returned by an evaluation with an
 * acceptable coverage.
 */
abstract class RecommenderEvalFunction {

  private final double minimumCoverage;

  RecommenderEvalFunction(double minimumCoverage) {
    checkArgument(0 <= minimumCoverage && minimumCoverage <= 1, "The minimum coverage must be"
        + "between 0 and 1.");

    this.minimumCoverage = minimumCoverage;
  }

  /**
   * Returns the minimum coverage.
   */
  protected double getMinimumCoverage() {
    return minimumCoverage;
  }

  /**
   * Returns a value that depends of the coverage instead of the parameters of the evaluation
   * function (like the number of neighbors, the number of features…). The minimum returned value is
   * 64, the maximum is 512.
   *
   * <ul>
   * <li>The returned value is worst than any value an evaluation can return.
   * <li>The nearest from the minimum coverage the given coverage is, the better the returned value
   * is.
   * </ul>
   *
   * <p>
   * For example, <code>valueFromCoverage(0.5) < valueFromCoverage(0.2)</code>.
   *
   * <p>
   * The minimum is 64 because the random recommender is considered the worst. In the worst case,
   * its MAE is 50 and its RMSE is ~58, thus the minimum returned value can be 64,: it fits for both
   * metrics.
   *
   * @param coverage the coverage of an evaluation, <code>0 <= coverage <= 1</code> and
   *        <code>coverage < minimumCoverage</code> and
   */
  protected double valueFromCoverage(double coverage) {
    checkArgument(0 <= coverage && coverage <= 1 && coverage < minimumCoverage);

    // 0 < coverageDifference < 1 ; 0.1 is better than 0.9
    double coverageDifference = minimumCoverage - coverage;
    // minimum of the returned function: 2^6, i.e. 64
    return Math.pow(2, 6 + coverageDifference * 3);
  }
}
