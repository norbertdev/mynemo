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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.mahout.cf.taste.common.NoSuchItemException;
import org.apache.mahout.cf.taste.common.NoSuchUserException;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.DataModelBuilder;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.eval.RecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.model.GenericDataModel;
import org.apache.mahout.cf.taste.impl.model.GenericPreference;
import org.apache.mahout.cf.taste.impl.model.GenericUserPreferenceArray;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.common.RandomUtils;

import com.google.common.collect.Lists;

/**
 * This class provides an evaluation of a recommender in the point of view of
 * only one user.
 */
public class PersonnalRecommenderEvaluator implements RecommenderEvaluator {
	public enum MetricType {
		MEAN_ABSOLUTE_ERROR, ROOT_MEAN_SQUARED_ERROR
	}

	private static float capEstimatedPreference(float estimate,
			DataModel dataModel) {
		if (dataModel.getMaxPreference() < estimate) {
			return dataModel.getMaxPreference();
		}
		if (estimate < dataModel.getMinPreference()) {
			return dataModel.getMinPreference();
		}
		return estimate;
	}

	/**
	 * Copies the preferences of the given user, from the given data model to
	 * the given map.
	 *
	 * @param dataModel
	 *            source of the preferences
	 * @param preferences
	 *            destination of the preferences
	 * @param user
	 *            user id
	 * @throws TasteException
	 */
	private static void copyUserPreferences(DataModel dataModel,
			FastByIDMap<PreferenceArray> preferences, long user)
			throws TasteException {
		List<Preference> oneUserTrainingPrefs = null;

		for (Preference preference : dataModel.getPreferencesFromUser(user)) {
			Preference newPref = new GenericPreference(user,
					preference.getItemID(), preference.getValue());
			if (oneUserTrainingPrefs == null) {
				oneUserTrainingPrefs = Lists.newArrayListWithCapacity(3);
			}
			oneUserTrainingPrefs.add(newPref);

		}

		preferences.put(user, new GenericUserPreferenceArray(
				oneUserTrainingPrefs));
	}

	/**
	 * Returns the number of test sets necessary for the given training
	 * percentage.
	 */
	private static int numberOfTestSets(double trainingPercentage,
			int numberOfPreferences) {
		int result;

		if (trainingPercentage == 1) {
			// avoid division by zero
			result = numberOfPreferences;
		} else {
			result = (int) (1 / (1 - trainingPercentage));
			// cap by the number of preferences
			if (numberOfPreferences < result) {
				result = numberOfPreferences;
			}
		}

		return result;
	}

	/** Duration of the last evaluation. */
	private long duration;
	/** Accumulated error. */
	private final SummaryStatistics errorStats;
	private final boolean exhaustive;
	private final MetricType metric;
	/** Accumulated error * error. */
	private final SummaryStatistics powerErrorStats;
	private long predictionRequestNumber;
	private final Random random;

	private final long targetUser;

	/**
	 * <p>
	 * Creates an evaluator for the given user.
	 * </p>
	 * <p>
	 * An exhaustive evaluation is based on all preferences. An exhaustive
	 * evaluation results in a lower speed and a better accuracy.
	 * </p>
	 *
	 * @param targetUser
	 *            the user to evaluate.
	 * @param metric
	 *            type of metric to return
	 * @param exhaustive
	 *            return a metric based on all preferences
	 */
	public PersonnalRecommenderEvaluator(long targetUser, MetricType metric,
			boolean exhaustive) {
		random = RandomUtils.getRandom();
		this.targetUser = targetUser;
		this.metric = metric;
		errorStats = new SummaryStatistics();
		powerErrorStats = new SummaryStatistics();
		this.exhaustive = exhaustive;
	}

	/**
	 * Copies the preferences of the target user to the given preference map,
	 * except the given ones.
	 */
	private void addUserPreferences(DataModel model,
			FastByIDMap<PreferenceArray> preferences,
			Collection<Preference> preferencesToExclude) throws TasteException {
		PreferenceArray targetUserPreferences = model
				.getPreferencesFromUser(targetUser);
		List<Preference> preferencesToAdd = new ArrayList<>(
				targetUserPreferences.length() - preferencesToExclude.size());

		// hash set to speed the method
		Set<Long> itemToExclude = new HashSet<>(preferencesToExclude.size());
		for (Preference preference : preferencesToExclude) {
			itemToExclude.add(preference.getItemID());
		}

		for (Preference currentPreference : targetUserPreferences) {
			// the equals method of Preference do not behave as expected, thus
			// preferencesToExclude.contains(currentPreference) can't be used
			if (!itemToExclude.contains(currentPreference.getItemID())) {
				preferencesToAdd.add(currentPreference);
			}
		}

		preferences.put(targetUser, new GenericUserPreferenceArray(
				preferencesToAdd));
	}

	/**
	 * <p>
	 * Build the training preferences with a given percentage of user. The
	 * preferences are extracted from the given data model. The returned map
	 * does not include the target user.
	 * </p>
	 * <p>
	 * The given percentage must be between 0 and 1.
	 * </p>
	 */
	private FastByIDMap<PreferenceArray> buildBaseTrainingPreferences(
			DataModel model, double evaluationPercentage) throws TasteException {
		FastByIDMap<PreferenceArray> result = new FastByIDMap<PreferenceArray>(
				model.getNumUsers() - 1);

		LongPrimitiveIterator it = model.getUserIDs();
		while (it.hasNext()) {
			long userId = it.nextLong();
			double nextDouble = random.nextDouble();
			if (nextDouble < evaluationPercentage && userId != targetUser) {
				copyUserPreferences(model, result, userId);
			}
		}

		return result;
	}

	private List<List<Preference>> buildNonExhaustiveTestSet(
			PreferenceArray targetUserPreferences, double trainingPercentage) {

		List<Preference> testSet = new ArrayList<>();

		for (Preference currentPreference : targetUserPreferences) {
			if (trainingPercentage < random.nextDouble()) {
				testSet.add(currentPreference);
			}
		}

		List<List<Preference>> result = new ArrayList<>(1);
		result.add(testSet);
		return result;
	}

	/**
	 * <p>
	 * Builds all test sets. The test sets are a random partition of the
	 * preferences of the target user. The number of partition is derived from
	 * the given percentage.
	 * </p>
	 * <p>
	 * If the evaluation is not exhaustive or if the given percentage is less
	 * than 0.5, then only one set is build, that contains the given percentage
	 * of preference.
	 * </p>
	 */
	private List<List<Preference>> buildTestSets(DataModel dataModel,
			double trainingPercentage) throws TasteException {

		PreferenceArray targetUserPreferences = dataModel
				.getPreferencesFromUser(targetUser);

		if (trainingPercentage < 0.5 || !exhaustive) {
			// create only one set, fill it, exit
			return buildNonExhaustiveTestSet(targetUserPreferences,
					trainingPercentage);
		}

		int numberOfPreferences = dataModel.getPreferencesFromUser(targetUser)
				.length();
		int numberOfSets = numberOfTestSets(trainingPercentage,
				numberOfPreferences);

		// Initialize the lists
		List<List<Preference>> result = new ArrayList<>(numberOfSets);
		for (int index = 0; index < numberOfSets; index++) {
			result.add(index, new ArrayList<Preference>());
		}
		if (numberOfSets == numberOfPreferences) {
			// fill each set with one preference. This ensures that each set
			// will contain one preference.
			for (int index = 0; index < result.size(); index++) {
				result.get(index).add(targetUserPreferences.get(index));
			}
		} else {
			// fill the sets randomly
			for (Preference currentPreference : dataModel
					.getPreferencesFromUser(targetUser)) {
				result.get((int) Math.floor(random.nextDouble() * numberOfSets))
						.add(currentPreference);
			}
		}

		return result;
	}

	/**
	 * <p>
	 * Evaluates the given preferences according to the given recommender using
	 * the given training model.
	 * </p>
	 * <p>
	 * The resulting errors are added to {@link #errorStats} and
	 * {@link #powerErrorStats}.
	 * </p>
	 */
	private void evaluate(DataModel trainingModel, Recommender recommender,
			List<Preference> preferencesToEvaluate) throws TasteException {
		for (Preference preference : preferencesToEvaluate) {
			float error = getError(trainingModel, recommender, preference);
			if (!Float.isNaN(error)) {
				errorStats.addValue(error);
				powerErrorStats.addValue(error * error);
			}
		}
	}

	/**
	 * <p>
	 * If the exhaustive evaluation is off, the training percentage behave
	 * normally. If the exhaustive evaluation is on, then the training
	 * percentage have two different behaviors:
	 * <ul>
	 * <li>percentage < 0.5: percentage behave normally, the evaluation is not
	 * exhaustive.</li>
	 * <li>
	 * </p>
	 * <p>
	 * 0.5 <= percentage: the preferences of the target user are split in
	 * several test sets. Each set contains approximately the same number of
	 * user preferences. For example, if <code>percentage=1</code>, then the
	 * number of set equals the number of preference of the target user, each
	 * set containing one preference. If <code>percentage=0.5</code>, then two
	 * sets are built, each set contains half of the preferences. If
	 * <code>percentage=0.9</code>, then ten sets are built, each set contains
	 * one tenth of the preferences. Then, each set is tested as if the
	 * evaluation is not exhaustive.
	 * </p>
	 * <p>
	 * Thus, <code>1</code> provides the most precise result, but the
	 * computation may be intensive.
	 * </p>
	 * </li> </ul>
	 */
	@Override
	public double evaluate(RecommenderBuilder recommenderBuilder,
			DataModelBuilder dataModelBuilder, DataModel dataModel,
			double trainingPercentage, double evaluationPercentage)
			throws TasteException {

		Timer timer = Timer.createStartedTimer();

		// clear the previously computed errors
		errorStats.clear();
		powerErrorStats.clear();
		predictionRequestNumber = 0;

		// all training preferences except the target user's one
		FastByIDMap<PreferenceArray> baseTrainingPreferences = buildBaseTrainingPreferences(
				dataModel, evaluationPercentage);

		List<List<Preference>> testSets = buildTestSets(dataModel,
				trainingPercentage);

		// the idea is to generate a recommendation for each preference of the
		// target user.
		for (List<Preference> currentTestSet : testSets) {
			// add the preferences of the target user
			FastByIDMap<PreferenceArray> currentTrainingPreferences = baseTrainingPreferences
					.clone();
			addUserPreferences(dataModel, currentTrainingPreferences,
					currentTestSet);

			DataModel currentTrainingModel = (dataModelBuilder == null) ? new GenericDataModel(
					currentTrainingPreferences) : dataModelBuilder
					.buildDataModel(currentTrainingPreferences);

			Recommender currentRecommender = recommenderBuilder
					.buildRecommender(currentTrainingModel);

			evaluate(currentTrainingModel, currentRecommender, currentTestSet);
		}

		duration = timer.stop().getDuration();

		return getEvaluationSummary(metric);
	}

	/**
	 * <p>
	 * Returns the absolute difference between the given preference and the
	 * estimated one.
	 * </p>
	 * <p>
	 * Depending on the recommender, the returned float may be NaN.
	 * </p>
	 */
	private float getError(DataModel trainingModel, Recommender recommender,
			Preference preference) throws TasteException {
		float estimation = Float.NaN;
		predictionRequestNumber++;
		try {
			// may return Float.NaN
			estimation = capEstimatedPreference(
					recommender.estimatePreference(targetUser,
							preference.getItemID()), trainingModel);
		} catch (NoSuchUserException | NoSuchItemException exception) {
			// It's possible that an item exists in the test data but not
			// training data in which case NSEE or NSIE will be thrown.
		}
		return Math.abs(estimation - preference.getValue());
	}

	/**
	 * Returns a report on the last evaluation.
	 */
	public EvaluationReport getEvaluationReport() {
		return new EvaluationReport(new SummaryStatistics(errorStats),
				new SummaryStatistics(powerErrorStats),
				predictionRequestNumber, duration);
	}

	/**
	 * Returns the MAE or the RMSE, depending of the choosen metric.
	 *
	 * @return the mean of errors
	 */
	private double getEvaluationSummary(MetricType metric) {
		double summary;

		switch (metric) {
		case MEAN_ABSOLUTE_ERROR:
			summary = errorStats.getMean();
			break;

		case ROOT_MEAN_SQUARED_ERROR:
			summary = Math.sqrt(powerErrorStats.getMean());
			break;

		default:
			throw new IllegalStateException();
		}

		return summary;
	}

	@Override
	public float getMaxPreference() {
		throw new UnsupportedOperationException();
	}

	@Override
	public float getMinPreference() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMaxPreference(float maxPreference) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMinPreference(float minPreference) {
		throw new UnsupportedOperationException();
	}
}
