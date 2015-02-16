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

import org.apache.commons.math3.optim.ConvergenceChecker;

/**
 * This checker declares a function convergent if the maximum number of iterations is reached or
 * exceeded.
 */
public class MaxIterationChecker<T> implements ConvergenceChecker<T> {

  private final int maxIteration;

  public MaxIterationChecker(int maxIterations) {
    this.maxIteration = maxIterations;
  }

  @Override
  public boolean converged(int iteration, T previous, T current) {
    return maxIteration <= iteration;
  }
}
