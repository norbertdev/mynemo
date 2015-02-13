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
package norbert.mynemo.core.recommendation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

public enum RecommenderType {
  // basic
  ITEM_AVERAGE(RecommenderFamily.BASIC),
  // based on item similarity
  ITEM_SIMILARITY_WITH_CITY_BLOCK_DISTANCE(RecommenderFamily.ITEM_SIMILARITY_BASED), ITEM_SIMILARITY_WITH_EUCLIDEAN_DISTANCE(
      RecommenderFamily.ITEM_SIMILARITY_BASED), ITEM_SIMILARITY_WITH_LOG_LIKELIHOOD(
      RecommenderFamily.ITEM_SIMILARITY_BASED), ITEM_SIMILARITY_WITH_PEARSON_CORRELATION(
      RecommenderFamily.ITEM_SIMILARITY_BASED), ITEM_SIMILARITY_WITH_TANIMOTO_COEFFICIENT(
      RecommenderFamily.ITEM_SIMILARITY_BASED), ITEM_SIMILARITY_WITH_UNCENTERED_COSINE(
      RecommenderFamily.ITEM_SIMILARITY_BASED), ITEM_SIMILARITY_WITH_WEIGHTED_EUCLIDEAN_DISTANCE(
      RecommenderFamily.ITEM_SIMILARITY_BASED), ITEM_SIMILARITY_WITH_WEIGHTED_PEARSON_CORRELATION(
      RecommenderFamily.ITEM_SIMILARITY_BASED), ITEM_SIMILARITY_WITH_WEIGHTED_UNCENTERED_COSINE(
      RecommenderFamily.ITEM_SIMILARITY_BASED),
  // basic, usually the worst
  RANDOM(RecommenderFamily.BASIC),
  // based on SVD
  SVD_WITH_ALSWR_FACTORIZER(RecommenderFamily.SVD_BASED), SVD_WITH_PARALLEL_SGD_FACTORIZER(
      RecommenderFamily.SVD_BASED), SVD_WITH_RATING_SGD_FACTORIZER(RecommenderFamily.SVD_BASED), SVD_WITH_SVDPLUSPLUS_FACTORIZER(
      RecommenderFamily.SVD_BASED),
  // basic
  USER_AVERAGE(RecommenderFamily.BASIC),
  // based on user similarity
  USER_SIMILARITY_WITH_CITY_BLOCK_DISTANCE(RecommenderFamily.USER_SIMILARITY_BASED), USER_SIMILARITY_WITH_EUCLIDEAN_DISTANCE(
      RecommenderFamily.USER_SIMILARITY_BASED), USER_SIMILARITY_WITH_LOG_LIKELIHOOD(
      RecommenderFamily.USER_SIMILARITY_BASED), USER_SIMILARITY_WITH_PEARSON_CORRELATION(
      RecommenderFamily.USER_SIMILARITY_BASED), USER_SIMILARITY_WITH_SPEARMAN_CORRELATION(
      RecommenderFamily.USER_SIMILARITY_BASED), USER_SIMILARITY_WITH_TANIMOTO_COEFFICIENT(
      RecommenderFamily.USER_SIMILARITY_BASED), USER_SIMILARITY_WITH_UNCENTERED_COSINE(
      RecommenderFamily.USER_SIMILARITY_BASED), USER_SIMILARITY_WITH_WEIGHTED_EUCLIDEAN_DISTANCE(
      RecommenderFamily.USER_SIMILARITY_BASED), USER_SIMILARITY_WITH_WEIGHTED_PEARSON_CORRELATION(
      RecommenderFamily.USER_SIMILARITY_BASED), USER_SIMILARITY_WITH_WEIGHTED_UNCENTERED_COSINE(
      RecommenderFamily.USER_SIMILARITY_BASED);

  /**
   * Returns available algorithm that are classified in the given family.
   */
  public static List<RecommenderType> getByFamily(RecommenderFamily family) {
    checkArgument(family != null, "The family must not be null.");
    List<RecommenderType> result = new ArrayList<>();
    for (RecommenderType current : getWorkingRecommenders()) {
      if (current.family == family) {
        result.add(current);
      }
    }
    return result;
  }

  /**
   * Returns the algorithms that are usually faster than those not returned.
   */
  public static List<RecommenderType> getFastRecommenders() {
    List<RecommenderType> result = new ArrayList<>();
    result.addAll(getByFamily(RecommenderFamily.ITEM_SIMILARITY_BASED));
    result.addAll(getByFamily(RecommenderFamily.USER_SIMILARITY_BASED));
    return result;
  }

  /**
   * Returns the available algorithms that currently don't work as expected.
   */
  private static List<RecommenderType> getNotWorkingRecommenders() {
    return newArrayList(SVD_WITH_SVDPLUSPLUS_FACTORIZER);
  }

  /**
   * Returns the working recommenders that are roughly ordered by execution time. The faster
   * recommenders are first.
   */
  public static List<RecommenderType> getSpeedOrderedRecommenders() {
    List<RecommenderType> result = getWorkingRecommenders();

    putItLast(result, ITEM_AVERAGE);
    putItLast(result, USER_AVERAGE);
    putItLast(result, SVD_WITH_PARALLEL_SGD_FACTORIZER);
    putItLast(result, USER_SIMILARITY_WITH_SPEARMAN_CORRELATION);
    putItLast(result, SVD_WITH_RATING_SGD_FACTORIZER);
    putItLast(result, SVD_WITH_ALSWR_FACTORIZER);

    return result;
  }

  /**
   * Returns the recommenders that provide result. Some recommender do not work, by looping forever
   * or by systematically throwing an exception.
   */
  public static List<RecommenderType> getWorkingRecommenders() {
    List<RecommenderType> result = Lists.newArrayList(RecommenderType.values());
    result.removeAll(getNotWorkingRecommenders());
    return result;
  }

  /**
   * Move the given recommender to the end of the list.
   *
   * @param list a list
   * @param element an element of the list to move to the end
   */
  private static void putItLast(List<RecommenderType> list, RecommenderType element) {
    list.remove(element);
    list.add(element);
  }

  private final RecommenderFamily family;

  private RecommenderType(RecommenderFamily family) {
    this.family = family;
  }

  public RecommenderFamily getFamily() {
    return family;
  }
}
