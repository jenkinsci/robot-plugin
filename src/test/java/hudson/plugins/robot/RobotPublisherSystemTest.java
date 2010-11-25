/*
 * Copyright 2008-2010 Nokia Siemens Networks Oyj
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

import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.model.Project;
import hudson.model.Run;

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
		RobotPublisher before = new RobotPublisher("a", "b", "c", "d", 11, 27, true);
		p.getPublishersList().add(before);

		submit(createWebClient().getPage(p, "configure")
				.getFormByName("config"));

		RobotPublisher after = p.getPublishersList().get(RobotPublisher.class);

		assertEqualBeans(before, after, "outputPath,outputFileName,reportFileName,logFileName,passThreshold,unstableThreshold,onlyCritical");
	}

	public void testConfigView() throws Exception{
		FreeStyleProject p = createFreeStyleProject();
		RobotPublisher before = new RobotPublisher("a", "b", "c", "d", 11, 27, true);
		p.getPublishersList().add(before);
		HtmlPage page = createWebClient().getPage(p,"configure");
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
	}

	@LocalData
	public void testPublish() throws Exception{
		Hudson hudson = Hudson.getInstance();
		List<Project> projects = hudson.getProjects();
		Project testProject = projects.get(0);
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
		File storedSplitLog = new File(lastBuild.getRootDir(), RobotPublisher.FILE_ARCHIVE_DIR + "/log.html");
		assertTrue("output.xml was not stored", storedOutput.exists());
		assertTrue("output-001.xml was not stored", storedSplitOutput.exists());
		assertTrue("report.html was not stored", storedReport.exists());
		assertTrue("report-001.html was not stored", storedSplitReport.exists());
		assertTrue("log.html was not stored", storedLog.exists());
		assertTrue("log-001.html was not stored", storedSplitLog.exists());
	}

	@LocalData
	public void testActionViewsWithNoData() throws Exception{
		WebClient wc = new WebClient();
		HtmlPage page = wc.goTo("job/robot/");

		WebAssert.assertElementPresentByXPath(page, "//div[@id='navigation']//a[@href='/job/robot/robot']");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/p[contains(.,'No results available yet.')]");
		WebAssert.assertElementNotPresentByXPath(page, "//td[@id='main-panel']//img[@src='robot/graph']");

		page = wc.goTo("job/robot/robot/");
		WebAssert.assertTextPresent(page, "No robot results available yet!");
	}

	@LocalData
	public void testActionViewsWithData() throws Exception{
		Hudson hudson = Hudson.getInstance();
		List<Project> projects = hudson.getProjects();
		Project testProject = projects.get(0);
		Future<Run> run = testProject.scheduleBuild2(0);

		while(!run.isDone()){
			Thread.sleep(5);
		}

		WebClient wc = new WebClient();

		HtmlPage page = wc.goTo("job/robot/");
		WebAssert.assertElementPresentByXPath(page, "//div[@id='navigation']//a[@href='/job/robot/robot']");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/h3[contains(.,'Latest Robot results:')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/ul/li[contains(.,'Pass ratio (Critical tests): 50%')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/ul/li[contains(.,'Pass ratio (All tests): 60%')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/ul/li[contains(.,'Tests passed (Critical tests): 4/8')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/ul/li[contains(.,'Tests passed (All tests): 6/10')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//img[@src='robot/graph']");

		page = wc.goTo("job/robot/robot/");
		WebAssert.assertTitleEquals(page, "Testcases & Othercases Test Report");

		page = wc.goTo("job/robot/1/");
		WebAssert.assertElementPresentByXPath(page, "//div[@id='navigation']//a[@href='/job/robot/1/robot']");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/h3[contains(.,'Robot test summary:')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/ul/li[contains(.,'Pass ratio (Critical tests): 50%')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/ul/li[contains(.,'Pass ratio (All tests): 60%')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/ul/li[contains(.,'Tests passed (Critical tests): 4/8')]");
		WebAssert.assertElementPresentByXPath(page, "//td[@id='main-panel']//a/ul/li[contains(.,'Tests passed (All tests): 6/10')]");

		page = wc.goTo("job/robot/1/robot/");	    
		WebAssert.assertTitleEquals(page, "Testcases & Othercases Test Report");
	}

	@LocalData
	public void testMissingReportFile() throws Exception{
		Hudson hudson = Hudson.getInstance();
		List<Project> projects = hudson.getProjects();
		Project testProject = projects.get(0);
		Future<Run> run = testProject.scheduleBuild2(0);

		while(!run.isDone()){
			Thread.sleep(5);
		}

		WebClient wc = new WebClient();
		
		File buildRoot = testProject.getLastBuild().getRootDir();
		File robotHtmlReport = new File(buildRoot, RobotPublisher.FILE_ARCHIVE_DIR + "/report.html");
		robotHtmlReport.delete();

		HtmlPage page = wc.goTo("job/robot/robot/");
		WebAssert.assertTextPresent(page, "No robot report found!");

		page = wc.goTo("job/robot/1/robot/");
		WebAssert.assertTextPresent(page, "No robot report found!");
	}
}
