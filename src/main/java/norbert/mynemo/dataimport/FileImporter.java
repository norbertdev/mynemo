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
package norbert.mynemo.dataimport;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import norbert.mynemo.dataimport.fileformat.MynemoRating;
import norbert.mynemo.dataimport.fileformat.input.ImportableRatingFile;
import norbert.mynemo.dataimport.fileformat.input.MovieLensRatingFile;
import norbert.mynemo.dataimport.fileformat.input.MynemoRatingFile;
import norbert.mynemo.dataimport.fileformat.input.TenMillionRatingFile;
import norbert.mynemo.dataimport.fileformat.output.MaxUserFilter;
import norbert.mynemo.dataimport.fileformat.output.MinRatingByMovieFilter;
import norbert.mynemo.dataimport.fileformat.output.RatingFileWriter;
import norbert.mynemo.dataimport.fileformat.output.RatingWriter;

import com.google.common.base.Optional;

/**
 * This importer can merge, convert and filter ratings from files. It produces
 * one output file from several input files. The format of the output file is
 * the Mynemo file format. The format of an input file is automatically
 * detected. The ratings can be filtered.
 */
public class FileImporter {
	private static final String DEFAULT_USER_ID = Integer
			.toString(Integer.MAX_VALUE);

	/**
	 * <p>
	 * Converts the given rating files to the Mynemo format. Handles the files
	 * generated from MovieLens, and the 10 million rating from a MovieLens data
	 * set. If the latter is amongst the rating files, a movie file must be
	 * provide.
	 * </p>
	 * <p>
	 * The movie file must contains the equivalence between MovieLens
	 * identifiers and IMDb identifiers.
	 * </p>
	 * <p>
	 * If an input file contains ratings without user id, the given
	 * <code>user</code> is used. If <code>user</code> is null, then a default
	 * user id is used. No check is done to find if the user already exists.
	 * </p>
	 * <p>
	 * The output file must not exist. At least one rating file must be
	 * provided, the file must exists.
	 * </p>
	 *
	 * @param outputFilepath
	 *            the file where the result is written
	 * @param inputFilepaths
	 *            the rating files to convert
	 * @param movieFilepath
	 *            the file containing the equivalences between identifiers
	 * @param user
	 *            the user id used for the input ratings without user
	 * @param maxUsers
	 *            maximum number of users, the output file will contain the
	 *            ratings of at most this number of users
	 * @param minRatingsByMovie
	 *            minimum ratings by movie, the output file won't contain movies
	 *            that have less than this number of ratings
	 * @throws IOException
	 */
	public static void convert(String outputFilepath,
			Collection<String> inputFilepaths, Optional<String> movieFilepath,
			Optional<String> user, Optional<Integer> maxUsers,
			Optional<Integer> minRatingsByMovie) throws IOException {
		checkNotNull(outputFilepath != null);
		checkArgument(inputFilepaths != null && !inputFilepaths.isEmpty(),
				"The rating file must contains at least one file.");
		checkArgument(!new File(outputFilepath).exists(),
				"The output file must not exist.");
		for (String filepath : inputFilepaths) {
			checkArgument(new File(filepath).exists(),
					"The input file must exist.");
		}
		checkArgument(
				!movieFilepath.isPresent()
						|| new File(movieFilepath.get()).exists(),
				"The movie file is not found.");

		RatingWriter writer = createFilters(
				new RatingFileWriter(outputFilepath), maxUsers,
				minRatingsByMovie);

		for (String ratingFilepath : inputFilepaths) {
			ImportableRatingFile importableFile = getFile(ratingFilepath,
					movieFilepath, user);
			for (MynemoRating rating : importableFile) {
				writer.write(rating);
			}
		}

		writer.close();
	}

	/**
	 * Interposes the necessary filters before the last writer, according to the
	 * given parameters.
	 */
	private static RatingWriter createFilters(RatingWriter lastWriter,
			Optional<Integer> maxUsers, Optional<Integer> minRatingsByMovie) {

		RatingWriter nextWriter = lastWriter;

		if (minRatingsByMovie.isPresent()) {
			nextWriter = new MinRatingByMovieFilter(nextWriter,
					minRatingsByMovie.get());
		}
		if (maxUsers.isPresent()) {
			nextWriter = new MaxUserFilter(nextWriter, maxUsers.get());
		}

		return nextWriter;
	}

	/**
	 * <p>
	 * Returns a rating file that can parse the ratings contained in the given
	 * file.
	 * </p>
	 * <p>
	 * Throws an {link {@link UnsupportedOperationException} if the file cannot
	 * be parsed.
	 * </p>
	 */
	private static ImportableRatingFile getFile(String ratingFilepath,
			Optional<String> movieFilepath, Optional<String> user)
			throws IOException {

		if (MovieLensRatingFile.canImport(ratingFilepath)) {
			return new MovieLensRatingFile(ratingFilepath,
					user.or(DEFAULT_USER_ID));
		} else if (TenMillionRatingFile.canImport(ratingFilepath)) {
			if (!movieFilepath.isPresent()) {
				throw new UnsupportedOperationException(
						"Unable to convert the file \"" + ratingFilepath
								+ "\": the movie file is missing.");
			}
			return new TenMillionRatingFile(ratingFilepath, movieFilepath.get());
		} else if (MynemoRatingFile.canImport(ratingFilepath)) {
			return new MynemoRatingFile(ratingFilepath);
		}

		throw new UnsupportedOperationException("Unable to convert the file \""
				+ ratingFilepath + "\".");
	}

	/**
	 * Instantiates a new object. Private to prevents instantiation.
	 */
	private FileImporter() {
		throw new AssertionError();
	}
}
