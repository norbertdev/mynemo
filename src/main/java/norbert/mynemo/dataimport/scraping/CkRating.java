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
package norbert.mynemo.dataimport.scraping;

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

/**
 * This class represents a rating from the CK web site.
 *
 * <p>
 * A list of ratings can be persisted in a tab-separated value file, where each line represents a
 * rating. The columns are:
 * <ul>
 * <li>user id from CK</li>
 * <li>movie id from CK</li>
 * <li>value of the rating</li>
 * </ul>
 * </p>
 */
public class CkRating {
  private static final String MOVIE_HEADER = "ck_movie";
  private static final String USER_HEADER = "ck_user";
  private static final String VALUE_HEADER = "rating_value";
  /**
   * Format of the CSV entries for the parser. The header names are not set in order to detect if a
   * wrong file is parsed. Indeed, when the {@link CSVRecord#get(String)} is called, an exception is
   * thrown if the header name doesn't exist.
   */
  private static final CSVFormat CSV_FORMAT_FOR_PARSER = CSVFormat.MYSQL.withHeader()
      .withSkipHeaderRecord();
  /** Format of the CSV entries for the printer. */
  private static final CSVFormat CSV_FORMAT_FOR_PRINTER = CSVFormat.MYSQL.withHeader(USER_HEADER,
      MOVIE_HEADER, VALUE_HEADER).withSkipHeaderRecord();

  /**
   * Returns a parser able to read a CK rating file.
   *
   * @param filepath file to read
   * @return a new parser
   */
  public static CSVParser createParser(String filepath) throws IOException {
    return new CSVParser(new BufferedReader(new FileReader(filepath)), CSV_FORMAT_FOR_PARSER);
  }

  /**
   * Creates a printer where a rating can be printed. The given file must not exist.
   *
   * @param filepath the filepath where the printer will write the data.
   * @return a new printer
   * @throws IOException
   */
  public static CSVPrinter createPrinter(String filepath) throws IOException {
    checkArgument(filepath != null, "The filepath must be not null.");
    checkArgument(!new File(filepath).exists(), "The file must not exist.");

    return new CSVPrinter(new BufferedWriter(new FileWriter(filepath)), CSV_FORMAT_FOR_PRINTER);
  }

  /**
   * Creates a rating from a record. The record was usually created from a parser created by the
   * {@link #createParser(String)} method.
   */
  public static CkRating createRating(CSVRecord record) {
    return new CkRating(record.get(USER_HEADER), record.get(MOVIE_HEADER), record.get(VALUE_HEADER));
  }

  private final String movie;

  private final String user;

  private final String value;

  public CkRating(String user, String movie, String value) {
    this.user = user;
    this.movie = movie;
    this.value = value;
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

  /**
   * Prints the internal data to the given printer. The printer is usually created by calling the
   * {@link #createPrinter(String)} method.
   */
  public void printOn(CSVPrinter printer) throws IOException {
    // the write order depends on the order of the headers in the csv format
    printer.print(user);
    printer.print(movie);
    printer.print(value);
    // end of the record
    printer.println();
  }
}
