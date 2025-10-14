/*
 * Copyright 2008-2014 Nokia Solutions and Networks Oy
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Run;
import hudson.plugins.robot.model.RobotTestObject;
import hudson.plugins.robot.model.RobotCaseResult;
import hudson.plugins.robot.model.RobotResult;
import hudson.plugins.robot.model.RobotSuiteResult;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.jenkinsci.remoting.RoleChecker;

public class RobotParser {

	public RobotResult parse(String outputFileLocations, String outputPath, Run<?, ?> build, FilePath workSpace, String logFileName, String reportFileName)
	throws InterruptedException, IOException {
		return new FilePath(workSpace, outputPath).act(
				new RobotParserCallable(outputFileLocations, logFileName, reportFileName));
	}

	public static final class RobotParserCallable implements
	FilePath.FileCallable<RobotResult> {

		@Serial
		private static final long serialVersionUID = 1L;
		private final String outputFileLocations;
		private final String logFileName;
		private final String reportFileName;

		private int schemaVersion;
		private String startLocalName = "starttime";
		private String elapsedLocalName = "elapsedtime";
		private String endLocalName = "endtime";

		public RobotParserCallable(String outputFileLocations, String logFileName, String reportFileName) {
			this.outputFileLocations = outputFileLocations;
			this.logFileName = logFileName;
			this.reportFileName = reportFileName;
		}

		public RobotResult invoke(File ws, VirtualChannel channel)
		throws IOException {
			FileSet setInWorkspace = Util
			.createFileSet(ws, outputFileLocations);
			DirectoryScanner resultScanner = setInWorkspace
			.getDirectoryScanner();

			String[] files = resultScanner.getIncludedFiles();
			if (files.length == 0) {
				throw new AbortException(
						"No files found in path " + ws.getAbsolutePath() + " with configured filemask: " + outputFileLocations);
			}
			RobotResult result = new RobotResult();
			result.setLogFile(this.logFileName);
			result.setReportFile(this.reportFileName);

			for(String file : files){
				XMLInputFactory factory = XMLInputFactory.newInstance();
				factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
				factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
				File baseDirectory = resultScanner.getBasedir();
				File reportFile = new File(baseDirectory, file);

				//get the potential directories emerging from the use of GLOB filemask accounted in the splitted file parsing
				String dirFromFileGLOB = new File(file).getParent();
				if(dirFromFileGLOB != null)
					baseDirectory = new File(baseDirectory, dirFromFileGLOB);
                try (FileInputStream inputStream = new FileInputStream(reportFile)) {
                    XMLStreamReader reader = factory.createXMLStreamReader(inputStream, "UTF-8");
                    try {
                        parseResult(result, reader, baseDirectory);
                    } finally {
                        reader.close();
                    }
                } catch (XMLStreamException e1) {
                    throw new IOException("Parsing of output xml failed!", e1);
                }
			}
			return result;
		}

		private RobotResult parseResult(RobotResult result, XMLStreamReader reader, File baseDirectory) throws XMLStreamException, IOException {
			while(reader.hasNext()){
				reader.next();
				if(reader.isStartElement()){
					String tagName = reader.getLocalName();
					if("statistics".equals(tagName))
						//we already have all data from suites and tests so we can stop parsing
						break;
					else if("robot".equals(tagName)){
						String value = reader.getAttributeValue(null, "schemaversion");
						value = value == null ? "0" : value;
						schemaVersion = Integer.parseInt(value);
						// RF schemaVersion does not follow major version number.
						// schemaVersion 5 == RF7.0
						if (schemaVersion >= 5) {
							startLocalName = "start";
							elapsedLocalName = "elapsed";
							// endLocalName is no longer used
						}
						result.setTimeStamp(reader.getAttributeValue(null, "generated"));
					} else if("suite".equals(tagName)){
						result.addSuite(processSuite(reader, result, baseDirectory));
					}
				}
			}
			return result;
		}

		private RobotSuiteResult processSuite(XMLStreamReader reader, RobotTestObject parent, File baseDirectory) throws IOException, XMLStreamException {
			String splitXMLPath = reader.getAttributeValue(null, "src");
			if (splitXMLPath != null) {
				return getSplitXMLSuite(parent, baseDirectory, splitXMLPath);
			}
			RobotSuiteResult suite = new RobotSuiteResult();
			suite.setSchemaVersion(schemaVersion);
			suite.setLogFile(this.logFileName);
			suite.setReportFile(this.reportFileName);
			suite.setParent(parent);
			suite.setName(reader.getAttributeValue(null, "name"));
			suite.setId(reader.getAttributeValue(null, "id"));
			suite.setDescription("");
			//parse children, which can be test cases or test suites
			while(reader.hasNext()){
				reader.next();
				if(reader.isStartElement()){
					String tagName = reader.getLocalName();
					if("doc".equals(tagName)){
						reader.next();
						if (reader.hasText()) {
							suite.setDescription(reader.getText());
						}
					} else if("suite".equals(tagName)){
						suite.addChild(processSuite(reader, suite, baseDirectory));
					} else if("test".equals(tagName)){
						suite.addCaseResult(processTest(reader, suite));
					} else if("kw".equals(tagName) && "teardown".equalsIgnoreCase(reader.getAttributeValue(null, "type"))) {
						ignoreUntilStarts(reader, "status");
						if ("FAIL".equals(reader.getAttributeValue(null, "status"))) {
							suite.failTeardown();
						}
					} else if("status".equals(tagName)){
						suite.setElapsedTime(reader.getAttributeValue(null, elapsedLocalName));
						suite.setStartTime(reader.getAttributeValue(null, startLocalName));
						suite.setEndTime(reader.getAttributeValue(null, endLocalName));
					}
				} else if (reader.isEndElement() && "suite".equals(reader.getLocalName())) {
					return suite;
				}
			}
			throw xmlException("No matching end tag found for test suite: " + suite.getName(), reader);
		}

		private XMLStreamException xmlException(String message, XMLStreamReader reader) {
			Location location = reader.getLocation();
			return new XMLStreamException(message +
					" (at line: "+
					location.getLineNumber()+
					" column: "+
					location.getColumnNumber()+")");
		}

		private RobotSuiteResult getSplitXMLSuite(RobotTestObject parent, File baseDirectory, String path) throws XMLStreamException, IOException {
			XMLInputFactory factory = XMLInputFactory.newInstance();
			factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
            try (FileInputStream inputStream = new FileInputStream(new File(baseDirectory, path))) {
                XMLStreamReader splitReader = factory.createXMLStreamReader(inputStream, "UTF-8");
                try {
                    while (splitReader.hasNext()) {
                        splitReader.next();
                        if (splitReader.isStartElement() && "suite".equals(splitReader.getLocalName())) {
                            return processSuite(splitReader, parent, baseDirectory);
                        }
                    }
                    throw xmlException("Illegal split xml output. Could not find suite element.", splitReader);
                } finally {
                    splitReader.close();
                }
            }
		}

		private String ignoreUntilStarts(XMLStreamReader reader, String... elements) throws XMLStreamException {
			List<String> elementStack = new ArrayList<>();
			while(reader.hasNext()) {
				reader.next();
				if (reader.isStartElement()) {
					String elem = reader.getLocalName();
					if (elementStack.isEmpty() && isNameInElements(elem, elements)) {
						return elem;
					} else {
						elementStack.add(reader.getLocalName());
					}
				} else if (reader.isEndElement()) {
					if (elementStack.isEmpty()) {
						throw xmlException("Could not find elements "+ Arrays.toString(elements), reader);
					}
					if (!elementStack.get(elementStack.size()-1).equals(reader.getLocalName())) {
						throw xmlException("Illegal xml input. End element "+
								reader.getLocalName()+
								" while waiting for element "+
								elementStack.get(elementStack.size()-1), reader);
					}
					elementStack.remove(elementStack.size()-1);
				}
			}
			throw xmlException("Could not find elements "+Arrays.toString(elements), reader);
		}

		private boolean isNameInElements(String name, String[] elements) {
			for (String element: elements) {
				if (name.equals(element))
						return true;
			}
			return false;
		}

		private void ignoreUntilEnds(XMLStreamReader reader, String element) throws XMLStreamException {
			List<String> elementStack = new ArrayList<>();
			while(reader.hasNext()) {
				reader.next();
				if (reader.isStartElement()) {
						elementStack.add(reader.getLocalName());
				} else if (reader.isEndElement()) {
					String tagName = reader.getLocalName();
					if (elementStack.isEmpty()) {
						if (tagName.equals(element)) {
							return;
						}
						throw xmlException("Illegal xml input. Could not find end of element "+
								element+ ". Unexpected end of element "+ tagName, reader);
					} else if (!elementStack.get(elementStack.size()-1).equals(tagName)) {
						throw xmlException("Illegal xml input. End element "+
								tagName + " while waiting for element "+
								elementStack.get(elementStack.size()-1), reader);
					} else {
						elementStack.remove(elementStack.size()-1);
					}
				}
			}
			throw xmlException("Could not find end of element "+element, reader);
		}

		private String getSpacesPerNestedLevel(int level) {
			StringBuilder spaces = new StringBuilder();
			if (level > 0) {
				spaces.append("\n");
			}
            spaces.append("  ".repeat(Math.max(0, level)));
			return spaces.toString();
		}

		private RobotCaseResult processTest(XMLStreamReader reader, RobotSuiteResult result) throws XMLStreamException {
			RobotCaseResult caseResult = new RobotCaseResult();
			caseResult.setParent(result);
			caseResult.setLogFile(this.logFileName);
			//parse attributes
			caseResult.setName(reader.getAttributeValue(null, "name"));
			caseResult.setId(reader.getAttributeValue(null, "id"));
			//parse test tags
			caseResult.setDescription("");
			caseResult.addTags(new ArrayList<>());
			StringBuilder stackTrace = new StringBuilder();

			//parse stacktrace
			String[] possible_elements = {"kw", "for", "if", "try", "while", "doc", "tags", "tag", "status"};
			String[] elements = {"kw", "for", "if", "try", "while"};
			String xmlTag = ignoreUntilStarts(reader, possible_elements);
			while (isNameInElements(xmlTag, elements)) {
				switch (xmlTag) {
					case "if":
					case "try":
						stackTrace.append(processBranchable(reader, 0));
						break;
					case "for":
					case "while":
						stackTrace.append(processLoop(reader, 0));
						break;
					default:
						stackTrace.append(processKeyword(reader, 0));
						break;
				}
				xmlTag = ignoreUntilStarts(reader, possible_elements);
			}

			caseResult.setStackTrace(stackTrace.toString().trim().replaceAll("\n+", "\n"));

			if (xmlTag.equals("doc")) {
				reader.next();
				if (reader.hasText()) {
					caseResult.setDescription(reader.getText());
				}
				reader.next();
				xmlTag = ignoreUntilStarts(reader, "tags", "tag", "status");
			}
			if (xmlTag.equals("tags") || xmlTag.equals("tag")) {
				caseResult.addTags(processTags(reader));
				if (!reader.getLocalName().equals("status")) {
					ignoreUntilStarts(reader, "status");
				}
			}
			//parse test details from nested status
			caseResult.setPassed("PASS".equals(reader.getAttributeValue(null, "status")));
			caseResult.setSkipped("SKIP".equals(reader.getAttributeValue(null, "status")));

			caseResult.setStarttime(reader.getAttributeValue(null, startLocalName));
			caseResult.setEndtime(reader.getAttributeValue(null, endLocalName));
			if (schemaVersion >= 5) {
				caseResult.setElapsedTime(reader.getAttributeValue(null, elapsedLocalName));
			}
			while(reader.hasNext()){
				reader.next();
				if(reader.isCharacters()){
					String error = reader.getText();
					caseResult.setErrorMsg(error);
				} else if (reader.isEndElement()) {
					if ("status".equals(reader.getLocalName())) {
						break;
					} else {
						throw xmlException("No end tag found for status while parsing test case: " +
								caseResult.getName(), reader);
					}
				}
			}
			// reset stack trace if the test is passed
			if (caseResult.isPassed() || caseResult.isSkipped()) {
				caseResult.setStackTrace("");
			}
			ignoreUntilEnds(reader, "test");
			return caseResult;
		}

		private String processLoop(XMLStreamReader reader, int nestedCount) throws XMLStreamException {
			StringBuilder stackTrace = new StringBuilder();
			String indentation = getSpacesPerNestedLevel(nestedCount);
			String kind = reader.getLocalName();
			stackTrace.append(indentation + kind.toUpperCase());
			if (kind.equals("for")) {
				stackTrace.append(" " + reader.getAttributeValue(null, "flavor"));
			}
			while (reader.hasNext()) {
				if (reader.isEndElement() && reader.getLocalName().equals(kind)) {
					break;
				}
				if (reader.isStartElement() && reader.getLocalName().equals("iter")) {
					stackTrace.append(processIteration(reader, nestedCount+1));
				}
				reader.next();
			}
			stackTrace.append(indentation + "END\n");
			return stackTrace.toString();
		}

		private String processIteration(XMLStreamReader reader, int nestedCount) throws XMLStreamException {
			StringBuilder stackTrace = new StringBuilder();
			while(reader.hasNext()) {
				if (reader.isEndElement() && reader.getLocalName().equals("iter")) {
					break;
				}
				if (reader.isStartElement()) {
					String xmlTag = reader.getLocalName();
					switch (xmlTag) {
						case "for":
						case "while":
							stackTrace.append(processLoop(reader, nestedCount));
							break;
						case "kw":
							stackTrace.append(processKeyword(reader, nestedCount));
							break;
						case "if":
						case "try":
							stackTrace.append(processBranchable(reader, nestedCount));
							break;
						case "return":
						case "break":
						case "continue":
							stackTrace.append(processReturnBreakContinue(reader, nestedCount));
							break;
						default:
							break;
					}
				}
				reader.next();
			}

			return stackTrace.toString();
		}

		private String processBranchable(XMLStreamReader reader, int nestedCount) throws XMLStreamException {
			StringBuilder stackTrace = new StringBuilder();
			String indentation = getSpacesPerNestedLevel(nestedCount);
			String kind = reader.getLocalName();
			while (reader.hasNext()) {
				if (reader.isEndElement() && reader.getLocalName().equals(kind)) {
					break;
				}
				if (reader.isStartElement() && reader.getLocalName().equals("branch")) {
					stackTrace.append(indentation + reader.getAttributeValue(null, "type"));
					stackTrace.append(processBranch(reader, nestedCount+1));
				}
				reader.next();
			}
			stackTrace.append(indentation + "END\n");
			return stackTrace.toString();
		}

		private String processBranch(XMLStreamReader reader, int nestedCount) throws XMLStreamException {
			StringBuilder stackTrace = new StringBuilder();
			while(reader.hasNext()) {
				if (reader.isEndElement() && reader.getLocalName().equals("branch")) {
					break;
				}
				if (reader.isStartElement()) {
					String xmlTag = reader.getLocalName();
					switch (xmlTag) {
						case "for":
						case "while":
							stackTrace.append(processLoop(reader, nestedCount));
							break;
						case "kw":
							stackTrace.append(processKeyword(reader, nestedCount));
							break;
						case "if":
						case "try":
							stackTrace.append(processBranchable(reader, nestedCount));
							break;
						case "return":
						case "break":
						case "continue":
							stackTrace.append(processReturnBreakContinue(reader, nestedCount));
							break;
						default:
							break;
					}
				}
				reader.next();
			}
			return stackTrace.toString();
		}

        @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
		private String processKeyword(XMLStreamReader reader, int nestedCount) throws XMLStreamException {
			StringBuilder stackTrace = new StringBuilder();
			String kw = reader.getAttributeValue(null, "name");
			String indentation = getSpacesPerNestedLevel(nestedCount);
			stackTrace.append(indentation).append(kw);
			reader.next();
			while(reader.hasNext()) {
				if (reader.isEndElement() && reader.getLocalName().equals("kw")) {
					break;
				}
				if (reader.isStartElement()) {
					String xmlTag = reader.getLocalName();
					switch (xmlTag) {
						case "arguments":
						case "arg":
							stackTrace.append(processArgs(reader));
                            // TODO: Refactor processArgs, so it won't return <kw> element and we can break out of this switch case normally and remove the SuppressFBWarnings above
							continue;	// processArgs returns with us already in <kw>. We don't want to use reader.next()
						case "for":
						case "while":
							stackTrace.append(processLoop(reader, nestedCount+1));
							break;
						case "kw":
							stackTrace.append(processKeyword(reader, nestedCount+1));
							break;
						case "if":
						case "try":
							stackTrace.append(processBranchable(reader, nestedCount+1));
							break;
						case "return":
						case "break":
						case "continue":
							stackTrace.append(processReturnBreakContinue(reader, nestedCount+1));
							break;
						default:
							break;
					}
				}
				reader.next();
			}
			stackTrace.append("\n");
			return stackTrace.toString();
		}

		private String processArgs(XMLStreamReader reader) throws XMLStreamException {
			StringBuilder stringBuilder = new StringBuilder();

			while(reader.hasNext()) {
				if (reader.isEndElement() || reader.isStartElement()) {
					String xmlTag = reader.getLocalName();
					if (reader.isEndElement() && xmlTag.equals("arguments") ||
							reader.isStartElement() && xmlTag.equals("status") ||
							reader.isStartElement() && xmlTag.equals("kw")) {
						break;
					}
					if (reader.isStartElement() && xmlTag.equals("arg")) {
						reader.next();
						try {
							stringBuilder.append("    ").append(reader.getText());
						} catch (IllegalStateException ignore) { }
					}
				}
				reader.next();
			}
			return stringBuilder.toString();
		}

		private List<String> processTags(XMLStreamReader reader) throws XMLStreamException {
			List<String> tagList = new ArrayList<>();

			while(reader.hasNext()){
				if(reader.isStartElement() && "tag".equals(reader.getLocalName())){
					while(reader.hasNext()){
						reader.next();
						if(reader.isCharacters()){
							tagList.add(reader.getText());
						} else if(reader.isEndElement() && "tag".equals(reader.getLocalName())){
							break;
						}
					}
				} else if((reader.isEndElement() && "tags".equals(reader.getLocalName())) || (reader.isStartElement() && "status".equals(reader.getLocalName()))){
					break;
				}
				reader.next();
			}
			return tagList;
		}

		private String processReturnBreakContinue(XMLStreamReader reader, int nestedCount) throws XMLStreamException {
			StringBuilder stringBuilder = new StringBuilder();
			String kind = reader.getLocalName();
			stringBuilder.append(getSpacesPerNestedLevel(nestedCount)).append(kind.toUpperCase());
			while(reader.hasNext()) {
				if (reader.isEndElement() && reader.getLocalName().equals(kind)) {
					break;
				}
				if (reader.isStartElement() && reader.getLocalName().equals("kw")) {
					stringBuilder.append(processKeyword(reader, nestedCount+1));
				}
				reader.next();
			}
			return stringBuilder.toString();
		}

		@Override
		public void checkRoles(RoleChecker roleChecker) throws SecurityException {

		}
	}
}
