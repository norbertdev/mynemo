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
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Pattern;

import norbert.mynemo.dataimport.scraping.CkMapping;
import norbert.mynemo.dataimport.scraping.CkRating;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private static final String KNOWN_STRING_AFTER_MOVIE = "rating";
  private static final String KNOWN_STRING_BEFORE_MOVIE = "film";
  private static final String MAPPING_URL_PREFIX = CK_PART1 + CK_PART2 + "film/";
  private static final String MAPPING_URL_SUFFIX = "/";
  /** Size in byte of the body by JSoup. */
  private static final int MAX_BODY_SIZE = (int) (20 * Math.pow(2, 20));
  /** Do not download ratings from user with more than this number of ratings. */
  private static int MAX_RATINGS_ALLOWED_BY_USER = 2048;
  private static final int MIN_RATINGS_ALLOWED_BY_USER = 1;
  private static final int MOVIE_ID_INDEX = 4;
  private static final String MOVIE_IN_RATING_LOCATOR = "film";
  private static final String MOVIE_IN_RATING_LOCATOR_ALTERNATE = "fl_name";
  private static final String MOVIELINK_IN_RATING_LOCATOR = "filmlink";
  private static final String NEXT_PAGE_USERRATING_LOCATOR = "fl_rankingsnav_current";
  private static final String NUM_RATING_LOCATOR = "up_userlevel";
  private static final Random RANDOM = new Random();
  private static final String RATING_URL_PREFIX = CK_PART1 + CK_PART2 + "resource/";
  private static final String RATING_URL_PREFIX_ALTERNATE = CK_PART1 + CK_PART2
      + "?fl&view=oth&user=";
  private static final String RATING_URL_SUFFIX = "/rankings.xml";
  private static final String RATING_URL_SUFFIX_ALTERNATE = "&filter=&page=1";
  private static final String RATING_VALUE_LOCATOR = "score";
  private static final String REFERER = CK_PART1 + CK_PART2;
  /** Duration between two requests. */
  private static final int REQUEST_TIMEOUT = 30 * 1000;
  private static final String RSS_LOCATOR = "up_rsslink";
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
   * Returns the ratings found in the given document. The document is an XML list of all ratings of
   * the given user.
   *
   * @see #parseRatingsAlternateOnePage(Document, String) for a similar method that use a HTML
   *      document instead
   */
  private static List<CkRating> parseRatings(final Document ratingDocument, final String user) {
    final List<CkRating> result = new ArrayList<>();

    for (Element element : ratingDocument.getElementsByTag(MOVIE_IN_RATING_LOCATOR)) {
      final String movieUrl = element.getElementsByTag(MOVIELINK_IN_RATING_LOCATOR).get(0).text();
      final String[] strings = SLASH_SPLITTER_PATTERN.split(movieUrl);

      checkState(KNOWN_STRING_BEFORE_MOVIE.equals(strings[MOVIE_ID_INDEX - 1]));
      checkState(KNOWN_STRING_AFTER_MOVIE.equals(strings[MOVIE_ID_INDEX + 1]));

      final String movie = strings[MOVIE_ID_INDEX];
      final String value = element.getElementsByTag(RATING_VALUE_LOCATOR).get(0).text();
      result.add(new CkRating(user, movie, value));
    }

    return result;
  }

  /**
   * Returns the list of ratings found in the given document. The document is a HTML paginated list
   * of some ratings of the given user.
   *
   * @see #parseRatings(Document, String) for a similar method that use an XML document instead
   */
  private static List<CkRating> parseRatingsAlternateOnePage(final Document ratingDocument,
      final String user) {
    final List<CkRating> result = new ArrayList<>();

    for (Element element : ratingDocument.getElementsByClass(MOVIE_IN_RATING_LOCATOR_ALTERNATE)) {
      String url = element.child(0).attr(HREF_ATTRIBUTE_NAME);
      String[] strings = SLASH_SPLITTER_PATTERN.split(url);
      String movie = strings[strings.length - 1];
      String value = element.nextElementSibling().text();
      result.add(new CkRating(user, movie, value));
    }

    return result;
  }

  /**
   * Returns the user id number found in the given document.
   */
  private static Optional<String> parseUserIdNumber(final Document userDocument) {
    final Elements rssElements = userDocument.getElementsByClass(RSS_LOCATOR);

    // the rss element is missing
    if (rssElements.isEmpty()) {
      return Optional.absent();
    }

    final Element link = rssElements.get(0).child(0);
    final String recentUrl = link.attr(HREF_ATTRIBUTE_NAME);
    final String[] strings = SLASH_SPLITTER_PATTERN.split(recentUrl);
    final String suffixedResult = strings[strings.length - 1];
    final String result = suffixedResult.substring(0, suffixedResult.length() - ".xml".length());

    return Optional.of(result);
  }

  /**
   * Returns the URL of the next rating page. If there is no next page, returns nothing.
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

  private final Logger LOGGER = LoggerFactory.getLogger(CkScraper.class);

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
   * Retrieves and returns a document from the given URL. A pause is done. If no response is
   * received before the timeout, the document is asked again. Thus, this method can hang for a
   * while.
   *
   * <p>
   * The document might be incomplete because JSoup silently truncates the input if the body size is
   * too long.
   */
  private Document fetchDocument(final String url, final Parser parser) throws IOException,
      InterruptedException {

    final String userAgent = userAgents.get(RANDOM.nextInt(userAgents.size()));
    Optional<Document> result = Optional.absent();

    while (!result.isPresent()) {
      try {
        result =
            Optional.of(Jsoup.connect(url).userAgent(userAgent).referrer(REFERER)
                .timeout(REQUEST_TIMEOUT).maxBodySize(MAX_BODY_SIZE).parser(parser).get());
      } catch (SocketTimeoutException e) {
        LOGGER.warn("Socket timeout while scraping. Retry.", e);
      } catch (HttpStatusException e) {
        if (e.getStatusCode() == 503) {
          LOGGER.warn("HTTP error 503 while scraping. Pause and retry.", e);
          Thread.sleep(DELAY_BIG);
        } else {
          LOGGER.warn("HTTP error " + e.getStatusCode() + " while scraping. Stop.", e);
          throw e;
        }
      }
    }

    Thread.sleep(getRandomDelay());

    if (RANDOM.nextInt(BIG_DELAY_FREQUENCY) == 0) {
      Thread.sleep(DELAY_BIG);
    }

    return result.get();
  }

  /**
   * Fetches a page and parses it with a HTML parser.
   *
   * @see #fetchXmlDocument(String)
   */
  private Document fetchHtmlDocument(final String url) throws IOException, InterruptedException {
    return fetchDocument(url, Parser.htmlParser());
  }

  /**
   * Download and returns the document of the movie.
   */
  private Document fetchMovieDocument(final String ckMovie) throws IOException,
      InterruptedException {
    return fetchHtmlDocument(MAPPING_URL_PREFIX + ckMovie + MAPPING_URL_SUFFIX);
  }

  private Document fetchRatingDocument(final String url) throws IOException, InterruptedException {
    return fetchXmlDocument(url);
  }

  /**
   * Download and returns the document of the user.
   */
  private Document fetchUserDocument(final String user) throws InterruptedException, IOException {
    return fetchHtmlDocument(USER_URL_PREFIX + user + USER_URL_SUFFIX);
  }

  /**
   * Fetches a page and parses it with an XML parser.
   *
   * @see #fetchHtmlDocument(String)
   */
  private Document fetchXmlDocument(final String url) throws IOException, InterruptedException {
    return fetchDocument(url, Parser.xmlParser());
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
   * Returns the scraped ratings of the given user. This scraping method is slower than the
   * alternate {@link #parseRatings(Document, String)} method.
   */
  private List<CkRating> scrapeRatingsAlternate(final String user) throws IOException,
      InterruptedException {
    final List<CkRating> result = new ArrayList<>();

    Optional<String> nextPageUrl =
        Optional.of(RATING_URL_PREFIX_ALTERNATE + user + RATING_URL_SUFFIX_ALTERNATE);
    do {
      final Document ratingDocument = fetchHtmlDocument(nextPageUrl.get());
      result.addAll(parseRatingsAlternateOnePage(ratingDocument, user));
      nextPageUrl = parseUserRatingNextPage(ratingDocument);
    } while (nextPageUrl.isPresent());

    return result;
  }

  /**
   * Returns the scraped ratings of the given user. The returned collection can be empty. If a users
   * has too many rating, its ratings are not scraped.
   */
  public List<CkRating> scrapeRatingsOfUser(String user) throws IOException, InterruptedException {

    final Document userDocument = fetchUserDocument(user);
    final Integer numRatings = parseNumRatingOfUser(userDocument).or(0);

    // check the number of ratings for the user
    if (MAX_RATINGS_ALLOWED_BY_USER < numRatings) {
      LOGGER.info("The user '{}' has to many ratings. Ignore it.", user);
      return Collections.emptyList();
    } else if (numRatings < MIN_RATINGS_ALLOWED_BY_USER) {
      LOGGER.info("The user '{}' has not enough ratings. Ignore it.", user);
      return Collections.emptyList();
    }

    // retrieve the ratings
    final Optional<String> userIdNumber = parseUserIdNumber(userDocument);

    // select the method to retrieve the ratings
    if (userIdNumber.isPresent()) {
      // fast method to scrape ratings
      final String ratingUrl = RATING_URL_PREFIX + userIdNumber.get() + RATING_URL_SUFFIX;
      final Document ratingDocument = fetchRatingDocument(ratingUrl);
      return parseRatings(ratingDocument, user);
    } else {
      // alternate method to scrape ratings
      return scrapeRatingsAlternate(user);
    }
  }
}
