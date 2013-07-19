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
package hudson.plugins.robot.model;

import hudson.plugins.robot.RobotParser;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

import junit.framework.TestCase;


public class RobotResultTest extends TestCase {

	private RobotResult result;

	protected void setUp() throws Exception {
		super.setUp();

		RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("output.xml");
		result = remoteOperation.invoke(new File(new RobotSuiteResultTest().getClass().getResource("output.xml").toURI()).getParentFile(), null);
		result.tally(null);
	}

	public void testShouldParseTimeStamp(){
		assertEquals("20100629 11:08:54.230", result.getTimeStamp());
	}

	public void testShouldParseSuites(){
		RobotSuiteResult suite = result.getSuite("Othercases_&_Testcases");
		assertNotNull(suite);
	}

	public void testShouldParseNestedSuites(){
		RobotSuiteResult suite = result.getSuite("Othercases_&_Testcases");
		RobotSuiteResult nestedSuite = suite.getSuite("Testcases");
		assertNotNull(nestedSuite);
	}

	public void testShouldParseCases(){
		RobotSuiteResult suite = result.getSuite("Othercases_&_Testcases");
		RobotCaseResult caseResult = suite.getCase("Hello");
		assertNotNull(caseResult);
	}

	public void testShouldParseCasesInNestedSuites(){
		RobotSuiteResult suite = result.getSuite("Othercases_&_Testcases");
		RobotSuiteResult nestedSuite = suite.getSuite("Testcases");
		RobotCaseResult caseResult = nestedSuite.getCase("Failer");
		assertNotNull(caseResult);
	}

	public void testShouldParseMultipleSameNamedSuites(){
		assertNotNull(result.getSuite("Somecases_1"));
	}

	//TODO; should add tests for all parsed fields? Refactor name to parsertest
	public void testShouldParseCriticalCases(){
		assertEquals(18, result.getCriticalTotal());
	}

	public void testShouldParseFailMessages(){
		RobotSuiteResult suite = result.getSuite("Othercases_&_Testcases");
		RobotSuiteResult childSuite = suite.getSuite("Othercases");
		RobotCaseResult caseResult = childSuite.getCase("Failer");
		String errorMsg = caseResult.getErrorMsg();
		assertEquals("Test failed miserably!", errorMsg.trim());
	}
	public void testShouldParseNewCriticalCases() throws Exception{

        RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("new_critical_output.xml");
		result = remoteOperation.invoke(new File(new RobotSuiteResultTest().getClass().getResource("new_critical_output.xml").toURI()).getParentFile(), null);
		result.tally(null);

		assertEquals(14, result.getCriticalTotal());
	}

	public void testShouldParseOverallCases(){
		assertEquals(21, result.getOverallTotal());
	}

	public void testShouldParseFailedCases(){
		assertEquals(10, result.getOverallFailed());
	}

	public void testShouldParseFailedCriticalCases(){
		assertEquals(9, result.getCriticalFailed());
	}

	public void testShouldParseFailedNewCriticalCases() throws Exception{
		 RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("new_critical_output.xml");
			result = remoteOperation.invoke(new File(new RobotSuiteResultTest().getClass().getResource("new_critical_output.xml").toURI()).getParentFile(), null);
		result.tally(null);

		assertEquals(7, result.getCriticalFailed());
	}

	public void testShouldParseCriticalityFromStatusInsteadOfTest() throws Exception{
		 RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("new_critical_output.xml");
			result = remoteOperation.invoke(new File(new RobotSuiteResultTest().getClass().getResource("new_critical_output.xml").toURI()).getParentFile(), null);
		result.tally(null);

		RobotSuiteResult suite = result.getSuite("Othercases_&_Testcases");
		RobotCaseResult caseResult = suite.getCase("Hello");

		assertFalse("Case shouldn't be critical", caseResult.isCritical());
	}

	public void testShouldReturnAllFailedCases(){
		List<RobotCaseResult> failers = result.getAllFailedCases();
		assertEquals(10, failers.size()
				);
	}

	public void testShouldReturnPackageName(){
		RobotSuiteResult suite = (RobotSuiteResult)result.findObjectById("Othercases_&_Testcases/Othercases/3rd_level_cases");
		assertEquals("Othercases & Testcases.Othercases.3rd level cases", suite.getRelativePackageName(result));
	}

	public void testShouldReturnSuiteById(){
		RobotSuiteResult suite = (RobotSuiteResult)result.findObjectById("Othercases_&_Testcases/Othercases/3rd_level_cases");
		assertEquals("3rd level cases", suite.getName());
	}

	public void testShouldReturnIdForSuite(){
		RobotSuiteResult suite = (RobotSuiteResult)result.findObjectById("Othercases_&_Testcases/Othercases/3rd_level_cases");
		assertEquals("Othercases_&_Testcases/Othercases/3rd_level_cases", suite.getRelativeId(result));
	}

	public void testShouldReturnCaseById(){
		RobotCaseResult caseResult = (RobotCaseResult)result.findObjectById("Othercases_&_Testcases/Othercases/3rd_level_cases/Hello3rd");
		assertEquals("Hello3rd", caseResult.getName());
	}


	public void testShouldParseSplittedOutput() throws Exception, URISyntaxException{
		 RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("testfile.xml");
			result = remoteOperation.invoke(new File(new RobotSuiteResultTest().getClass().getResource("testfile.xml").toURI()).getParentFile(), null);

		RobotSuiteResult suite = result.getSuite("nestedSuites");
		RobotSuiteResult splittedSuite = suite.getSuite("subSuite");
		RobotSuiteResult splittedNestedSuite = splittedSuite.getSuite("Testcases");
		assertNotNull(splittedNestedSuite);
	}

	public void testShouldParseSuiteTeardownFailures() throws Exception, URISyntaxException{
		RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("teardown_fail.xml");
		RobotResult res = remoteOperation.invoke(new File(new RobotSuiteResultTest().getClass().getResource("teardown_fail.xml").toURI()).getParentFile(), null);
		List<RobotCaseResult> failers = res.getAllFailedCases();
		assertEquals(3, failers.size());
	}

}
