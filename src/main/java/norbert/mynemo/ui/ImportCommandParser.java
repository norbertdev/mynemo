/*
 * Copyright 2015 Norbert
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package norbert.mynemo.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import norbert.mynemo.dataimport.FileImporter;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.common.base.Optional;

/**
 * This parser handle a command line to import some input rating files. The
 * input files are merged. The imported rating are written into an output file.
 * The ratings can be filtered.
 */
public class ImportCommandParser {
	private static final String COMMAND_SYNTAX = "import  --out <file>"
			+ "   --in <file> [<file>…]  [--movies <file>] [--user <id>]"
			+ "  [--max-users <number>]  [--min-ratings-by-movie <number>]";

	// maximum number of users
	private static final String MAX_USERS_ARG_NAME = "number";
	private static final String MAX_USERS_DESCRIPTION = "maximum number of"
			+ " users allowed in the output file. Once the limit is reach,"
			+ "the ratings of other users are ignored.";
	private static final String MAX_USERS_LONG_OPTION = "max-users";

	// minimum number of ratings for a movie
	private static final String MIN_RATINGS_BY_MOVIE_ARG_NAME = "number";
	private static final String MIN_RATINGS_BY_MOVIE_DESCRIPTION = "minimum"
			+ " ratings for an item. The ouput file will only contain items"
			+ " that have at least this number of ratings";
	private static final String MIN_RATINGS_BY_MOVIE_LONG_OPTION = "min-ratings"
			+ "-by-movie";

	// movie
	private static final String MOVIES_ARG_NAME = "file";
	private static final char MOVIES_CHAR_OPTION = 'm';
	private static final String MOVIES_DESCRIPTION = "file containing the"
			+ " correspondance between MovieLens ids and IMDb ids."
			+ " This file is only required if the 10 million rating file from"
			+ " the MovieLens data set is an input file."
			+ " This file can be generated from the MovieLens website:"
			+ " download the result of the search for any genre.";
	private static final String MOVIES_LONG_OPTION = "movies";

	// output file
	private static final String OUT_ARG_NAME = "file";
	private static final char OUT_CHAR_OPTION = 'o';
	private static final String OUT_DESCRIPTION = "output file where the"
			+ " converted ratings are written. If more than one input files"
			+ " is provided, they are merged.";
	private static final String OUT_LONG_OPTION = "out";

	// rating file
	private static final String RATINGS_ARG_NAME = "file";
	private static final char RATINGS_CHAR_OPTION = 'i';
	private static final String RATINGS_DESCRIPTION = "file containing"
			+ " ratings from MovieLensMovieLens.";
	private static final String RATINGS_LONG_OPTION = "in";

	// user
	private static final String USER_ARG_NAME = "id";
	private static final char USER_CHAR_OPTION = 'u';
	private static final String USER_DESCRIPTION = "user identifier of the"
			+ " input ratings.";
	private static final String USER_LONG_OPTION = "user";

	/**
	 * Performs various checks on the parameters.
	 */
	private static void check(String ouputFilepath, String[] inputFilepaths,
			Optional<String> inputFilepath) throws FileNotFoundException {

		// output filepath
		if (new File(ouputFilepath).exists()) {
			throw new IllegalArgumentException("Error: the output file "
					+ ouputFilepath + " already exist.");
		}

		// input filepaths
		for (String filepath : inputFilepaths) {
			if (!new File(filepath).exists()) {
				throw new FileNotFoundException(
						"Error: cannot find the intput file " + filepath);
			}
		}
		if (inputFilepath.isPresent()
				&& !new File(inputFilepath.get()).exists()) {
			throw new FileNotFoundException(
					"Error: cannot find the intput file " + inputFilepath);
		}
	}

	private static Options getOptions() {
		OptionBuilder.isRequired();
		OptionBuilder.hasArg();
		OptionBuilder.withArgName(OUT_ARG_NAME);
		OptionBuilder.withDescription(OUT_DESCRIPTION);
		OptionBuilder.withLongOpt(OUT_LONG_OPTION);
		Option out = OptionBuilder.create(OUT_CHAR_OPTION);

		OptionBuilder.isRequired();
		OptionBuilder.hasArgs();
		OptionBuilder.withArgName(RATINGS_ARG_NAME);
		OptionBuilder.withLongOpt(RATINGS_LONG_OPTION);
		OptionBuilder.withDescription(RATINGS_DESCRIPTION);
		Option ratings = OptionBuilder.create(RATINGS_CHAR_OPTION);

		OptionBuilder.hasArg();
		OptionBuilder.withArgName(MOVIES_ARG_NAME);
		OptionBuilder.withLongOpt(MOVIES_LONG_OPTION);
		OptionBuilder.withDescription(MOVIES_DESCRIPTION);
		Option movies = OptionBuilder.create(MOVIES_CHAR_OPTION);

		OptionBuilder.hasArg();
		OptionBuilder.withArgName(USER_ARG_NAME);
		OptionBuilder.withLongOpt(USER_LONG_OPTION);
		OptionBuilder.withDescription(USER_DESCRIPTION);
		Option user = OptionBuilder.create(USER_CHAR_OPTION);

		OptionBuilder.hasArg();
		OptionBuilder.withArgName(MAX_USERS_ARG_NAME);
		OptionBuilder.withLongOpt(MAX_USERS_LONG_OPTION);
		OptionBuilder.withDescription(MAX_USERS_DESCRIPTION);
		Option maxUsers = OptionBuilder.create();

		OptionBuilder.hasArg();
		OptionBuilder.withArgName(MIN_RATINGS_BY_MOVIE_ARG_NAME);
		OptionBuilder.withLongOpt(MIN_RATINGS_BY_MOVIE_LONG_OPTION);
		OptionBuilder.withDescription(MIN_RATINGS_BY_MOVIE_DESCRIPTION);
		Option minRatingsByMovie = OptionBuilder.create();

		return new Options().addOption(out).addOption(ratings)
				.addOption(movies).addOption(user).addOption(maxUsers)
				.addOption(minRatingsByMovie);
	}

	public static void main(String[] args) {
		try {
			ImportCommandParser.parse(args);
		} catch (ParseException e) {
			System.err.println(e.getMessage());
			ImportCommandParser.printUsage();
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Parses and checks the given arguments, then calls
	 * {@link FileImporter#convert(String, java.util.Collection, Optional, Optional, Optional, Optional)
	 * FileImporter.convert(…)}.
	 */
	public static void parse(String[] args) throws ParseException, IOException {
		CommandLineParser parser = new BasicParser();

		Options options = getOptions();

		CommandLine commandLine = parser.parse(options, args);

		String outputFilepath = commandLine.getOptionValue(OUT_LONG_OPTION);
		String[] ratingsFilepath = commandLine
				.getOptionValues(RATINGS_LONG_OPTION);
		Optional<String> moviesFilepath = Optional.fromNullable(commandLine
				.getOptionValue(MOVIES_LONG_OPTION));
		Optional<String> user = Optional.fromNullable(commandLine
				.getOptionValue(USER_LONG_OPTION));
		Optional<Integer> maxUsers = parseMaxUser(commandLine
				.getOptionValue(MAX_USERS_LONG_OPTION));
		Optional<Integer> minRatingsByMovie = parseMinRatingsByMovie(commandLine
				.getOptionValue(MIN_RATINGS_BY_MOVIE_LONG_OPTION));

		check(outputFilepath, ratingsFilepath, moviesFilepath);

		FileImporter.convert(outputFilepath, Arrays.asList(ratingsFilepath),
				moviesFilepath, user, maxUsers, minRatingsByMovie);
	}

	/**
	 * Parses and checks the "max-users" option.
	 */
	private static Optional<Integer> parseMaxUser(String optionValue) {
		if (optionValue == null) {
			return Optional.absent();
		}

		Integer result;

		try {
			result = Integer.parseInt(optionValue);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Error: the maximum number of"
					+ " users is not a valid integer.", e);
		}

		// check
		if (result <= 0) {
			throw new IllegalArgumentException("Error: the maximum number of"
					+ " users must be greater than 0.");
		}

		return Optional.of(result);
	}

	/**
	 * Parses and checks the "min-ratings-by-movie" option.
	 */
	private static Optional<Integer> parseMinRatingsByMovie(String optionValue) {
		if (optionValue == null) {
			return Optional.absent();
		}

		Integer result;

		try {
			result = Integer.parseInt(optionValue);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Error: the minimum ratings"
					+ " for a movie is not a valid integer.", e);
		}

		// minimum ratings for a movie
		if (result <= 0) {
			throw new IllegalArgumentException("Error: the minimum number of"
					+ " ratings for a movie must be greater than 0.");
		}

		return Optional.of(result);
	}

	public static void printUsage() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(COMMAND_SYNTAX, getOptions());
	}

	/**
	 * Instantiates a new object. Private to prevents instantiation.
	 */
	private ImportCommandParser() {
		throw new AssertionError();
	}
}
