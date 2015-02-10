/*
 * Copyright 2015 Norbert
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package norbert.mynemo.core.evaluation;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

/**
 * This class represents a report produced by an evaluator. It contains the key
 * numbers from the evaluation of a recommender.
 */
public class EvaluationReport {

	private final long duration;
	private final SummaryStatistics errors;
	private final SummaryStatistics powerErrors;
	private final long predictionRequestNumber;

	public EvaluationReport(SummaryStatistics errors,
			SummaryStatistics powerErrors, long predictionRequestNumber,
			long duration) {
		this.errors = errors;
		this.powerErrors = powerErrors;
		this.predictionRequestNumber = predictionRequestNumber;
		this.duration = duration;
	}

	/**
	 * <p>
	 * Returns the ratio between the number of fulfilled prediction requests and
	 * the total number of requests. For example, if 10 predictions are
	 * requested to a recommender, and the recommender provides 8 predictions
	 * (so 2 requests are unfulfilled), then the ratio is 0.8.
	 * </p>
	 * <p>
	 * This evaluator only works with the already rated items by the target
	 * user. Thus, the coverage do not cover the entire item set, but only the
	 * items already rated by the user.
	 * </p>
	 */
	public double getCoverage() {
		return errors.getN() / (double) predictionRequestNumber;
	}

	/**
	 * Returns the duration of the evaluation in second.
	 */
	public long getDuration() {
		return duration;
	}

	/**
	 * Returns the mean absolute error of the evaluation. The returned value can
	 * be NaN.
	 */
	public double getMae() {
		return errors.getMean();
	}

	/**
	 * Returns the standard deviation of the mean absolute error of the
	 * evaluation. The returned value can be NaN.
	 */
	public double getMaeStandardDeviation() {
		return errors.getStandardDeviation();
	}

	/**
	 * Returns the root mean squared error of the evaluation. The returned value
	 * can be NaN.
	 */
	public double getRmse() {
		return powerErrors.getMean();
	}

	/**
	 * Returns the standard deviation of the root mean squared error of the
	 * evaluation. The returned value can be NaN.
	 */
	public double getRmseStandardDeviation() {
		return powerErrors.getStandardDeviation();
	}

}
