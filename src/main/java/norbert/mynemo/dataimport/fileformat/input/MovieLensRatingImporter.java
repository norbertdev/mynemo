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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import norbert.mynemo.dataimport.fileformat.MynemoRating;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.mahout.cf.taste.common.TasteException;

import com.google.common.collect.UnmodifiableIterator;

/**
 * This importer can parse a TSV file exported from the MovieLens web site. Such file contains the
 * ratings of only one user.
 *
 * <p>
 * Each line of the source file represents a rating. The columns are:
 * <ol>
 * <li>MovieLens id</li>
 * <li>rating value</li>
 * <li>average</li>
 * <li>IMDb id</li>
 * <li>title and year</i>
 * </ol>
 */
public class MovieLensRatingImporter implements RatingImporter {
  /**
   * Iterator over a MovieLens rating file that converts the CSV records to Mynemo ratings.
   */
  private final class RatingIterator extends UnmodifiableIterator<MynemoRating> {
    private Iterator<CSVRecord> iterator = parser.iterator();

    /**
     * Closes the parser.
     */
    private void close() {
      try {
        parser.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public boolean hasNext() {
      boolean result = iterator.hasNext();

      if (!result) {
        close();
      }

      return result;
    }

    @Override
    public MynemoRating next() {
      CSVRecord record = iterator.next();
      String movie = record.get(MOVIE_INDEX);
      String value = record.get(VALUE_INDEX);
      MynemoRating rating;
      try {
        rating = MynemoRating.createRatingFromMovieLens(user, movie, value);
      } catch (TasteException e) {
        close();
        throw new RuntimeException(e);
      }
      return rating;
    }
  }

  private static final int MOVIE_INDEX = 3;
  private static final String TEST_USER = "0";
  private static final int VALUE_INDEX = 1;

  /**
   * Returns <code>true</code> if the given file can be parsed, <code>false</code> otherwise.
   */
  public static boolean canImport(String filepath) {
    try (CSVParser parser = createParser(filepath)) {

      for (CSVRecord record : parser) {
        // try to create just one rating
        MynemoRating.createRatingFromMovieLens(TEST_USER, record.get(MOVIE_INDEX),
            record.get(VALUE_INDEX));
        break;
      }

    } catch (Exception e) {
      // the file cannot be imported
      return false;
    }
    // everything seems right
    return true;
  }

  private static CSVParser createParser(String filepath) throws IOException {
    return new CSVParser(new CleanRatingsReader(new BufferedReader(new FileReader(filepath))),
        CSVFormat.MYSQL);
  }

  private final CSVParser parser;
  private final String user;

  public MovieLensRatingImporter(String filepath, String user) throws IOException {
    checkNotNull(filepath);
    checkNotNull(user);
    checkArgument(new File(filepath).exists(), "The file must exist.");

    this.user = user;
    parser = createParser(filepath);
  }

  /**
   * Returns an iterator on the ratings.
   */
  @Override
  public Iterator<MynemoRating> iterator() {
    return new RatingIterator();
  }
}
