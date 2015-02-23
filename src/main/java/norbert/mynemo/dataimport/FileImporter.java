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
package norbert.mynemo.dataimport;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import norbert.mynemo.dataimport.fileformat.MynemoRating;
import norbert.mynemo.dataimport.fileformat.input.CkRatingImporter;
import norbert.mynemo.dataimport.fileformat.input.MovieLensRatingImporter;
import norbert.mynemo.dataimport.fileformat.input.MynemoRatingImporter;
import norbert.mynemo.dataimport.fileformat.input.RatingImporter;
import norbert.mynemo.dataimport.fileformat.input.TenMillionRatingImporter;
import norbert.mynemo.dataimport.fileformat.output.DuplicateRemover;
import norbert.mynemo.dataimport.fileformat.output.MaxNeighborUserFilter;
import norbert.mynemo.dataimport.fileformat.output.MaxUserFilter;
import norbert.mynemo.dataimport.fileformat.output.MinRatingByMovieFilter;
import norbert.mynemo.dataimport.fileformat.output.RatingFileWriter;
import norbert.mynemo.dataimport.fileformat.output.RatingWriter;
import norbert.mynemo.dataimport.fileformat.output.ScaleValueWriter;
import norbert.mynemo.dataimport.fileformat.output.UnivalueRemover;
import norbert.mynemo.dataimport.fileformat.output.UserSimilarityType;

import com.google.common.base.Optional;

/**
 * This importer can merge, convert and filter ratings from files. It produces one output file from
 * several input files. The format of the output file is the Mynemo file format. The format of an
 * input file is automatically detected. The ratings can be filtered.
 */
public class FileImporter {

  private static final String DEFAULT_USER_ID = Integer.toString(Integer.MAX_VALUE);

  /**
   * Converts the given rating files to the Mynemo format. Handles the files generated from
   * MovieLens, and the 10 million ratings from a MovieLens data set. If the latter is amongst the
   * rating files, a movie file must be provided. Handles also the files generated by the scraping
   * of CK. CK rating files need a mapping file in be imported.
   *
   * <p>
   * The mapping files must contains the equivalence between MovieLens ids and IMDb ids, or the
   * equivalence between CK identifiers and IMDb ids. The files are automatically recognized.
   *
   * <p>
   * If an input file contains ratings without user id, the given <code>user</code> is used. If
   * <code>user</code> is absent, then a default user id is used. No check is done to find if the id
   * already exists.
   *
   * <p>
   * The output file must not exist. At least one existing rating file must be provided.
   *
   * @param outputFilepath the file where the imported ratings are written
   * @param inputFilepaths the rating files to convert
   * @param movieFilepath the file containing the equivalences between ids
   * @param user the user id used for the input ratings without user
   * @param maxUsers maximum number of users, the output file will contain the ratings of at most
   *        this number of users
   * @param minRatingsByMovie minimum ratings by movie, the output file won't contain movies that
   *        have less than this number of ratings
   * @param similarityType type of similarity used to find the nearest users of the target user
   */
  public static void convert(String outputFilepath, Collection<String> inputFilepaths,
      Collection<String> movieFilepath, Optional<String> user, Optional<Integer> maxUsers,
      Optional<Integer> minRatingsByMovie, Optional<UserSimilarityType> similarityType)
      throws IOException {
    checkNotNull(outputFilepath);
    checkNotNull(inputFilepaths);
    checkArgument(!inputFilepaths.isEmpty(), "At least one input file must be given.");
    checkArgument(!new File(outputFilepath).exists(), "The output file must not exist.");
    for (String filepath : inputFilepaths) {
      checkArgument(new File(filepath).exists(), "The input file must exist.");
    }
    for (String filepath : movieFilepath) {
      checkArgument(new File(filepath).exists(), "The movie file must exist.");
    }

    RatingWriter writer =
        createFilters(new RatingFileWriter(outputFilepath), maxUsers, minRatingsByMovie,
            similarityType, user);

    for (String ratingFilepath : inputFilepaths) {
      RatingImporter importableFile = getFile(ratingFilepath, movieFilepath, user);
      for (MynemoRating rating : importableFile) {
        writer.write(rating);
      }
    }

    writer.close();
  }

  /**
   * Interposes the necessary filters before the last writer, according to the given parameters.
   */
  private static RatingWriter createFilters(RatingWriter lastWriter, Optional<Integer> maxUsers,
      Optional<Integer> minRatingsByMovie, Optional<UserSimilarityType> similarityType,
      Optional<String> targetUser) {

    RatingWriter nextWriter = lastWriter;

    if (minRatingsByMovie.isPresent()) {
      nextWriter = new MinRatingByMovieFilter(nextWriter, minRatingsByMovie.get());
    }
    if (maxUsers.isPresent()) {
      if (similarityType.isPresent()) {
        nextWriter =
            new MaxNeighborUserFilter(nextWriter, targetUser.get(), maxUsers.get(),
                similarityType.get());
      } else {
        nextWriter = new MaxUserFilter(nextWriter, maxUsers.get());
      }
    }
    nextWriter = new ScaleValueWriter(nextWriter);
    nextWriter = new DuplicateRemover(nextWriter);
    nextWriter = new UnivalueRemover(nextWriter);

    return nextWriter;
  }

  /**
   * Returns a rating file that can parse the ratings contained in the given file.
   *
   * @throws UnsupportedOperationException if the file cannot be parsed
   */
  private static RatingImporter getFile(String ratingFilepath, Collection<String> mappingFilepaths,
      Optional<String> user) throws IOException {

    for (String mappingFilepath : mappingFilepaths) {
      if (CkRatingImporter.canImport(ratingFilepath, mappingFilepath)) {
        return new CkRatingImporter(ratingFilepath, mappingFilepath);
      }
      if (TenMillionRatingImporter.canImport(ratingFilepath, mappingFilepath)) {
        return new TenMillionRatingImporter(ratingFilepath, mappingFilepath);
      }
    }

    if (MovieLensRatingImporter.canImport(ratingFilepath)) {
      return new MovieLensRatingImporter(ratingFilepath, user.or(DEFAULT_USER_ID));
    }

    // the Mynemo rating file must stay last because it does not use headers, so the parser can
    // parse several file formats.
    if (MynemoRatingImporter.canImport(ratingFilepath)) {
      return new MynemoRatingImporter(ratingFilepath);
    }

    throw new UnsupportedOperationException("Unable to convert the file \"" + ratingFilepath
        + "\".");
  }

  /**
   * Instantiates a new object. Private to prevents instantiation.
   */
  private FileImporter() {
    throw new AssertionError();
  }
}
