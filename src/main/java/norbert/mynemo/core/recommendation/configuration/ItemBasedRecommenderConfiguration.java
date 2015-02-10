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
package norbert.mynemo.core.recommendation.configuration;

import static com.google.common.base.Preconditions.checkArgument;
import norbert.mynemo.core.recommendation.RecommenderFamily;
import norbert.mynemo.core.recommendation.RecommenderType;
import norbert.mynemo.core.recommendation.recommender.ItemSimilarityRecommender;

/**
 * This class represents a configuration for the
 * {@link ItemSimilarityRecommender}.
 */
public class ItemBasedRecommenderConfiguration implements
		RecommenderConfiguration {

	private final RecommenderType type;

	public ItemBasedRecommenderConfiguration(RecommenderType type) {
		checkArgument(type.getFamily() == RecommenderFamily.ITEM_SIMILARITY_BASED);

		this.type = type;
	}

	@Override
	public RecommenderType getType() {
		return type;
	}

	@Override
	public String toString() {
		return type.toString();
	}
}
