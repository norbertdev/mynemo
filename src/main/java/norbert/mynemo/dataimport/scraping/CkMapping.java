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
import static com.google.common.base.Preconditions.checkNotNull;

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
 * A CK mapping maps a CK movie id to an IMDb movie id.
 *
 * <p>
 * A list of mappings can be persisted in a tab-separated value file, where each line represents a
 * mapping. The columns are:
 * <ul>
 * <li>CK movie id
 * <li>IMDb movie id
 * </ul>
 */
public class CkMapping {

  private static final String CK_MOVIE_HEADER = "ck_movie";
  private static final String IMDB_MOVIE_HEADER = "imdb_movie";
  /**
   * Format of the CSV entries for the parser. The header names are not set in order to detect if a
   * wrong file is parsed. Indeed, when the {@link CSVRecord#get(String)} is called, an exception is
   * thrown if the header name doesn't exist.
   */
  private static final CSVFormat CSV_FORMAT_FOR_PARSER = CSVFormat.MYSQL.withHeader()
      .withSkipHeaderRecord();
  /** Format of the CSV entries for the printer. */
  private static final CSVFormat CSV_FORMAT_FOR_PRINTER = CSVFormat.MYSQL.withHeader(
      CK_MOVIE_HEADER, IMDB_MOVIE_HEADER).withSkipHeaderRecord();

  /**
   * Creates a mapping from a record. The record was usually created from a parser created by the
   * {@link #createParser(String)} method.
   */
  public static CkMapping createMapping(CSVRecord record) {
    return new CkMapping(record.get(CK_MOVIE_HEADER), record.get(IMDB_MOVIE_HEADER));
  }

  /**
   * Returns a parser able to read a CK movie mapping file.
   *
   * @param filepath file to read
   * @return a new parser
   */
  public static CSVParser createParser(String filepath) throws IOException {
    return new CSVParser(new BufferedReader(new FileReader(filepath)), CSV_FORMAT_FOR_PARSER);
  }

  /**
   * Creates a printer where a mapping can be printed. The given file must not exist.
   *
   * @param filepath the filepath where the printer will write the data.
   * @return a new printer
   */
  public static CSVPrinter createPrinter(String filepath) throws IOException {
    checkNotNull(filepath);
    checkArgument(!new File(filepath).exists(), "The file must not exist.");

    return new CSVPrinter(new BufferedWriter(new FileWriter(filepath)), CSV_FORMAT_FOR_PRINTER);
  }

  /**
   * Returns <code>true</code> if the mapping can be created from the given parameters. Returns
   * <code>false</code> otherwise.
   */
  public static boolean isValid(CSVRecord record) {
    return record.isConsistent() && record.isMapped(CK_MOVIE_HEADER)
        && record.isMapped(IMDB_MOVIE_HEADER);
  }

  private final String ckMovie;

  private final String imdbMovie;

  public CkMapping(String ckMovie, String imdbMovie) {
    this.imdbMovie = imdbMovie;
    this.ckMovie = ckMovie;
  }

  public String getCkMovie() {
    return ckMovie;
  }

  public String getImdbMovie() {
    return imdbMovie;
  }

  /**
   * Prints the internal data to the given printer. The printer is usually created by calling the
   * {@link #createPrinter(String)} method.
   */
  public void printOn(CSVPrinter printer) throws IOException {
    // the write order depends on the order of the headers in the csv format
    printer.print(ckMovie);
    printer.print(imdbMovie);
    // end of the record
    printer.println();
  }
}
