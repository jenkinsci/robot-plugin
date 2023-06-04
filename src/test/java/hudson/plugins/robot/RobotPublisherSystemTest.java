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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import static org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.recipes.LocalData;

import org.htmlunit.WebAssert;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlTable;

public class RobotPublisherSystemTest {

	@Rule
	public JenkinsRule j = new JenkinsRule();

	@Test
	public void testRoundTripConfig() throws Exception {
		FreeStyleProject p = j.jenkins.createProject(FreeStyleProject.class, "testRoundTripConfig");
		RobotPublisher before = new RobotPublisher(null, "a", "b", false, "c", "d", 11, 27, true, "dir1/*.jpg, dir2/*.png",
				false, "");
		p.getPublishersList().add(before);
		j.configRoundtrip(p);
		RobotPublisher after = p.getPublishersList().get(RobotPublisher.class);
		assertThat(
				"outputPath,outputFileName,reportFileName,logFileName,passThreshold,unstableThreshold,onlyCritical,otherFiles",
				before, samePropertyValuesAs(after));
	}

	@Test
	public void testConfigView() throws Exception {
		FreeStyleProject p = j.jenkins.createProject(FreeStyleProject.class, "testConfigView");
		RobotPublisher before = new RobotPublisher(null, "a", "b", false, "c", "d", 11, 27, true, "dir1/*.jpg, dir2/*.png",
				false, "");
		p.getPublishersList().add(before);
		HtmlPage page = j.createWebClient().getPage(p, "configure");
		WebAssert.assertTextPresent(page, "Publish Robot Framework");
		WebAssert.assertInputPresent(page, "_.outputPath");
		WebAssert.assertInputContainsValue(page, "_.outputPath", "a");
		WebAssert.assertInputPresent(page, "_.outputFileName");
		WebAssert.assertInputContainsValue(page, "_.outputFileName", "b");
		WebAssert.assertInputPresent(page, "_.reportFileName");
		WebAssert.assertInputContainsValue(page, "_.reportFileName", "c");
		WebAssert.assertInputPresent(page, "_.logFileName");
		WebAssert.assertInputContainsValue(page, "_.logFileName", "d");
		WebAssert.assertInputPresent(page, "_.unstableThreshold");
		WebAssert.assertInputContainsValue(page, "_.unstableThreshold", "27.0");
		WebAssert.assertInputPresent(page, "_.passThreshold");
		WebAssert.assertInputContainsValue(page, "_.passThreshold", "11.0");
		WebAssert.assertInputPresent(page, "_.onlyCritical");
		WebAssert.assertInputContainsValue(page, "_.onlyCritical", "on");
		WebAssert.assertInputPresent(page, "_.otherFiles");
		WebAssert.assertInputContainsValue(page, "_.otherFiles", "dir1/*.jpg,dir2/*.png");
	}

	@LocalData
	@Test
	public void testPublish() throws Exception {
		Run lastBuild = this.executeJobWithSuccess("robot");

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

		assertTrue("output.xml was not stored", storedOutput.exists());
		assertTrue("output-001.xml was not stored", storedSplitOutput.exists());
		assertTrue("report.html was not stored", storedReport.exists());
		assertTrue("report-001.html was not stored", storedSplitReport.exists());
		assertTrue("log.html was not stored", storedLog.exists());
		assertTrue("log-001.html was not stored", storedSplitLog.exists());
		assertTrue("log.js was not stored", storedJs.exists());
		assertTrue("log-001.js was not stored", storedSplitJs1.exists());
		assertTrue("screenshot.png was not stored", storedImage1.exists());
		assertTrue("screenshot2.png was not stored", storedImage2.exists());
		assertFalse("dummy.file was copied", storedDummy.exists());
	}

	@LocalData
	@Test
	public void testDontCopyOuputWhendisableArchiveOutput() throws Exception {
		Run lastBuild = this.executeJobWithSuccess("disable-archive-output-xml");

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

		assertFalse("output.xml was copied", storedOutput.exists());
		assertFalse("output-001.xml was copied", storedSplitOutput.exists());
		assertTrue("report.html was not stored", storedReport.exists());
		assertTrue("report-001.html was not stored", storedSplitReport.exists());
		assertTrue("log.html was not stored", storedLog.exists());
		assertTrue("log-001.html was not stored", storedSplitLog.exists());
		assertTrue("log.js was not stored", storedJs.exists());
		assertTrue("log-001.js was not stored", storedSplitJs1.exists());
		assertTrue("screenshot.png was not stored", storedImage1.exists());
		assertTrue("screenshot2.png was not stored", storedImage2.exists());
		assertFalse("dummy.file was copied", storedDummy.exists());
	}

	@LocalData
	@Test
	public void testDontCopyExcessFilesWhenOtherFilesEmpty() throws Exception {
		Run lastBuild = this.executeJobWithSuccess("dont-copy");

		File storedOutput = new File(lastBuild.getRootDir(), RobotPublisher.DEFAULT_ARCHIVE_DIR + "/output.xml");
		File storedSplitOutput = new File(lastBuild.getRootDir(), RobotPublisher.DEFAULT_ARCHIVE_DIR + "/output-001.xml");
		File storedDummy = new File(lastBuild.getRootDir(), RobotPublisher.DEFAULT_ARCHIVE_DIR + "/dummy.file");

		assertTrue("output.xml was not stored", storedOutput.exists());
		assertTrue("output-001.xml was not stored", storedSplitOutput.exists());
		assertFalse("dummy.file was copied", storedDummy.exists());
	}

	@LocalData
	@Test
	public void testActionViewsWithNoRuns() throws Exception {
		WebClient wc = j.createWebClient();
		HtmlPage page = wc.goTo("job/robot/");

		WebAssert.assertElementPresentByXPath(page, "//div[@id='tasks']//a[@href='/jenkins/job/robot/robot']");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//p[contains(.,'No results available yet.')]");
		WebAssert.assertElementNotPresentByXPath(page, "//div[@id='main-panel']//img[@id='passfailgraph']");

		page = wc.goTo("job/robot/robot/");
		WebAssert.assertTextPresent(page, "No robot results available yet!");
	}

	@LocalData
	@Test
	public void testOldActionViewsWithData() throws Exception {
		WebClient wc = j.createWebClient();
		HtmlPage page = wc.goTo("job/oldrobotbuild/");
		WebAssert.assertElementPresentByXPath(page, "//div[@id='tasks']//a[@href='/jenkins/job/oldrobotbuild/robot']");
		WebAssert.assertElementPresentByXPath(page, "//div[@id='main-panel']//h4[contains(.,'Latest Robot Results:')]");
		WebAssert.assertElementPresentByXPath(page, "//div[@id='main-panel']//img[@id='passfailgraph']");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//a[@href='/jenkins/job/oldrobotbuild/1/robot' and contains(text(),'Browse results')]");
		this.verifyTotalsTable(page, 8, 4, 0, "50.0", 8, 4, "50.0");

		page = wc.goTo("job/oldrobotbuild/robot/");
		WebAssert.assertTitleEquals(page, "Testcases & Othercases Test Report");

		page = wc.goTo("job/oldrobotbuild/1/");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='tasks']//a[@href='/jenkins/job/oldrobotbuild/1/robot']");
		WebAssert.assertElementPresentByXPath(page, "//div[@id='main-panel']//h4[contains(.,'Robot Test Summary:')]");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//a[@href='/jenkins/job/oldrobotbuild/1/robot' and contains(text(),'Browse results')]");
		this.verifyTotalsTable(page, 8, 4, 0,"50.0", 8, 4, "50.0");

		page = wc.goTo("job/oldrobotbuild/1/robot/");
		WebAssert.assertTitleEquals(page, "Testcases & Othercases Test Report");
	}

	@LocalData
	@Test
	public void testSummariesWithData() throws Exception {
		WebClient wc = this.executeJobAndGetWebClient("robot");

		HtmlPage page = wc.goTo("job/robot/");
		WebAssert.assertElementPresentByXPath(page, "//div[@id='tasks']//a[@href='/jenkins/job/robot/robot']");
		WebAssert.assertElementPresentByXPath(page, "//div[@id='main-panel']//h4[contains(.,'Latest Robot Results:')]");
		WebAssert.assertElementPresentByXPath(page, "//div[@id='main-panel']//img[@id='passfailgraph']");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//a[@href='/jenkins/job/robot/1/robot' and contains(text(),'Browse results')]");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//a[@href='/jenkins/job/robot/1/robot/report/report.html' and contains(text(), 'Open report.html')]");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//a[@href='/jenkins/job/robot/1/robot/report/log.html' and contains(text(), 'Open log.html')]");
		verifyTotalsTable(page, 8, 4, 0,"50.0", 8, 4, "50.0");

		page = wc.goTo("job/robot/1/");
		WebAssert.assertElementPresentByXPath(page, "//div[@id='tasks']//a[@href='/jenkins/job/robot/1/robot']");
		WebAssert.assertElementPresentByXPath(page, "//div[@id='main-panel']//h4[contains(.,'Robot Test Summary:')]");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//a[@href='/jenkins/job/robot/1/robot' and contains(text(),'Browse results')]");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//a[@href='/jenkins/job/robot/1/robot/report/report.html' and contains(text(), 'Open report.html')]");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//a[@href='/jenkins/job/robot/1/robot/report/log.html' and contains(text(), 'Open log.html')]");
		verifyTotalsTable(page, 8, 4, 0,"50.0", 8, 4, "50.0");
	}

	@LocalData
	@Test
	public void testSummariesWithVariablesInFileNames() throws Exception {
		WebClient wc = this.executeJobAndGetWebClient("files-with-env-vars");

		HtmlPage page = wc.goTo("job/files-with-env-vars/");
		WebAssert.assertElementPresentByXPath(page, "//div[@id='tasks']//a[@href='/jenkins/job/files-with-env-vars/robot']");
		WebAssert.assertElementPresentByXPath(page, "//div[@id='main-panel']//h4[contains(.,'Latest Robot Results:')]");
		WebAssert.assertElementPresentByXPath(page, "//div[@id='main-panel']//img[@id='passfailgraph']");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//a[@href='/jenkins/job/files-with-env-vars/1/robot' and contains(text(),'Browse results')]");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//a[@href='/jenkins/job/files-with-env-vars/1/robot/report/report_1.html' and contains(text(), 'Open report_1.html')]");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//a[@href='/jenkins/job/files-with-env-vars/1/robot/report/log_1.html' and contains(text(), 'Open log_1.html')]");
		verifyTotalsTable(page, 6, 1, 0,"83.3", 6, 1, "83.3");

		page = wc.goTo("job/files-with-env-vars/1/");
		WebAssert.assertElementPresentByXPath(page, "//div[@id='tasks']//a[@href='/jenkins/job/files-with-env-vars/1/robot']");
		WebAssert.assertElementPresentByXPath(page, "//div[@id='main-panel']//h4[contains(.,'Robot Test Summary:')]");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//a[@href='/jenkins/job/files-with-env-vars/1/robot' and contains(text(),'Browse results')]");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//a[@href='/jenkins/job/files-with-env-vars/1/robot/report/report_1.html' and contains(text(), 'Open report_1.html')]");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//a[@href='/jenkins/job/files-with-env-vars/1/robot/report/log_1.html' and contains(text(), 'Open log_1.html')]");
		verifyTotalsTable(page, 6, 1, 0,"83.3", 6, 1, "83.3");
	}

	@LocalData
	@Test
	public void testRobot29Outputs() throws Exception {
		WebClient wc = this.executeJobAndGetWebClient("robot29output");
		HtmlPage page = wc.goTo("job/robot29output/");
		verifyTotalsTable(page, 1, 0, 0,"100.0", 1, 0, "100.0");
	}

	@LocalData
	@Test
	public void testCombinedOutputs() throws Exception {
		WebClient wc = this.executeJobAndGetWebClient("several-outputs");

		HtmlPage page = wc.goTo("job/several-outputs/");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='tasks']//a[@href='/jenkins/job/several-outputs/robot']");
		WebAssert.assertElementPresentByXPath(page, "//div[@id='main-panel']//h4[contains(.,'Latest Robot Results:')]");
		WebAssert.assertElementPresentByXPath(page, "//div[@id='main-panel']//img[@id='passfailgraph']");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//a[@href='/jenkins/job/several-outputs/1/robot' and contains(text(),'Browse results')]");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//a[@href='/jenkins/job/several-outputs/1/robot/report/**/report.html' and contains(text(), 'Open **/report.html')]");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//a[@href='/jenkins/job/several-outputs/1/robot/report/**/log.html' and contains(text(), 'Open **/log.html')]");
		verifyTotalsTable(page, 2, 0, 0,"100.0", 2, 0, "100.0");
	}

	@LocalData
	@Test
	public void testReportPage() throws Exception {
		WebClient wc = this.executeJobAndGetWebClient("robot");

		HtmlPage page = wc.goTo("job/robot/robot/");
		WebAssert.assertTextPresent(page, "Robot Framework Test Results");
		WebAssert.assertTextPresent(page, "4 passed, 4 failed");
		WebAssert.assertTextPresent(page, "0:00:00.041 (+0:00:00.041)");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//a[@href='Testcases%20&%20Othercases/Testcases/Not%20equal' and contains(.,'Testcases & Othercases.Testcases.Not equal')]");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//a[@href='Testcases%20&%20Othercases/Othercases' and contains(.,'Testcases & Othercases.Othercases')]");

		page = wc.goTo("job/robot/1/robot/report/");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//a[@href='output.xml' and contains(.,'output.xml')]");

		page = wc.goTo("job/robot/1/robot/Testcases%20&%20Othercases");
		WebAssert.assertTextPresent(page, "4 passed, 4 failed");
		WebAssert.assertTextPresent(page, "0:00:00.041 (+0:00:00.041)");
		WebAssert.assertTextPresent(page, "Failed Test Cases");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//a[@href='Testcases/Not%20equal' and contains(.,'Testcases.Not equal')]");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//a[@href='Othercases' and contains(.,'Othercases')]");

		page = wc.goTo("job/robot/1/robot/Testcases%20&%20Othercases/Othercases");
		WebAssert.assertTextPresent(page, "2 passed, 2 failed");
		WebAssert.assertTextPresent(page, "0:00:00.008 (+0:00:00.008)");
		WebAssert.assertTextPresent(page, "Test Cases");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//a[@href='Not%20equal' and contains(.,'Not equal')]");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//a[@href='Contains%20string' and contains(.,'Contains string')]");

		page = wc.goTo("job/robot/1/robot/Testcases%20&%20Othercases/Othercases/Not%20equal");
		WebAssert.assertTextPresent(page, "Not equal");
		WebAssert.assertTextPresent(page, "FAIL");
		WebAssert.assertTextPresent(page, "Message:");
		WebAssert.assertTextPresent(page, "Hello, world! != Good bye, world!");
		WebAssert.assertTextPresent(page, "0:00:00.001 (+0:00:00.001)");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//img[@src='durationGraph?maxBuildsToShow=0']");

		page = wc.goTo("job/robot/1/robot/Testcases%20&%20Othercases/Othercases/Contains%20string");
		WebAssert.assertTextPresent(page, "PASS");
		WebAssert.assertTextNotPresent(page, "Message:");

	}

	@LocalData
	@Test
	public void testMissingReportFileWithOld() throws Exception {
		Project testProject = this.getProject("oldrobotbuild");

		WebClient wc = j.createWebClient();

		File buildRoot = testProject.getLastBuild().getRootDir();
		File robotHtmlReport = new File(buildRoot, RobotPublisher.DEFAULT_ARCHIVE_DIR + "/report.html");
		if (!robotHtmlReport.delete())
			fail("Unable to delete report directory");

		HtmlPage page = wc.goTo("job/oldrobotbuild/robot/");
		WebAssert.assertTextPresent(page, "No Robot html report found!");

		page = wc.goTo("job/oldrobotbuild/1/robot/");
		WebAssert.assertTextPresent(page, "No Robot html report found!");
	}

	@LocalData
	@Test
	public void testFailedSince() {
		Jenkins jenkins = j.getInstance();
		List<Project> projects = jenkins.getAllItems(Project.class);
		Run lastRun = null;
		for (Project project : projects) {
			if (project.getName().equalsIgnoreCase("failingtests")) {
				lastRun = project.getLastCompletedBuild();
			}
		}
		if (lastRun == null)
			fail("No build including Robot results was found");

		RobotBuildAction action = lastRun.getAction(RobotBuildAction.class);
		RobotResult result = action.getResult();
		RobotCaseResult firstFailed = result.getAllFailedCases().get(0);
		assertEquals(2, firstFailed.getFailedSince());
	}

	@LocalData
	@Test
	public void testMatrixBuildReportLinks() throws Exception {
		WebClient wc = j.createWebClient();
		HtmlPage page = wc.goTo("job/matrix-robot/FOO=bar/2");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//a[@href='/jenkins/job/matrix-robot/FOO=bar/2/robot' and contains(.,'Browse results')]");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//a[@href='/jenkins/job/matrix-robot/FOO=bar/2/robot/report/report.html' and contains(.,'Open report.html')]");
	}

	@LocalData
	@Test
	public void testMatrixBuildSummary() throws Exception {
		Jenkins jenkins = j.getInstance();
		List<MatrixProject> projects = jenkins.getAllItems(MatrixProject.class);
		MatrixProject testProject = null;
		for (MatrixProject project : projects){
			System.out.println(project.getName());
			if(project.getName().equals("matrix-robot")) testProject = project;
		}
		if(testProject == null) fail("Couldn't find example project");
		
		j.assertBuildStatusSuccess(testProject.scheduleBuild2(0));
		WebClient wc = j.createWebClient();
		HtmlPage page = wc.goTo("job/matrix-robot");
		WebAssert.assertElementPresentByXPath(page, "//div[@id='tasks']//a[@href='/jenkins/job/matrix-robot/robot']");
		WebAssert.assertElementPresentByXPath(page, "//div[@id='main-panel']//img[@id='passfailgraph']");

		page = wc.goTo("job/matrix-robot/3");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//a[@href='/jenkins/job/matrix-robot/3/robot']");
		WebAssert.assertElementPresentByXPath(page, "//div[@id='main-panel']//h4[contains(.,'Robot Test Summary:')]");
		WebAssert.assertElementPresentByXPath(page,
				"//div[@id='main-panel']//a[@href='/jenkins/job/matrix-robot/3/robot' and contains(text(),'Browse results')]");
	}
	
	@Test 
    public void testRobotPipelineStep() throws Exception {
        WorkflowJob pipelineJob = j.jenkins.createProject(WorkflowJob.class, "pipelineJob");
        // Replace because of Windows path escapes in pipeline config
        String outputPath = new File("src/test/resources/hudson/plugins/robot").getAbsolutePath().replace("\\", "\\\\");
        String outputFileName = "low_failure_output.xml";
        pipelineJob.setDefinition(new CpsFlowDefinition("node {robot outputFileName: '"+outputFileName+"', outputPath: '"+outputPath+"'}", true));
        j.assertLogContains("Done publishing Robot results.", j.assertBuildStatusSuccess(pipelineJob.scheduleBuild2(0)));
    }
	
	private WebClient executeJobAndGetWebClient(String projectName) throws Exception {
		this.executeJobWithSuccess(projectName);
		return j.createWebClient();
	}

	private AbstractBuild executeJobWithSuccess(String projectName) throws Exception {
		return this.executeJob(projectName, true);
	}

	private AbstractBuild executeJob(String projectName, boolean success) throws Exception {
		Jenkins jenkins = j.getInstance();
		List<Project> projects = jenkins.getAllItems(Project.class);
		Project testProject = null;
		for (Project project : projects) {
			if (project.getName().equals(projectName))
				testProject = project;
		}
		if (testProject == null)
			fail("Couldn't find example project");
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
			if (project.getName().equals(projectName))
				testProject = project;
		}
		if (testProject == null)
			fail("Couldn't find example project");
		return testProject;
	}

	private void verifyTotalsTable(HtmlPage page, int totalTests, int totalFailed, int totalSkipped, String totalPercents,
			int totalCritical, int criticalFailed, String criticalPercents) {
		HtmlTable table = page.getHtmlElementById("robot-summary-table");
		String expectedTable = "<tableclass=\"table\"id=\"robot-summary-table\"><tbody><tr><th/><th>Total</th><th>Failed</th><th>Passed</th><th>Skipped</th><th>Pass%</th></tr><tr><th>Criticaltests</th>"
				+ "<tdclass=\"table-upper-row\"style=\"border-left:0px;\">" + totalCritical + "</td>"
				+ "<tdclass=\"table-upper-row\"><spanclass=\"" + (criticalFailed == 0 ? "pass" : "fail") + "\">" + criticalFailed + "</span></td>"
				+ "<tdclass=\"table-upper-row\">" + (totalCritical - totalFailed) + "</td>"
				+ "<tdclass=\"table-upper-row\">0</td>"
				+ "<tdclass=\"table-upper-row\">" + criticalPercents + "</td></tr>"
				+ "<tr><th>Alltests</th><tdstyle=\"border-left:0px;\">" + totalTests + "</td>"
				+ "<td><spanclass=\"" + (totalFailed == 0 ? "pass" : "fail") + "\">" + totalFailed + "</span></td>"
				+ "<td>" + (totalTests - totalFailed) + "</td>"
				+ "<td>" + totalSkipped + "</td>"
				+ "<td>" + totalPercents + "</td></tr></tbody></table>";
		assertTrue(table.asXml().replaceAll("\\s", "").contains(expectedTable));
	}

}
