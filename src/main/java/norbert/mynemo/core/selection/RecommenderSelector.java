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
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import norbert.mynemo.core.evaluation.MetricType;
import norbert.mynemo.core.evaluation.PersonnalRecommenderEvaluator;
import norbert.mynemo.core.evaluation.PreferenceMaskerModelBuilder;
import norbert.mynemo.core.recommendation.RecommenderFamily;
import norbert.mynemo.core.recommendation.RecommenderType;
import norbert.mynemo.core.recommendation.configuration.BasicRecommenderConfiguration;
import norbert.mynemo.core.recommendation.configuration.ItemBasedRecommenderConfiguration;
import norbert.mynemo.core.recommendation.configuration.RecommenderConfiguration;
import norbert.mynemo.core.recommendation.recommender.BasicRecommender;
import norbert.mynemo.core.recommendation.recommender.ItemSimilarityRecommender;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TTest;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.DataModelBuilder;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.model.DataModel;

import com.google.common.base.Optional;

/**
 * This class selects the best recommender for a given user.
 */
public class RecommenderSelector {
  /**
   * Configuration option regarding the speed and the precision of the selection.
   *
   * <p>
   * Expect that:
   * <ul>
   * <li><code>very_fast</code> may provide a misleading result</li>
   * <li><code>fast</code>: 2× slower, provides an imprecise result</li>
   * <li><code>normal</code>: 5× slower, provides an accurate result</li>
   * <li><code>slow</code>: 10× slower, provides an very accurate result</li>
   * <li><code>very_slow</code>: 20× slower, provides an extremely accurate result</li>
   * <li><code>extremely_slow</code>: way more slower, provides an exact and deterministic (if
   * possible) result</li>
   * </ul>
   *
   * <p>
   * The class is optimized for <code>trainingPercentage=1</code> and
   * <code>speed=EXTREMELY_SLOW</code>.
   */
  public enum SpeedOption {
    EXTREMELY_SLOW(1, true), FAST(0.5, true), NORMAL(0.8, true), SLOW(0.9, true), VERY_FAST(0.7,
        false), VERY_SLOW(0.95, true);
    private final boolean exhaustive;
    private final double trainingPercentage;

    private SpeedOption(double trainingPercentage, boolean exhaustive) {
      this.trainingPercentage = trainingPercentage;
      this.exhaustive = exhaustive;
    }

    public double getTrainingPercentage() {
      return trainingPercentage;
    }
  }

  public static final double DEFAULT_EVALUATION_PERCENTAGE = 1;
  public static final MetricType DEFAULT_METRIC = MetricType.ROOT_MEAN_SQUARED_ERROR;
  public static final double DEFAULT_MINIMUM_COVERAGE = 0.5;
  /** If false, prevent any optimization based on reusing data between evaluations. */
  private static final boolean DEFAULT_REUSE_STATE = true;
  public static final SpeedOption DEFAULT_SPEED = SpeedOption.EXTREMELY_SLOW;
  /**
   * The significance level is the maximum allowed for a p-value to consider a difference relevant.
   * If this level is lowered, more evaluation will be considered similar. On the contrary, if this
   * level is set higher, less evaluations will be considered similar.
   */
  private static final double SIGNIFICANCE_LEVEL = 0.05;

  /**
   * Removes from the given collection the evaluations with a coverage lower than the given minimum.
   */
  private static void removeUnallowedCoverage(Iterable<RecommenderEvaluation> evaluations,
      double minimumCoverage) {

    Iterator<RecommenderEvaluation> iterator = evaluations.iterator();
    while (iterator.hasNext()) {
      if (iterator.next().getEvaluationReport().getCoverage() < minimumCoverage) {
        iterator.remove();
      }
    }
  }

  private final PersonnalRecommenderEvaluator evaluator;
  private final MetricType metric;
  private final SelectorConfiguration selectorConfiguration;
  private final SvdRecommenderSelector svdRecommenderSelector;
  private final long targetUser;
  private final UserRecommenderSelector userRecommenderSelector;

  /**
   * Builds a selector for the given user.
   */
  public RecommenderSelector(DataModel model, long user) throws TasteException {
    this(model, user, DEFAULT_METRIC, DEFAULT_SPEED, DEFAULT_EVALUATION_PERCENTAGE);
  }

  /**
   * Builds a selector for the given user. The selector will try to optimize the given metric.
   */
  public RecommenderSelector(DataModel model, long user, MetricType metric, SpeedOption speed,
      double evaluationPercentage) throws TasteException {
    targetUser = user;
    this.metric = metric;
    evaluator = new PersonnalRecommenderEvaluator(targetUser, metric, speed.exhaustive);

    DataModelBuilder dataModelBuilder = null;
    if (evaluationPercentage == 1) {
      // the evaluation percentage must be 1 because the model builder will use the whole data
      // model (except some missing preferences from the target user), not the given one by the
      // evaluator
      dataModelBuilder = new PreferenceMaskerModelBuilder(model, targetUser);
    }

    boolean reuseIsAllowed =
        DEFAULT_REUSE_STATE && evaluationPercentage == 1 && speed.trainingPercentage == 1;
    selectorConfiguration =
        new SelectorConfiguration(model, user, evaluator, evaluationPercentage, reuseIsAllowed,
            speed, dataModelBuilder);
    userRecommenderSelector = new UserRecommenderSelector(selectorConfiguration);
    svdRecommenderSelector = new SvdRecommenderSelector(selectorConfiguration);
  }

  private boolean areSignificantlyDifferent(RecommenderEvaluation evalA, RecommenderEvaluation evalB) {
    DescriptiveStatistics valuesA = evalA.getEvaluationReport().getValues(DEFAULT_METRIC);
    DescriptiveStatistics valuesB = evalB.getEvaluationReport().getValues(DEFAULT_METRIC);
    return new TTest().tTest(valuesA, valuesB, SIGNIFICANCE_LEVEL);
  }

  /**
   * Runs the evaluator with the given builder, and generates an evaluation based on the given
   * configuration.
   */
  private RecommenderEvaluation evaluate(RecommenderConfiguration recommenderConfiguration,
      RecommenderBuilder recommenderBuilder) throws TasteException {

    DataModelBuilder modelBuilder = selectorConfiguration.getDataModelBuilder();

    evaluator.evaluate(recommenderBuilder, modelBuilder, selectorConfiguration.getDataModel(),
        selectorConfiguration.getSpeed().trainingPercentage,
        selectorConfiguration.getEvaluationPercentage());

    return new RecommenderEvaluation(recommenderConfiguration, evaluator.getEvaluationReport());
  }

  /**
   * Evaluates all given algorithms, and returns an unsorted list of evaluations. An algorithm may
   * be evaluated with different parameter values, thus the number of evaluations is usually greater
   * than the number of given algorithms.
   */
  public List<RecommenderEvaluation> evaluateAll(Collection<RecommenderType> recommenderTypes,
      double minimumCoverage) throws TasteException {

    List<RecommenderEvaluation> result = new ArrayList<>();

    for (RecommenderType current : recommenderTypes) {
      switch (current.getFamily()) {
        case BASIC:
          result.add(evaluateBasic(current));
          break;

        case ITEM_SIMILARITY_BASED:
          result.add(evaluateItemBased(current));
          break;

        case SVD_BASED:
          result.addAll(svdRecommenderSelector.select(current, minimumCoverage));
          break;

        case USER_SIMILARITY_BASED:
          result.addAll(userRecommenderSelector.select(current, minimumCoverage));
          break;

        default:
          throw new IllegalStateException();
      }
    }

    return result;
  }

  /**
   * Evaluates the given recommender. The given recommender must be part of the item-similarity
   * based family.
   */
  private RecommenderEvaluation evaluateBasic(RecommenderType type) throws TasteException {
    checkArgument(type.getFamily() == RecommenderFamily.BASIC);

    BasicRecommenderConfiguration configuration = new BasicRecommenderConfiguration(type);

    return evaluate(configuration, new BasicRecommender(configuration));
  }

  /**
   * Evaluates the given recommender. The recommender type must be part of the item-similarity based
   * family.
   */
  private RecommenderEvaluation evaluateItemBased(RecommenderType type) throws TasteException {
    checkArgument(type.getFamily() == RecommenderFamily.ITEM_SIMILARITY_BASED);

    ItemBasedRecommenderConfiguration configuration = new ItemBasedRecommenderConfiguration(type);

    return evaluate(configuration, new ItemSimilarityRecommender(configuration));
  }

  /**
   * Removes from the given collection all evaluations that are significantly worst. The left
   * evaluations are non significantly different.
   */
  private void retainBestEvaluations(Collection<RecommenderEvaluation> evaluations) {
    checkNotNull(evaluations);

    EvaluationComparator comparator = new EvaluationComparator(metric);
    Collection<RecommenderEvaluation> rejectedEvaluations = new ArrayList<>();

    for (RecommenderEvaluation evalA : evaluations) {
      for (RecommenderEvaluation evalB : evaluations) {
        if (areSignificantlyDifferent(evalA, evalB)) {
          rejectedEvaluations.add(Collections.max(newArrayList(evalA, evalB), comparator));
        }
      }
    }

    evaluations.removeAll(rejectedEvaluations);
  }

  /**
   * Returns the best recommender for the target user among available recommenders.
   */
  public Optional<RecommenderEvaluation> select() throws TasteException {
    return selectAmong(RecommenderType.getSpeedOrderedRecommenders(),
        RecommenderSelector.DEFAULT_MINIMUM_COVERAGE);
  }

  /**
   * Returns the best recommender for the target user among the given recommenders. The given
   * algorithms are evaluated, then the minimum evaluation computed with the
   * {@link EvaluationComparator} class is returned.
   *
   * <p>
   * The evaluations are compared with the given metric. The evaluations with a coverage lower than
   * the given one are ignored.
   */
  public Optional<RecommenderEvaluation> selectAmong(List<RecommenderType> types,
      double minimumCoverage) throws TasteException {
    checkNotNull(types);
    checkArgument(!types.isEmpty(), "The algorithm list must contain at least one algorithm.");
    checkArgument(0 <= minimumCoverage && minimumCoverage <= 1, "The minimum coverage must not be"
        + " lesser than 0 or greater than 1.");

    List<RecommenderEvaluation> evaluations = evaluateAll(types, minimumCoverage);
    removeUnallowedCoverage(evaluations, minimumCoverage);
    retainBestEvaluations(evaluations);

    Optional<RecommenderEvaluation> result;
    if (evaluations.isEmpty()) {
      result = Optional.absent();
    } else {
      result = Optional.of(Collections.min(evaluations, new EvaluationComparator(metric)));
    }

    return result;
  }
}
