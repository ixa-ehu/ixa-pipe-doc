/*
 *  Copyright 2017 Rodrigo Agerri

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package eus.ixa.ixa.pipe.doc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.jdom2.JDOMException;

import com.google.common.io.Files;

import eus.ixa.ixa.pipe.ml.utils.Flags;
import ixa.kaflib.KAFDocument;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;
import opennlp.tools.cmdline.CmdLineUtil;
import opennlp.tools.sentiment.SentimentModel;
import opennlp.tools.util.TrainingParameters;

/**
 * Main class of ixa-pipe-doc, the ixa pipes (ixa2.si.ehu.es/ixa-pipes) document
 * classifier.
 * 
 * @author ragerri
 * @version 2017-05-31
 * 
 */
public class CLI {

  /**
   * Get dynamically the version of ixa-pipe-doc by looking at the MANIFEST
   * file.
   */
  private final String version = CLI.class.getPackage()
      .getImplementationVersion();
  /**
   * Get the git commit of the ixa-pipe-doc compiled by looking at the MANIFEST
   * file.
   */
  private final String commit = CLI.class.getPackage()
      .getSpecificationVersion();
  /**
   * Name space of the arguments provided at the CLI.
   */
  private Namespace parsedArguments = null;
  /**
   * Argument parser instance.
   */
  private ArgumentParser argParser = ArgumentParsers
      .newArgumentParser("ixa-pipe-test-" + version + "-exec.jar")
      .description("ixa-pipe-test-" + version
          + " testing document classifiers.\n");
  /**
   * Sub parser instance.
   */
  private Subparsers subParsers = argParser.addSubparsers()
      .help("sub-command help");
  /**
   * Parser to manage the annotation subcommand.
   */
  private Subparser docParser;
  private Subparser trainParser;
  private Subparser evalParser;

  /**
   * Construct a CLI object with the sub-parsers to manage the command line
   * parameters.
   */
  public CLI() {
    docParser = subParsers.addParser("tag").help("Document Classification CLI");
    loadDocParameters();
    trainParser = subParsers.addParser("train")
        .help("Training CLI");
    loadTrainingParameters();
    evalParser = subParsers.addParser("eval")
        .help("Evaluation CLI");
    //loadEvalParameters();
  }

  public static void main(final String[] args)
      throws IOException, JDOMException {

    CLI cmdLine = new CLI();
    cmdLine.parseCLI(args);
  }

  public final void parseCLI(final String[] args)
      throws IOException, JDOMException {
    try {
      parsedArguments = argParser.parseArgs(args);
      System.err.println("CLI options: " + parsedArguments);
      if (args[0].equals("tag")) {
        classify(System.in, System.out);
      } else if (args[0].equals("train")) {
        train();
      } else if (args[0].equals("eval")) {
        //eval(System.in, System.out);
      }
    } catch (ArgumentParserException e) {
      argParser.handleError(e);
      System.out.println("Run java -jar target/ixa-pipe-test-" + version
          + ".jar (tag|train|eval) -help for details");
      System.exit(1);
    }
  }


  public final void classify(final InputStream inputStream,
      final OutputStream outputStream) throws IOException, JDOMException {

    BufferedReader breader = new BufferedReader(
        new InputStreamReader(inputStream, "UTF-8"));
    BufferedWriter bwriter = new BufferedWriter(
        new OutputStreamWriter(outputStream, "UTF-8"));
    // read KAF document from inputstream
    KAFDocument kaf = KAFDocument.createFromStream(breader);
    // load parameters into a properties
    String model = parsedArguments.getString("model");
    // language parameter
    String lang = null;
    if (parsedArguments.getString("language") != null) {
      lang = parsedArguments.getString("language");
      if (!kaf.getLang().equalsIgnoreCase(lang)) {
        System.err.println("Language parameter in NAF and CLI do not match!!");
        System.exit(1);
      }
    } else {
      lang = kaf.getLang();
    }
    
    KAFDocument.LinguisticProcessor newLp = kaf.addLinguisticProcessor("topics",
        "ixa-pipe-doc-" + Files.getNameWithoutExtension(model),
        version + "-" + commit);
    newLp.setBeginTimestamp();
    Annotate docClassifier = new Annotate(model);
    docClassifier.classify(kaf);
    newLp.setEndTimestamp();
   String kafToString = docClassifier.serializeToNAF(kaf);
    
    bwriter.write(kafToString);
    bwriter.close();
    breader.close();
  }
  
  private void train() throws IOException {
    // load training parameters file
    final String paramFile = this.parsedArguments.getString("params");
    final TrainingParameters params = SentimentTrainer.loadTrainingParameters(paramFile);
    String outModel = null;
    if (params.getSettings().get("OutputModel") == null
        || params.getSettings().get("OutputModel").length() == 0) {
      outModel = Files.getNameWithoutExtension(paramFile) + ".bin";
      params.put("OutputModel", outModel);
    } else {
      outModel = Flags.getModel(params);
    }
    final SentimentTrainer docTrainer = new SentimentTrainer(
        params);
    final SentimentModel trainedModel = docTrainer.train(params);
    CmdLineUtil.writeModel("test-model", new File(outModel), trainedModel);
  }


 
  /**
   * Create the available parameters for Opinion Target Extraction.
   */
  private void loadDocParameters() {

    docParser.addArgument("-m", "--model").required(true)
        .help("Pass the model to do the tagging as a parameter.\n");
  }
  
  private void loadTrainingParameters() {
    this.trainParser.addArgument("-p", "--params").required(true)
        .help("Load the training parameters file\n");
  }

}
