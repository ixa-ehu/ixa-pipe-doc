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
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

import org.jdom2.JDOMException;

import com.google.common.io.Files;

import ixa.kaflib.KAFDocument;

public class DocClassifierServer {

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
   * The model.
   */
  private String model = null;
  /**
   * The annotation output format, one of NAF (default) or tabulated.
   */
  private String outputFormat = null;

  /**
   * Construct a DocumentClassification server.
   * 
   * @param properties
   *          the properties
   */
  public DocClassifierServer(Properties properties) {

    Integer port = Integer.parseInt(properties.getProperty("port"));
    model = properties.getProperty("model");
    outputFormat = properties.getProperty("outputFormat");
    
    String kafToString;
    ServerSocket socketServer = null;
    Socket activeSocket;
    BufferedReader inFromClient = null;
    BufferedWriter outToClient = null;

    try {
      Annotate annotator = new Annotate(properties);
      System.out.println("-> Trying to listen port... " + port);
      socketServer = new ServerSocket(port);
      System.out.println("-> Connected and listening to port " + port);
      while (true) {
        try {
          activeSocket = socketServer.accept();
          inFromClient = new BufferedReader(new InputStreamReader(activeSocket.getInputStream(), "UTF-8"));
          outToClient = new BufferedWriter(new OutputStreamWriter(activeSocket.getOutputStream(), "UTF-8"));
          //get data from client
          String stringFromClient = getClientData(inFromClient);
          // annotate
          kafToString = getAnnotations(annotator, stringFromClient);
        } catch (JDOMException e) {
          kafToString = "\n-> ERROR: Badly formatted NAF document!!\n";
          sendDataToClient(outToClient, kafToString);
          continue;
        } catch (UnsupportedEncodingException e) {
          kafToString = "\n-> ERROR: UTF-8 not valid!!\n";
          sendDataToClient(outToClient, kafToString);
          continue;
        } catch (IOException e) {
          kafToString = "\n -> ERROR: Input data not correct!!\n";
          sendDataToClient(outToClient, kafToString);
          continue;
        }
        //send data to server after all exceptions and close the outToClient
        sendDataToClient(outToClient, kafToString);
        //close the resources
        inFromClient.close();
        activeSocket.close();
      } //end of processing block
    } catch (IOException e) {
      e.printStackTrace();
      System.err.println("-> IOException due to failing to create the TCP socket or to wrongly provided model path.");
    } finally {
      System.out.println("closing tcp socket...");
      try {
        socketServer.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
  
  /**
   * Read data from the client and output to a String.
   * @param inFromClient the client inputstream
   * @return the string from the client
   */
  private String getClientData(BufferedReader inFromClient) {
    StringBuilder stringFromClient = new StringBuilder();
    try {
      String line;
      while ((line = inFromClient.readLine()) != null) {
        if (line.matches("<ENDOFDOCUMENT>")) {
          break;
        }
        stringFromClient.append(line).append("\n");
        if (line.matches("</NAF>")) {
          break;
        }
      }
    }catch (IOException e) {
      e.printStackTrace();
    }
    return stringFromClient.toString();
  }

  /**
   * Send data back to server after annotation.
   * @param outToClient the outputstream to the client
   * @param kafToString the string to be processed
   * @throws IOException if io error
   */
  private void sendDataToClient(BufferedWriter outToClient, String kafToString) throws IOException {
    outToClient.write(kafToString);
    outToClient.close();
  }
  
  /**
   * Topic annotator.
   * 
   * @param annotator
   *          the annotator
   * @param stringFromClient
   *          the string to be annotated
   * @return the annotation result
   * @throws IOException
   *           if io error
   * @throws JDOMException
   *           if xml error
   */
  private String getAnnotations(Annotate annotator, String stringFromClient)
      throws IOException, JDOMException {
    // get a breader from the string coming from the client
    BufferedReader clientReader = new BufferedReader(
        new StringReader(stringFromClient));
    KAFDocument kaf = KAFDocument.createFromStream(clientReader);
    KAFDocument.LinguisticProcessor newLp = kaf.addLinguisticProcessor("topics",
        "ixa-pipe-doc-" + Files.getNameWithoutExtension(model),
        version + "-" + commit);
    newLp.setBeginTimestamp();
    annotator.classify(kaf);
    newLp.setEndTimestamp();
    // get outputFormat
    String kafToString = null;
    if (outputFormat.equalsIgnoreCase("tabulated")) {
      kafToString = annotator.serializeToTabulated(kaf);
    } else {
      kafToString = annotator.serializeToNAF(kaf);
    }
    return kafToString;
  }

}
