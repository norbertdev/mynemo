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
 * is not reached, all ratings are written to the next writer. As soon as the maximum is reached:
 * <ul>
 * <li>a rating of an unknown user is not written
 * <li>a rating of a known user is written
 * </ul>
 */
public class MaxUserFilter implements RatingWriter {

  private final int maxUsers;
  private final RatingWriter nextWriter;
  private final Set<String> writableUsers;

  public MaxUserFilter(RatingWriter nextWriter, int maxUsers) {
    checkNotNull(nextWriter);
    checkArgument(1 <= maxUsers, "The maximum number of users must be at least 1.");

    this.maxUsers = maxUsers;
    this.nextWriter = nextWriter;

    writableUsers = new HashSet<>();
  }

  @Override
  public void close() throws IOException {
    writableUsers.clear();
    nextWriter.close();
  }

  @Override
  public void write(MynemoRating rating) throws IOException {
    checkNotNull(rating);

    String user = rating.getUser();

    if (writableUsers.size() < maxUsers) {
      writableUsers.add(user);
    }

    if (writableUsers.contains(user)) {
      nextWriter.write(rating);
    }
  }
}
