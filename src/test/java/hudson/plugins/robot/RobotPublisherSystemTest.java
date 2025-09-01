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

import hudson.matrix.MatrixProject;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Project;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.robot.model.RobotCaseResult;
import hudson.plugins.robot.model.RobotResult;
import jenkins.model.Jenkins;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlTable;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;

import java.io.File;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs;
import static org.htmlunit.WebAssert.assertElementNotPresentByXPath;
import static org.htmlunit.WebAssert.assertElementPresentByXPath;
import static org.htmlunit.WebAssert.assertInputContainsValue;
import static org.htmlunit.WebAssert.assertInputPresent;
import static org.htmlunit.WebAssert.assertTextNotPresent;
import static org.htmlunit.WebAssert.assertTextPresent;
import static org.htmlunit.WebAssert.assertTitleEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WithJenkins
class RobotPublisherSystemTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void testRoundTripConfig() throws Exception {
        FreeStyleProject p = j.jenkins.createProject(FreeStyleProject.class, "testRoundTripConfig");
        RobotPublisher before = new RobotPublisher(null, "a", "b", false, "c", "d", 11, 27, true, "dir1/*.jpg, dir2/*.png",
                false, "", false);
        p.getPublishersList().add(before);
        j.configRoundtrip(p);
        RobotPublisher after = p.getPublishersList().get(RobotPublisher.class);
        assertThat(
                "outputPath,outputFileName,reportFileName,logFileName,passThreshold,unstableThreshold,countSkippedTests,otherFiles",
                before, samePropertyValuesAs(after));
    }

    @Test
    void testConfigView() throws Exception {
        FreeStyleProject p = j.jenkins.createProject(FreeStyleProject.class, "testConfigView");
        RobotPublisher before = new RobotPublisher(null, "a", "b", false, "c", "d", 11, 27, true, "dir1/*.jpg, dir2/*.png",
                false, "", false);
        p.getPublishersList().add(before);
        HtmlPage page = j.createWebClient().getPage(p, "configure");
        assertTextPresent(page, "Publish Robot Framework");
        assertInputPresent(page, "_.outputPath");
        assertInputContainsValue(page, "_.outputPath", "a");
        assertInputPresent(page, "_.outputFileName");
        assertInputContainsValue(page, "_.outputFileName", "b");
        assertInputPresent(page, "_.reportFileName");
        assertInputContainsValue(page, "_.reportFileName", "c");
        assertInputPresent(page, "_.logFileName");
        assertInputContainsValue(page, "_.logFileName", "d");
        assertInputPresent(page, "_.unstableThreshold");
        assertInputContainsValue(page, "_.unstableThreshold", "27.0");
        assertInputPresent(page, "_.passThreshold");
        assertInputContainsValue(page, "_.passThreshold", "11.0");
        assertInputPresent(page, "_.countSkippedTests");
        assertInputContainsValue(page, "_.countSkippedTests", "on");
        assertInputPresent(page, "_.otherFiles");
        assertInputContainsValue(page, "_.otherFiles", "dir1/*.jpg,dir2/*.png");
    }

    @LocalData
    @Test
    void testPublish() throws Exception {
        Run lastBuild = executeJobWithSuccess("robot");

        File storedOutput = new File(lastBuild.getRootDir(), RobotPublisher.DEFAULT_ARCHIVE_DIR + "/output.xml");
        File storedSplitOutput = new File(lastBuild.getRootDir(), RobotPublisher.DEFAULT_ARCHIVE_DIR + "/output-001.xml");
        File storedReport = new File(lastBuild.getRootDir(), RobotPublisher.DEFAULT_ARCHIVE_DIR + "/report.html");
        File storedSplitReport = new File(lastBuild.getRootDir(), RobotPublisher.DEFAULT_ARCHIVE_DIR + "/report.html");
        File storedLog = new File(lastBuild.getRootDir(), RobotPublisher.DEFAULT_ARCHIVE_DIR + "/log.html");
        File storedSplitLog = new File(lastBuild.getRootDir(), RobotPublisher.DEFAULT_ARCHIVE_DIR + "/log-001.html");
        File storedJs = new File(lastBuild.getRootDir(), RobotPublisher.DEFAULT_ARCHIVE_DIR + "/log.js");
        File storedSplitJs1 = new File(lastBuild.getRootDir(), RobotPublisher.DEFAULT_ARCHIVE_DIR + "/log-001.js");
        File storedImage1 = new File(lastBuild.getRootDir(), RobotPublisher.DEFAULT_ARCHIVE_DIR + "/screenshot.png");
        File storedImage2 = new File(lastBuild.getRootDir(),
                RobotPublisher.DEFAULT_ARCHIVE_DIR + "/subfolder/screenshot2.png");
        File storedDummy = new File(lastBuild.getRootDir(), RobotPublisher.DEFAULT_ARCHIVE_DIR + "dummy.file");

        assertTrue(storedOutput.exists(), "output.xml was not stored");
        assertTrue(storedSplitOutput.exists(), "output-001.xml was not stored");
        assertTrue(storedReport.exists(), "report.html was not stored");
        assertTrue(storedSplitReport.exists(), "report-001.html was not stored");
        assertTrue(storedLog.exists(), "log.html was not stored");
        assertTrue(storedSplitLog.exists(), "log-001.html was not stored");
        assertTrue(storedJs.exists(), "log.js was not stored");
        assertTrue(storedSplitJs1.exists(), "log-001.js was not stored");
        assertTrue(storedImage1.exists(), "screenshot.png was not stored");
        assertTrue(storedImage2.exists(), "screenshot2.png was not stored");
        assertFalse(storedDummy.exists(), "dummy.file was copied");
    }

    @LocalData
    @Test
    void testDontCopyOutputWhenDisableArchiveOutput() throws Exception {
        Run lastBuild = executeJobWithSuccess("disable-archive-output-xml");

        File storedOutput = new File(lastBuild.getRootDir(), RobotPublisher.DEFAULT_ARCHIVE_DIR + "/output.xml");
        File storedSplitOutput = new File(lastBuild.getRootDir(), RobotPublisher.DEFAULT_ARCHIVE_DIR + "/output-001.xml");
        File storedReport = new File(lastBuild.getRootDir(), RobotPublisher.DEFAULT_ARCHIVE_DIR + "/report.html");
        File storedSplitReport = new File(lastBuild.getRootDir(), RobotPublisher.DEFAULT_ARCHIVE_DIR + "/report.html");
        File storedLog = new File(lastBuild.getRootDir(), RobotPublisher.DEFAULT_ARCHIVE_DIR + "/log.html");
        File storedSplitLog = new File(lastBuild.getRootDir(), RobotPublisher.DEFAULT_ARCHIVE_DIR + "/log-001.html");
        File storedJs = new File(lastBuild.getRootDir(), RobotPublisher.DEFAULT_ARCHIVE_DIR + "/log.js");
        File storedSplitJs1 = new File(lastBuild.getRootDir(), RobotPublisher.DEFAULT_ARCHIVE_DIR + "/log-001.js");
        File storedImage1 = new File(lastBuild.getRootDir(), RobotPublisher.DEFAULT_ARCHIVE_DIR + "/screenshot.png");
        File storedImage2 = new File(lastBuild.getRootDir(),
                RobotPublisher.DEFAULT_ARCHIVE_DIR + "/subfolder/screenshot2.png");
        File storedDummy = new File(lastBuild.getRootDir(), RobotPublisher.DEFAULT_ARCHIVE_DIR + "dummy.file");

        assertFalse(storedOutput.exists(), "output.xml was copied");
        assertFalse(storedSplitOutput.exists(), "output-001.xml was copied");
        assertTrue(storedReport.exists(), "report.html was not stored");
        assertTrue(storedSplitReport.exists(), "report-001.html was not stored");
        assertTrue(storedLog.exists(), "log.html was not stored");
        assertTrue(storedSplitLog.exists(), "log-001.html was not stored");
        assertTrue(storedJs.exists(), "log.js was not stored");
        assertTrue(storedSplitJs1.exists(), "log-001.js was not stored");
        assertTrue(storedImage1.exists(), "screenshot.png was not stored");
        assertTrue(storedImage2.exists(), "screenshot2.png was not stored");
        assertFalse(storedDummy.exists(), "dummy.file was copied");
    }

    @LocalData
    @Test
    void testDontCopyExcessFilesWhenOtherFilesEmpty() throws Exception {
        Run lastBuild = executeJobWithSuccess("dont-copy");

        File storedOutput = new File(lastBuild.getRootDir(), RobotPublisher.DEFAULT_ARCHIVE_DIR + "/output.xml");
        File storedSplitOutput = new File(lastBuild.getRootDir(), RobotPublisher.DEFAULT_ARCHIVE_DIR + "/output-001.xml");
        File storedDummy = new File(lastBuild.getRootDir(), RobotPublisher.DEFAULT_ARCHIVE_DIR + "/dummy.file");

        assertTrue(storedOutput.exists(), "output.xml was not stored");
        assertTrue(storedSplitOutput.exists(), "output-001.xml was not stored");
        assertFalse(storedDummy.exists(), "dummy.file was copied");
    }

    @LocalData
    @Test
    void testActionViewsWithNoRuns() throws Exception {
        WebClient wc = j.createWebClient();
        HtmlPage page = wc.goTo("job/robot/");

        assertElementPresentByXPath(page, "//div[@id='tasks']//a[@href='/jenkins/job/robot/robot']");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//p[contains(.,'No results available yet.')]");
        assertElementNotPresentByXPath(page, "//div[@id='main-panel']//img[@id='passfailgraph']");

        page = wc.goTo("job/robot/robot/");
        assertTextPresent(page, "No robot results available yet!");
    }

    @LocalData
    @Test
    void testOldActionViewsWithData() throws Exception {
        WebClient wc = j.createWebClient();
        HtmlPage page = wc.goTo("job/oldrobotbuild/");
        assertElementPresentByXPath(page, "//div[@id='tasks']//a[@href='/jenkins/job/oldrobotbuild/robot']");
        assertElementPresentByXPath(page, "//div[@id='main-panel']//h4[contains(.,'Latest Robot Results:')]");
        assertElementPresentByXPath(page, "//div[@id='main-panel']//img[@id='passfailgraph']");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//a[@href='/jenkins/job/oldrobotbuild/1/robot' and contains(text(),'Browse results')]");
        verifyTotalsTable(page, 8, 4, 0, "50.0");

        page = wc.goTo("job/oldrobotbuild/robot/");
        assertTitleEquals(page, "Testcases & Othercases Test Report");

        page = wc.goTo("job/oldrobotbuild/1/");
        assertElementPresentByXPath(page,
                "//div[@id='tasks']//a[@href='/jenkins/job/oldrobotbuild/1/robot']");
        assertElementPresentByXPath(page, "//div[@id='main-panel']//h4[contains(.,'Robot Test Summary:')]");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//a[@href='/jenkins/job/oldrobotbuild/1/robot' and contains(text(),'Browse results')]");
        verifyTotalsTable(page, 8, 4, 0, "50.0");

        page = wc.goTo("job/oldrobotbuild/1/robot/");
        assertTitleEquals(page, "Testcases & Othercases Test Report");
    }

    @LocalData
    @Test
    void testSummariesWithData() throws Exception {
        WebClient wc = executeJobAndGetWebClient("robot");

        HtmlPage page = wc.goTo("job/robot/");
        assertElementPresentByXPath(page, "//div[@id='tasks']//a[@href='/jenkins/job/robot/robot']");
        assertElementPresentByXPath(page, "//div[@id='main-panel']//h4[contains(.,'Latest Robot Results:')]");
        assertElementPresentByXPath(page, "//div[@id='main-panel']//img[@id='passfailgraph']");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//a[@href='/jenkins/job/robot/1/robot' and contains(text(),'Browse results')]");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//a[@href='/jenkins/job/robot/1/robot/report/report.html' and contains(text(), 'Open report.html')]");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//a[@href='/jenkins/job/robot/1/robot/report/log.html' and contains(text(), 'Open log.html')]");
        verifyTotalsTable(page, 8, 4, 0, "50.0");

        page = wc.goTo("job/robot/1/");
        assertElementPresentByXPath(page, "//div[@id='tasks']//a[@href='/jenkins/job/robot/1/robot']");
        assertElementPresentByXPath(page, "//div[@id='main-panel']//h4[contains(.,'Robot Test Summary:')]");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//a[@href='/jenkins/job/robot/1/robot' and contains(text(),'Browse results')]");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//a[@href='/jenkins/job/robot/1/robot/report/report.html' and contains(text(), 'Open report.html')]");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//a[@href='/jenkins/job/robot/1/robot/report/log.html' and contains(text(), 'Open log.html')]");
        verifyTotalsTable(page, 8, 4, 0, "50.0");
    }

    @LocalData
    @Test
    void testSummariesWithVariablesInFileNames() throws Exception {
        WebClient wc = executeJobAndGetWebClient("files-with-env-vars");

        HtmlPage page = wc.goTo("job/files-with-env-vars/");
        assertElementPresentByXPath(page, "//div[@id='tasks']//a[@href='/jenkins/job/files-with-env-vars/robot']");
        assertElementPresentByXPath(page, "//div[@id='main-panel']//h4[contains(.,'Latest Robot Results:')]");
        assertElementPresentByXPath(page, "//div[@id='main-panel']//img[@id='passfailgraph']");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//a[@href='/jenkins/job/files-with-env-vars/1/robot' and contains(text(),'Browse results')]");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//a[@href='/jenkins/job/files-with-env-vars/1/robot/report/report_1.html' and contains(text(), 'Open report_1.html')]");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//a[@href='/jenkins/job/files-with-env-vars/1/robot/report/log_1.html' and contains(text(), 'Open log_1.html')]");
        verifyTotalsTable(page, 6, 1, 0, "83.3");

        page = wc.goTo("job/files-with-env-vars/1/");
        assertElementPresentByXPath(page, "//div[@id='tasks']//a[@href='/jenkins/job/files-with-env-vars/1/robot']");
        assertElementPresentByXPath(page, "//div[@id='main-panel']//h4[contains(.,'Robot Test Summary:')]");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//a[@href='/jenkins/job/files-with-env-vars/1/robot' and contains(text(),'Browse results')]");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//a[@href='/jenkins/job/files-with-env-vars/1/robot/report/report_1.html' and contains(text(), 'Open report_1.html')]");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//a[@href='/jenkins/job/files-with-env-vars/1/robot/report/log_1.html' and contains(text(), 'Open log_1.html')]");
        verifyTotalsTable(page, 6, 1, 0, "83.3");
    }

    @LocalData
    @Test
    void testRobot29Outputs() throws Exception {
        WebClient wc = executeJobAndGetWebClient("robot29output");
        HtmlPage page = wc.goTo("job/robot29output/");
        verifyTotalsTable(page, 1, 0, 0, "100.0");
    }

    @LocalData
    @Test
    void testCombinedOutputs() throws Exception {
        WebClient wc = executeJobAndGetWebClient("several-outputs");

        HtmlPage page = wc.goTo("job/several-outputs/");
        assertElementPresentByXPath(page,
                "//div[@id='tasks']//a[@href='/jenkins/job/several-outputs/robot']");
        assertElementPresentByXPath(page, "//div[@id='main-panel']//h4[contains(.,'Latest Robot Results:')]");
        assertElementPresentByXPath(page, "//div[@id='main-panel']//img[@id='passfailgraph']");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//a[@href='/jenkins/job/several-outputs/1/robot' and contains(text(),'Browse results')]");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//a[@href='/jenkins/job/several-outputs/1/robot/report/**/report.html' and contains(text(), 'Open **/report.html')]");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//a[@href='/jenkins/job/several-outputs/1/robot/report/**/log.html' and contains(text(), 'Open **/log.html')]");
        verifyTotalsTable(page, 2, 0, 0, "100.0");
    }

    @LocalData
    @Test
    void testReportPage() throws Exception {
        WebClient wc = executeJobAndGetWebClient("robot");

        HtmlPage page = wc.goTo("job/robot/robot/");
        assertTextPresent(page, "Robot Framework Test Results");
        assertTextPresent(page, "4 passed, 4 failed");
        assertTextPresent(page, "0:00:00.041 (+0:00:00.041)");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//a[@href='Testcases%20&%20Othercases/Testcases/Not%20equal' and contains(.,'Testcases & Othercases.Testcases.Not equal')]");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//a[@href='Testcases%20&%20Othercases/Othercases' and contains(.,'Testcases & Othercases.Othercases')]");

        page = wc.goTo("job/robot/1/robot/report/");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//a[@href='output.xml' and contains(.,'output.xml')]");

        page = wc.goTo("job/robot/1/robot/Testcases%20&%20Othercases");
        assertTextPresent(page, "4 passed, 4 failed");
        assertTextPresent(page, "0:00:00.041 (+0:00:00.041)");
        assertTextPresent(page, "Failed Test Cases");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//a[@href='Testcases/Not%20equal' and contains(.,'Testcases.Not equal')]");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//a[@href='Othercases' and contains(.,'Othercases')]");

        page = wc.goTo("job/robot/1/robot/Testcases%20&%20Othercases/Othercases");
        assertTextPresent(page, "2 passed, 2 failed");
        assertTextPresent(page, "0:00:00.008 (+0:00:00.008)");
        assertTextPresent(page, "Test Cases");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//a[@href='Not%20equal' and contains(.,'Not equal')]");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//a[@href='Contains%20string' and contains(.,'Contains string')]");

        page = wc.goTo("job/robot/1/robot/Testcases%20&%20Othercases/Othercases/Not%20equal");
        assertTextPresent(page, "Not equal");
        assertTextPresent(page, "FAIL");
        assertTextPresent(page, "Message:");
        assertTextPresent(page, "Hello, world! != Good bye, world!");
        assertTextPresent(page, "0:00:00.001 (+0:00:00.001)");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//img[@src='durationGraph?maxBuildsToShow=0']");

        page = wc.goTo("job/robot/1/robot/Testcases%20&%20Othercases/Othercases/Contains%20string");
        assertTextPresent(page, "PASS");
        assertTextNotPresent(page, "Message:");
    }

    @LocalData
    @Test
    void testMissingReportFileWithOld() throws Exception {
        Project testProject = getProject("oldrobotbuild");

        WebClient wc = j.createWebClient();

        File buildRoot = testProject.getLastBuild().getRootDir();
        File robotHtmlReport = new File(buildRoot, RobotPublisher.DEFAULT_ARCHIVE_DIR + "/report.html");

        assertTrue(robotHtmlReport.delete(), "Unable to delete report directory");

        HtmlPage page = wc.goTo("job/oldrobotbuild/robot/");
        assertTextPresent(page, "No Robot html report found!");

        page = wc.goTo("job/oldrobotbuild/1/robot/");
        assertTextPresent(page, "No Robot html report found!");
    }

    @LocalData
    @Test
    void testFailedSince() {
        Jenkins jenkins = j.getInstance();
        List<Project> projects = jenkins.getAllItems(Project.class);
        Run lastRun = null;
        for (Project project : projects) {
            if (project.getName().equalsIgnoreCase("failingtests")) {
                lastRun = project.getLastCompletedBuild();
            }
        }

        assertNotNull(lastRun, "No build including Robot results was found");

        RobotBuildAction action = lastRun.getAction(RobotBuildAction.class);
        RobotResult result = action.getResult();
        RobotCaseResult firstFailed = result.getAllFailedCases().get(0);
        assertEquals(2, firstFailed.getFailedSince());
    }

    @LocalData
    @Test
    void testMatrixBuildReportLinks() throws Exception {
        WebClient wc = j.createWebClient();
        HtmlPage page = wc.goTo("job/matrix-robot/FOO=bar/2");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//a[@href='/jenkins/job/matrix-robot/FOO=bar/2/robot' and contains(.,'Browse results')]");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//a[@href='/jenkins/job/matrix-robot/FOO=bar/2/robot/report/report.html' and contains(.,'Open report.html')]");
    }

    @LocalData
    @Test
    void testMatrixBuildSummary() throws Exception {
        Jenkins jenkins = j.getInstance();
        List<MatrixProject> projects = jenkins.getAllItems(MatrixProject.class);
        MatrixProject testProject = null;
        for (MatrixProject project : projects) {
            System.out.println(project.getName());
            if (project.getName().equals("matrix-robot")) {
                testProject = project;
            }
        }

        assertNotNull(testProject, "Couldn't find example project");

        j.assertBuildStatusSuccess(testProject.scheduleBuild2(0));
        WebClient wc = j.createWebClient();
        HtmlPage page = wc.goTo("job/matrix-robot");
        assertElementPresentByXPath(page, "//div[@id='tasks']//a[@href='/jenkins/job/matrix-robot/robot']");
        assertElementPresentByXPath(page, "//div[@id='main-panel']//img[@id='passfailgraph']");

        page = wc.goTo("job/matrix-robot/3");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//a[@href='/jenkins/job/matrix-robot/3/robot']");
        assertElementPresentByXPath(page, "//div[@id='main-panel']//h4[contains(.,'Robot Test Summary:')]");
        assertElementPresentByXPath(page,
                "//div[@id='main-panel']//a[@href='/jenkins/job/matrix-robot/3/robot' and contains(text(),'Browse results')]");
    }

    @Test
    void testRobotPipelineStep() throws Exception {
        WorkflowJob pipelineJob = j.jenkins.createProject(WorkflowJob.class, "pipelineJob");
        // Replace because of Windows path escapes in pipeline config
        String outputPath = new File("src/test/resources/hudson/plugins/robot").getAbsolutePath().replace("\\", "\\\\");
        String outputFileName = "low_failure_output.xml";
        pipelineJob.setDefinition(new CpsFlowDefinition("node {robot outputFileName: '" + outputFileName + "', outputPath: '" + outputPath + "'}", true));
        j.assertLogContains("Done publishing Robot results.", j.assertBuildStatusSuccess(pipelineJob.scheduleBuild2(0)));
    }

    private WebClient executeJobAndGetWebClient(String projectName) throws Exception {
        executeJobWithSuccess(projectName);
        return j.createWebClient();
    }

    private AbstractBuild executeJobWithSuccess(String projectName) throws Exception {
        return executeJob(projectName, true);
    }

    private AbstractBuild executeJob(String projectName, boolean success) throws Exception {
        Jenkins jenkins = j.getInstance();
        List<Project> projects = jenkins.getAllItems(Project.class);
        Project testProject = null;
        for (Project project : projects) {
            if (project.getName().equals(projectName)) {
                testProject = project;
            }
        }

        assertNotNull(testProject, "Couldn't find example project");

        if (success) {
            j.assertBuildStatusSuccess(testProject.scheduleBuild2(0));
        } else {
            j.assertBuildStatus(Result.FAILURE, testProject.scheduleBuild2(0));
        }
        return testProject.getLastBuild();
    }

    private Project getProject(String projectName) {
        Jenkins jenkins = j.getInstance();
        List<Project> projects = jenkins.getAllItems(Project.class);
        Project testProject = null;
        for (Project project : projects) {
            if (project.getName().equals(projectName)) {
                testProject = project;
            }
        }

        assertNotNull(testProject, "Couldn't find example project");

        return testProject;
    }

    private void verifyTotalsTable(HtmlPage page, int totalTests, int totalFailed, int totalSkipped, String totalPercents) {
        HtmlTable table = page.getHtmlElementById("robot-summary-table");
        String expectedTable = "<tableclass=\"table\"id=\"robot-summary-table\"><tbody><tr><th/><th>Total</th><th>Failed</th><th>Passed</th><th>Skipped</th><th>Pass%</th></tr>"
                + "<tr><th>Alltests</th><tdstyle=\"border-left:0px;\">" + totalTests + "</td>"
                + "<td><spanclass=\"" + (totalFailed == 0 ? "pass" : "fail") + "\">" + totalFailed + "</span></td>"
                + "<td>" + (totalTests - totalFailed) + "</td>"
                + "<td>" + totalSkipped + "</td>"
                + "<td>" + totalPercents + "</td></tr></tbody></table>";
        assertTrue(table.asXml().replaceAll("\\s", "").contains(expectedTable));
    }

}
