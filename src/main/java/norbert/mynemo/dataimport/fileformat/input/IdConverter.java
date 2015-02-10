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
package norbert.mynemo.dataimport.fileformat.input;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * This class provides a conversion from the MovieLens ids of movies to the IMDb
 * ids. This information is extracted from a file that can be exported from the
 * MovieLens web site.
 */
public class IdConverter {
	private static final int IMDB_MOVIE_ID_INDEX = 3;
	private static final int MOVIELENS_MOVIE_ID_INDEX = 0;
	private static final int RECORD_SIZE = 5;
	/**
	 * A map containing a MovieLens id as key, and its corresponding IMDb id as
	 * value.
	 */
	private final Map<String, String> ids;

	/**
	 * <p>
	 * Loads the movie file.
	 * </p>
	 * <p>
	 * The columns of the movie file are:
	 * <ol>
	 * <li>MovieLens id of the movie</li>
	 * <li>rating</li>
	 * <li>average</li>
	 * <li>IMDb id of the movie</li>
	 * <li>title and year</li>
	 * </ol>
	 * </p>
	 *
	 * @param movieFilepath
	 *            the file that contains the movies
	 */
	public IdConverter(String movieFilepath) throws IOException {
		checkArgument(new File(movieFilepath).exists(), "The movie file must"
				+ " exist.");

		CSVParser parser = new CSVParser(new CleanRatingsReader(
				new BufferedReader(new FileReader(movieFilepath))),
				CSVFormat.MYSQL);

		ids = new HashMap<>();
		for (CSVRecord record : parser) {
			if (record.size() != RECORD_SIZE) {
				parser.close();
				throw new IllegalStateException(
						"Error: unable to parse the movie file \""
								+ movieFilepath
								+ "\". A list of five tab separated values"
								+ " is expected. Approximate line number: "
								+ record.getRecordNumber());
			}
			ids.put(record.get(MOVIELENS_MOVIE_ID_INDEX),
					record.get(IMDB_MOVIE_ID_INDEX));
		}

		parser.close();
	}

	/**
	 * Returns <code>true</code> if a corresponding IMDb movie id exists.
	 * Returns <code>false</code> otherwise.
	 *
	 * @param movielensId
	 *            an id of a movie from MovieLens
	 * @return <code>true</code> if a correspondence exists, <code>false</code>
	 *         otherwise
	 */
	public boolean canConvert(String movielensId) {
		return ids.containsKey(movielensId);
	}

	/**
	 * <p>
	 * Returns the corresponding IMDb id of the given MovieLens id of a movie.
	 * </p>
	 * <p>
	 * Throws an exception if the given MovieLens id has no corresponding IMDb
	 * id.
	 * </p>
	 *
	 * @param movielensId
	 *            an id of a movie from MovieLens
	 * @return the corresponding IMDb id
	 */
	public String convert(String movielensId) {
		checkArgument(ids.containsKey(movielensId), "The MovieLens id of a"
				+ " movie must have a corresponding imdb id.");

		return ids.get(movielensId);
	}
}
