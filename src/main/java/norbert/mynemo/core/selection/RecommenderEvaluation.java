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
package norbert.mynemo.core.selection;

import static com.google.common.base.Preconditions.checkArgument;
import norbert.mynemo.core.evaluation.EvaluationReport;
import norbert.mynemo.core.recommendation.configuration.RecommenderConfiguration;

/**
 * This class represents an evaluation of a recommender.
 */
public class RecommenderEvaluation {
	private final RecommenderConfiguration configuration;
	private final EvaluationReport evaluationReport;

	public RecommenderEvaluation(RecommenderConfiguration configuration,
			EvaluationReport report) {
		checkArgument(configuration != null, "The description must not be"
				+ " null.");
		checkArgument(report != null, "The report must not be null.");

		this.configuration = configuration;
		this.evaluationReport = report;
	}

	public EvaluationReport getEvaluationReport() {
		return evaluationReport;
	}

	public RecommenderConfiguration getRecommenderConfiguration() {
		return configuration;
	}

	private double removeDigits(double number) {
		return Math.round(number * 100) / 100.0;
	}

	@Override
	public String toString() {
		double maeSd = evaluationReport.getMaeStandardDeviation();
		String mae = Double.toString(removeDigits(evaluationReport.getMae()));
		if (1 < maeSd) {
			mae = mae + "(" + removeDigits(maeSd) + ")";
		}
		double rmseSd = evaluationReport.getRmseStandardDeviation();
		String rmse = Double.toString(removeDigits(evaluationReport.getRmse()));
		if (1 < maeSd) {
			rmse = rmse + "(" + removeDigits(rmseSd) + ")";
		}

		return configuration + ". Evaluation result: mae= " + mae + ", rmse="
				+ rmse + ", coverage="
				+ Math.round(evaluationReport.getCoverage() * 100)
				+ ", duration=" + evaluationReport.getDuration() + ".";
	}
}