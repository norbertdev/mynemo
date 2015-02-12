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
package norbert.mynemo.ui;

import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import norbert.mynemo.core.evaluation.PersonnalRecommenderEvaluator.MetricType;
import norbert.mynemo.core.recommendation.RecommenderType;
import norbert.mynemo.core.recommendation.configuration.SvdBasedRecommenderConfiguration;
import norbert.mynemo.core.recommendation.configuration.UserBasedRecommenderConfiguration;
import norbert.mynemo.core.selection.BestRecommenderSelector;
import norbert.mynemo.core.selection.BestRecommenderSelector.SpeedOption;
import norbert.mynemo.core.selection.EvaluationComparator;
import norbert.mynemo.core.selection.RecommenderEvaluation;
import norbert.mynemo.dataimport.StringUserDataModel;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.mahout.cf.taste.common.NoSuchUserException;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.model.DataModel;

import com.google.common.base.Optional;

/**
 * This parser handles a command line to select the best suited algorithm for a user.
 */
public class SelectCommandParser {
  // algorithms
  private static final String ALGORITHMS_ARG_NAME = "algos";
  private static final char ALGORITHMS_CHAR_OPTION = 'a';
  private static final String ALGORITHMS_DESCRIPTION = "list of space-separated algorithms to be"
      + " used by the selection"
      + " process. If no list is provided, the selection is done among some fast algorithms.";
  private static final String ALGORITHMS_LONG_OPTION = "algorithms";

  private static final String COMMAND_SYNTAX = "select  --data-model <file>  --user <id>"
      + "  [--algorithms <algo1> [<algo2>…]]  [--metric <metric>]  [--speed <speed>]"
      + "  [--coverage <number>]";

  // coverage
  private static final String COVERAGE_ARG_NAME = "number";
  private static final char COVERAGE_CHAR_OPTION = 'c';
  private static final String COVERAGE_DESCRIPTION = "minimum coverage required for the"
      + " evaluations. A recommender may be unable to provide a recommendation for an item. The"
      + " coverage reflects the ratio between the number of provided recommendation for the"
      + " already rated items and the number of already rated item. The coverage must be between"
      + " 0 and 1. The default value is " + EvaluationComparator.DEFAULT_MINIMUM_COVERAGE + ".";
  private static final String COVERAGE_LONG_OPTION = "coverage";

  // data model
  private static final String DATAMODEL_ARG_NAME = "file";
  private static final char DATAMODEL_CHAR_OPTION = 'm';
  private static final String DATAMODEL_DESCRIPTION = "data model used by the algorithms.";
  private static final String DATAMODEL_LONG_OPTION = "data-model";

  private static final List<RecommenderType> DEFAULT_ALGORITHM_LIST = RecommenderType
      .getFastRecommenders();
  private static final MetricType DEFAULT_METRIC = MetricType.MEAN_ABSOLUTE_ERROR;
  private static final SpeedOption DEFAULT_SPEED = SpeedOption.EXTREMELY_SLOW;
  private static final double FORCED_EVALUATION_PERCENTAGE = 1;

  // metric
  private static final String METRIC_ARG_NAME = "metric";
  private static final char METRIC_CHAR_OPTION = 't';
  private static final String METRIC_DESCRIPTION = "metric to evaluate the algorithms. The"
      + " default value is \"" + DEFAULT_METRIC.toString().toLowerCase() + "\".";
  private static final String METRIC_LONG_OPTION = "metric";

  // speed
  private static final String SPEED_ARG_NAME = "speed";
  private static final char SPEED_CHAR_OPTION = 's';
  private static final String SPEED_DESCRIPTION = "speed and precision of evaluations. The"
      + " default value is \"" + DEFAULT_SPEED.toString().toLowerCase() + "\".";
  private static final String SPEED_LONG_OPTION = "speed";

  // user
  private static final String USER_ARG_NAME = "id";
  private static final char USER_CHAR_OPTION = 'u';
  private static final String USER_DESCRIPTION = "target user of the recommender system";
  private static final String USER_LONG_OPTION = "user";

  /**
   * Performs checks on the parameters that can't be done when parsing the values. Indeed, some
   * checks need the value of more than one option to be performed.
   */
  private static void check(DataModel dataModel, Long user) throws TasteException {

    // check the user and the data model
    try {
      if (dataModel.getItemIDsFromUser(user).isEmpty()) {
        throw new IllegalArgumentException("Error: the user has no preference in the data"
            + " model.");
      }
    } catch (NoSuchUserException e) {
      throw new IllegalArgumentException("Error: the user is not in the data model.", e);
    }
  }

  /**
   * Generates the parsable string containing the option name and its value. Adds also a space
   * character at the end.
   */
  private static String generateLongOption(String optionName, String argumentValue) {
    return "--" + optionName + " " + argumentValue + " ";
  }

  private static Options getOptions() {
    OptionBuilder.isRequired();
    OptionBuilder.hasArg();
    OptionBuilder.withArgName(DATAMODEL_ARG_NAME);
    OptionBuilder.withDescription(DATAMODEL_DESCRIPTION);
    OptionBuilder.withLongOpt(DATAMODEL_LONG_OPTION);
    Option dataModel = OptionBuilder.create(DATAMODEL_CHAR_OPTION);

    OptionBuilder.isRequired();
    OptionBuilder.hasArg();
    OptionBuilder.withArgName(USER_ARG_NAME);
    OptionBuilder.withLongOpt(USER_LONG_OPTION);
    OptionBuilder.withDescription(USER_DESCRIPTION);
    Option user = OptionBuilder.create(USER_CHAR_OPTION);

    OptionBuilder.hasArgs();
    OptionBuilder.withArgName(ALGORITHMS_ARG_NAME);
    OptionBuilder.withLongOpt(ALGORITHMS_LONG_OPTION);
    OptionBuilder.withDescription(ALGORITHMS_DESCRIPTION);
    Option algorithms = OptionBuilder.create(ALGORITHMS_CHAR_OPTION);

    OptionBuilder.hasArg();
    OptionBuilder.withArgName(METRIC_ARG_NAME);
    OptionBuilder.withLongOpt(METRIC_LONG_OPTION);
    OptionBuilder.withDescription(METRIC_DESCRIPTION);
    Option metric = OptionBuilder.create(METRIC_CHAR_OPTION);

    OptionBuilder.hasArg();
    OptionBuilder.withArgName(SPEED_ARG_NAME);
    OptionBuilder.withLongOpt(SPEED_LONG_OPTION);
    OptionBuilder.withDescription(SPEED_DESCRIPTION);
    Option speed = OptionBuilder.create(SPEED_CHAR_OPTION);

    OptionBuilder.hasArg();
    OptionBuilder.withArgName(COVERAGE_ARG_NAME);
    OptionBuilder.withLongOpt(COVERAGE_LONG_OPTION);
    OptionBuilder.withDescription(COVERAGE_DESCRIPTION);
    Option coverage = OptionBuilder.create(COVERAGE_CHAR_OPTION);

    return new Options().addOption(dataModel).addOption(user).addOption(algorithms)
        .addOption(metric).addOption(speed).addOption(coverage);
  }

  public static void main(String[] args) {
    try {
      SelectCommandParser.parse(args);
    } catch (ParseException e) {
      System.err.println(e.getMessage());
      SelectCommandParser.printUsage();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
      SelectCommandParser.printUsage();
    }
  }

  /**
   * Parses and checks the given arguments, runs the selection process and print the result.
   */
  public static void parse(String[] args) throws ParseException, IOException, TasteException {

    CommandLine commandLine = new BasicParser().parse(getOptions(), args);

    // retrieve some option values
    String dataModelValue = commandLine.getOptionValue(DATAMODEL_LONG_OPTION);
    String userValue = commandLine.getOptionValue(USER_LONG_OPTION);

    // parse the option values
    Long user = parseUser(userValue);
    List<RecommenderType> algorithms =
        parseAlgorithms(commandLine.getOptionValues(ALGORITHMS_LONG_OPTION));
    MetricType metric = parseMetric(commandLine.getOptionValue(METRIC_LONG_OPTION));
    SpeedOption speed = parseSpeed(commandLine.getOptionValue(SPEED_LONG_OPTION));
    double coverage = parseCoverage(commandLine.getOptionValue(COVERAGE_LONG_OPTION));
    // loading the data model can be long, thus it is the last parsed option
    DataModel dataModel = parseDataModel(dataModelValue);

    // the parsing is finished, execute
    check(dataModel, user);
    Optional<RecommenderEvaluation> selection =
        select(dataModel, user, algorithms, metric, speed, coverage);
    printSelection(selection, dataModelValue, userValue);
  }

  private static RecommenderType parseAlgorithm(String algorithm) {
    RecommenderType result;

    try {
      result = RecommenderType.valueOf(algorithm.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Error: unable to find the given algorithm (" + algorithm
          + ").", e);
    }

    return result;
  }

  /**
   * Parses and checks the "algorithms" option.
   */
  private static List<RecommenderType> parseAlgorithms(String[] algorithmsValue) {
    if (algorithmsValue == null) {
      return DEFAULT_ALGORITHM_LIST;
    }

    List<RecommenderType> result = new ArrayList<>();

    for (String algorithm : algorithmsValue) {
      result.add(parseAlgorithm(algorithm));
    }

    // check the parsed value
    if (result.isEmpty()) {
      throw new IllegalArgumentException("Error: at last one algorithm must be provided.");
    }

    return result;
  }

  /**
   * Parses and checks the coverage option.
   */
  private static double parseCoverage(String coverageValue) {
    if (coverageValue == null) {
      return EvaluationComparator.DEFAULT_MINIMUM_COVERAGE;
    }

    double result;

    try {
      result = Double.parseDouble(coverageValue);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Error: the provided coverage number is not a valid"
          + " number.", e);
    }

    // check the value
    if (result < 0 || 1 < result) {
      throw new IllegalArgumentException("Error: the minimum coverage must be between 0 and 1.");
    }

    return result;
  }

  /**
   * Parses and checks the "data-model" option.
   */
  private static DataModel parseDataModel(String dataModelValue) throws TasteException {
    if (!new File(dataModelValue).exists()) {
      throw new IllegalArgumentException("Error: unable to find the the data model file.");
    }

    DataModel result;

    try {
      result = new StringUserDataModel(new File(dataModelValue));
    } catch (IOException e) {
      throw new IllegalStateException("Error: unable to load the data model.", e);
    }

    // check
    if (result.getNumUsers() == 0 || result.getNumItems() == 0) {
      throw new IllegalArgumentException("Error: the data model doesn't contain any data.");
    }

    return result;
  }

  private static MetricType parseMetric(String metricValue) {
    if (metricValue == null) {
      return DEFAULT_METRIC;
    }
    MetricType result;

    try {
      result = MetricType.valueOf(metricValue.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Error: unable to find the given metric.", e);
    }

    return result;
  }

  private static SpeedOption parseSpeed(String speedValue) {
    if (speedValue == null) {
      return DEFAULT_SPEED;
    }

    SpeedOption result;

    try {
      result = SpeedOption.valueOf(speedValue.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Error: unable to find the given metric.", e);
    }

    return result;
  }

  private static long parseUser(String user) {
    long result;

    try {
      result = StringUserDataModel.userNameToLong(user);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Error: the provided user is not a valid id.", e);
    }

    return result;
  }

  private static void printSelection(Optional<RecommenderEvaluation> optionalSelection,
      String dataModelFilepath, String user) {
    if (!optionalSelection.isPresent()) {
      System.out.println("Unable to find a algorithm. The minimum coverage value may be too"
          + " high.");
    }

    RecommenderEvaluation selection = optionalSelection.get();
    StringBuilder commandLine = new StringBuilder();
    RecommenderType algorithm = selection.getRecommenderConfiguration().getType();

    // algorithm
    commandLine.append(generateLongOption(RecommendCommandParser.ALGORITHM_LONG_OPTION,
        algorithm.name()));

    // model
    commandLine.append(generateLongOption(RecommendCommandParser.DATAMODEL_LONG_OPTION,
        dataModelFilepath));

    // user
    commandLine.append(generateLongOption(RecommendCommandParser.USER_LONG_OPTION, user));

    // parameters of the algorithm
    switch (algorithm.getFamily()) {
      case BASIC:
        // the basic algorithms do not have anymore option
        break;

      case ITEM_SIMILARITY_BASED:
        // the item similarity based algorithms do not have anymore option
        break;

      case SVD_BASED:
        checkState(selection.getRecommenderConfiguration() instanceof SvdBasedRecommenderConfiguration);
        SvdBasedRecommenderConfiguration svdConfiguration =
            (SvdBasedRecommenderConfiguration) selection.getRecommenderConfiguration();
        commandLine.append(generateLongOption(RecommendCommandParser.FEATURES_LONG_OPTION,
            Integer.toString(svdConfiguration.getFeatureNumber())));
        commandLine.append(generateLongOption(RecommendCommandParser.ITERATIONS_LONG_OPTION,
            Integer.toString(svdConfiguration.getIterationNumber())));
        break;

      case USER_SIMILARITY_BASED:
        checkState(selection.getRecommenderConfiguration() instanceof UserBasedRecommenderConfiguration);
        UserBasedRecommenderConfiguration userConfiguration =
            (UserBasedRecommenderConfiguration) selection.getRecommenderConfiguration();
        commandLine.append(generateLongOption(RecommendCommandParser.NEIGHBORS_LONG_OPTION,
            Integer.toString(userConfiguration.getNeighborNumber())));
        break;

      default:
        throw new UnsupportedOperationException("Error: unable to handle the selected algorithm."
            + " The " + SelectCommandParser.class + " class must be updated.");

    }
    System.out.println(commandLine);
  }

  public static void printUsage() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(COMMAND_SYNTAX, getOptions());

    System.out.print("Available algorithms: ");
    for (RecommenderType current : RecommenderType.values()) {
      System.out.print(current.name().toLowerCase() + "  ");
    }
    System.out.println();

    System.out.print("Available metrics: ");
    for (MetricType current : MetricType.values()) {
      System.out.print(current.name().toLowerCase() + "  ");
    }
    System.out.println();

    System.out.print("Available speeds: ");
    for (SpeedOption current : SpeedOption.values()) {
      System.out.print(current.name().toLowerCase() + "  ");
    }
    System.out.println();
  }

  private static Optional<RecommenderEvaluation> select(DataModel dataModel, Long user,
      List<RecommenderType> algorithms, MetricType metric, SpeedOption speed, double coverage)
      throws TasteException {
    BestRecommenderSelector selection =
        new BestRecommenderSelector(dataModel, user, metric, speed, FORCED_EVALUATION_PERCENTAGE);

    return selection.selectAmong(algorithms, coverage);
  }

  /**
   * Instantiates a new object. Private to prevents instantiation.
   */
  private SelectCommandParser() {
    throw new AssertionError();
  }
}
