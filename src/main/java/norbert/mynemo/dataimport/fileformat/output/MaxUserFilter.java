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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import norbert.mynemo.dataimport.fileformat.MynemoRating;

/**
 * This filter writes only the ratings of a given number of users. While the number of maximum users
 * is not reach, all ratings are written to the next writer. As soon as the number of maximum
 * different users is reached:
 * <ul>
 * <li>a rating of an unknown user is not written</li>
 * <li>a rating of a known user is written</li>
 * </ul>
 */
public class MaxUserFilter implements RatingWriter {

  private final Set<String> knownUsers;
  private final int maximumUsers;
  private final RatingWriter nextWriter;

  public MaxUserFilter(RatingWriter nextWriter, int maximumUsers) {
    checkNotNull(nextWriter);
    checkArgument(0 < maximumUsers, "The maximum number of users must be at least 1.");

    this.maximumUsers = maximumUsers;
    this.nextWriter = nextWriter;
    knownUsers = new HashSet<>();
  }

  @Override
  public void close() throws IOException {
    nextWriter.close();
  }

  @Override
  public void write(MynemoRating rating) throws IOException {
    checkNotNull(rating);

    String user = rating.getUser();

    if (knownUsers.size() < maximumUsers) {
      knownUsers.add(user);
    }

    if (knownUsers.contains(user)) {
      nextWriter.write(rating);
    }
  }
}
