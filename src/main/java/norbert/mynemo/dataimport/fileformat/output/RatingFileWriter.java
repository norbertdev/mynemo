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

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;

import norbert.mynemo.dataimport.fileformat.MynemoRating;

import org.apache.commons.csv.CSVPrinter;

/**
 * This writer writes ratings to a file.
 */
public class RatingFileWriter implements RatingWriter {

  private final CSVPrinter printer;

  public RatingFileWriter(String outputFilepath) throws IOException {
    checkArgument(!new File(outputFilepath).exists(), "The output file must not exist.");

    printer = MynemoRating.createPrinter(outputFilepath);
  }

  @Override
  public void close() throws IOException {
    printer.close();
  }

  @Override
  public void write(MynemoRating rating) throws IOException {
    rating.printOn(printer);
  }
}
