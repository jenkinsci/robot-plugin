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
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class RobotResultTest {

    private RobotResult result;

    @BeforeEach
    void setUp() throws Exception {
        RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("output.xml", null, null);
        result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("output.xml").toURI()).getParentFile(), null);
        result.tally(null);
    }

    @Test
    void testShouldParseTimeStamp() {
        assertEquals("20100629 11:08:54.230", result.getTimeStamp());
    }

    @Test
    void testShouldParseSuites() {
        RobotSuiteResult suite = result.getSuite("Othercases & Testcases");
        assertNotNull(suite);
    }

    @Test
    void testShouldParseSuiteDescription() {
        RobotSuiteResult suite = result.getSuite("Othercases & Testcases");
        String desc = suite.getDescription();
        assertEquals("Exampledocumentation", desc);
    }

    @Test
    void testShouldParseNestedSuites() {
        RobotSuiteResult suite = result.getSuite("Othercases & Testcases");
        RobotSuiteResult nestedSuite = suite.getSuite("Testcases");
        assertNotNull(nestedSuite);
    }

    @Test
    void testShouldParseCases() {
        RobotSuiteResult suite = result.getSuite("Othercases & Testcases");
        RobotCaseResult caseResult = suite.getCase("Hello");
        assertNotNull(caseResult);
    }

    @Test
    void testShouldParseCaseDescription() {
        RobotSuiteResult suite = result.getSuite("Othercases & Testcases");
        RobotCaseResult caseResult = suite.getCase("Hello");
        String desc = caseResult.getDescription();
        assertEquals("Test", desc);
    }

    @Test
    void testShouldParseCasesInNestedSuites() {
        RobotSuiteResult suite = result.getSuite("Othercases & Testcases");
        RobotSuiteResult nestedSuite = suite.getSuite("Testcases");
        RobotCaseResult caseResult = nestedSuite.getCase("Failer");
        assertNotNull(caseResult);
    }

    @Test
    void testShouldParseMultipleSameNamedSuites() {
        assertNotNull(result.getSuite("Somecases_1"));
    }

    @Test
    void testShouldParseFailMessages() {
        RobotSuiteResult suite = result.getSuite("Othercases & Testcases");
        RobotSuiteResult childSuite = suite.getSuite("Othercases");
        RobotCaseResult caseResult = childSuite.getCase("Failer");
        String errorMsg = caseResult.getErrorMsg();
        assertEquals("Test failed miserably!", errorMsg.trim());
    }

    @Test
    void testShouldParseOverallCases() {
        assertEquals(22, result.getOverallTotal());
    }

    @Test
    void testShouldParseFailedCases() {
        assertEquals(10, result.getOverallFailed());
    }

    @Test
    void testShouldAcceptNoLogAndReport() throws Exception {
        RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("new_critical_output.xml", null, null);
        result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("new_critical_output.xml").toURI()).getParentFile(), null);
        result.tally(null);

        assertFalse(result.getAllFailedCases().get(0).getHasLog());
        assertFalse(result.getAllFailedCases().get(0).getHasReport());
    }

    @Test
    void testShouldGetLogWhenAvailable() throws Exception {
        RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("new_critical_output.xml", "log.html", "report.html");
        result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("new_critical_output.xml").toURI()).getParentFile(), null);
        result.tally(null);

        assertTrue(result.getAllFailedCases().get(0).getHasLog());
    }

    @Test
    void testShouldGetLogAndReportInSuites() throws Exception {
        RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("output.xml", "log.html", "report.html");
        result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("output.xml").toURI()).getParentFile(), null);
        result.tally(null);

        RobotSuiteResult suite = result.getSuite("Othercases & Testcases");
        assertTrue(suite.getHasLog());
        assertTrue(suite.getHasReport());
    }

    @Test
    void testIdShouldBeEmptyWhenNotAvailable() throws Exception {
        RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("new_critical_output.xml", "log.html", null);
        result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("new_critical_output.xml").toURI()).getParentFile(), null);
        result.tally(null);

        assertEquals("", result.getAllFailedCases().get(0).getId());
    }

    @Test
    void testShouldGetIdWhenAvailable() throws Exception {
        RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("teardown_fail.xml", "log.html", null);
        result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("teardown_fail.xml").toURI()).getParentFile(), null);
        result.tally(null);
        RobotSuiteResult suite = result.getSuite("Fail");
        RobotSuiteResult subSuite = suite.getSuite("Suite");
        RobotCaseResult caseResult = subSuite.getCase("Example test 2");

        assertEquals("s1-s1-t2", caseResult.getId());
    }

    @Test
    void testShouldReturnAllFailedCases() {
        List<RobotCaseResult> failers = result.getAllFailedCases();
        assertEquals(10, failers.size());
    }

    @Test
    void testShouldReturnPackageName() {
        RobotSuiteResult suite = (RobotSuiteResult) result.findObjectById("Othercases & Testcases/Othercases/3rd level cases");
        assertEquals("Othercases & Testcases.Othercases.3rd level cases", suite.getRelativePackageName(result));
    }

    @Test
    void testShouldReturnSuiteById() {
        RobotSuiteResult suite = (RobotSuiteResult) result.findObjectById("Othercases & Testcases/Othercases/3rd level cases");
        assertEquals("3rd level cases", suite.getName());
    }

    @Test
    void testShouldReturnIdForSuite() {
        RobotSuiteResult suite = (RobotSuiteResult) result.findObjectById("Othercases & Testcases/Othercases/3rd level cases");
        assertEquals("Othercases%20&%20Testcases/Othercases/3rd%20level%20cases", suite.getRelativeId(result));
    }

    @Test
    void testShouldURLEncodeSpecialChars() {
        RobotCaseResult caseResult = (RobotCaseResult) result.findObjectById("Othercases & Testcases/Othercases/3rd level cases/Passing Test Case With !\"#€%&()=?`´*'^<>©@£$∞§|[]≈±:;.,");
        assertEquals("Othercases%20&%20Testcases/Othercases/3rd%20level%20cases/Passing%20Test%20Case%20With%20!%22%23%E2%82%AC%25&()=%3F%60%C2%B4*'%5E%3C%3E%C2%A9@%C2%A3$%E2%88%9E%C2%A7%7C%5B%5D%E2%89%88%C2%B1%3A%3B.,", caseResult.getRelativeId(result));
    }

    @Test
    void testShouldReturnCaseById() {
        RobotCaseResult caseResult = (RobotCaseResult) result.findObjectById("Othercases & Testcases/Othercases/3rd level cases/Hello3rd");
        assertEquals("Hello3rd", caseResult.getName());
    }

    @Test
    void testShouldReturnCaseTags() {
        RobotCaseResult caseResult = (RobotCaseResult) result.findObjectById("Othercases & Testcases/Othercases/3rd level cases/Hello3rd");
        String tags = StringUtils.join(caseResult.getTags(), ",");
        assertEquals("tag1,tag2", tags);
    }

    @Test
    void testShouldParseSplittedOutput() throws Exception {
        RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("testfile.xml", null, null);
        result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("testfile.xml").toURI()).getParentFile(), null);

        RobotSuiteResult suite = result.getSuite("nestedSuites");
        RobotSuiteResult splittedSuite = suite.getSuite("subSuite");
        RobotSuiteResult splittedNestedSuite = splittedSuite.getSuite("Testcases");
        assertNotNull(splittedNestedSuite);
    }

    @Test
    void testShouldParseSuiteTeardownFailures() throws Exception {
        RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("teardown_fail.xml", null, null);
        RobotResult res = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("teardown_fail.xml").toURI()).getParentFile(), null);
        List<RobotCaseResult> failers = res.getAllFailedCases();
        assertEquals(3, failers.size());
    }

    @Test
    void testShouldHandleNameCollisions() throws Exception {
        RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("collisions.xml", null, null);
        RobotResult res = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("collisions.xml").toURI()).getParentFile(), null);
        List<RobotCaseResult> failers = res.getAllFailedCases();
        assertEquals(4, failers.size());
    }

    @Test
    void testShouldParseWholeSuiteDuration() throws Exception {
        RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("suite-setup-and-teardown.xml", null, null);
        RobotResult res = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("suite-setup-and-teardown.xml").toURI()).getParentFile(), null);
        res.tally(null);
        assertEquals(10141, res.getDuration());
    }

    @Test
    void testShouldParseSkippedTests() throws Exception {
        RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("robot4_skip.xml", null, null);
        result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("robot4_skip.xml").toURI()).getParentFile(), null);
        result.tally(null);
        assertEquals(6, result.getSkipped());
    }

    @Test
    void testShouldParsePassedFromRobot4() throws Exception {
        RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("robot4_skip.xml", null, null);
        result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("robot4_skip.xml").toURI()).getParentFile(), null);
        result.tally(null);
        assertEquals(4, result.getPassed());
        assertEquals(66.6, result.getPassPercentage(), 0.1);
    }

    @Test
    void testShouldParseFailedFromRobot4() throws Exception {
        RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("robot4_skip.xml", null, null);
        result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("robot4_skip.xml").toURI()).getParentFile(), null);
        result.tally(null);
        assertEquals(2, result.getFailed());
    }

    @Test
    void testShouldParseTotalFromRobot4() throws Exception {
        RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("robot4_skip.xml", null, null);
        result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("robot4_skip.xml").toURI()).getParentFile(), null);
        result.tally(null);
        assertEquals(12, result.getOverallTotal());
    }

    @Test
    void testShouldParseTagsFromRobot4() throws Exception {
        RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("robot4_skip.xml", null, null);
        result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("robot4_skip.xml").toURI()).getParentFile(), null);
        result.tally(null);

        RobotCaseResult caseResult = (RobotCaseResult) result.findObjectById("Skip/Test 8 Will Always Fail");
        String tags = StringUtils.join(caseResult.getTags(), ",");
        assertEquals("fail,tag2,tag3", tags);
    }

    @Test
    @Issue("JENKINS-69807")
    void testSuiteNameAttributeMightBeMissingInRobot4() throws Exception {
        RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("robot4_empty_suite_name.xml", null, null);
        result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("robot4_empty_suite_name.xml").toURI()).getParentFile(), null);

        RobotSuiteResult r = result.getSuites().iterator().next();
        r = r.getChildSuites().iterator().next();
        RobotCaseResult caseResult = r.getCaseResults().iterator().next();

        // passing null as `thisObject` should not matter, as the name resolving fails first
        // due to getName() returning `null`
        caseResult.getRelativePackageName(null);
    }

    @Test
    void testCaseResultsShouldBeCorrectlySet() throws Exception {
        File directory = new File(RobotResultTest.class.getResource("../robot7").toURI());
        RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("inline_var_output.xml", null, null);
        result = remoteOperation.invoke(directory, null);
        result.tally(null);

        RobotSuiteResult suite = result.getSuite("Rf7");
        RobotCaseResult caseResult = suite.getCase("Test Inline Var");

        // suite results
        assertEquals(35.0, suite.getDuration(), 0.01);

        // case results
        assertEquals("2023-11-13T15:33:07.168330", caseResult.getStarttime());
        assertEquals(0.001748, caseResult.getElapsedtime(), 0.01);
        assertNull(caseResult.getEndtime());
    }

    @Test
    void testGetPassPercentageWithoutSkippedTests() throws Exception {
        RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("robot4_skip.xml", null, null);
        result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("robot4_skip.xml").toURI()).getParentFile(), null);
        result.tally(null);

        assertEquals(66.6, result.getPassPercentage(false), 0);
    }

    @Test
    void testGetPassPercentageWithSkippedTests() throws Exception {
        RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("robot4_skip.xml", null, null);
        result = remoteOperation.invoke(new File(RobotSuiteResultTest.class.getResource("robot4_skip.xml").toURI()).getParentFile(), null);
        result.tally(null);

        assertEquals(33.3, result.getPassPercentage(true), 0);
    }
}
