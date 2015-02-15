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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import norbert.mynemo.dataimport.fileformat.MynemoRating;

/**
 * <p>
 * This filter writes only the ratings on the movies that have at least a given number of ratings.
 * </p>
 *
 * <p>
 * The ratings are kept in memory until the {@link #close()} method is called. Then, the ratings are
 * written to the next writer.
 * </p>
 */
public class MinRatingByMovieFilter implements RatingWriter {

  /**
   * All ratings by movie. Every rating that is given to the {@link #write(MynemoRating) write}
   * method is kept in this map. The key is the movie of the rating.
   */
  private final Map<String, Collection<MynemoRating>> allRatings;
  private final int minRatingByMovie;
  private final RatingWriter nextWriter;

  public MinRatingByMovieFilter(RatingWriter nextWriter, int minRatingByMovie) {
    checkArgument(nextWriter != null, "The next writer must not be null.");
    checkArgument(0 < minRatingByMovie, "The minimum rating movie must be at least 1.");

    this.minRatingByMovie = minRatingByMovie;
    this.nextWriter = nextWriter;
    allRatings = new HashMap<>();
  }

  @Override
  public void close() throws IOException {
    for (Entry<String, Collection<MynemoRating>> entry : allRatings.entrySet()) {
      Collection<MynemoRating> ratings = entry.getValue();
      if (minRatingByMovie <= ratings.size()) {
        writeAll(ratings);
      }
    }

    nextWriter.close();
  }

  @Override
  public void write(MynemoRating rating) throws IOException {
    checkNotNull(rating);

    String movie = rating.getMovie();

    // keep the rating
    if (!allRatings.containsKey(movie)) {
      allRatings.put(movie, newArrayList(rating));
    } else {
      allRatings.get(movie).add(rating);
    }
  }

  /**
   * Writes into the next writer the given ratings.
   */
  private void writeAll(Collection<MynemoRating> ratings) throws IOException {
    for (MynemoRating rating : ratings) {
      nextWriter.write(rating);
    }
  }
}
