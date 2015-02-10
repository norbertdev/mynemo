/*
 * Copyright 2015 Norbert
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package norbert.mynemo.core.selection;

import norbert.mynemo.core.recommendation.RecommenderType;

/**
 * This class represents a recommender and its configuration. The created
 * objects are immutable.
 */
public class RecommenderDescription {
	private final RecommenderType type;

	public RecommenderDescription(RecommenderType type) {
		this.type = type;
	}

	public RecommenderType getType() {
		return type;
	}

	@Override
	public String toString() {
		StringBuilder string = new StringBuilder();
		switch (type) {
		case ITEM_AVERAGE:
			string.append("Item average recommender");
			break;

		case RANDOM:
			string.append("Random recommender");
			break;

		case USER_AVERAGE:
			string.append("User average recommender");
			break;

		case ITEM_SIMILARITY_WITH_CITY_BLOCK_DISTANCE:
			string.append("Item based recommender with city block distance");
			break;

		case ITEM_SIMILARITY_WITH_EUCLIDEAN_DISTANCE:
			string.append("Item based recommender with Euclidian distance");
			break;

		case ITEM_SIMILARITY_WITH_LOG_LIKELIHOOD:
			string.append("Item based recommender with log likelihood");
			break;

		case ITEM_SIMILARITY_WITH_PEARSON_CORRELATION:
			string.append("Item based recommender with Pearson correlation");
			break;

		case ITEM_SIMILARITY_WITH_UNCENTERED_COSINE:
			string.append("Item based recommender with uncentered cosine");
			break;

		case ITEM_SIMILARITY_WITH_TANIMOTO_COEFFICIENT:
			string.append("Item based recommender with Tanimoto coefficient");
			break;

		case ITEM_SIMILARITY_WITH_WEIGHTED_EUCLIDEAN_DISTANCE:
			string.append("Item based recommender with weighted Euclidian distance");
			break;

		case ITEM_SIMILARITY_WITH_WEIGHTED_PEARSON_CORRELATION:
			string.append("Item based recommender with weighted Pearson correlation");
			break;

		case SVD_WITH_ALSWR_FACTORIZER:
			string.append("SVD based recommender with ALSWR factorizer");
			break;

		case SVD_WITH_PARALLEL_SGD_FACTORIZER:
			string.append("SVD based recommender with SGD factorizer");
			break;

		case SVD_WITH_RATING_SGD_FACTORIZER:
			string.append("SVD based recommender with rating SGD factorizer");
			break;

		case SVD_WITH_SVDPLUSPLUS_FACTORIZER:
			string.append("SVD based recommender with SVD++ factorizer");
			break;

		case USER_SIMILARITY_WITH_CITY_BLOCK_DISTANCE:
			string.append("User based recommender with city block similarity");
			break;

		case USER_SIMILARITY_WITH_EUCLIDEAN_DISTANCE:
			string.append("User based recommender with Euclidian distance");
			break;

		case USER_SIMILARITY_WITH_LOG_LIKELIHOOD:
			string.append("User based recommender with log likelihood");
			break;

		case USER_SIMILARITY_WITH_PEARSON_CORRELATION:
			string.append("User based recommender with Pearson correlation");
			break;

		case USER_SIMILARITY_WITH_SPEARMAN_CORRELATION:
			string.append("User based recommender with Spearman correlation");
			break;

		case USER_SIMILARITY_WITH_TANIMOTO_COEFFICIENT:
			string.append("User based recommender with Tanimoto coefficient");
			break;

		case USER_SIMILARITY_WITH_UNCENTERED_COSINE:
			string.append("User based recommender with uncentered cosine");
			break;

		case USER_SIMILARITY_WITH_WEIGHTED_EUCLIDEAN_DISTANCE:
			string.append("User based recommender with weighted Euclidian distance");
			break;

		case USER_SIMILARITY_WITH_WEIGHTED_PEARSON_CORRELATION:
			string.append("User based recommender with weighted Pearson correlation");
			break;

		default:
			throw new IllegalStateException();
		}
		return string.toString();
	}
}
