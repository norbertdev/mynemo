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

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import norbert.mynemo.dataimport.fileformat.MynemoRating;

/**
 * This scaler linearly scales values of ratings. The scaling result depends of the maximum value of
 * the rating of its user. For example, if a user has two ratings with value 2 and 5, their scaled
 * values are 40 and 100.
 *
 * <p>
 * The scaler is updated via the {@link #updateWith(MynemoRating)} method.
 *
 * <p>
 * The scaler can't scale the ratings of a user if its maximum rating is greater than 50. See the
 * method {@link #cantScaleRatingsOf(String)}.
 */
class Scaler {

  private static final float LOWER_THAN_MINIMUM_VALUE = MynemoRating.MINIMUM_RATING_VALUE - 1;
  /** Threshold that determines if the ratings of an user must be scaled. */
  private static final int MAX_VALUE_FOR_SCALING = 50;

  /**
   * Returns the scaled value, based on the given maximum value.
   */
  private static String scale(final Float value, final Float maximumValue) {
    final Float newValue = value * MynemoRating.MAXIMUM_RATING_VALUE / maximumValue;
    return Long.toString(Math.round(newValue));
  }

  /** A key is a user, the value is the maximum known value of its ratings. */
  private final Map<String, Float> values;

  public Scaler() {
    values = new HashMap<>();
  }

  /**
   * Returns <code>true</code> if the ratings of the given user are already scaled.
   */
  public boolean cantScaleRatingsOf(String user) {
    checkNotNull(user);

    return MAX_VALUE_FOR_SCALING < getMaxValueOf(user);
  }

  /**
   * Returns the maximum value of the given user. If the maximum value is unknown, returns a lower
   * value than any possible real value.
   */
  private float getMaxValueOf(final String user) {
    return fromNullable(values.get(user)).or(LOWER_THAN_MINIMUM_VALUE);
  }

  /**
   * Returns a new rating identical to the given rating, except its value that is scaled.
   *
   * @throws IllegalArgumentException if the given rating can't be scaled
   */
  public MynemoRating scale(final MynemoRating rating) {
    checkArgument(!cantScaleRatingsOf(rating.getUser()), "The given rating can't be scaled.");

    final String user = rating.getUser();
    final String scaledValue = scale(rating.getFloatValue(), getMaxValueOf(user));
    return new MynemoRating(user, rating.getMovie(), scaledValue);
  }

  /**
   * Updates the internal state with the given rating. Returns itself.
   */
  public Scaler updateWith(final MynemoRating rating) throws IOException {
    checkNotNull(rating);

    final String user = rating.getUser();
    final Float newValue = rating.getFloatValue();
    final Float currentValue = getMaxValueOf(user);
    final Float updatedValue = Math.max(newValue, currentValue);

    values.put(user, updatedValue);

    return this;
  }
}
