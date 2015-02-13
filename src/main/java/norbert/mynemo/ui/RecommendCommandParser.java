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

import java.io.File;
import java.io.IOException;

import norbert.mynemo.core.recommendation.RecommenderFamily;
import norbert.mynemo.core.recommendation.RecommenderType;
import norbert.mynemo.core.recommendation.configuration.BasicRecommenderConfiguration;
import norbert.mynemo.core.recommendation.configuration.ItemBasedRecommenderConfiguration;
import norbert.mynemo.core.recommendation.configuration.SvdBasedRecommenderConfiguration;
import norbert.mynemo.core.recommendation.configuration.UserBasedRecommenderConfiguration;
import norbert.mynemo.core.recommendation.recommender.BasicRecommender;
import norbert.mynemo.core.recommendation.recommender.ItemSimilarityRecommender;
import norbert.mynemo.core.recommendation.recommender.SvdBasedRecommender;
import norbert.mynemo.core.recommendation.recommender.UserSimilarityRecommender;
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
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;

import com.google.common.base.Optional;

/**
 * This parser handles a command line to recommend items to an user.
 */
public class RecommendCommandParser {
  // algorithm
  private static final String ALGORITHM_ARG_NAME = "algo";
  private static final char ALGORITHM_CHAR_OPTION = 'a';
  private static final String ALGORITHM_DESCRIPTION = "algorithm used to find the remcommended"
      + " items";
  public static final String ALGORITHM_LONG_OPTION = "algorithm";

  private static final String COMMAND_SYNTAX = "recommend  --algorithm <algo>"
      + "  --data-model <file>  --user <id>  [--recommendations <number>]  [--neighbors <number>]"
      + "  [--features <number>  --iterations <number>]";

  // data model
  private static final String DATAMODEL_ARG_NAME = "file";
  private static final char DATAMODEL_CHAR_OPTION = 'm';
  private static final String DATAMODEL_DESCRIPTION = "data model used by the recommender system";
  public static final String DATAMODEL_LONG_OPTION = "data-model";

  private static final int DEFAULT_RECOMMENDATION_NUMBER = 10;

  // features
  private static final String FEATURES_ARG_NAME = "number";
  private static final char FEATURES_CHAR_OPTION = 'f';
  private static final String FEATURES_DESCRIPTION = "number of features for SVD algorithms";
  public static final String FEATURES_LONG_OPTION = "features";

  // iterations
  private static final String ITERATIONS_ARG_NAME = "number";
  private static final char ITERATIONS_CHAR_OPTION = 'i';
  private static final String ITERATIONS_DESCRIPTION = "number of iterations for SVD algorithms";
  public static final String ITERATIONS_LONG_OPTION = "iterations";

  // neighbors
  private static final String NEIGHBORS_ARG_NAME = "maximum";
  private static final char NEIGHBORS_CHAR_OPTION = 'n';
  private static final String NEIGHBORS_DESCRIPTION = "maximum allowed neighbors for item"
      + " similarity based algorithms";
  public static final String NEIGHBORS_LONG_OPTION = "neighbors";

  // recommendations
  private static final String RECOMMENDATIONS_ARG_NAME = "number";
  private static final char RECOMMENDATIONS_CHAR_OPTION = 'r';
  private static final String RECOMMENDATIONS_DESCRIPTION = "maximum number of recommendations"
      + " to compute";
  private static final String RECOMMENDATIONS_LONG_OPTION = "recommendations";

  // user
  private static final String USER_ARG_NAME = "id";
  private static final char USER_CHAR_OPTION = 'u';
  private static final String USER_DESCRIPTION = "target user of the recommender system";
  public static final String USER_LONG_OPTION = "user";

  /**
   * Performs various checks on the parameters that can't be done when parsing the values. Indeed,
   * some checks need the value of more than one option to be performed.
   */
  private static void check(RecommenderType algorithm, DataModel dataModel, Long user,
      Optional<Integer> features, Optional<Integer> iterations, Optional<Integer> neighbors)
      throws TasteException {

    // user and data model
    try {
      if (dataModel.getItemIDsFromUser(user).isEmpty()) {
        throw new IllegalArgumentException("Error: the user has no preference in the data"
            + " model.");
      }
    } catch (NoSuchUserException e) {
      throw new IllegalArgumentException("Error: the user is not in the data model.", e);
    }

    // features and algorithm
    if (algorithm.getFamily() == RecommenderFamily.SVD_BASED && !features.isPresent()) {
      throw new IllegalArgumentException("Error: the " + FEATURES_LONG_OPTION + " option must"
          + " be provided for a SVD based algorithm.");
    }

    // iterations and algorithm
    if (algorithm.getFamily() == RecommenderFamily.SVD_BASED && !iterations.isPresent()) {
      throw new IllegalArgumentException("Error: the " + ITERATIONS_LONG_OPTION + " option must"
          + " be provided for a SVD based algorithm.");
    }

    // neighbors and algorithm
    if (algorithm.getFamily() == RecommenderFamily.USER_SIMILARITY_BASED && !neighbors.isPresent()) {
      throw new IllegalArgumentException("Error: the " + NEIGHBORS_LONG_OPTION + " option must be"
          + " provided for an item similarity based algorithm.");
    }
  }

  private static void execute(RecommenderType algorithm, DataModel dataModel, Long user,
      Integer maximumRecommendations, Optional<Integer> features, Optional<Integer> iterations,
      Optional<Integer> neighbors) {

    RecommenderBuilder builder;
    switch (algorithm.getFamily()) {
      case BASIC:
        builder = new BasicRecommender(new BasicRecommenderConfiguration(algorithm));
        break;

      case ITEM_SIMILARITY_BASED:
        builder = new ItemSimilarityRecommender(new ItemBasedRecommenderConfiguration(algorithm));
        break;

      case SVD_BASED:
        builder =
            new SvdBasedRecommender(new SvdBasedRecommenderConfiguration(algorithm, features.get(),
                iterations.get(), dataModel, false));
        break;

      case USER_SIMILARITY_BASED:
        builder =
            new UserSimilarityRecommender(new UserBasedRecommenderConfiguration(algorithm,
                neighbors.get(), dataModel, true));
        break;

      default:
        throw new UnsupportedOperationException("Error: unable to handle the given algorithm."
            + " The recommend command parser must be updated.");
    }

    try {
      System.out.println("IMDb ids of the recommended movies (score/100):");
      for (RecommendedItem recommendation : builder.buildRecommender(dataModel).recommend(user,
          maximumRecommendations)) {
        System.out.println(recommendation.getItemID() + " ("
            + Math.round(recommendation.getValue()) + ")");
      }
    } catch (TasteException e) {
      throw new IllegalStateException("Error: an unknown error occurs while the recommendation"
          + " is computed.", e);
    }
  }

  private static Options getOptions() {
    // algorithm option
    OptionBuilder.isRequired();
    OptionBuilder.hasArg();
    OptionBuilder.withArgName(ALGORITHM_ARG_NAME);
    OptionBuilder.withLongOpt(ALGORITHM_LONG_OPTION);
    OptionBuilder.withDescription(ALGORITHM_DESCRIPTION);
    Option algorithm = OptionBuilder.create(ALGORITHM_CHAR_OPTION);

    // data model option
    OptionBuilder.isRequired();
    OptionBuilder.hasArg();
    OptionBuilder.withArgName(DATAMODEL_ARG_NAME);
    OptionBuilder.withLongOpt(DATAMODEL_LONG_OPTION);
    OptionBuilder.withDescription(DATAMODEL_DESCRIPTION);
    Option dataModel = OptionBuilder.create(DATAMODEL_CHAR_OPTION);

    // user option
    OptionBuilder.isRequired();
    OptionBuilder.hasArg();
    OptionBuilder.withArgName(USER_ARG_NAME);
    OptionBuilder.withLongOpt(USER_LONG_OPTION);
    OptionBuilder.withDescription(USER_DESCRIPTION);
    Option user = OptionBuilder.create(USER_CHAR_OPTION);

    // recommendations option
    OptionBuilder.hasArg();
    OptionBuilder.withArgName(RECOMMENDATIONS_ARG_NAME);
    OptionBuilder.withLongOpt(RECOMMENDATIONS_LONG_OPTION);
    OptionBuilder.withDescription(RECOMMENDATIONS_DESCRIPTION);
    Option recommendation = OptionBuilder.create(RECOMMENDATIONS_CHAR_OPTION);

    // features option
    OptionBuilder.hasArg();
    OptionBuilder.withArgName(FEATURES_ARG_NAME);
    OptionBuilder.withLongOpt(FEATURES_LONG_OPTION);
    OptionBuilder.withDescription(FEATURES_DESCRIPTION);
    Option features = OptionBuilder.create(FEATURES_CHAR_OPTION);

    // iterations option
    OptionBuilder.hasArg();
    OptionBuilder.withArgName(ITERATIONS_ARG_NAME);
    OptionBuilder.withLongOpt(ITERATIONS_LONG_OPTION);
    OptionBuilder.withDescription(ITERATIONS_DESCRIPTION);
    Option iterations = OptionBuilder.create(ITERATIONS_CHAR_OPTION);

    // neighbors option
    OptionBuilder.hasArg();
    OptionBuilder.withArgName(NEIGHBORS_ARG_NAME);
    OptionBuilder.withLongOpt(NEIGHBORS_LONG_OPTION);
    OptionBuilder.withDescription(NEIGHBORS_DESCRIPTION);
    Option neighbors = OptionBuilder.create(NEIGHBORS_CHAR_OPTION);

    return new Options().addOption(algorithm).addOption(dataModel).addOption(user)
        .addOption(neighbors).addOption(recommendation).addOption(features).addOption(iterations);
  }

  public static void main(String[] args) {
    try {
      RecommendCommandParser.parse(args);
    } catch (ParseException e) {
      System.err.println(e.getMessage());
      RecommendCommandParser.printUsage();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      RecommendCommandParser.printUsage();
    }
  }

  /**
   * Parses and checks the given arguments, then runs the recommendation algorithm.
   */
  public static void parse(String[] args) throws ParseException, IOException, TasteException {

    CommandLine commandLine = new BasicParser().parse(getOptions(), args);

    // parse the options and create the data types
    RecommenderType algorithm = parseAlgorithm(commandLine.getOptionValue(ALGORITHM_CHAR_OPTION));
    long user = parseUser(commandLine.getOptionValue(USER_CHAR_OPTION));
    Integer recommendations =
        parseRecommendations(Optional.fromNullable(commandLine
            .getOptionValue(RECOMMENDATIONS_CHAR_OPTION)));
    Optional<Integer> features = parseFeatures(commandLine.getOptionValue(FEATURES_CHAR_OPTION));
    Optional<Integer> iterations =
        parseIterations(commandLine.getOptionValue(ITERATIONS_CHAR_OPTION));
    Optional<Integer> neighbors = parseNeighbors(commandLine.getOptionValue(NEIGHBORS_CHAR_OPTION));
    // loading the data model can be long, thus it is the last parsed option
    DataModel dataModel = parseDataModel(commandLine.getOptionValue(DATAMODEL_CHAR_OPTION));

    check(algorithm, dataModel, user, features, iterations, neighbors);

    execute(algorithm, dataModel, user, recommendations, features, iterations, neighbors);
  }

  private static RecommenderType parseAlgorithm(String algorithm) {
    if (algorithm == null) {
      return null;
    }

    RecommenderType result;

    try {
      result = RecommenderType.valueOf(algorithm.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Error: unable to find the given algorithm.", e);
    }

    return result;
  }

  /**
   * Parses and checks the "data-model" option.
   */
  private static DataModel parseDataModel(String dataModel) throws TasteException {
    if (!new File(dataModel).exists()) {
      throw new IllegalArgumentException("Error: unable to find the the data model file.");
    }

    DataModel result;

    try {
      result = new StringUserDataModel(new File(dataModel));
    } catch (IOException e) {
      throw new IllegalStateException("Error: unable to load the data model.", e);
    }

    // check
    if (result.getNumUsers() == 0 || result.getNumItems() == 0) {
      throw new IllegalArgumentException("Error: the data model doesn't contain any data.");
    }

    return result;
  }

  /**
   * Parses and checks the "features" option.
   */
  private static Optional<Integer> parseFeatures(String featuresValue) {
    if (featuresValue == null) {
      return Optional.absent();
    }

    Integer result;

    try {
      result = Integer.parseInt(featuresValue);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Error: the provided feature number is not a valid"
          + " integer.", e);
    }

    if (result <= 0) {
      throw new IllegalArgumentException("Error: the number of features must be greater than 0.");
    }

    return Optional.of(result);
  }

  /**
   * Parses and checks the "iterations" option.
   */
  private static Optional<Integer> parseIterations(String iterationsValue) {
    if (iterationsValue == null) {
      return Optional.absent();
    }

    Integer result;

    try {
      result = Integer.parseInt(iterationsValue);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Error: the provided iteration number is not a valid"
          + " integer.", e);
    }

    // check
    if (result <= 0) {
      throw new IllegalArgumentException("Error: the number of iterations must be greater than"
          + " 0.");
    }

    return Optional.of(result);
  }

  /**
   * Parses and checks the "neighbors" option.
   */
  private static Optional<Integer> parseNeighbors(String neighborsValue) {
    if (neighborsValue == null) {
      return Optional.absent();
    }

    Integer result;

    try {
      result = Integer.parseInt(neighborsValue);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Error: the provided neighbors number is not a valid"
          + " integer.", e);
    }

    // check
    if (result <= 0) {
      throw new IllegalArgumentException("Error: the number of neighbors must be greater than 0");
    }

    return Optional.of(result);
  }

  /**
   * Parses and checks the "recommendations" option.
   */
  private static Integer parseRecommendations(Optional<String> recommendationsValue) {

    if (!recommendationsValue.isPresent()) {
      return DEFAULT_RECOMMENDATION_NUMBER;
    }

    Integer result;

    try {
      result = Integer.parseInt(recommendationsValue.get());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Error: the provided maximum recommendation number is"
          + " not a valid integer.", e);
    }

    // check
    if (result <= 0) {
      throw new IllegalArgumentException("Error: the maximum number of recommendations must be"
          + " greater than 0.");
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

  public static void printUsage() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(COMMAND_SYNTAX, getOptions());

    System.out.print("Available algorithms: ");
    for (RecommenderType current : RecommenderType.values()) {
      System.out.print(current.name().toLowerCase() + "  ");
    }
    System.out.println();
  }

  /**
   * Instantiates a new object. Private to prevents instantiation.
   */
  private RecommendCommandParser() {
    throw new AssertionError();
  }
}
