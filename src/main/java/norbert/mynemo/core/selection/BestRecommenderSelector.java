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
import static com.google.common.collect.Lists.newArrayList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import norbert.mynemo.core.evaluation.PersonnalRecommenderEvaluator;
import norbert.mynemo.core.evaluation.PersonnalRecommenderEvaluator.MetricType;
import norbert.mynemo.core.evaluation.PreferenceMaskerModelBuilder;
import norbert.mynemo.core.recommendation.RecommenderFamily;
import norbert.mynemo.core.recommendation.RecommenderType;
import norbert.mynemo.core.recommendation.configuration.BasicRecommenderConfiguration;
import norbert.mynemo.core.recommendation.configuration.ItemBasedRecommenderConfiguration;
import norbert.mynemo.core.recommendation.configuration.RecommenderConfiguration;
import norbert.mynemo.core.recommendation.configuration.SvdBasedRecommenderConfiguration;
import norbert.mynemo.core.recommendation.configuration.UserBasedRecommenderConfiguration;
import norbert.mynemo.core.recommendation.recommender.BasicRecommender;
import norbert.mynemo.core.recommendation.recommender.ItemSimilarityRecommender;
import norbert.mynemo.core.recommendation.recommender.SvdBasedRecommender;
import norbert.mynemo.core.recommendation.recommender.UserSimilarityRecommender;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.DataModelBuilder;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.model.DataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

/**
 * This class selects the best recommender for a given user.
 */
public class BestRecommenderSelector {
	/**
	 * <p>
	 * Configuration option regarding the speed and the precision of the
	 * selection.
	 * </p>
	 * <p>
	 * Expect that:
	 * <ul>
	 * <li><code>very_fast</code> may provide a misleading result</li>
	 * <li><code>fast</code>: 2× slower, provides an imprecise result</li>
	 * <li><code>normal</code>: 5× slower, provides an accurate result</li>
	 * <li><code>slow</code>: 10× slower, provides an very accurate result</li>
	 * <li><code>very_slow</code>: 20× slower, provides an extremely accurate
	 * result</li>
	 * <li><code>extremely_slow</code>: way more slower, provides an exact
	 * result</li>
	 * </ul>
	 * </p>
	 * <p>
	 * The class is optimized for <code>trainingPercentage=1</code>.
	 * </p>
	 */
	public enum SpeedOption {
		EXTREMELY_SLOW(1, true), FAST(0.5, true), NORMAL(0.8, true), SLOW(0.9,
				true), VERY_FAST(0.7, false), VERY_SLOW(0.95, true);
		private final boolean exhaustive;
		private final double trainingPercentage;

		private SpeedOption(double trainingPercentage, boolean exhaustive) {
			this.trainingPercentage = trainingPercentage;
			this.exhaustive = exhaustive;
		}
	}

	public static final double DEFAULT_EVALUATION_PERCENTAGE = 1;
	public static final MetricType DEFAULT_METRIC = MetricType.MEAN_ABSOLUTE_ERROR;
	/**
	 * Configure the proportion of neighbor amongst the number of user. Used by
	 * the user similarity based recommender.
	 */
	private static final boolean DEFAULT_REUSE_STATE = true;
	public static final SpeedOption DEFAULT_SPEED = SpeedOption.NORMAL;
	private static final Logger LOGGER = LoggerFactory
			.getLogger(BestRecommenderSelector.class);
	private final DataModel dataModel;
	private final double evaluationPercentage;
	private final PersonnalRecommenderEvaluator evaluator;
	private final MetricType metric;
	private final SpeedOption speed;
	private final long targetUser;

	/**
	 * Builds a selector for the given user.
	 */
	public BestRecommenderSelector(DataModel model, long user)
			throws TasteException {
		this(model, user, DEFAULT_METRIC, DEFAULT_SPEED,
				DEFAULT_EVALUATION_PERCENTAGE);
	}

	/**
	 * Builds a selector for the given user. The selector will try to optimize
	 * the given metric.
	 */
	public BestRecommenderSelector(DataModel model, long user,
			MetricType metric, SpeedOption speed, double evaluationPercentage)
			throws TasteException {
		targetUser = user;
		dataModel = model;
		this.metric = metric;
		this.speed = speed;
		this.evaluationPercentage = evaluationPercentage;
		evaluator = new PersonnalRecommenderEvaluator(targetUser, metric,
				speed.exhaustive);
	}

	/**
	 * Runs the evaluator with the given builder, and generates an evaluation
	 * based on the given configuration.
	 */
	private RecommenderEvaluation evaluate(
			RecommenderConfiguration configuration,
			RecommenderBuilder recommenderBuilder) throws TasteException {

		DataModelBuilder modelBuilder = null;
		if (evaluationPercentage == 1) {
			// the evaluation percentage must be 1 because the model builder
			// will use the whole data model (except some missing
			// preferences from the target user), not the given one by the
			// evaluator
			modelBuilder = new PreferenceMaskerModelBuilder(dataModel,
					targetUser);
		}

		evaluator.evaluate(recommenderBuilder, modelBuilder, dataModel,
				speed.trainingPercentage, evaluationPercentage);

		LOGGER.info(
				"An evaluation has been performed. {}",
				new RecommenderEvaluation(configuration, evaluator
						.getEvaluationReport()));

		return new RecommenderEvaluation(configuration,
				evaluator.getEvaluationReport());
	}

	/**
	 * Evaluates all given algorithms, and returns an unsorted list of
	 * evaluations. An algorithm may be evaluated with different parameter
	 * values, thus the number of evaluations is usually greater than the number
	 * of given algorithms.
	 */
	public List<RecommenderEvaluation> evaluateAll(
			Collection<RecommenderType> recommenderTypes) throws TasteException {

		List<RecommenderEvaluation> result = new ArrayList<>();

		for (RecommenderType current : recommenderTypes) {
			switch (current.getFamily()) {
			case BASIC:
				result.addAll(evaluateBasic(current));
				break;

			case ITEM_SIMILARITY_BASED:
				result.addAll(evaluateItemBased(current));
				break;

			case SVD_BASED:
				result.addAll(evaluateSvdBased(current));
				break;

			case USER_SIMILARITY_BASED:
				result.addAll(evaluateUserBased(current));
				break;

			default:
				throw new IllegalStateException();
			}
		}

		return result;
	}

	/**
	 * Evaluates the given recommender. The given recommender must be a basic
	 * one.
	 */
	private Collection<RecommenderEvaluation> evaluateBasic(RecommenderType type)
			throws TasteException {
		checkArgument(type.getFamily() == RecommenderFamily.BASIC);

		BasicRecommenderConfiguration configuration = new BasicRecommenderConfiguration(
				type);

		return newArrayList(evaluate(configuration, new BasicRecommender(
				configuration)));
	}

	private Collection<RecommenderEvaluation> evaluateItemBased(
			RecommenderType recommenderType) throws TasteException {
		ItemBasedRecommenderConfiguration configuration = new ItemBasedRecommenderConfiguration(
				recommenderType);

		return newArrayList(evaluate(configuration,
				new ItemSimilarityRecommender(configuration)));
	}

	/**
	 * Evaluates the given recommender type for several configuration. The given
	 * recommender must be based on SVD.
	 */
	private Collection<RecommenderEvaluation> evaluateSvdBased(
			RecommenderType type) throws TasteException {
		checkArgument(type.getFamily() == RecommenderFamily.SVD_BASED);

		List<RecommenderEvaluation> result = new ArrayList<>();
		int numberOfIteration = 4;

		// try several configurations
		for (int numberOfFeatures : newArrayList(1, 3, 5, 10)) {

			SvdBasedRecommenderConfiguration configuration = new SvdBasedRecommenderConfiguration(
					type, numberOfFeatures, numberOfIteration);
			SvdBasedRecommender builder = new SvdBasedRecommender(configuration);
			result.add(evaluate(configuration, builder));
		}

		return result;
	}

	/**
	 * Evaluates the given recommender type for several configuration. The given
	 * recommender must be based on user similarity.
	 */
	private Collection<RecommenderEvaluation> evaluateUserBased(
			RecommenderType type) throws TasteException {
		checkArgument(type.getFamily() == RecommenderFamily.USER_SIMILARITY_BASED);

		List<RecommenderEvaluation> result = new ArrayList<>();
		boolean allowReuse = DEFAULT_REUSE_STATE && evaluationPercentage == 1;

		// try several configurations
		for (double neighborPercentage : Lists.newArrayList(0.01, 0.1, 1.0,
				2.0, 4.0, 8.0, 16.0, 32.0, 64.0, 90.0)) {
			// do not forget evaluationPercentage
			int numberOfUsers = Math.max(
					(int) Math.round(neighborPercentage / 100.0
							* dataModel.getNumUsers() * evaluationPercentage),
					1);

			UserBasedRecommenderConfiguration configuration = new UserBasedRecommenderConfiguration(
					type, numberOfUsers, dataModel, allowReuse);
			UserSimilarityRecommender recommenderBuilder = new UserSimilarityRecommender(
					configuration);

			result.add(evaluate(configuration, recommenderBuilder));
		}

		return result;
	}

	/**
	 * Returns the best recommender for the target user among all recommenders
	 * available.
	 */
	public Optional<RecommenderEvaluation> select() throws TasteException {
		return selectAmong(RecommenderType.getSpeedOrderedRecommenders(),
				EvaluationComparator.DEFAULT_MINIMUM_COVERAGE);
	}

	/**
	 * <p>
	 * Returns the best recommender for the target user among the given
	 * recommenders. The given algorithms are evaluated, then the minimum
	 * evaluation computed with {@link EvaluationComparator} class is returned.
	 * </p>
	 * <p>
	 * The evaluations are compared with the given metric. The evaluations with
	 * a coverage lower than the given one are considered the worst.
	 * </p>
	 */
	public Optional<RecommenderEvaluation> selectAmong(
			List<RecommenderType> types, double minimumCoverage)
			throws TasteException {
		checkArgument(types != null, "The algorithm list must not be null.");
		checkArgument(!types.isEmpty(), "The algorithm list must contain at"
				+ " least one algorithm.");
		checkArgument(0 <= minimumCoverage && minimumCoverage <= 1, "The"
				+ " minimum coverage must not be between lesser than 0 or"
				+ " greater than 1.");

		List<RecommenderEvaluation> evaluations = evaluateAll(types);

		RecommenderEvaluation bestEvaluation = Collections.min(evaluations,
				new EvaluationComparator(metric, minimumCoverage));

		if (bestEvaluation.getEvaluationReport().getCoverage() <= minimumCoverage) {
			return Optional.absent();
		}

		return Optional.of(bestEvaluation);
	}
}
