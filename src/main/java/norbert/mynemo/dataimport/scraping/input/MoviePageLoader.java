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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * This loader retrieves from a movie file the user ids. A movie file is an HTML page downloaded
 * from the CK web site.
 */
public class MoviePageLoader {

  private static final String A_TAG_NAME = "a";
  private static final String HREF_ATTRIBUTE_NAME = "href";
  private static final String KNOWN_FIRST_LINE = "<!DOCTYPE html>";
  private static final String MISSING_USER_SUFFIX_MARK = "//";
  private static final String SLASH_SEPARATOR = "/";
  private static final Pattern SPLITTER_PATTERN_FOR_USER = Pattern.compile(SLASH_SEPARATOR);
  private static final String TEXT_CHECK = "profile";
  private static final String USER_CLASS_LOCATOR = "fi_targettable_name";

  /**
   * Returns <code>true</code> if the given file can be parsed, <code>false</code> otherwise.
   */
  public static boolean canParse(String filepath) {
    checkArgument(new File(filepath).exists(), "The given file must exist.");

    // test the first line
    try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
      if (!KNOWN_FIRST_LINE.equals(reader.readLine())) {
        return false;
      }
    } catch (IOException e) {
      // the file cannot be parsed
      return false;
    }

    // try to find particular elements
    Elements elements;
    Document movieDocument;
    try {
      movieDocument = Jsoup.parse(new File(filepath), "UTF-8", "");
      elements = movieDocument.getElementsByClass(USER_CLASS_LOCATOR);
    } catch (IOException e) {
      // the file cannot be parsed
      return false;
    }

    return !elements.isEmpty();
  }

  /**
   * Returns the user ids found in the given document.
   */
  private static List<String> extractUserIds(Document movieDocument) {
    List<String> result = new ArrayList<>();

    Elements elements = movieDocument.getElementsByClass(USER_CLASS_LOCATOR);
    for (Element element : elements) {
      // get the first <a> tag
      Element link = element.getElementsByTag(A_TAG_NAME).get(0);
      // get the href value
      String userUrl = link.attr(HREF_ATTRIBUTE_NAME);
      // sometimes, the user is missing, ignore it
      if (userUrl.endsWith(MISSING_USER_SUFFIX_MARK)) {
        continue;
      }
      // extract the user id
      String[] strings = SPLITTER_PATTERN_FOR_USER.split(userUrl);
      checkState(2 <= strings.length);
      checkState(TEXT_CHECK.equals(strings[strings.length - 2]));
      String userName = strings[strings.length - 1];
      // save user id
      result.add(userName);
    }

    return result;
  }

  /**
   * Returns the user ids found in the given file.
   */
  public static Collection<? extends String> getUsers(String filepath) throws IOException {
    checkArgument(new File(filepath).exists(), "The file must exists.");

    Document movieDocument = Jsoup.parse(new File(filepath), "UTF-8", "");

    return extractUserIds(movieDocument);
  }
}
