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
package norbert.mynemo.core.recommendation.similarity;

import java.util.Collection;

import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.apache.mahout.cf.taste.common.Refreshable;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.FastIDSet;
import org.apache.mahout.cf.taste.impl.common.RefreshHelper;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.similarity.PreferenceInferrer;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

import com.google.common.base.Preconditions;

/**
 * This similarity implements the Spearman correlation. Identical rating values are assigned a rank
 * equal to the average of their positions.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Spearman%27s_rank_correlation_coefficient">
 *      Spearman's rank correlation coefficient (Wikipedia)</a>
 */
public class OriginalSpearmanCorrelationSimilarity implements UserSimilarity {

  private static final SpearmansCorrelation SPEARMAN_CORRELATION = new SpearmansCorrelation(
      new NaturalRanking());

  private final DataModel dataModel;

  public OriginalSpearmanCorrelationSimilarity(final DataModel dataModel) {
    this.dataModel = Preconditions.checkNotNull(dataModel);
  }

  @Override
  public void setPreferenceInferrer(final PreferenceInferrer inferrer) {
    throw new UnsupportedOperationException();
  }

  @Override
  public double userSimilarity(final long userId1, final long userId2) throws TasteException {

    final PreferenceArray xPrefs = dataModel.getPreferencesFromUser(userId1).clone();
    final PreferenceArray yPrefs = dataModel.getPreferencesFromUser(userId2).clone();

    final int numCommonPref = numCommonPreferences(userId1, userId2);
    if (numCommonPref < 2) {
      return Double.NaN;
    }

    xPrefs.sortByItem();
    yPrefs.sortByItem();

    final double[] xCommonPrefs = new double[numCommonPref];
    final double[] yCommonPrefs = new double[numCommonPref];

    // copy the common preferences
    int xNewPrefIndex = 0;
    for (int xIndex = 0; xIndex < xPrefs.length(); xIndex++) {
      if (yPrefs.hasPrefWithItemID(xPrefs.getItemID(xIndex))) {
        xCommonPrefs[xNewPrefIndex] = xPrefs.getValue(xIndex);
        xNewPrefIndex++;
      }
    }
    int yNewPrefIndex = 0;
    for (int yIndex = 0; yIndex < yPrefs.length(); yIndex++) {
      if (xPrefs.hasPrefWithItemID(yPrefs.getItemID(yIndex))) {
        yCommonPrefs[yNewPrefIndex] = yPrefs.getValue(yIndex);
        yNewPrefIndex++;
      }
    }

    return SPEARMAN_CORRELATION.correlation(xCommonPrefs, yCommonPrefs);
  }

  /**
   * Returns the number of items that are rated by the two given users. An item rated by only one
   * user is not counted.
   */
  private int numCommonPreferences(final long userId1, final long userId2) throws TasteException {

    final FastIDSet prefs1 = dataModel.getItemIDsFromUser(userId1);
    final FastIDSet prefs2 = dataModel.getItemIDsFromUser(userId2);

    return prefs1.size() < prefs2.size() ? prefs2.intersectionSize(prefs1) : prefs1
        .intersectionSize(prefs2);
  }

  @Override
  public void refresh(Collection<Refreshable> alreadyRefreshed) {
    Collection<Refreshable> newAlreadyRefreshed = RefreshHelper.buildRefreshed(alreadyRefreshed);
    RefreshHelper.maybeRefresh(newAlreadyRefreshed, dataModel);
  }
}
