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

import java.io.IOException;
import java.util.Collection;

import norbert.mynemo.dataimport.fileformat.MynemoRating;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * This writer scales the value of a rating if necessary. The Mynemo scale goes from 0 to 100. If
 * the maximum value of the ratings of one user is lower than 50, then all its ratings are linearly
 * scaled such as the new maximum value is 100.
 */
public class ScaleValueWriter implements RatingWriter {

  private final RatingWriter nextWriter;
  private final Scaler scaler;
  private final Multimap<String, MynemoRating> unwrittenRatings;

  public ScaleValueWriter(RatingWriter nextWriter) {
    checkNotNull(nextWriter);

    this.nextWriter = nextWriter;

    scaler = new Scaler();
    unwrittenRatings = HashMultimap.create();
  }

  @Override
  public void close() throws IOException {
    for (MynemoRating rating : unwrittenRatings.values()) {
      nextWriter.write(scaler.scale(rating));
    }
    unwrittenRatings.clear();
    nextWriter.close();
  }

  @Override
  public void write(MynemoRating rating) throws IOException {
    checkNotNull(rating);

    final String user = rating.getUser();

    if (scaler.cantScaleRatingsOf(user)) {
      nextWriter.write(rating);
      return;
    }

    unwrittenRatings.put(user, rating);

    if (scaler.updateWith(rating).cantScaleRatingsOf(user)) {
      writeAll(unwrittenRatings.get(user));
      unwrittenRatings.removeAll(user);
    }
  }

  /**
   * Writes the given ratings into the next writer.
   */
  private void writeAll(Collection<MynemoRating> ratings) throws IOException {
    for (MynemoRating rating : ratings) {
      nextWriter.write(rating);
    }
  }
}
