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
package norbert.mynemo.dataimport.fileformat.output;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import norbert.mynemo.dataimport.fileformat.MynemoRating;

/**
 * This writer scales the value of a rating if necessary. The Mynemo scale goes from 0 to 100. If
 * the maximum value of the ratings of one user is lower than 50, then all its ratings are linearly
 * scaled such as the new maximum value is 100.
 */
public class ScaleValueWriter implements RatingWriter {
  /**
   * This comparator orders the ratings by their numeric value. <code>r1 < r2</code> if
   * <code>r1.value < r2.value</code>.
   */
  private class RatingComparator implements Comparator<MynemoRating> {
    @Override
    public int compare(MynemoRating o1, MynemoRating o2) {
      double difference = parseValue(o1) - parseValue(o2);
      if (difference < 0) {
        return -1;
      } else if (0 < difference) {
        return 1;
      } else {
        return 0;
      }
    }
  }

  /**
   * Returns the numeric value of the given rating.
   */
  private static double parseValue(MynemoRating rating) {
    return Double.parseDouble(rating.getValue());
  }

  /** Map containing all ratings to write. The key is the user, the value is a list of its ratings. */
  private final Map<String, List<MynemoRating>> allRatings;
  private final Comparator<? super MynemoRating> COMPARATOR = new RatingComparator();
  private final RatingWriter nextWriter;

  public ScaleValueWriter(RatingWriter nextWriter) {
    checkNotNull(nextWriter);

    this.nextWriter = nextWriter;
    allRatings = new HashMap<>();
  }

  @Override
  public void close() throws IOException {
    writeAllRatings();
    nextWriter.close();
  }

  @Override
  public void write(MynemoRating rating) throws IOException {
    checkNotNull(rating);

    String user = rating.getUser();
    if (!allRatings.containsKey(user)) {
      allRatings.put(user, newArrayList(rating));
    } else {
      allRatings.get(user).add(rating);
    }
  }

  /**
   * Writes all the ratings contained in the {@link #allRatings} field. Adapts the value of each
   * rating if necessary.
   */
  private void writeAllRatings() throws IOException {
    for (Entry<String, List<MynemoRating>> entry : allRatings.entrySet()) {
      List<MynemoRating> ratings = entry.getValue();

      // find the maximum value
      double maxValue = parseValue(Collections.max(ratings, COMPARATOR));

      // rewrite the values if necessary
      if (maxValue <= 50) {
        double factor = 100 / maxValue;
        List<MynemoRating> newRatings = newArrayList();
        for (MynemoRating rating : ratings) {
          newRatings.add(new MynemoRating(rating.getUser(), rating.getMovie(), Integer
              .toString((int) (parseValue(rating) * factor))));
        }
        ratings = newRatings;
      }

      // write
      for (MynemoRating rating : ratings) {
        nextWriter.write(rating);
      }
    }
  }
}
