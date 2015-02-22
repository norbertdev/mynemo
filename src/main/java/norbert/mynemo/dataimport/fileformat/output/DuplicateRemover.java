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

import norbert.mynemo.dataimport.fileformat.MynemoRating;
import norbert.mynemo.dataimport.scraping.input.CkScraper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * This filter writes all ratings except the ratings already written. Two ratings are considered
 * equal if their users are equal and their movies are equals. The values are not taken in account.
 */
public class DuplicateRemover implements RatingWriter {

  private static final Logger LOGGER = LoggerFactory.getLogger(CkScraper.class);

  private final RatingWriter nextWriter;
  /** Ratings already written. A key is a user, a value is one of its rated movie. */
  private final Multimap<String, String> unwritableRatings;

  public DuplicateRemover(final RatingWriter nextWriter) {
    checkNotNull(nextWriter);

    this.nextWriter = nextWriter;
    unwritableRatings = HashMultimap.create();
  }

  @Override
  public void close() throws IOException {
    unwritableRatings.clear();
    nextWriter.close();
  }

  @Override
  public void write(final MynemoRating rating) throws IOException {
    checkNotNull(rating);

    final String user = rating.getUser();
    final String movie = rating.getMovie();

    if (!unwritableRatings.containsEntry(user, movie)) {
      unwritableRatings.put(user, movie);
      nextWriter.write(rating);
    } else {
      LOGGER.warn("A duplicate rating is detected: {} has rated {} more than once.", user, movie);
    }
  }
}
