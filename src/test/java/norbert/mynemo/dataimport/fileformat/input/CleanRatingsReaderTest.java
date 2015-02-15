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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

public class CleanRatingsReaderTest {
  private static final String CONTENT_PREFIX = "<text>\nMovieId\tRating\tAverage\tImdbId\tTitle\n";
  private static final String CONTENT_SUFFIX = "</text>\n";
  private static final String EXPECTED_A = "a\n";
  private static final String EXPECTED_B = "abc\tdefg\nh\tij\n";
  private static final String EXPECTED_C = "";
  private static final String PROVIDED_A = CONTENT_PREFIX + EXPECTED_A + CONTENT_SUFFIX;
  private static final String PROVIDED_B = CONTENT_PREFIX + EXPECTED_B + CONTENT_SUFFIX;
  private static final String PROVIDED_C = CONTENT_PREFIX + EXPECTED_C + CONTENT_SUFFIX;

  @Test
  public void differentBufferSizesShouldWork() {
    // big buffer
    readWithBufferSize(new char[1024], 0, 1024);
    readWithBufferSize(new char[1024], 0, 128);

    // small buffer
    readWithBufferSize(new char[2], 0, 2);
    readWithBufferSize(new char[2], 0, 1);

    // minimum buffer
    readWithBufferSize(new char[1], 0, 1);
  }

  @Test
  public void differentOffsetsShouldWork() {
    readWithBufferSize(new char[1024], 512, 256);
    readWithBufferSize(new char[8], 3, 2);
    readWithBufferSize(new char[8], 6, 1);
  }

  private String readWithBuffer(BufferedReader bufferReader, char[] buffer, int offset, int length) {
    CleanRatingsReader testedReader = new CleanRatingsReader(bufferReader);
    StringBuilder obtainedString = new StringBuilder();

    try {
      int returnedLength;

      while ((returnedLength = testedReader.read(buffer, offset, length)) != -1) {
        obtainedString.append(buffer, offset, returnedLength);
      }

      testedReader.close();
    } catch (IOException e) {
      fail();
    }
    return new String(obtainedString);
  }

  private void readWithBufferSize(char[] buffer, int offset, int length) {
    String obtainedString;

    obtainedString =
        readWithBuffer(new BufferedReader(new StringReader(PROVIDED_A)), buffer, offset, length);
    assertEquals(EXPECTED_A, obtainedString);

    obtainedString =
        readWithBuffer(new BufferedReader(new StringReader(PROVIDED_B)), buffer, offset, length);
    assertEquals(EXPECTED_B, obtainedString);

    obtainedString =
        readWithBuffer(new BufferedReader(new StringReader(PROVIDED_C)), buffer, offset, length);
    assertEquals(EXPECTED_C, obtainedString);
  }
}
