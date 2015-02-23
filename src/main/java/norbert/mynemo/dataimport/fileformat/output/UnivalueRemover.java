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
package norbert.mynemo.dataimport.fileformat.output;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import norbert.mynemo.dataimport.fileformat.MynemoRating;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * This filter writes only the ratings of an user if at least two of its ratings have a different
 * values. In other words, it removes every user that rates with only one value.
 */
public class UnivalueRemover implements RatingWriter {

  /**
   * Returns <code>true</code> if the given ratings contains at least two different values. Returns
   * <code>false</code> otherwise.
   */
  private static boolean containDifferentValues(final Collection<MynemoRating> ratings) {
    final Set<String> set = new HashSet<String>();

    for (MynemoRating rating : ratings) {
      set.add(rating.getValue());
    }

    return 2 <= set.size();
  }

  private final RatingWriter nextWriter;
  /**
   * Unwritten ratings by user. Every rating given to the 'write' method is kept in this map until
   * the ratings become writable.
   */
  private final Multimap<String, MynemoRating> unwrittenRatings;
  /** A rating of a user in this set can be written to the next writer. */
  private final Set<String> writableUsers;

  public UnivalueRemover(final RatingWriter nextWriter) {
    checkNotNull(nextWriter);

    this.nextWriter = nextWriter;

    unwrittenRatings = HashMultimap.create();
    writableUsers = new HashSet<>();
  }

  @Override
  public void close() throws IOException {
    unwrittenRatings.clear();
    writableUsers.clear();

    nextWriter.close();
  }

  @Override
  public void write(final MynemoRating rating) throws IOException {
    checkNotNull(rating);

    final String user = rating.getUser();

    // write and return if possible
    if (writableUsers.contains(user)) {
      nextWriter.write(rating);
      return;
    }

    unwrittenRatings.put(user, rating);
    final Collection<MynemoRating> userRatings = unwrittenRatings.get(user);

    // check if the user has become writable
    if (containDifferentValues(userRatings)) {
      writableUsers.add(user);
      writeAll(userRatings);
      unwrittenRatings.removeAll(user);
    }
  }

  /**
   * Writes the given ratings into the next writer.
   */
  private void writeAll(Collection<MynemoRating> ratings) throws IOException {
    for (MynemoRating rating : ratings) {
      nextWriter.write(rating);
    }
  }
}
