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
package norbert.mynemo.dataimport.fileformat.input;

import static com.google.common.base.Preconditions.checkState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * This class discards the first two lines and the last line from a stream. The last line must begin
 * with the character '<code>&lt;</code>'. The first line must be "<code>&lt;text&gt;</code>".
 */
public class CleanRatingsReader extends Reader {

  private static final String EXPECTED_FIRST_LINE = "<text>";
  private boolean firstReading;
  private boolean lastReading;
  private final BufferedReader previousReader;

  public CleanRatingsReader(BufferedReader previousReader) {
    this.previousReader = previousReader;
    firstReading = true;
    lastReading = false;
  }

  @Override
  public void close() throws IOException {
    previousReader.close();
  }

  @Override
  public int read(char[] buffer, int offset, int length) throws IOException {
    if (lastReading) {
      // the end of the stream has been reached
      return -1;
    }
    if (firstReading) {
      // discard the first two lines
      String firstLine = previousReader.readLine();
      checkState(firstLine.equals(EXPECTED_FIRST_LINE), "Error: the first line of the file must be"
          + " \"" + EXPECTED_FIRST_LINE + "\".");
      previousReader.readLine();
      firstReading = false;
    }

    int readingLength = previousReader.read(buffer, offset, length);

    // check if the last line that begins with '<' is reached
    int index = 0;
    while (index < readingLength) {
      if (buffer[index + offset] == '<') {
        // the last line is reached
        lastReading = true;
        // the input reader will not be read anymore
        return index;
      }
      index++;
    }

    return index;
  }
}
