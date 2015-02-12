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
package norbert.mynemo.dataimport.fileformat.input;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Pattern;

import norbert.mynemo.dataimport.fileformat.MynemoRating;

import org.apache.commons.io.LineIterator;
import org.apache.mahout.cf.taste.common.TasteException;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;

/**
 * This class represents the file containing 10 millions of ratings from the MovieLens data set.
 *
 * <p>
 * In the source file, each line represents a rating. The columns are:
 * <ol>
 * <li>user id</li>
 * <li>MovieLens id of a movie</li>
 * <li>rating</li>
 * <li>timestamp</li>
 * </ol>
 * </p>
 *
 * <p>
 * If a rating is on a movie with a MovieLens id that do not have a corresponding IMDb id, then this
 * rating is ignored.
 * </p>
 */
public class TenMillionRatingFile implements ImportableRatingFile {
  /**
   * This iterator converts each given line from the 10 million rating file into a Mynemo rating.
   */
  private class ConverterIterator extends UnmodifiableIterator<MynemoRating> {

    private final Iterator<String> delegate;

    public ConverterIterator(Iterator<String> delegateIterator) {
      delegate = delegateIterator;
    }

    /**
     * Closes the line iterator, that closes the file.
     */
    private void close() {
      lineIterator.close();
    }

    @Override
    public boolean hasNext() {
      boolean result = delegate.hasNext();

      if (!result) {
        close();
      }

      return result;
    }

    @Override
    public MynemoRating next() {
      // convert the line into a rating
      String line = delegate.next();

      // parse the line
      String[] values = PATTERN.split(line);
      // retrieve data from the current line
      String movielensId = values[MOVIE_INDEX];
      String imdbId = idConverter.convert(movielensId);
      String user = values[USER_INDEX];
      String value = values[VALUE_INDEX];

      // create the rating
      MynemoRating nextRating;
      try {
        nextRating = MynemoRating.createRatingFromMovieLens(user, imdbId, value);
      } catch (TasteException e) {
        close();
        throw new RuntimeException(e);
      }

      return nextRating;
    }
  }

  /**
   * This predicate is true only if the given line from the 10 million rating file is a rating on a
   * movie that has an IMDb id.
   */
  private class HasCorrespondingImdbId implements Predicate<String> {
    @Override
    public boolean apply(String input) {
      // parse the line
      String[] values = PATTERN.split(input);
      String movielensId = values[MOVIE_INDEX];
      return idConverter.canConvert(movielensId);
    }
  }

  private static final String KNOWN_FIRST_LINE = "1::122::5::838985046";
  private static final int MOVIE_INDEX = 1;
  private static final String MOVIELENS_VALUE_SEPARATOR = "::";
  private static final Pattern PATTERN = Pattern.compile(MOVIELENS_VALUE_SEPARATOR);
  private static final int USER_INDEX = 0;
  private static final int VALUE_INDEX = 2;

  /**
   * Returns <code>true</code> if the given file can be parsed, <code>false</code> otherwise.
   */
  public static boolean canImport(String filepath) {
    try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
      return KNOWN_FIRST_LINE.equals(reader.readLine());
    } catch (Exception e) {
      return false;
    }
  }

  private final IdConverter idConverter;

  private final LineIterator lineIterator;

  public TenMillionRatingFile(String ratingFilepath, String mappingFilepath) throws IOException {
    checkNotNull(ratingFilepath);
    checkNotNull(mappingFilepath);

    idConverter = new IdConverter(mappingFilepath);
    lineIterator = new LineIterator(new BufferedReader(new FileReader(ratingFilepath)));
  }

  /**
   * Returns an iterator on the ratings.
   */
  @Override
  public Iterator<MynemoRating> iterator() {
    return new ConverterIterator(Iterators.filter(lineIterator, new HasCorrespondingImdbId()));
  }
}
