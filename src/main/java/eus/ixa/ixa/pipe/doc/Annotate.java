/*  Copyright 2017 Rodrigo Agerri
/*

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

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import eus.ixa.ixa.pipe.ml.StatisticalDocumentClassifier;
import eus.ixa.ixa.pipe.ml.document.DocSample;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Topic;
import ixa.kaflib.WF;

/**
 * Annotation class for Document Classification.
 * 
 * @author ragerri
 * @version 2017-05-31
 * 
 */
public class Annotate {

  /**
   * The DocumentClassifier.
   */
  private StatisticalDocumentClassifier docClassifier;
  private String source;

  
  public Annotate(final Properties properties) throws IOException {

    this.source = properties.getProperty("model");
    docClassifier = new StatisticalDocumentClassifier(properties);
  }
  
  /**
   * Extract Document Labels into Topics NAF layer.
   * @param kaf the KAFDocument
   * @throws IOException if io errors
   */  
  public final void classify(final KAFDocument kaf) {
	  List<List<WF>> sentences = kaf.getSentences();
	  List<String> tokens = new ArrayList<>();
	  for (List<WF> sentence : sentences) {
	    for (WF wf : sentence) {
	      tokens.add(wf.getForm());
	    }
	  }
	  String[] document = tokens.toArray(new String[tokens.size()]);
	  String label = docClassifier.classify(document);
	  Topic topic = kaf.newTopic(label);
      double[] probs = docClassifier.classifyProb(document);
      topic.setConfidence((float)probs[0]);
      topic.setSource(Paths.get(source).getFileName().toString());
      topic.setMethod("ixa-pipe-doc");
  }
  
  public final String serializeToNAF(final KAFDocument kaf) {
    return kaf.toString();
  }
  
  /**
   * Output annotation in tabulated format.
   * 
   * @param kaf
   *          the naf document
   * @return the string containing the annotated document
   */
  public final String serializeToTabulated(KAFDocument kaf) {
    StringBuilder sb = new StringBuilder();
    List<List<WF>> sentences = kaf.getSentences();
    List<String> tokens = new ArrayList<>();
    for (List<WF> sentence : sentences) {
      for (WF wf : sentence) {
        tokens.add(wf.getForm());
      }
    }
    String[] document = tokens.toArray(new String[tokens.size()]);
    String label = kaf.getTopics().get(0).getTopicValue();
    DocSample docSample = new DocSample(label, document, false);
    sb.append(docSample.toString()).append("\n");
    return sb.toString();
  }
}
