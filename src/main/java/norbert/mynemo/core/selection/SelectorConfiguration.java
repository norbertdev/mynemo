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
package norbert.mynemo.core.selection;

import norbert.mynemo.core.evaluation.PersonnalRecommenderEvaluator;
import norbert.mynemo.core.selection.RecommenderSelector.SpeedOption;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.DataModelBuilder;
import org.apache.mahout.cf.taste.model.DataModel;

/**
 * This class encapsulates the information shared between the different selectors. Instances of this
 * class are immutable.
 */
class SelectorConfiguration {

  private final DataModel dataModel;
  private final DataModelBuilder dataModelBuilder;
  private final double evaluationPercentage;
  private final PersonnalRecommenderEvaluator evaluator;
  private final boolean reuseIsAllowed;
  private final SpeedOption speed;
  private final long targetUser;

  SelectorConfiguration(DataModel dataModel, long targetUser,
      PersonnalRecommenderEvaluator evaluator, double evaluationPercentage, boolean reuseIsAllowed,
      SpeedOption speed, DataModelBuilder dataModelBuilder) throws TasteException {
    this.dataModel = dataModel;
    this.dataModelBuilder = dataModelBuilder;
    this.evaluator = evaluator;
    this.targetUser = targetUser;
    this.evaluationPercentage = evaluationPercentage;
    this.reuseIsAllowed = reuseIsAllowed;
    this.speed = speed;
  }

  public DataModel getDataModel() {
    return dataModel;
  }

  public DataModelBuilder getDataModelBuilder() {
    return dataModelBuilder;
  }

  public double getEvaluationPercentage() {
    return evaluationPercentage;
  }

  public PersonnalRecommenderEvaluator getEvaluator() {
    return evaluator;
  }

  public SpeedOption getSpeed() {
    return speed;
  }

  public long getTargetUser() {
    return targetUser;
  }

  public boolean reuseIsAllowed() {
    return reuseIsAllowed;
  }
}
