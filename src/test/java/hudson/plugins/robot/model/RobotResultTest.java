/*
* Copyright 2008-2014 Nokia Solutions and Networks Oy
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*	http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package hudson.plugins.robot.model;

import hudson.plugins.robot.RobotParser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;


public class RobotResultTest {

	private RobotResult result;

	@Before
	public void setUp() throws Exception {
		RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("output.xml", null, null);
		result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("output.xml").toURI()).getParentFile(), null);
		result.tally(null);
	}

	@Test
	public void testShouldParseTimeStamp(){
		assertEquals("20100629 11:08:54.230", result.getTimeStamp());
	}

	@Test
	public void testShouldParseSuites(){
		RobotSuiteResult suite = result.getSuite("Othercases & Testcases");
		assertNotNull(suite);
	}

	@Test
	public void testShouldParseSuiteDescription(){
		RobotSuiteResult suite = result.getSuite("Othercases & Testcases");
		String desc = suite.getDescription();
		assertEquals("Exampledocumentation", desc);
	}

	@Test
	public void testShouldParseNestedSuites(){
		RobotSuiteResult suite = result.getSuite("Othercases & Testcases");
		RobotSuiteResult nestedSuite = suite.getSuite("Testcases");
		assertNotNull(nestedSuite);
	}

	@Test
	public void testShouldParseCases(){
		RobotSuiteResult suite = result.getSuite("Othercases & Testcases");
		RobotCaseResult caseResult = suite.getCase("Hello");
		assertNotNull(caseResult);
	}

	@Test
	public void testShouldParseCaseDescription(){
		RobotSuiteResult suite = result.getSuite("Othercases & Testcases");
		RobotCaseResult caseResult = suite.getCase("Hello");
		String desc = caseResult.getDescription();
		assertEquals("Test", desc);
	}

	@Test
	public void testShouldParseCasesInNestedSuites(){
		RobotSuiteResult suite = result.getSuite("Othercases & Testcases");
		RobotSuiteResult nestedSuite = suite.getSuite("Testcases");
		RobotCaseResult caseResult = nestedSuite.getCase("Failer");
		assertNotNull(caseResult);
	}

	@Test
	public void testShouldParseMultipleSameNamedSuites(){
		assertNotNull(result.getSuite("Somecases_1"));
	}

	@Test
	//TODO; should add tests for all parsed fields? Refactor name to parsertest
	public void testShouldParseCriticalCases(){
		assertEquals(19, result.getCriticalTotal());
	}

	@Test
	public void testShouldParseFailMessages(){
		RobotSuiteResult suite = result.getSuite("Othercases & Testcases");
		RobotSuiteResult childSuite = suite.getSuite("Othercases");
		RobotCaseResult caseResult = childSuite.getCase("Failer");
		String errorMsg = caseResult.getErrorMsg();
		assertEquals("Test failed miserably!", errorMsg.trim());
	}

	@Test
	public void testShouldParseNewCriticalCases() throws Exception{

		RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("new_critical_output.xml", null, null);
		result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("new_critical_output.xml").toURI()).getParentFile(), null);
		result.tally(null);

		assertEquals(14, result.getCriticalTotal());
	}

	@Test
	public void testShouldParseOverallCases(){
		assertEquals(22, result.getOverallTotal());
	}

	@Test
	public void testShouldParseFailedCases(){
		assertEquals(10, result.getOverallFailed());
	}

	@Test
	public void testShouldParseFailedCriticalCases(){
		assertEquals(9, result.getCriticalFailed());
	}

	@Test
	public void testShouldParseFailedNewCriticalCases() throws Exception{
		RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("new_critical_output.xml", null, null);
		result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("new_critical_output.xml").toURI()).getParentFile(), null);
		result.tally(null);

		assertEquals(7, result.getCriticalFailed());
	}

	@Test
	public void testShouldAcceptNoLogAndReport() throws Exception{
		RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("new_critical_output.xml", null, null);
		result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("new_critical_output.xml").toURI()).getParentFile(), null);
		result.tally(null);

		assertFalse(result.getAllFailedCases().get(0).getHasLog());
		assertFalse(result.getAllFailedCases().get(0).getHasReport());
	}

	@Test
	public void testShouldGetLogWhenAvailable() throws Exception{
		RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("new_critical_output.xml", "log.html", "report.html");
		result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("new_critical_output.xml").toURI()).getParentFile(), null);
		result.tally(null);

		assertTrue(result.getAllFailedCases().get(0).getHasLog());
	}

	@Test
	public void testShouldGetLogAndReportInSuites() throws Exception{
		RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("output.xml", "log.html", "report.html");
		result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("output.xml").toURI()).getParentFile(), null);
		result.tally(null);

		RobotSuiteResult suite = result.getSuite("Othercases & Testcases");
		assertTrue(suite.getHasLog());
		assertTrue(suite.getHasReport());
	}

	@Test
	public void testIdShouldBeEmptyWhenNotAvailable() throws Exception{
		RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("new_critical_output.xml", "log.html", null);
		result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("new_critical_output.xml").toURI()).getParentFile(), null);
		result.tally(null);

		assertEquals("", result.getAllFailedCases().get(0).getId());
	}

	@Test
	public void testShouldGetIdWhenAvailable() throws Exception{
		RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("teardown_fail.xml", "log.html", null);
		result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("teardown_fail.xml").toURI()).getParentFile(), null);
		result.tally(null);
		RobotSuiteResult suite = result.getSuite("Fail");
		RobotSuiteResult subSuite = suite.getSuite("Suite");
		RobotCaseResult caseResult = subSuite.getCase("Example test 2");

		assertEquals("s1-s1-t2", caseResult.getId());
	}

	@Test
	public void testShouldParseCriticalityFromStatusInsteadOfTest() throws Exception{
		 RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("new_critical_output.xml", null, null);
		result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("new_critical_output.xml").toURI()).getParentFile(), null);
		result.tally(null);

		RobotSuiteResult suite = result.getSuite("Othercases & Testcases");
		RobotCaseResult caseResult = suite.getCase("Hello");

		assertFalse("Case shouldn't be critical", caseResult.isCritical());
	}

	@Test
	public void testShouldReturnAllFailedCases(){
		List<RobotCaseResult> failers = result.getAllFailedCases();
		assertEquals(10, failers.size());
	}

	@Test
	public void testShouldReturnPackageName(){
		RobotSuiteResult suite = (RobotSuiteResult)result.findObjectById("Othercases & Testcases/Othercases/3rd level cases");
		assertEquals("Othercases & Testcases.Othercases.3rd level cases", suite.getRelativePackageName(result));
	}

	@Test
	public void testShouldReturnSuiteById(){
		RobotSuiteResult suite = (RobotSuiteResult)result.findObjectById("Othercases & Testcases/Othercases/3rd level cases");
		assertEquals("3rd level cases", suite.getName());
	}

	@Test
	public void testShouldReturnIdForSuite(){
		RobotSuiteResult suite = (RobotSuiteResult)result.findObjectById("Othercases & Testcases/Othercases/3rd level cases");
		assertEquals("Othercases%20&%20Testcases/Othercases/3rd%20level%20cases", suite.getRelativeId(result));
	}

	@Test
	public void testShouldURLEncodeSpecialChars(){
		RobotCaseResult caseResult = (RobotCaseResult)result.findObjectById("Othercases & Testcases/Othercases/3rd level cases/Passing Test Case With !\"#€%&()=?`´*'^<>©@£$∞§|[]≈±:;.,");
		assertEquals("Othercases%20&%20Testcases/Othercases/3rd%20level%20cases/Passing%20Test%20Case%20With%20!%22%23%E2%82%AC%25&()=%3F%60%C2%B4*'%5E%3C%3E%C2%A9@%C2%A3$%E2%88%9E%C2%A7%7C%5B%5D%E2%89%88%C2%B1%3A%3B.,", caseResult.getRelativeId(result));
	}

	@Test
	public void testShouldReturnCaseById(){
		RobotCaseResult caseResult = (RobotCaseResult)result.findObjectById("Othercases & Testcases/Othercases/3rd level cases/Hello3rd");
		assertEquals("Hello3rd", caseResult.getName());
	}

	@Test
	public void testShouldReturnCaseTags(){
		RobotCaseResult caseResult = (RobotCaseResult)result.findObjectById("Othercases & Testcases/Othercases/3rd level cases/Hello3rd");
		String tags = StringUtils.join(caseResult.getTags(), ",");
		assertEquals("tag1,tag2", tags);
	}

	@Test
	public void testShouldParseSplittedOutput() throws Exception {
	 	RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("testfile.xml", null, null);
		result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("testfile.xml").toURI()).getParentFile(), null);

		RobotSuiteResult suite = result.getSuite("nestedSuites");
		RobotSuiteResult splittedSuite = suite.getSuite("subSuite");
		RobotSuiteResult splittedNestedSuite = splittedSuite.getSuite("Testcases");
		assertNotNull(splittedNestedSuite);
	}

	@Test
	public void testShouldParseSuiteTeardownFailures() throws Exception {
		RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("teardown_fail.xml", null, null);
		RobotResult res = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("teardown_fail.xml").toURI()).getParentFile(), null);
		List<RobotCaseResult> failers = res.getAllFailedCases();
		assertEquals(3, failers.size());
	}

	@Test
	public void testShouldHandleNameCollisions() throws Exception {
		RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("collisions.xml", null, null);
		RobotResult res = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("collisions.xml").toURI()).getParentFile(), null);
		List<RobotCaseResult> failers = res.getAllFailedCases();
		assertEquals(4, failers.size());
	}

	@Test
	public void testShouldParseWholeSuiteDuration() throws Exception {
		RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("suite-setup-and-teardown.xml", null, null);
		RobotResult res = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("suite-setup-and-teardown.xml").toURI()).getParentFile(), null);
		res.tally(null);
		assertEquals(10141, res.getDuration());
	}

	@Test
	public void testShouldParseSkippedTests() throws Exception {
		RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("robot4_skip.xml", null, null);
		result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("robot4_skip.xml").toURI()).getParentFile(), null);
		result.tally(null);
		assertEquals(6, result.getSkipped());
	}

	@Test
	public void testShouldParsePassedFromRobot4() throws Exception {
		RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("robot4_skip.xml", null, null);
		result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("robot4_skip.xml").toURI()).getParentFile(), null);
		result.tally(null);
		assertEquals(4, result.getPassed());
		assertEquals(66.6, result.getPassPercentage(), 0.1);
	}

	@Test
	public void testShouldParseFailedFromRobot4() throws Exception {
		RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("robot4_skip.xml", null, null);
		result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("robot4_skip.xml").toURI()).getParentFile(), null);
		result.tally(null);
		assertEquals(2, result.getFailed());
	}

	@Test
	public void testShouldParseTotalFromRobot4() throws Exception {
		RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("robot4_skip.xml", null, null);
		result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("robot4_skip.xml").toURI()).getParentFile(), null);
		result.tally(null);
		assertEquals(12, result.getOverallTotal());
	}

	@Test
	public void testShouldParseTagsFromRobot4() throws Exception {
		RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("robot4_skip.xml", null, null);
		result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("robot4_skip.xml").toURI()).getParentFile(), null);
		result.tally(null);

		RobotCaseResult caseResult = (RobotCaseResult)result.findObjectById("Skip/Test 8 Will Always Fail");
		String tags = StringUtils.join(caseResult.getTags(), ",");
		assertEquals("fail,tag2,tag3", tags);
	}

	@Test
	public void testSuiteNameAttributeMightBeMissingInRobot4() throws Exception {
		/**
		 * see more: https://issues.jenkins.io/browse/JENKINS-69807
		 **/
		RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("robot4_empty_suite_name.xml", null, null);
		result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("robot4_empty_suite_name.xml").toURI()).getParentFile(), null);

		RobotSuiteResult r = result.getSuites().iterator().next();
		r = r.getChildSuites().iterator().next();
		RobotCaseResult caseResult = r.getCaseResults().iterator().next();

		// passing null as `thisObject` should not matter, as the name resolving fails first
		// due to getName() returning `null`
		caseResult.getRelativePackageName(null);

	}
}
