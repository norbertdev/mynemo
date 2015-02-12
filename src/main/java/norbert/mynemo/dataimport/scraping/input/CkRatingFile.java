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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import norbert.mynemo.dataimport.scraping.CkRating;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.google.common.collect.UnmodifiableIterator;

/**
 * This class represents a file containing ratings of CK web site. The format of this file is
 * defined in the {@link CkRating} class.
 */
public class CkRatingFile implements Iterable<CkRating> {
  private final class RatingIterator extends UnmodifiableIterator<CkRating> {
    private Iterator<CSVRecord> iterator = parser.iterator();

    @Override
    public boolean hasNext() {
      boolean result = iterator.hasNext();

      if (!result) {
        try {
          parser.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
      return result;
    }

    @Override
    public CkRating next() {
      return CkRating.createRating(iterator.next());
    }
  }

  /**
   * Returns <code>true</code> if the given file can be parsed, <code>false</code> otherwise.
   */
  public static boolean canParse(String filepath) {
    checkArgument(new File(filepath).exists(), "The file must exists.");

    try (CSVParser parser = CkRating.createParser(filepath)) {

      for (CSVRecord record : parser) {
        CkRating.createRating(record);
        break;
      }

    } catch (Exception e) {
      // the file is not parsable
      return false;
    }

    // everything seems right
    return true;
  }

  private final CSVParser parser;

  public CkRatingFile(String filepath) throws IOException {
    checkArgument(filepath != null, "The rating filepath must be not null.");
    checkArgument(new File(filepath).exists(), "The file must exist.");

    parser = CkRating.createParser(filepath);
  }

  @Override
  public Iterator<CkRating> iterator() {
    return new RatingIterator();
  }
}
