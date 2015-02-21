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
package norbert.mynemo.dataimport.fileformat.input;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import norbert.mynemo.dataimport.fileformat.MynemoRating;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.google.common.collect.UnmodifiableIterator;

/**
 * This importer can load a file containing Mynamo ratings. The format of this file is defined by
 * the {@link MynemoRating} class.
 */
public class MynemoRatingImporter implements RatingImporter {

  private final class RatingIterator extends UnmodifiableIterator<MynemoRating> {
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
    public MynemoRating next() {
      return MynemoRating.createRating(iterator.next());
    }
  }

  /**
   * Returns <code>true</code> if the given file can be parsed, <code>false</code> otherwise.
   */
  public static boolean canImport(String ratingFilepath) {
    checkArgument(new File(ratingFilepath).exists(), "The file must exists.");

    try (CSVParser parser = MynemoRating.createParser(ratingFilepath)) {
      Iterator<CSVRecord> iterator = parser.iterator();
      if (!iterator.hasNext()) {
        // the file contains zero rating, but is nonetheless parsable
        return true;
      }
      MynemoRating.createRating(iterator.next());
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  private final CSVParser parser;

  public MynemoRatingImporter(String filepath) throws IOException {
    checkArgument(filepath != null, "The rating filepath must be not null.");
    checkArgument(new File(filepath).exists(), "The file must exist.");

    parser = MynemoRating.createParser(filepath);
  }

  @Override
  public Iterator<MynemoRating> iterator() {
    return new RatingIterator();
  }
}
