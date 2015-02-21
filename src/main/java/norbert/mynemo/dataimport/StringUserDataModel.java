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
package norbert.mynemo.dataimport;

import java.io.File;
import java.io.IOException;

import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;

import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 * This data model loads the data from a file, and accepts user name that are not numbers.
 */
public class StringUserDataModel extends FileDataModel {

  /** Performance configuration value. */
  private static final long NEW_DEFAULT_MIN_RELOAD_INTERVAL_MS = 5 * 1000L;
  private static final long serialVersionUID = 1L;

  /**
   * Returns the hash of the given user name.
   *
   * @param name a user name
   * @return the hash of the user name
   */
  public static long convertUsername(String name) {
    HashFunction hashFonction = Hashing.murmur3_128();
    return hashFonction.newHasher().putString(name, Charsets.UTF_8).hash().asLong();
  }

  public StringUserDataModel(File dataFile) throws IOException {
    super(dataFile, false, NEW_DEFAULT_MIN_RELOAD_INTERVAL_MS);
  }

  @Override
  protected long readUserIDFromString(String name) {
    return convertUsername(name);
  }
}
