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
package norbert.mynemo.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import norbert.mynemo.dataimport.Scraper;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.mahout.cf.taste.common.TasteException;

/**
 * This parser handles a command line to scrape a web site in order to get new ratings.
 */
public class ScrapeCommandParser {

  private static final String COMMAND_SYNTAX = "scrape  --out-ratings <file>  --out-movies <file>"
      + "  --in <file> [<file>…]";

  // input files
  private static final String IN_ARG_NAME = "files";
  private static final char IN_CHAR_OPTION = 'i';
  private static final String IN_DESCRIPTION = "space-separated input files containing users to"
      + " scrap, already scraped users and already scraped movies. A file containing some users"
      + " to scrape are the best TCI html pages downloaded from the web site. The others files"
      + " were previously generated from this command, they are merged with the new scraped data."
      + " At last one file must be provided.";
  private static final String IN_LONG_OPTION = "in";

  // output file for scraped movies
  private static final String OUT_MOVIES_ARG_NAME = "file";
  private static final char OUT_MOVIES_CHAR_OPTION = 'm';
  private static final String OUT_MOVIES_DESCRIPTION = "output file where the movies will be"
      + " written.";
  private static final String OUT_MOVIES_LONG_OPTION = "out-movies";

  // output file for scraped ratings
  private static final String OUT_RATINGS_ARG_NAME = "file";
  private static final char OUT_RATINGS_CHAR_OPTION = 'r';
  private static final String OUT_RATINGS_DESCRIPTION = "output file where the ratings will be"
      + " written.";
  private static final String OUT_RATINGS_LONG_OPTION = "out-ratings";

  /**
   * Performs various checks on the parameters.
   */
  private static void check(String outMoviesFilepath, String outRatingsFilepath,
      String[] inputFilepaths) throws FileNotFoundException {

    // check output filepaths
    if (new File(outMoviesFilepath).exists()) {
      throw new IllegalArgumentException("Error: the output file " + outMoviesFilepath
          + " already exist.");
    }
    if (new File(outRatingsFilepath).exists()) {
      throw new IllegalArgumentException("Error: the output file " + outRatingsFilepath
          + " already exist.");
    }

    // check input filepaths
    for (String filepath : inputFilepaths) {
      if (!new File(filepath).exists()) {
        throw new FileNotFoundException("Error: cannot find the intput file " + filepath + ".");
      }
    }
  }

  /**
   * Runs the scraper.
   */
  private static void execute(String outMoviesFilepath, String outRatingsFilepath,
      String[] inputFilepaths) throws IOException, InterruptedException {
    new Scraper(outMoviesFilepath, outRatingsFilepath, inputFilepaths).scrape();
  }

  private static Options getOptions() {
    // out movies
    OptionBuilder.isRequired();
    OptionBuilder.hasArg();
    OptionBuilder.withArgName(OUT_MOVIES_ARG_NAME);
    OptionBuilder.withLongOpt(OUT_MOVIES_LONG_OPTION);
    OptionBuilder.withDescription(OUT_MOVIES_DESCRIPTION);
    Option outMovies = OptionBuilder.create(OUT_MOVIES_CHAR_OPTION);

    // out ratings
    OptionBuilder.isRequired();
    OptionBuilder.hasArg();
    OptionBuilder.withArgName(OUT_RATINGS_ARG_NAME);
    OptionBuilder.withLongOpt(OUT_RATINGS_LONG_OPTION);
    OptionBuilder.withDescription(OUT_RATINGS_DESCRIPTION);
    Option outRatings = OptionBuilder.create(OUT_RATINGS_CHAR_OPTION);

    // input files
    OptionBuilder.isRequired();
    OptionBuilder.hasArgs();
    OptionBuilder.withArgName(IN_ARG_NAME);
    OptionBuilder.withLongOpt(IN_LONG_OPTION);
    OptionBuilder.withDescription(IN_DESCRIPTION);
    Option in = OptionBuilder.create(IN_CHAR_OPTION);

    return new Options().addOption(outMovies).addOption(outRatings).addOption(in);
  }

  public static void main(String[] args) {
    try {
      ScrapeCommandParser.parse(args);
    } catch (ParseException e) {
      System.err.println(e.getMessage());
      ScrapeCommandParser.printUsage();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Parses and checks the given arguments, then runs the scraper.
   */
  public static void parse(String[] args) throws ParseException, IOException, TasteException,
      InterruptedException {

    CommandLine commandLine = new BasicParser().parse(getOptions(), args);

    // parse the options
    String outMoviesFilepath = commandLine.getOptionValue(OUT_MOVIES_LONG_OPTION);
    String outRatingsFilepath = commandLine.getOptionValue(OUT_RATINGS_LONG_OPTION);
    String[] inputFilepaths = commandLine.getOptionValues(IN_LONG_OPTION);

    // check
    check(outMoviesFilepath, outRatingsFilepath, inputFilepaths);

    // run
    execute(outMoviesFilepath, outRatingsFilepath, inputFilepaths);
  }

  public static void printUsage() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(COMMAND_SYNTAX, getOptions());
  }

  /**
   * Instantiates a new object. Private to prevents instantiation.
   */
  private ScrapeCommandParser() {
    throw new AssertionError();
  }
}
