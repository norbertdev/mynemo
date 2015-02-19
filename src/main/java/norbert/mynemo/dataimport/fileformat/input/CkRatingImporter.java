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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Iterator;

import norbert.mynemo.dataimport.fileformat.MynemoRating;
import norbert.mynemo.dataimport.scraping.CkRating;
import norbert.mynemo.dataimport.scraping.input.CkMappingFile;
import norbert.mynemo.dataimport.scraping.input.CkRatingFile;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;

/**
 * This importer provides an iterator over the ratings contained in a file. It loads CK ratings from
 * a file and converts them into Mynemo ratings. Ratings of movies are converted from CK id to IMDb
 * ids. Ratings of movies without corresponding IMDb id are ignored.
 */
public class CkRatingImporter implements RatingImporter {
  /**
   * This iterator converts each given rating from the CK rating file into a Mynemo rating.
   */
  private class ConverterIterator extends UnmodifiableIterator<MynemoRating> {

    private final Iterator<CkRating> delegate;

    public ConverterIterator(Iterator<CkRating> delegateIterator) {
      delegate = delegateIterator;
    }

    @Override
    public boolean hasNext() {
      return delegate.hasNext();
    }

    @Override
    public MynemoRating next() {
      // retrieve the rating
      CkRating rating = delegate.next();

      // create the rating
      MynemoRating nextRating;
      nextRating =
          new MynemoRating(rating.getUser(), idConverter.convert(rating.getMovie()),
              rating.getValue());

      return nextRating;
    }
  }

  /**
   * This predicate is true only if the given rating is on movie that has a corresponding IMDb id.
   */
  private class HasCorrespondingImdbId implements Predicate<CkRating> {
    @Override
    public boolean apply(CkRating rating) {
      return idConverter.canConvert(rating.getMovie());
    }
  }

  public static boolean canImport(String ratingFilepath, String mappingFilepath) throws IOException {
    return CkRatingFile.canParse(ratingFilepath) && CkMappingFile.canParse(mappingFilepath);
  }

  private final CkIdConverter idConverter;
  private final String ratingFilepath;

  public CkRatingImporter(String ratingFilepath, String mappingFilepath) throws IOException {
    checkNotNull(ratingFilepath);
    checkNotNull(mappingFilepath);

    this.ratingFilepath = ratingFilepath;
    idConverter = new CkIdConverter(mappingFilepath);
  }

  /**
   * Returns an iterator on the ratings.
   */
  @Override
  public Iterator<MynemoRating> iterator() {
    Iterator<CkRating> ckRatingIterator;
    try {
      ckRatingIterator = new CkRatingFile(ratingFilepath).iterator();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return new ConverterIterator(Iterators.filter(ckRatingIterator, new HasCorrespondingImdbId()));
  }
}
