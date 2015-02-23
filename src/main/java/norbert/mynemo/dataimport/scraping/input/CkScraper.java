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
package norbert.mynemo.dataimport.scraping.input;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Pattern;

import norbert.mynemo.dataimport.scraping.CkMapping;
import norbert.mynemo.dataimport.scraping.CkRating;
import norbert.mynemo.dataimport.scraping.CkRatingBuilder;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.common.base.Optional;

/**
 * This scraper uses the CK web site to provide the ratings of a given user and the id mapping of a
 * given movie.
 */
public class CkScraper {
  /**
   * Frequency of the bigger delay. Set the average number of requests before the next longer pause.
   */
  private static final int BIG_DELAY_FREQUENCY = 20;
  private static final String CK_PART1 = "http://www.criti";
  private static final String CK_PART2 = "cker.com/";
  /** Time in millisecond. Sometimes, a bigger pause is done. */
  private static final long DELAY_BIG = 15 * 1000;
  private static final int DELAY_DELTA = 1 * 1000;
  /** Time in millisecond. Minimum pause duration between two requests. */
  private static final int DELAY_MINIMUM = 10 * 1000;
  private static final String HREF_ATTRIBUTE_NAME = "href";
  private static final String IMDB_ID_PREFIX = "tt";
  private static final String IMDB_LOCATOR = "fi_info_imdb";
  private static final String MAPPING_URL_PREFIX = CK_PART1 + CK_PART2 + "film/";
  private static final String MAPPING_URL_SUFFIX = "/";
  /** Do not download ratings from user with more than this number of ratings. */
  private static int MAX_RATINGS_ALLOWED_BY_USER = 2048;
  private static final int MIN_RATINGS_ALLOWED_BY_USER = 1;
  private static final String MOVIE_IN_RATING_LOCATOR = "fl_name";
  private static final String NEXT_PAGE_USERRATING_LOCATOR = "fl_rankingsnav_current";
  private static final String NUM_RATING_LOCATOR = "up_userlevel";
  private static final Random RANDOM = new Random();
  private static final String RATING_URL_PREFIX = CK_PART1 + CK_PART2 + "?fl&view=oth&user=";
  private static final String RATING_URL_SUFFIX = "&filter=&page=1";
  private static final String REFERER = CK_PART1 + CK_PART2;
  /** Duration between two requests. */
  private static final int REQUEST_TIMEOUT = 30 * 1000;
  private static final Pattern SLASH_SPLITTER_PATTERN = Pattern.compile(MAPPING_URL_SUFFIX);
  private static final String USER_URL_PREFIX = CK_PART1 + CK_PART2 + "profile/";
  private static final String USER_URL_SUFFIX = "";

  /**
   * Returns a number of milliseconds M such as: {@link #DELAY_MINIMUM} < M < (
   * {@link #DELAY_MINIMUM} + {@link #DELAY_DELTA})
   */
  private static long getRandomDelay() {
    return RANDOM.nextInt(DELAY_DELTA) + DELAY_MINIMUM;
  }

  /**
   * Returns the number of ratings from the given document. Returns nothing if the number of ratings
   * cannot be determined.
   */
  private static Optional<Integer> parseNumRatingOfUser(final Document userDocument) {
    final Element element = userDocument.getElementById(NUM_RATING_LOCATOR);
    if (element == null) {
      return Optional.absent();
    }
    final String text = element.text();
    final Scanner scanner = new Scanner(text);
    final int result = scanner.useDelimiter("[^0-9]+").nextInt();
    scanner.close();

    return Optional.of(result);
  }

  /**
   * Returns the list of ratings found in the given document.
   */
  private static List<CkRatingBuilder> parseRatings(Document ratingDocument) {
    List<CkRatingBuilder> result = new ArrayList<>();

    for (Element element : ratingDocument.getElementsByClass(MOVIE_IN_RATING_LOCATOR)) {
      String url = element.child(0).attr(HREF_ATTRIBUTE_NAME);
      String[] strings = SLASH_SPLITTER_PATTERN.split(url);
      String movie = strings[strings.length - 1];
      String value = element.nextElementSibling().text();
      result.add(new CkRatingBuilder(movie, value));
    }

    return result;
  }

  /**
   * Returns the URL of the next page. If there is no next page, return nothing.
   */
  private static Optional<String> parseUserRatingNextPage(Document ratingPage) {
    Elements currentPage = ratingPage.getElementsByClass(NEXT_PAGE_USERRATING_LOCATOR);
    Element span = currentPage.first().nextElementSibling();
    if (span == null) {
      // the last page does not have a next page
      return Optional.absent();
    }

    return Optional.of(span.child(0).attr(HREF_ATTRIBUTE_NAME));
  }

  /** User agents used by the connection. */
  private final List<String> userAgents;

  /**
   * Creates a CK scraper that will used the given user agents its connections.
   */
  public CkScraper(List<String> userAgents) {
    checkArgument(!userAgents.isEmpty(), "At least one user agent must be given.");

    this.userAgents = userAgents;
  }

  /**
   * Retrieves and returns a document from the given URL. A pause is done.
   */
  private Document fetchDocument(final String url) throws IOException, InterruptedException {

    final String userAgent = userAgents.get(RANDOM.nextInt(userAgents.size()));
    final Document result =
        Jsoup.connect(url).userAgent(userAgent).referrer(REFERER).timeout(REQUEST_TIMEOUT).get();

    Thread.sleep(getRandomDelay());

    if (RANDOM.nextInt(BIG_DELAY_FREQUENCY) == 0) {
      Thread.sleep(DELAY_BIG);
    }

    return result;
  }

  /**
   * Download and returns the document of the movie.
   */
  private Document fetchMovieDocument(final String ckMovie) throws IOException,
      InterruptedException {
    return fetchDocument(MAPPING_URL_PREFIX + ckMovie + MAPPING_URL_SUFFIX);
  }

  /**
   * Download and returns the document of the user.
   */
  private Document fetchUserDocument(final String user) throws InterruptedException, IOException {
    return fetchDocument(USER_URL_PREFIX + user + USER_URL_SUFFIX);
  }

  /**
   * Returns the scraped IMDb id of the given CK movie. Returns nothing if the IMDb id can't be
   * found.
   */
  public Optional<CkMapping> scrapeMovieMapping(String ckMovie) throws IOException,
      InterruptedException {

    Document movieDocument = fetchMovieDocument(ckMovie);
    movieDocument.getElementById(IMDB_LOCATOR);
    Element paragraph = movieDocument.getElementById(IMDB_LOCATOR);
    // some movies do not have an imdb link
    if (paragraph == null) {
      return Optional.absent();
    }

    Element paragraphSon = paragraph.child(0);
    String imdbUrl = paragraphSon.attr(HREF_ATTRIBUTE_NAME);
    String[] strings = SLASH_SPLITTER_PATTERN.split(imdbUrl);
    String prefixedImdbMovie = strings[strings.length - 1];
    // check
    checkState(prefixedImdbMovie.startsWith(IMDB_ID_PREFIX));
    // strip the prefix "tt"
    String imdbMovie = prefixedImdbMovie.substring(IMDB_ID_PREFIX.length());

    return Optional.of(new CkMapping(ckMovie, imdbMovie));
  }

  /**
   * Returns the scraped ratings of the given user. The returned collection can be empty. If a users
   * has too many rating, its ratings are not scraped.
   */
  public List<CkRating> scrapeRatingsOfUser(String user) throws IOException, InterruptedException {

    Integer numRatings = parseNumRatingOfUser(fetchUserDocument(user)).or(0);

    // check the number of ratings for the user
    if (MAX_RATINGS_ALLOWED_BY_USER < numRatings) {
      return Collections.emptyList();
    } else if (numRatings < MIN_RATINGS_ALLOWED_BY_USER) {
      return Collections.emptyList();
    }

    // retrieve the ratings of all pages
    List<CkRatingBuilder> ratingBuilder = new ArrayList<>();
    Optional<String> nextPageUrl = Optional.of(RATING_URL_PREFIX + user + RATING_URL_SUFFIX);
    do {
      Document ratingDocument = fetchDocument(nextPageUrl.get());
      ratingBuilder.addAll(parseRatings(ratingDocument));
      nextPageUrl = parseUserRatingNextPage(ratingDocument);
    } while (nextPageUrl.isPresent());

    // create the CK ratings
    List<CkRating> result = new ArrayList<>();
    for (CkRatingBuilder builder : ratingBuilder) {
      result.add(builder.createRating(user));
    }

    return result;
  }
}
