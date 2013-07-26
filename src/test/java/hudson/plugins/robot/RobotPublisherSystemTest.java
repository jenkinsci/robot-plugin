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

import hudson.model.Result;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.model.Project;
import hudson.model.Run;
import hudson.plugins.robot.model.RobotCaseResult;
import hudson.plugins.robot.model.RobotResult;

import java.io.File;
import java.util.List;
import java.util.concurrent.Future;

import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.recipes.LocalData;

import com.gargoylesoftware.htmlunit.WebAssert;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class RobotPublisherSystemTest extends HudsonTestCase {

	public void testRoundTripConfig() throws Exception{
		FreeStyleProject p = createFreeStyleProject();
		RobotPublisher before = new RobotPublisher("a", "b", "c", "d", 11, 27, true, "", "dir1/*.jpg, dir2/*.png");
		p.getPublishersList().add(before);

		submit(getWebClient().getPage(p, "configure")
				.getFormByName("config"));

		RobotPublisher after = p.getPublishersList().get(RobotPublisher.class);

		assertEqualBeans(before, after, "outputPath,outputFileName,reportFileName,logFileName,passThreshold,unstableThreshold,onlyCritical,otherFiles");
	}

	public void testConfigView() throws Exception{
		FreeStyleProject p = createFreeStyleProject();
		RobotPublisher before = new RobotPublisher("a", "b", "c", "d", 11, 27, true, "", "dir1/*.jpg, dir2/*.png");
		p.getPublishersList().add(before);
		HtmlPage page = getWebClient().getPage(p,"configure");
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
	public void testPublish() throws Exception{
		Hudson hudson = Hudson.getInstance();
		List<Project> projects = hudson.getProjects();
		Project testProject = null;
		for (Project project : projects){
			if(project.getName().equals("robot")) testProject = project;
		}
		if(testProject == null) fail("Couldn't find example project");
		Future<Run> run = testProject.scheduleBuild2(0);

		while(!run.isDone()){
			Thread.sleep(5);
		}

		Run lastBuild = testProject.getLastBuild();
		assertTrue("Build wasn't a success", lastBuild.getResult() == Result.SUCCESS);

		File storedOutput = new File(lastBuild.getRootDir(), RobotPublisher.FILE_ARCHIVE_DIR + "/output.xml");
		File storedSplitOutput = new File(lastBuild.getRootDir(), RobotPublisher.FILE_ARCHIVE_DIR + "/output-001.xml");
		File storedReport = new File(lastBuild.getRootDir(), RobotPublisher.FILE_ARCHIVE_DIR + "/report.html");
		File storedSplitReport = new File(lastBuild.getRootDir(), RobotPublisher.FILE_ARCHIVE_DIR + "/report.html");
		File storedLog = new File(lastBuild.getRootDir(), RobotPublisher.FILE_ARCHIVE_DIR + "/log.html");
		File storedSplitLog = new File(lastBuild.getRootDir(), RobotPublisher.FILE_ARCHIVE_DIR + "/log-001.html");
		File storedJs = new File(lastBuild.getRootDir(), RobotPublisher.FILE_ARCHIVE_DIR + "/log.js");
		File storedSplitJs1 = new File(lastBuild.getRootDir(), RobotPublisher.FILE_ARCHIVE_DIR + "/log-001.js");
		File storedImage1 = new File(lastBuild.getRootDir(), RobotPublisher.FILE_ARCHIVE_DIR + "/screenshot.png");
		File storedImage2 = new File(lastBuild.getRootDir(), RobotPublisher.FILE_ARCHIVE_DIR + "/subfolder/screenshot2.png");

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
	}

	@LocalData
	public void testActionViewsWithNoRuns() throws Exception{
		WebClient wc = getWebClient();
		HtmlPage page = wc.goTo("job/robot/");

		WebAssert.assertElementPresentByXPath(page, "//div[@id='navigation']//a[@href='/job/robot/robot']");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/p[contains(.,'No results available yet.')]");
		WebAssert.assertElementNotPresentByXPath(page, "//td[@id='main-panel']//img[@src='robot/graph']");

		page = wc.goTo("job/robot/robot/");
		WebAssert.assertTextPresent(page, "No robot results available yet!");
	}

	@LocalData
	public void testOldActionViewsWithData() throws Exception{
		WebClient wc = getWebClient();
		HtmlPage page = wc.goTo("job/oldrobotbuild/");
		WebAssert.assertElementPresentByXPath(page, "//div[@id='navigation']//a[@href='/job/oldrobotbuild/robot']");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/h3[contains(.,'Latest Robot Results:')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/ul/li[contains(.,'Pass ratio (Critical tests): 50%')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/ul/li[contains(.,'Pass ratio (All tests): 50%')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/ul/li[contains(.,'Tests passed (Critical tests): 4/8')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/ul/li[contains(.,'Tests passed (All tests): 4/8')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//img[@src='robot/graph']");

		page = wc.goTo("job/oldrobotbuild/robot/");
		WebAssert.assertTitleEquals(page, "Testcases & Othercases Test Report");

		page = wc.goTo("job/oldrobotbuild/1/");
		WebAssert.assertElementPresentByXPath(page, "//div[@id='navigation']//a[@href='/job/oldrobotbuild/1/robot']");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/h3[contains(.,'Robot Test Summary:')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/ul/li[contains(.,'Pass ratio (Critical tests): 50%')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/ul/li[contains(.,'Pass ratio (All tests): 50%')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/ul/li[contains(.,'Tests passed (Critical tests): 4/8')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/ul/li[contains(.,'Tests passed (All tests): 4/8')]");

		page = wc.goTo("job/oldrobotbuild/1/robot/");
		WebAssert.assertTitleEquals(page, "Testcases & Othercases Test Report");
	}

	@LocalData
	public void testSummariesWithData() throws Exception{
		Hudson hudson = Hudson.getInstance();
		List<Project> projects = hudson.getProjects();
		Project testProject = null;
		for (Project project : projects){
			if(project.getName().equals("robot")) testProject = project;
		}
		if(testProject == null) fail("Couldn't find example project");;
		Future<Run> run = testProject.scheduleBuild2(0);

		while(!run.isDone()){
			Thread.sleep(5);
		}

		Run lastBuild = testProject.getLastBuild();
		assertTrue("Build wasn't a success", lastBuild.getResult() == Result.SUCCESS);

		WebClient wc = getWebClient();

		HtmlPage page = wc.goTo("job/robot/");
		WebAssert.assertElementPresentByXPath(page, "//div[@id='navigation']//a[@href='/job/robot/robot']");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/h3[contains(.,'Latest Robot Results:')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/ul/li[contains(.,'Pass ratio (Critical tests): 50%')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/ul/li[contains(.,'Pass ratio (All tests): 50%')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/ul/li[contains(.,'Tests passed (Critical tests): 4/8')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/ul/li[contains(.,'Tests passed (All tests): 4/8')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//img[@src='robot/graph']");

		page = wc.goTo("job/robot/1/");
		WebAssert.assertElementPresentByXPath(page, "//div[@id='navigation']//a[@href='/job/robot/1/robot']");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/h3[contains(.,'Robot Test Summary:')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/ul/li[contains(.,'Pass ratio (Critical tests): 50%')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/ul/li[contains(.,'Pass ratio (All tests): 50%')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/ul/li[contains(.,'Tests passed (Critical tests): 4/8')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/ul/li[contains(.,'Tests passed (All tests): 4/8')]");
	}

	@LocalData
	public void testReportPage() throws Exception {
		Hudson hudson = Hudson.getInstance();
		List<Project> projects = hudson.getProjects();
		Project testProject = null;
		for (Project project : projects){
			if(project.getName().equals("robot")) testProject = project;
		}
		if(testProject == null) fail("Couldn't find example project");;
		Future<Run> run = testProject.scheduleBuild2(0);

		while(!run.isDone()){
			Thread.sleep(5);
		}
		Run lastBuild = testProject.getLastBuild();
		assertTrue("Build wasn't a success", lastBuild.getResult() == Result.SUCCESS);

		WebClient wc = getWebClient();
		HtmlPage page = wc.goTo("job/robot/robot/");
		WebAssert.assertTextPresent(page, "Robot Framework test results");
		WebAssert.assertTextPresent(page, "4 failed tests, 4 critical");
		WebAssert.assertTextPresent(page, "Tests took 9ms (+9)");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a[@href='Testcases+%26+Othercases/Testcases/Not+equal' and contains(.,'Testcases & Othercases.Testcases.Not equal')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a[@href='Testcases+%26+Othercases/Othercases' and contains(.,'Testcases & Othercases.Othercases')]");

		page = wc.goTo("job/robot/1/robot/report/");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a[@href='output.xml' and contains(.,'output.xml')]");

		page = wc.goTo("job/robot/1/robot/Testcases+%26+Othercases");
		WebAssert.assertTextPresent(page, "4 failed tests, 4 critical");
		WebAssert.assertTextPresent(page, "Tests took 9ms (+9)");
		WebAssert.assertTextNotPresent(page, "All Testcases");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a[@href='Testcases/Not+equal' and contains(.,'Testcases.Not equal')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a[@href='Othercases' and contains(.,'Othercases')]");

		page = wc.goTo("job/robot/1/robot/Testcases+%26+Othercases/Othercases");
		WebAssert.assertTextPresent(page, "2 failed tests, 2 critical");
		WebAssert.assertTextPresent(page, "Tests took 5ms (+5)");
		WebAssert.assertTextPresent(page, "All Testcases");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a[@href='Not+equal' and contains(.,'Not equal')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a[@href='Contains+string' and contains(.,'Contains string')]");

		page = wc.goTo("job/robot/1/robot/Testcases+%26+Othercases/Othercases/Not+equal");
		WebAssert.assertTextPresent(page, "Critical test case: \"Not equal\"");
		WebAssert.assertTextPresent(page, "Failed!");
		WebAssert.assertTextPresent(page, "Error message:");
		WebAssert.assertTextPresent(page, "Hello, world! != Good bye, world!");
		WebAssert.assertTextPresent(page, "Test took 1ms (+1)");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//img[@src='durationGraph']");
	}

	@LocalData
	public void testMissingReportFileWithOld() throws Exception{
		Hudson hudson = Hudson.getInstance();
		List<Project> projects = hudson.getProjects();
		Project testProject = null;
		for (Project project : projects){
			if(project.getName().equals("oldrobotbuild")) testProject = project;
		}
		if(testProject == null) fail("Couldn't find example project");

		WebClient wc = getWebClient();

		File buildRoot = testProject.getLastBuild().getRootDir();
		File robotHtmlReport = new File(buildRoot, RobotPublisher.FILE_ARCHIVE_DIR + "/report.html");
		if(!robotHtmlReport.delete()) fail("Unable to delete report directory");

		HtmlPage page = wc.goTo("job/oldrobotbuild/robot/");
		WebAssert.assertTextPresent(page, "No Robot html report found!");

		page = wc.goTo("job/oldrobotbuild/1/robot/");
		WebAssert.assertTextPresent(page, "No Robot html report found!");
	}

	@LocalData
	public void testFailedSince(){
		Hudson hudson = Hudson.getInstance();
		List<Project> projects = hudson.getProjects();
		Run lastRun = null;
		for (Project project : projects){
			if(project.getName().equalsIgnoreCase("failingtests")){
				lastRun = project.getLastCompletedBuild();
			}
		}
		if (lastRun == null) fail("No build including Robot results was found");

		RobotBuildAction action = lastRun.getAction(RobotBuildAction.class);
		RobotResult result = action.getResult();
		RobotCaseResult firstFailed = result.getAllFailedCases().get(0);
		// FIXME: this does not work against the old format for some reason.
		//assertEquals(2,firstFailed.getFailedSince());
	}

	private WebClient getWebClient(){
		WebClient wc = new WebClient();
		wc.setIncorrectnessListener(new SilentIncorrectnessListener());
		wc.setCssErrorHandler(new QuietCssErrorHandler());
		return wc;
	}
}
