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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;

import org.jdom2.JDOMException;

import com.google.common.io.Files;

import eus.ixa.ixa.pipe.ml.utils.Flags;
import ixa.kaflib.KAFDocument;

/**
 * Main class of ixa-pipe-doc, the ixa pipes (ixa2.si.ehu.es/ixa-pipes) document
 * classifier.
 * 
 * @author ragerri
 * @version 2017-12-15
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
      .newArgumentParser("ixa-pipe-doc-" + version + "-exec.jar")
      .description("ixa-pipe-doc-" + version
          + " is a document classifier developed by IXA NLP Group.\n");
  /**
   * Sub parser instance.
   */
  private Subparsers subParsers = argParser.addSubparsers()
      .help("sub-command help");
  /**
   * Parser to manage the annotation subcommand.
   */
  private Subparser docParser;
  /**
   * Parser to start TCP socket for server-client functionality.
   */
  private Subparser serverParser;
  /**
   * Sends queries to the serverParser for annotation.
   */
  private Subparser clientParser;

  /**
   * Construct a CLI object with the sub-parsers to manage the command line
   * parameters.
   */
  public CLI() {
    docParser = subParsers.addParser("tag").help("Document Classification CLI");
    loadDocParameters();
    serverParser = subParsers.addParser("server")
        .help("Start TCP socket server");
    loadServerParameters();
    clientParser = subParsers.addParser("client")
        .help("Send queries to the TCP socket server");
    loadClientParameters();
  }

  /**
   * Main entry point of ixa-pipe-doc.
   * 
   * @param args
   *          the arguments passed through the CLI
   * @throws IOException
   *           exception if input data not available
   * @throws JDOMException
   *           if problems with the xml formatting of NAF
   */
  public static void main(final String[] args)
      throws IOException, JDOMException {

    CLI cmdLine = new CLI();
    cmdLine.parseCLI(args);
  }

  /**
   * Parse the command interface parameters with the argParser.
   * 
   * @param args
   *          the arguments passed through the CLI
   * @throws IOException
   *           exception if problems with the incoming data
   * @throws JDOMException
   *           if xml format problems
   */
  public final void parseCLI(final String[] args)
      throws IOException, JDOMException {
    try {
      parsedArguments = argParser.parseArgs(args);
      System.err.println("CLI options: " + parsedArguments);
      if (args[0].equals("tag")) {
        classify(System.in, System.out);
      } else if (args[0].equals("server")) {
        server();
      } else if (args[0].equals("client")) {
        client(System.in, System.out);
      }
    } catch (ArgumentParserException e) {
      argParser.handleError(e);
      System.out.println("Run java -jar target/ixa-pipe-doc-" + version
          + ".jar (tag|server|client) -help for details");
      System.exit(1);
    }
  }

  /**
   * Main method to do Document Classification.
   * 
   * @param inputStream
   *          the input stream containing the content to tag
   * @param outputStream
   *          the output stream providing the opinion targets
   * @throws IOException
   *           exception if problems in input or output streams
   * @throws JDOMException
   *           if xml formatting problems
   */
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
    String outputFormat = parsedArguments.getString("outputFormat");
    String clearFeatures = parsedArguments.getString("clearFeatures");
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
    Properties properties = setDocProperties(model, lang, clearFeatures);
    KAFDocument.LinguisticProcessor newLp = kaf.addLinguisticProcessor("topics",
        "ixa-pipe-doc-" + Files.getNameWithoutExtension(model),
        version + "-" + commit);
    newLp.setBeginTimestamp();
    Annotate docClassifier = new Annotate(properties);
    docClassifier.classify(kaf);
    newLp.setEndTimestamp();
    String kafToString = null;
    if (outputFormat.equalsIgnoreCase("tabulated")) {
      kafToString = docClassifier.serializeToTabulated(kaf);
    } else {
      kafToString = docClassifier.serializeToNAF(kaf);
    }
    bwriter.write(kafToString);
    bwriter.close();
    breader.close();
  }

  /**
   * Set up the TCP socket for annotation.
   */
  public final void server() {

    // load parameters into a properties
    String port = parsedArguments.getString("port");
    String model = parsedArguments.getString("model");
    String clearFeatures = parsedArguments.getString("clearFeatures");
    String outputFormat = parsedArguments.getString("outputFormat");
    // language parameter
    String lang = parsedArguments.getString("language");
    Properties serverproperties = setDocServerProperties(port, model, lang,
        clearFeatures, outputFormat);
    new DocClassifierServer(serverproperties);
  }

  /**
   * The client to query the TCP server for annotation.
   * 
   * @param inputStream
   *          the stdin
   * @param outputStream
   *          stdout
   */
  public final void client(final InputStream inputStream,
      final OutputStream outputStream) {

    String host = parsedArguments.getString("host");
    String port = parsedArguments.getString("port");
    try (Socket socketClient = new Socket(host, Integer.parseInt(port));
        BufferedReader inFromUser = new BufferedReader(
            new InputStreamReader(System.in, "UTF-8"));
        BufferedWriter outToUser = new BufferedWriter(
            new OutputStreamWriter(System.out, "UTF-8"));
        BufferedWriter outToServer = new BufferedWriter(
            new OutputStreamWriter(socketClient.getOutputStream(), "UTF-8"));
        BufferedReader inFromServer = new BufferedReader(
            new InputStreamReader(socketClient.getInputStream(), "UTF-8"));) {

      // send data to server socket
      StringBuilder inText = new StringBuilder();
      String line;
      while ((line = inFromUser.readLine()) != null) {
        inText.append(line).append("\n");
      }
      inText.append("<ENDOFDOCUMENT>").append("\n");
      outToServer.write(inText.toString());
      outToServer.flush();

      // get data from server
      StringBuilder sb = new StringBuilder();
      String kafString;
      while ((kafString = inFromServer.readLine()) != null) {
        sb.append(kafString).append("\n");
      }
      outToUser.write(sb.toString());
    } catch (UnsupportedEncodingException e) {
      // this cannot happen but...
      throw new AssertionError("UTF-8 not supported");
    } catch (UnknownHostException e) {
      System.err.println("ERROR: Unknown hostname or IP address!");
      System.exit(1);
    } catch (NumberFormatException e) {
      System.err.println("Port number not correct!");
      System.exit(1);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Create the available parameters for Opinion Target Extraction.
   */
  private void loadDocParameters() {

    docParser.addArgument("-m", "--model").required(true)
        .help("Pass the model to do the tagging as a parameter.\n");
    docParser.addArgument("--clearFeatures").required(false)
        .choices("yes", "no", "docstart").setDefault(Flags.DEFAULT_FEATURE_FLAG)
        .help(
            "Reset the adaptive features every sentence; defaults to 'no'; if -DOCSTART- marks"
                + " are present, choose 'docstart'.\n");
    docParser.addArgument("-l", "--language").required(false).choices("en")
        .help(
            "Choose language; it defaults to the language value in incoming NAF file.\n");
    docParser.addArgument("-o", "--outputFormat").required(false)
        .choices("naf", "tabulated").setDefault(Flags.DEFAULT_OUTPUT_FORMAT)
        .help("Choose output format; it defaults to NAF.\n");
  }

  /**
   * Create the available parameters for Document Classification.
   */
  private void loadServerParameters() {

    serverParser.addArgument("-p", "--port").required(true)
        .help("Port to be assigned to the server.\n");
    serverParser.addArgument("-m", "--model").required(true)
        .help("Pass the model to do the tagging as a parameter.\n");
    serverParser.addArgument("--clearFeatures").required(false)
        .choices("yes", "no", "docstart").setDefault(Flags.DEFAULT_FEATURE_FLAG)
        .help(
            "Reset the adaptive features every sentence; defaults to 'no'; if -DOCSTART- marks"
                + " are present, choose 'docstart'.\n");
    serverParser.addArgument("-l", "--language").required(true)
        .choices("de", "en", "es", "eu", "it", "nl").help("Choose language.\n");
    serverParser.addArgument("-o", "--outputFormat").required(false)
        .choices("naf", "tabulated").setDefault(Flags.DEFAULT_OUTPUT_FORMAT)
        .help("Choose output format; it defaults to NAF.\n");
  }

  private void loadClientParameters() {

    clientParser.addArgument("-p", "--port").required(true)
        .help("Port of the TCP server.\n");
    clientParser.addArgument("--host").required(false)
        .setDefault(Flags.DEFAULT_HOSTNAME)
        .help("Hostname or IP where the TCP server is running.\n");
  }

  /**
   * Set a Properties object with the CLI parameters for Document
   * Classification.
   * 
   * @param model
   *          the model parameter
   * @param language
   *          language parameter
   * @param clearFeatures
   *          resetting the features
   * @return the properties object
   */
  private Properties setDocProperties(String model, String language,
      String clearFeatures) {
    Properties oteProperties = new Properties();
    oteProperties.setProperty("model", model);
    oteProperties.setProperty("language", language);
    oteProperties.setProperty("clearFeatures", clearFeatures);
    return oteProperties;
  }

  private Properties setDocServerProperties(String port, String model,
      String language, String clearFeatures, String outputFormat) {
    Properties serverProperties = new Properties();
    serverProperties.setProperty("port", port);
    serverProperties.setProperty("model", model);
    serverProperties.setProperty("language", language);
    serverProperties.setProperty("clearFeatures", clearFeatures);
    serverProperties.setProperty("outputFormat", outputFormat);
    return serverProperties;
  }

}
