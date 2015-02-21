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
package norbert.mynemo.core.evaluation;

import static com.google.common.base.Preconditions.checkState;

/**
 * This timer deals with seconds, not milliseconds.
 */
public class Timer {
  public static Timer createStartedTimer() {
    return new Timer().start();
  }

  private boolean isStarted;
  private long startTime;
  private long stopTime;

  public Timer() {
    isStarted = false;
  }

  /**
   * Returns the duration in second between the call to {@link #start()} and the call to
   * {@link #stop}.
   */
  public long getDuration() {
    return Math.round((stopTime - startTime) / 1000.0);
  }

  /**
   * Start the timer. Throws an exception if the timer is already started.
   */
  public Timer start() {
    checkState(!isStarted, "The timer is already started.");
    startTime = System.currentTimeMillis();
    isStarted = true;
    return this;
  }

  /**
   * Stops the timer. Throws an exception if the timer is already stopped.
   */
  public Timer stop() {
    checkState(isStarted, "The timer must be started before.");
    stopTime = System.currentTimeMillis();
    isStarted = false;
    return this;
  }
}
