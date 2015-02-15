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

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

/**
 * This class represent a movie with a title and a year. The data are retrieve from an remote
 * service. Thus some delay may appear the first time a getter is called.
 */
public class Movie {
  private static final String IMDB_URL_PREFIX = "http://www.imdb.com/title/";
  private static final String IMDB_URL_SUFFIX = "/";
  private static final int REQUEST_TIMEOUT = 10 * 1000;
  private static final String OMDB_URL_PREFIX = "http://www.omdbapi.com/?i=";
  private static final String OMDB_URL_SUFFIX = "&r=xml";
  private static final String USER_AGENT =
      "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:22.0) Gecko/20100101 Firefox/22.0";
  private boolean dataIsReady;
  private String imdbId;
  private String title;
  private String year;

  /**
   * Creates a movie identified by it's IMDb id. The IMDb id look like "tt4242424".
   */
  public Movie(String imdbId) {
    this.imdbId = imdbId;
  }

  private void fetchData() throws IOException {
    String url = generateUrl();
    Document document =
        Jsoup.connect(url).userAgent(USER_AGENT).timeout(REQUEST_TIMEOUT)
            .parser(Parser.xmlParser()).get();
    Element movieElement = document.getElementsByTag("movie").get(0);
    title = movieElement.attr("title");
    year = (movieElement.attr("year"));
    dataIsReady = true;
  }

  private String generateUrl() {
    String completeId = "tt" + ("0000000" + imdbId).substring(imdbId.length());
    return OMDB_URL_PREFIX + completeId + OMDB_URL_SUFFIX;
  }

  public String getTitle() throws IOException {
    if (!dataIsReady) {
      fetchData();
    }
    return title;
  }

  public String getImdbUrl() {
    return IMDB_URL_PREFIX + imdbId + IMDB_URL_SUFFIX;
  }

  public String getYear() throws IOException {
    if (!dataIsReady) {
      fetchData();
    }
    return year;
  }
}
