
ixa-pipe-doc
=============
[![Build Status](https://travis-ci.org/ixa-ehu/ixa-pipe-doc.svg?branch=master)](https://travis-ci.org/ixa-ehu/ixa-pipe-doc)
[![GitHub license](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://raw.githubusercontent.com/apache/opennlp/master/LICENSE)

ixa-pipe-doc is a document classifier; ixa-pipe-doc is part of the IXA pipes, a multilingual set of NLP tools developed
by the IXA NLP Group [http://ixa2.si.ehu.es/ixa-pipes]

Please go to [http://ixa2.si.ehu.es/ixa-pipes] for general information about the IXA
pipes tools but also for **official releases, including source code and binary
packages for all the tools in the IXA pipes toolkit**.

This document is intended to be the **usage guide of ixa-pipe-doc**. If you really need to clone
and install this repository instead of using the releases provided in
[http://ixa2.si.ehu.es/ixa-pipes], please scroll down to the end of the document for
the [installation instructions](#installation).

**NOTICE!!**: ixa-pipe-doc is now in [Maven Central](http://search.maven.org/)
for easy access to its API.

## TABLE OF CONTENTS

1. [Overview of ixa-pipe-doc](#overview)
2. [Usage of ixa-pipe-nerc](#cli-usage)
  + [Document Classification](#tagging)
  + [Server mode](#server)
3. [API via Maven Dependency](#api)
4. [Git installation](#installation)

## OVERVIEW

ixa-pipe-doc provides document classification into NAF and tabulated formats.
Every model is self-contained, that is, the prop files are not needed to use them.

We provide competitive models based on robust local features and exploiting unlabeled data
via clustering features. The clustering features are based on Brown, Clark (2003)
and Word2Vec clustering plus some gazetteers in some cases.
To avoid duplication of efforts, we use and contribute to the API provided by the
[Apache OpenNLP project](http://opennlp.apache.org) with our own custom developed features for each of the three tasks.

### Features

**A description of every feature is provided in the docClassificationTrainer.properties properties
file** distributed with ixa-pipe-doc. As the training functionality is configured in
properties files, please do check this document. For each model distributed,
there is a prop file which describes the training of the model, as well as a
log file which provides details about the evaluation and training process.

## CLI-USAGE

ixa-pipe-doc provides a runable jar with the following command-line basic functionalities:

1. **server**: starts a TCP service loading the model and required resources.
2. **client**: sends a NAF document to a running TCP server.
3. **tag**: reads a NAF document containing *wf* elements and classifies the whole document.
   
Each of these functionalities are accessible by adding (server|client|tag) as a
subcommand to ixa-pipe-doc-${version}-exec.jar. Please read below and check the -help
parameter:

````shell
java -jar target/ixa-pipe-doc-${version}-exec.jar tag -help
````

### Tagging

If you are in hurry, just execute:

````shell
cat file.txt | java -jar ixa-pipe-tok-${version}-exec.java tok -l $lang | java -jar ixa-pipe-doc-${version}-exec.jar tag -m model.bin
````

If you want to know more, please follow reading.

ixa-pipe-doc reads NAF documents (with *wf* elements) via standard input and outputs NAF
through standard output. The NAF format specification is here:

(http://wordpress.let.vupr.nl/naf/)

There are several options to tag with ixa-pipe-doc:

+ **model**: pass the model as a parameter.
+ **language**: pass the language as a parameter.
+ **outputFormat**: Output annotation in a format: tabulated and NAF. It defaults to NAF.

### Server

We can start the TCP server as follows:

````shell
java -jar target/ixa-pipe-doc-${version}-exec.jar server -l en --port 2060 -m model.bin
````
Once the server is running we can send NAF documents containing (at least) the text layer like this:

````shell
 cat file.tok.naf | java -jar ixa-pipe-doc-${version}-exec.jar client -p 2060
````

## API

The easiest way to use ixa-pipe-doc programatically is via Apache Maven. Add
this dependency to your pom.xml:

````shell
<dependency>
    <groupId>eus.ixa</groupId>
    <artifactId>ixa-pipe-doc</artifactId>
    <version>1.0.0</version>
</dependency>
````

## JAVADOC

The javadoc of the module is located here:

````shell
ixa-pipe-doc/target/ixa-pipe-doc-$version-javadoc.jar
````

## Module contents

The contents of the module are the following:

    + formatter.xml           Apache OpenNLP code formatter for Eclipse SDK
    + pom.xml                 maven pom file which deals with everything related to compilation and execution of the module
    + src/                    java source code of the module and required resources
    + Furthermore, the installation process, as described in the README.md, will generate another directory:
    target/                 it contains binary executable and other directories

## INSTALLATION

Installing the ixa-pipe-nerc requires the following steps:

If you already have installed in your machine the Java 1.8+ and MAVEN 3, please go to step 3
directly. Otherwise, follow these steps:

### 1. Install JDK 1.8

If you do not install JDK 1.8+ in a default location, you will probably need to configure the PATH in .bashrc or .bash_profile:

````shell
export JAVA_HOME=/yourpath/local/java8
export PATH=${JAVA_HOME}/bin:${PATH}
````

If you use tcsh you will need to specify it in your .login as follows:

````shell
setenv JAVA_HOME /usr/java/java18
setenv PATH ${JAVA_HOME}/bin:${PATH}
````

If you re-login into your shell and run the command

````shell
java -version
````

You should now see that your JDK is 1.8.

### 2. Install MAVEN 3

Download MAVEN 3.

````
Now you need to configure the PATH. For Bash Shell:

````shell
export MAVEN_HOME=/home/ragerri/local/apache-maven-3.3.9
export PATH=${MAVEN_HOME}/bin:${PATH}
````

For tcsh shell:

````shell
setenv MAVEN3_HOME ~/local/apache-maven-3.3.9
setenv PATH ${MAVEN3}/bin:{PATH}
````

If you re-login into your shell and run the command

````shell
mvn -version
````

You should see reference to the MAVEN version you have just installed plus the JDK that is using.

### 3. Get module source code

If you must get the module source code from here do this:

````shell
git clone https://github.com/ixa-ehu/ixa-pipe-doc
````

### 4. Compile

Execute this command to compile ixa-pipe-nerc:

````shell
cd ixa-pipe-doc
mvn clean package
````
This step will create a directory called target/ which contains various directories and files.
Most importantly, there you will find the module executable:

ixa-pipe-doc-${version}-exec.jar

This executable contains every dependency the module needs, so it is completely portable as long
as you have a JVM 1.8 installed.

To install the module in the local maven repository, usually located in ~/.m2/, execute:

````shell
mvn clean install
````

## Contact information

````shell
Rodrigo Agerri
IXA NLP Group
University of the Basque Country (UPV/EHU)
E-20018 Donostia-San Sebastián
rodrigo.agerri@ehu.eus
````
