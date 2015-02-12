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
package norbert.mynemo.dataimport.scraping.output;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.util.List;

import norbert.mynemo.dataimport.scraping.CkMapping;

import org.apache.commons.csv.CSVPrinter;

/**
 * Writer that writes CK movie mappings into a given file.
 */
public class CkMappingWriter implements Closeable, Flushable {

  private final CSVPrinter printer;

  public CkMappingWriter(String outputFilepath) throws IOException {
    checkArgument(!new File(outputFilepath).exists(), "The output file must not exist.");

    printer = CkMapping.createPrinter(outputFilepath);
  }

  @Override
  public void close() throws IOException {
    printer.close();
  }

  @Override
  public void flush() throws IOException {
    printer.flush();
  }

  public void write(CkMapping mapping) throws IOException {
    mapping.printOn(printer);
  }

  public void writeAll(List<CkMapping> mappings) throws IOException {
    for (CkMapping mapping : mappings) {
      write(mapping);
    }
  }
}
