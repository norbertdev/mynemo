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

/**
 * Types of similarity that can be used for computing the neighbors of a user by the
 * {@link MaxNeighborUserFilter}.
 */
public enum UserSimilarityType {
  CITY_BLOCK_DISTANCE, EUCLIDEAN_DISTANCE, LOG_LIKELIHOOD, ORIGINAL_SPEARMAN_CORRELATION, PEARSON_CORRELATION, SPEARMAN_CORRELATION, TANIMOTO_COEFFICIENT, UNCENTERED_COSINE, WEIGHTED_EUCLIDEAN_DISTANCE, WEIGHTED_PEARSON_CORRELATION, WEIGHTED_UNCENTERED_COSINE
}
