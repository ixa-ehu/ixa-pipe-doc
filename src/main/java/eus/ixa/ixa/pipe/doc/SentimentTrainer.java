package eus.ixa.ixa.pipe.doc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import eus.ixa.ixa.pipe.ml.utils.IOUtils;
import opennlp.tools.cmdline.TerminateToolException;
import opennlp.tools.sentiment.Sentiment;
import opennlp.tools.sentiment.SentimentEvaluator;
import opennlp.tools.sentiment.SentimentFactory;
import opennlp.tools.sentiment.SentimentME;
import opennlp.tools.sentiment.SentimentModel;
import opennlp.tools.sentiment.SentimentSample;
import opennlp.tools.sentiment.SentimentSampleStream;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.TrainingParameters;

/**
 * Trainer based on Apache OpenNLP Machine Learning API. This class creates a
 * feature set based on the features activated in the docClassicationTrainer.properties
 * file
 * 
 * @author ragerri
 * @version 2017-05-27
 */

public class SentimentTrainer {

  /**
   * String holding the training data.
   */
  private final String trainData;
  /**
   * String pointing to the test data.
   */
  private final String testData;
  /**
   * ObjectStream of the training data.
   */
  private ObjectStream<SentimentSample> trainSamples;
  /**
   * ObjectStream of the test data.
   */
  private ObjectStream<SentimentSample> testSamples;
  private SentimentFactory sentimentFactory;

  /**
   * Construct a trainer with training and test data and language options.
   * 
   * @param params
   *          the training parameters
   * @throws IOException
   *           io exception
   */
  public SentimentTrainer(final TrainingParameters params)
      throws IOException {

    this.trainData = params.getSettings().get("TrainSet");
    this.testData = params.getSettings().get("TestSet");
    this.trainSamples = getDocumentStream(trainData);
    this.testSamples = getDocumentStream(testData);
    sentimentFactory = new SentimentFactory();
  }

  public final SentimentModel train(final TrainingParameters params) {
    if (getDocumentClassificationFactory() == null) {
      throw new IllegalStateException(
          "The DocumentClassificationFactory must be instantiated!!");
    }
    SentimentModel trainedModel = null;
    SentimentEvaluator docEvaluator = null;
    try {
      final Sentiment docClassifier = new SentimentME("en", params, sentimentFactory);
      trainedModel = docClassifier.train(trainSamples);
      docEvaluator = new SentimentEvaluator(docClassifier);
      docEvaluator.evaluate(testSamples);
    } catch (final IOException e) {
      System.err.println("IO error while loading traing and test sets!");
      e.printStackTrace();
      System.exit(1);
    }
    System.out.println("Final Result: \n" + docEvaluator.getFMeasure());
    return trainedModel;
  }

  /**
   * Getting the stream with the right corpus format.
   * 
   * @param inputData
   *          the input data
   * @param clearFeatures whether to reset the features for each document
   * @return the stream from the several corpus formats
   * @throws IOException
   *           the io exception
   */
  public static ObjectStream<SentimentSample> getDocumentStream(
      final String inputData, String clearFeatures) throws IOException {
    final ObjectStream<String> docStream = IOUtils
        .readFileIntoMarkableStreamFactory(inputData);
    ObjectStream<SentimentSample> sampleStream = new SentimentSampleStream(docStream);
    return sampleStream;
  }

  /**
   * Get the features which are implemented in each of the trainers extending
   * this class.
   * 
   * @return the features
   */
  public final SentimentFactory getDocumentClassificationFactory() {
    return this.sentimentFactory;
  }
  
  public final SentimentFactory setDocumentClassifierFactory(
      final SentimentFactory tokenNameFinderFactory) {
    this.sentimentFactory = tokenNameFinderFactory;
    return this.sentimentFactory;
  }
  
  public static ObjectStream<SentimentSample> getDocumentStream(
      final String inputData) throws IOException {
    final ObjectStream<String> docStream = IOUtils
        .readFileIntoMarkableStreamFactory(inputData);
    ObjectStream<SentimentSample> sampleStream = new SentimentSampleStream(docStream);
    return sampleStream;
  }
  
  /**
   * Load the parameters in the {@code TrainingParameters} file.
   * 
   * @param paramFile
   *          the training parameters file
   * @return default loading of the parameters
   */
  public static TrainingParameters loadTrainingParameters(
      final String paramFile) {
    return loadTrainingParameters(paramFile, false);
  }

  /**
   * Load the parameters in the {@code TrainingParameters} file.
   *
   * @param paramFile
   *          the parameter file
   * @param supportSequenceTraining
   *          wheter sequence training is supported
   * @return the parameters
   */
  private static TrainingParameters loadTrainingParameters(
      final String paramFile, final boolean supportSequenceTraining) {

    TrainingParameters params = null;

    if (paramFile != null) {

      checkInputFile("Training Parameter", new File(paramFile));

      InputStream paramsIn = null;
      try {
        paramsIn = new FileInputStream(new File(paramFile));

        params = new opennlp.tools.util.TrainingParameters(paramsIn);
      } catch (final IOException e) {
        throw new TerminateToolException(-1,
            "Error during parameters loading: " + e.getMessage(), e);
      } finally {
        try {
          if (paramsIn != null) {
            paramsIn.close();
          }
        } catch (final IOException e) {
          System.err.println("Error closing the input stream");
        }
      }
    }

    return params;
  }
  
  /**
   * Check input file integrity.
   * 
   * @param name
   *          the name of the file
   * @param inFile
   *          the file
   */
  private static void checkInputFile(final String name, final File inFile) {

    String isFailure = null;

    if (inFile.isDirectory()) {
      isFailure = "The " + name + " file is a directory!";
    } else if (!inFile.exists()) {
      isFailure = "The " + name + " file does not exist!";
    } else if (!inFile.canRead()) {
      isFailure = "No permissions to read the " + name + " file!";
    }

    if (null != isFailure) {
      throw new TerminateToolException(-1,
          isFailure + " Path: " + inFile.getAbsolutePath());
    }
  }

}
