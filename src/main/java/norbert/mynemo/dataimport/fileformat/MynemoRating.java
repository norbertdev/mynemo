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
package norbert.mynemo.dataimport.fileformat;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * A Mynemo rating encapsulates the user that give the rating, the rated movie and the value of the
 * rating.
 *
 * <p>
 * A list of ratings can be persisted in a tab-separated value file, where each line represents a
 * rating. The columns are:
 * <ul>
 * <li>user id
 * <li>IMDb id of the movie
 * <li>value of the rating
 * </ul>
 */
public class MynemoRating {
  /**
   * Format of the CSV entries. That defines the Mynemo rating file format.
   */
  private static final CSVFormat CSV_FORMAT = CSVFormat.MYSQL;
  /**
   * Rating value superior to this maximum is invalid.
   */
  private static final int MAXIMUM_RATING_VALUE = 100;
  /**
   * Rating value inferior to this minimum is invalid.
   */
  private static final int MINIMUM_RATING_VALUE = 0;
  /**
   * The movie index in a rating record. That defines the Mynemo rating file format.
   */
  private static final int MOVIE_INDEX = 1;
  /**
   * The user index in a rating record. That defines the Mynemo rating file format.
   */
  private static final int USER_INDEX = 0;
  /**
   * The value index in a rating record. That defines the Mynemo rating file format.
   */
  private static final int VALUE_INDEX = 2;

  /**
   * Returns a parser able to read a Mynemo rating file.
   *
   * @param filepath file to read
   * @return a new parser
   */
  public static CSVParser createParser(String filepath) throws IOException {
    return new CSVParser(new BufferedReader(new FileReader(filepath)), CSV_FORMAT);
  }

  /**
   * Creates a printer where a rating can be printed. The given file must not exist.
   *
   * @param filepath the filepath where the printer will write the data.
   * @return a new printer
   */
  public static CSVPrinter createPrinter(String filepath) throws IOException {
    checkArgument(filepath != null, "The filepath must be not null.");
    checkArgument(!new File(filepath).exists(), "The file must not exist.");

    return new CSVPrinter(new BufferedWriter(new FileWriter(filepath)), CSV_FORMAT);
  }

  /**
   * Creates a rating from a record. The record was usually created from a parser created by the
   * {@link #createParser(String)} method.
   */
  public static MynemoRating createRating(CSVRecord record) {
    return new MynemoRating(record.get(USER_INDEX), record.get(MOVIE_INDEX),
        record.get(VALUE_INDEX));
  }

  private final String movie;
  private final String user;
  private final String value;

  /**
   * Creates a rating with the given value by the given user to the given movie.
   */
  public MynemoRating(String user, String movie, String value) {
    checkArgument(user != null, "The user must not be null.");
    checkArgument(movie != null, "The movie must not be null.");
    checkArgument(value != null, "The value must not be null.");
    int numericValue = Integer.parseInt(value);
    checkArgument(MINIMUM_RATING_VALUE <= numericValue && numericValue <= MAXIMUM_RATING_VALUE,
        "The rating value is not valid.");

    this.user = user;
    this.movie = movie;
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof MynemoRating)) {
      return false;
    }
    MynemoRating r = (MynemoRating) o;
    return movie.equals(r.movie) && user.equals(r.user) && value.equals(r.value);
  }

  public String getMovie() {
    return movie;
  }

  public String getUser() {
    return user;
  }

  public String getValue() {
    return value;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(movie).append(user).append(value).toHashCode();
  }

  /**
   * Prints the internal data to the given printer. The printer is usually created by calling the
   * {@link #createPrinter(String)} method.
   */
  public void printOn(CSVPrinter printer) throws IOException {
    // the write order depends on the *_INDEX values
    printer.print(user);
    printer.print(movie);
    printer.print(value);
    // end of the record
    printer.println();
  }
}
