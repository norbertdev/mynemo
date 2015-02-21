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

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import norbert.mynemo.dataimport.fileformat.MynemoRating;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * This filter writes only the ratings of the movies that have at least a given number of ratings.
 */
public class MinRatingByMovieFilter implements RatingWriter {

  private final int minRatingsByMovie;
  private final RatingWriter nextWriter;
  /**
   * Unwritten ratings by movie. Every rating given to the {@link #write(MynemoRating) write} method
   * is kept in this map until the minimum number is reached.
   */
  private final Multimap<String, MynemoRating> unwrittenRatings;
  /** A rating for a movie in this set can be written to the next writer. */
  private final Set<String> writableMovies;

  /**
   * Creates a writer that writes into the given writer. The given minimum must be at least 1.
   */
  public MinRatingByMovieFilter(RatingWriter nextWriter, int minRatingsByMovie) {
    checkNotNull(nextWriter);
    checkArgument(1 <= minRatingsByMovie, "The minimum rating by movie must be at least 1.");

    this.minRatingsByMovie = minRatingsByMovie;
    this.nextWriter = nextWriter;

    unwrittenRatings = HashMultimap.create();
    writableMovies = new HashSet<>();
  }

  @Override
  public void close() throws IOException {
    unwrittenRatings.clear();
    writableMovies.clear();

    nextWriter.close();
  }

  @Override
  public void write(MynemoRating rating) throws IOException {
    checkNotNull(rating);

    String movie = rating.getMovie();

    // write and return if possible
    if (writableMovies.contains(movie)) {
      nextWriter.write(rating);
      return;
    }

    unwrittenRatings.put(movie, rating);
    final Collection<MynemoRating> movieRatings = unwrittenRatings.get(movie);

    // check if the movie has become writable
    if (minRatingsByMovie <= movieRatings.size()) {
      writableMovies.add(movie);
      writeAll(movieRatings);
      unwrittenRatings.removeAll(movie);
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
