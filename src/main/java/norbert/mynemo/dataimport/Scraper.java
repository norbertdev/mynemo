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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import norbert.mynemo.dataimport.scraping.CkMapping;
import norbert.mynemo.dataimport.scraping.CkRating;
import norbert.mynemo.dataimport.scraping.input.CkMappingFile;
import norbert.mynemo.dataimport.scraping.input.CkRatingFile;
import norbert.mynemo.dataimport.scraping.input.CkScraper;
import norbert.mynemo.dataimport.scraping.input.TciPageLoader;
import norbert.mynemo.dataimport.scraping.output.CkMappingWriter;
import norbert.mynemo.dataimport.scraping.output.CkRatingWriter;

import com.google.common.base.Optional;

/**
 * This scraper retrieves ratings and mappings from the CK web site.
 */
public class Scraper {

  private final List<CkMapping> loadedMappings;
  private final List<CkRating> loadedRatings;
  private final List<String> loadedUsers;
  private final String outputMappingFilepath;
  private final String outputRatingFilepath;

  public Scraper(String outputMappingFilepath, String outputRatingFilepath, String[] inputFilepaths)
      throws IOException {
    // check
    checkArgument(!new File(outputMappingFilepath).exists(), "The given output file for movies"
        + " must not exist.");
    checkArgument(!new File(outputRatingFilepath).exists(), "The given output file for ratings"
        + " must not exist.");
    for (String filepath : inputFilepaths) {
      checkArgument(new File(filepath).exists(), "The given input file of user must exist.");
    }

    // initialize
    this.outputMappingFilepath = outputMappingFilepath;
    this.outputRatingFilepath = outputRatingFilepath;

    loadedUsers = new ArrayList<>();
    loadedMappings = new ArrayList<>();
    loadedRatings = new ArrayList<>();

    // load the input files
    for (String filepath : inputFilepaths) {
      loadFile(filepath);
    }
  }

  /**
   * Returns the movies in the {@link #loadedRatings} field that does not have a mapping in the
   * {@link #loadedMappings} field.
   */
  private Collection<String> getMovieWithouMapping() {
    Set<String> moviesWithMapping = new HashSet<>();

    // put the existing mapping into a hash set for speed improvement
    for (CkMapping mapping : loadedMappings) {
      moviesWithMapping.add(mapping.getCkMovie());
    }

    // the result must be a set because the same movie may be added several times
    Set<String> result = new HashSet<>();

    // test the movie of each rating
    for (CkRating rating : loadedRatings) {
      String movie = rating.getMovie();
      if (!moviesWithMapping.contains(movie)) {
        result.add(movie);
      }
    }

    return result;
  }

  /**
   * Returns the users in the {@link #loadedUsers} field that does not have any rating in the
   * {@link #loadedRatings} field.
   */
  private List<String> getUserWithoutRating() {
    Set<String> usersWithRatings = new HashSet<>();

    // put the users with ratings into a hash set for speed improvement
    for (CkRating rating : loadedRatings) {
      usersWithRatings.add(rating.getUser());
    }

    List<String> result = new ArrayList<>();

    // test each user
    for (String user : loadedUsers) {
      if (!usersWithRatings.contains(user)) {
        result.add(user);
      }
    }

    return result;
  }

  /**
   * Loads the given file. The file can be:
   * <ul>
   * <li>a best TCI HTML page downloaded from the web site</li>
   * <li>a file containing some ratings already scraped</li>
   * <li>a file containing some movie mappings already scraped</li>
   * </ul>
   *
   * <p>
   * The {@link #loadedMappings}, {@link #loadedRatings} and {@link #loadedUsers} fields are filled
   * accordingly.
   * </p>
   *
   * <p>
   * If the file cannot be loaded, an exception is thrown.
   * </p>
   */
  private void loadFile(String filepath) throws IOException {
    if (TciPageLoader.canParse(filepath)) {

      loadedUsers.addAll(TciPageLoader.getUsers(filepath));

    } else if (CkRatingFile.canParse(filepath)) {

      for (CkRating rating : new CkRatingFile(filepath)) {
        loadedRatings.add(rating);
      }

    } else if (CkMappingFile.canParse(filepath)) {

      for (CkMapping mapping : new CkMappingFile(filepath)) {
        loadedMappings.add(mapping);
      }

    } else {

      throw new UnsupportedOperationException("The given input file cannot be loaded: \""
          + filepath + "\".");

    }
  }

  /**
   * Scrapes the user ratings and the movie mappings from the CK web site.
   */
  public void scrape() throws IOException, InterruptedException {

    scrapeRatings();

    // reload the ratings to include the newly scraped ratings
    loadedRatings.clear();
    loadFile(outputRatingFilepath);

    // scrape the movies without imdb id
    scrapeMappings();
  }

  /**
   * Scrapes the movies without IMDb id to get the mapping.
   */
  private void scrapeMappings() throws IOException, InterruptedException {
    try (CkMappingWriter movieMappingWriter = new CkMappingWriter(outputMappingFilepath)) {
      movieMappingWriter.writeAll(loadedMappings);
      movieMappingWriter.flush();
      for (String movie : getMovieWithouMapping()) {
        Optional<CkMapping> mapping = CkScraper.scrapeMovieMapping(movie);
        if (mapping.isPresent()) {
          movieMappingWriter.write(mapping.get());
          movieMappingWriter.flush();
        }
      }
    }
  }

  /**
   * Scrapes the ratings from the users without ratings.
   */
  private void scrapeRatings() throws IOException, InterruptedException {
    try (CkRatingWriter ratingWriter = new CkRatingWriter(outputRatingFilepath)) {
      ratingWriter.writeAll(loadedRatings);
      ratingWriter.flush();
      for (String user : getUserWithoutRating()) {
        ratingWriter.writeAll(CkScraper.scrapeRatingsOfUser(user));
        ratingWriter.flush();
      }
    }
  }
}
