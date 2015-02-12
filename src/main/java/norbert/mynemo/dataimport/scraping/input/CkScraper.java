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

import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
 * This scraper use the CK web site to provides the ratings of a given user and the id mapping of a
 * given movie.
 */
public class CkScraper {
  private static final String CK_PART1 = "http://www.criti";
  private static final String CK_PART2 = "cker.com/";
  private static final int DELAY_DELTA = 5 * 1000;
  private static final int DELAY_MINIMUM = 1 * 1000;
  private static final String HREF_ATTRIBUTE_NAME = "href";
  private static final String IMDB_ID_PREFIX = "tt";
  private static final String IMDB_LOCATOR = "fi_info_imdb";
  private static final String MAPPING_URL_PREFIX = CK_PART1 + CK_PART2 + "film/";
  private static final String MAPPING_URL_SUFFIX = "/";
  private static final String MOVIE_IN_RATING_LOCATOR = "fl_name";
  private static final String NEXT_PAGE_USERRATING_LOCATOR = "fl_rankingsnav_current";
  private static final Random RANDOM = new Random();
  private static final String RATING_URL_PREFIX = CK_PART1 + CK_PART2 + "?fl&view=oth&user=";
  private static final String RATING_URL_SUFFIX = "&filter=&page=1";
  /** Duration between two requests. */
  private static final int REQUEST_TIMEOUT = 10 * 1000;
  private static final Pattern SLASH_SPLITTER_PATTERN = Pattern.compile(MAPPING_URL_SUFFIX);
  private static final String USER_AGENT =
      "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:22.0) Gecko/20100101 Firefox/22.0";

  /**
   * Returns a number of milliseconds M such as: {@link #DELAY_MINIMUM} < M < (
   * {@link #DELAY_MINIMUM} +{@link #DELAY_DELTA})
   */
  private static long getRandomDelay() {
    int result = RANDOM.nextInt(DELAY_DELTA) + DELAY_MINIMUM;
    return result;
  }

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

  private static Optional<String> parseUserRatingNextPage(Document ratingPage) {
    Elements currentPage = ratingPage.getElementsByClass(NEXT_PAGE_USERRATING_LOCATOR);
    Element span = currentPage.first().nextElementSibling();
    if (span == null) {
      // the last page does not have a next page
      return Optional.absent();
    }

    return Optional.of(span.child(0).attr(HREF_ATTRIBUTE_NAME));
  }

  /**
   * Returns the scraped IMDb id of the given CK movie. Returns nothing if the IMDb id can't be
   * found.
   */
  public static Optional<CkMapping> scrapeMovieMapping(String ckMovie) throws IOException,
      InterruptedException {
    String url = MAPPING_URL_PREFIX + ckMovie + MAPPING_URL_SUFFIX;
    Document movieDocument =
        Jsoup.connect(url).userAgent(USER_AGENT).timeout(REQUEST_TIMEOUT).get();
    movieDocument.getElementById(IMDB_LOCATOR);
    Element paragraph = movieDocument.getElementById(IMDB_LOCATOR);
    // some movies do not have an imdb link
    if (paragraph == null) {
      Thread.sleep(getRandomDelay());
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
    Thread.sleep(getRandomDelay());

    return Optional.of(new CkMapping(ckMovie, imdbMovie));
  }

  /**
   * Returns the scraped ratings of the given user.
   */
  public static List<CkRating> scrapeRatingsOfUser(String user) throws IOException,
      InterruptedException {
    List<CkRating> result = new ArrayList<>();
    List<CkRatingBuilder> ratingBuilder = new ArrayList<>();

    Optional<String> nextPageUrl = Optional.of(RATING_URL_PREFIX + user + RATING_URL_SUFFIX);
    do {
      Document ratingDocument =
          Jsoup.connect(nextPageUrl.get()).userAgent(USER_AGENT).timeout(REQUEST_TIMEOUT).get();
      ratingBuilder.addAll(parseRatings(ratingDocument));
      nextPageUrl = parseUserRatingNextPage(ratingDocument);
      Thread.sleep(getRandomDelay());
    } while (nextPageUrl.isPresent());

    for (CkRatingBuilder builder : ratingBuilder) {
      result.add(builder.createRating(user));
    }

    return result;
  }
}
