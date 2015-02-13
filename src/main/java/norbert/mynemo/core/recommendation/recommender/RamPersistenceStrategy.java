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
package norbert.mynemo.core.recommendation.recommender;

import java.io.IOException;

import org.apache.mahout.cf.taste.impl.recommender.svd.Factorization;
import org.apache.mahout.cf.taste.impl.recommender.svd.PersistenceStrategy;

/**
 * This class keeps the factorization in the main memory. Thus the data is persisted until the
 * program is shut down.
 */
class RamPersistenceStrategy implements PersistenceStrategy {

  private Factorization factorization;

  @Override
  public Factorization load() throws IOException {
    return factorization;
  }

  @Override
  public void maybePersist(Factorization factorization) throws IOException {
    this.factorization = factorization;
  }
}
