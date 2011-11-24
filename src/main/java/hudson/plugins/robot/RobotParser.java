/*
 * Copyright 2008-2011 Nokia Siemens Networks Oyj
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hudson.plugins.robot;

import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.plugins.robot.model.RobotTestObject;
import hudson.plugins.robot.model.RobotCaseResult;
import hudson.plugins.robot.model.RobotResult;
import hudson.plugins.robot.model.RobotSuiteResult;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

public class RobotParser {

	public RobotResult parse(String outputFileLocations, String outputPath, AbstractBuild<?, ?> build,
			Launcher launcher, TaskListener listener)
	throws InterruptedException, IOException {
		RobotResult result = new FilePath(build.getWorkspace(), outputPath).act(
				new RobotParserCallable(outputFileLocations));
		return result;
	}

	public static final class RobotParserCallable implements
	FilePath.FileCallable<RobotResult> {

		private static final long serialVersionUID = 1L;
		private final String outputFileLocations;

		public RobotParserCallable(String outputFileLocations) {
			this.outputFileLocations = outputFileLocations;
		}

		public RobotResult invoke(File ws, VirtualChannel channel)
		throws IOException {
			FileSet setInWorkspace = Util
			.createFileSet(ws, outputFileLocations);
			DirectoryScanner resultScanner = setInWorkspace
			.getDirectoryScanner();
			RobotResult result = new RobotResult();
			
			String[] files = resultScanner.getIncludedFiles();
			if (files.length == 0) {
				throw new AbortException(
						"No files found in path " + ws.getAbsolutePath() + " with configured filemask: " + outputFileLocations);
			}
			
			for(String file : files){
				XMLInputFactory factory = XMLInputFactory.newInstance();
				factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
				File baseDirectory = resultScanner.getBasedir();
				File reportFile = new File(baseDirectory, file);

				//get the potential directories emerging from the use of GLOB filemask accounted in the splitted file parsing
				String dirFromFileGLOB = new File(file).getParent();
				if(dirFromFileGLOB != null)
					baseDirectory = new File(baseDirectory, dirFromFileGLOB.toString());
				
				try {
					XMLStreamReader reader = factory.createXMLStreamReader(
							new FileReader(reportFile));
					result =  parseResult(reader, baseDirectory);
				} catch (XMLStreamException e1) {
					throw new IOException("Parsing of output xml failed!", e1);
				}
			}
			return result;
		}

		private RobotResult parseResult(XMLStreamReader reader, File baseDirectory) throws XMLStreamException, FileNotFoundException {
			RobotResult result = new RobotResult();
			while(reader.hasNext()){
				reader.next();
				if(reader.getEventType() == XMLStreamReader.START_ELEMENT){
					QName tagName = reader.getName();
					
					//we already have all data from suites and tests so no need for statistics
					if("statistics".equals(tagName.getLocalPart()))
						break;
					else if("robot".equals(tagName.getLocalPart())){
						for(int i = 0; i < reader.getAttributeCount(); i++){
							if(reader.getAttributeLocalName(i).equals("generated")){
								String timestamp = reader.getAttributeValue(i);
								result.setTimeStamp(timestamp);
							}
						}
					} else if("suite".equals(tagName.getLocalPart())){
						RobotSuiteResult suite = processSuite(reader, result, baseDirectory);
						result.addSuite(suite);
					}
				}
			}
			return result;
		}

		private RobotSuiteResult processSuite(XMLStreamReader reader, RobotTestObject parent, File baseDirectory) throws FileNotFoundException, XMLStreamException {
			RobotSuiteResult suite = new RobotSuiteResult();
			suite.setParent(parent);
			
			//parse attributes
			for(int i = 0; i < reader.getAttributeCount(); i++){
				if(reader.getAttributeLocalName(i).equals("name")){
					String name = reader.getAttributeValue(i);
					suite.setName(name);
				} else if(reader.getAttributeLocalName(i).equals("src")) {
					String path = reader.getAttributeValue(i);
					
					XMLInputFactory factory = XMLInputFactory.newInstance();
					factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
					XMLStreamReader splitReader = factory.createXMLStreamReader(new FileReader(new File(baseDirectory, path)));
					while(splitReader.hasNext()){
						splitReader.next();
						if(splitReader.getEventType() == XMLStreamReader.START_ELEMENT){
							QName tagName = splitReader.getName();
							if("suite".equals(tagName.getLocalPart())){
								return processSuite(splitReader, parent, baseDirectory);
							}
						}
					}
				}
			}
			
			//parse children, which can be test cases or test suites
			while(reader.hasNext()){
				reader.next();
				if(reader.getEventType() == XMLStreamReader.START_ELEMENT){
					QName tagName = reader.getName();
					if("suite".equals(tagName.getLocalPart())){
						RobotSuiteResult childSuite = processSuite(reader, suite, baseDirectory);
						suite.addChild(childSuite);
					} else if("test".equals(tagName.getLocalPart())){
						RobotCaseResult test = processTest(reader, suite);
						suite.addCaseResult(test);
					}
				} else if (reader.getEventType() == XMLStreamReader.END_ELEMENT){
					QName tagName = reader.getName();
					if("suite".equals(tagName.getLocalPart()))
						return suite;
				}
			}
			throw new XMLStreamException("No matching end tag found for test suite: " + suite.getName());
		}

		private RobotCaseResult processTest(XMLStreamReader reader, RobotSuiteResult result) throws XMLStreamException {
			RobotCaseResult caseResult = new RobotCaseResult();
			caseResult.setParent(result);
			
			//parse attributes
			for(int i = 0; i < reader.getAttributeCount(); i++){
				if(reader.getAttributeLocalName(i).equals("name")){
					String name = reader.getAttributeValue(i);
					caseResult.setName(name);
				} else if(reader.getAttributeLocalName(i).equals("critical")) {
					String critical = reader.getAttributeValue(i);
					if(critical.equals("yes"))
						caseResult.setCritical(true);
					else
						caseResult.setCritical(false);
				}
			}
			
			//parse test details from nested status
			while(reader.hasNext()){
				reader.next();
				if(reader.getEventType() == XMLStreamReader.START_ELEMENT){
					QName tagName = reader.getName();
					if("status".equals(tagName.getLocalPart())){
						for(int i = 0; i < reader.getAttributeCount(); i++){
							if(reader.getAttributeLocalName(i).equals("status")){
								String status = reader.getAttributeValue(i);
								if(status.equals("PASS")){
									caseResult.setPassed(true);
								} else {
									caseResult.setPassed(false);
								}
							} else if(reader.getAttributeLocalName(i).equals("starttime")) {
								String startTime = reader.getAttributeValue(i);
								caseResult.setStarttime(startTime);
							} else if(reader.getAttributeLocalName(i).equals("endtime")){
								String endTime = reader.getAttributeValue(i);
								caseResult.setEndtime(endTime);
							} else if(reader.getAttributeLocalName(i).equals("critical")){
								String criticality = reader.getAttributeValue(i);
								if(criticality.equals("yes"))
									caseResult.setCritical(true);
								else
									caseResult.setCritical(false);
							}
						}
						
						//parse character data from status, fail if no end tag found
						while(reader.hasNext()){
							reader.next();
							if(reader.getEventType() == XMLStreamReader.CHARACTERS){
								String error = reader.getText();
								caseResult.setErrorMsg(error);
							} else if (reader.getEventType() == XMLStreamReader.END_ELEMENT){
								QName statusName = reader.getName();
								if("status".equals(statusName.getLocalPart())) 
									break;
								else {
									throw new XMLStreamException("No end tag found for status while parsing test case: " + caseResult.getName());
								}
							}
						}
					}
				} else if (reader.getEventType() == XMLStreamReader.END_ELEMENT){
					QName tagName = reader.getName();
					if("test".equals(tagName.getLocalPart()))
						return caseResult;
				}
			}
			throw new XMLStreamException("No matching end tag found for test case " + caseResult.getName());
		}
	}
}
