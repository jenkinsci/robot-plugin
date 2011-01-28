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
package hudson.plugins.robot.model;

import hudson.FilePath;
import hudson.model.DirectoryBrowserSupport;
import hudson.plugins.robot.Messages;
import hudson.plugins.robot.RobotBuildAction;
import hudson.plugins.robot.graph.RobotGraph;
import hudson.plugins.robot.graph.RobotGraphHelper;
import hudson.util.ChartUtil;
import hudson.util.Graph;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.tools.ant.DirectoryScanner;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;


/**
 * Class representing Robot Framework test results.
 *
 */
public class RobotResult extends RobotTestObject {

	private static final long serialVersionUID = 1L;	
	
	private String timeStamp;
	
	int passed, failed, criticalPassed, criticalFailed;
	long duration;

	//backwards compatibility with old builds
	private transient List<RobotResultStatistics> overallStats;

	private Map<String, RobotSuiteResult> suites;	
	
	public RobotResult(DirectoryScanner scanner) throws DocumentException{
		parse(scanner);
	}
	
	public void parse(DirectoryScanner scanner) throws DocumentException{
		suites = new HashMap<String, RobotSuiteResult>();
		String[] files = scanner.getIncludedFiles();
		File baseDirectory = scanner.getBasedir();
		
		for(String file : files){
			SAXReader reader = new SAXReader();
				File reportFile = new File(baseDirectory, file);
				Document resultFile = reader.read(reportFile);
				Element root = resultFile.getRootElement();
				
				timeStamp = root.attributeValue("generated");
				for(Element suite : (List<Element>) root.elements("suite")){
					RobotSuiteResult suiteResult = new RobotSuiteResult(this, suite, baseDirectory);
					if(suites.get(suiteResult.getSafeName()) == null)
							suites.put(suiteResult.getSafeName(), suiteResult);
					else{
						RobotSuiteResult existingSuite = suites.get(suiteResult.getSafeName());
						existingSuite.addChildren(suiteResult.getChildSuites());
						existingSuite.addCaseResults(suiteResult.getCaseResults());
					}
				}

			
		}
	}
	
	public RobotTestObject findObjectById(String id){
		if(id.indexOf("/") >= 0){
			String suiteName = id.substring(0, id.indexOf("/"));
			String childId = id.substring(id.indexOf("/")+1, id.length());
			RobotSuiteResult suite = suites.get(suiteName);
			return suite.findObjectById(childId);
		} else return null;
	}
	
	@Override
	public String getName() {
		return "";
	}
	
	/*
	 * The data structure of passed and failed tests is awkward and fragile but
	 * needs some consideration before migrating so that old builds won't break.
	 */

	/**
	 * Get number of passed critical tests.
	 * @return
	 */
	public long getCriticalPassed(){
		if(overallStats == null) return criticalPassed;
		if(overallStats.isEmpty()) return 0;
		return overallStats.get(0).getPass();
	}
	
	/**
	 * Get number of failed critical tests.
	 * @return
	 */
	public long getCriticalFail(){
		if(overallStats == null) return criticalFailed;
		if( overallStats.isEmpty()) return 0;
		return overallStats.get(0).getFail();
	}
	
	/**
	 * Get total number of critical tests.
	 * @return
	 */
	public long getCriticalTotal(){
		if(overallStats == null) return criticalFailed + criticalPassed;
		if(overallStats.isEmpty()) return 0;
		return overallStats.get(0).getTotal();
	}
	
	/**
	 * Get number of all passed tests.
	 * @return
	 */
	public long getOverallPassed(){
		if(overallStats == null) return passed;
		if(overallStats.isEmpty()) return 0;
		return overallStats.get(1).getPass();
	}
	
	/**
	 * Get number of all failed tests.
	 * @return
	 */
	public long getOverallFailed(){
		if(overallStats == null) return failed;
		if(overallStats.isEmpty()) return 0;
		return overallStats.get(1).getFail();
	}
	
	/**
	 * Get number of all tests.
	 * @return
	 */
	public long getOverallTotal(){
		if(overallStats == null) return failed + passed;
		if(overallStats.isEmpty()) return 0;
		return overallStats.get(1).getTotal();
	}
	
	/**
	 * Get pass/fail stats by category.
	 * @return List containing 'critical tests' and 'all tests'
	 */
	public List<RobotResultStatistics> getStatsByCategory() {
		return overallStats;
	}

	public void setStatsByCategory(List<RobotResultStatistics> statsByCategory) {
		this.overallStats = statsByCategory;
	}

	/**
	 * Get the timestamp of the original test run
	 * @return
	 */
	public String getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}

	/**
	 * Returns pass percentage of passed tests per total tests.
	 * @param onlyCritical true if only critical tests are to be calculated
	 * @return Percentage value rounded to 1 decimal
	 */
	public double getPassPercentage(boolean onlyCritical) {
		long passed, total;
		if(onlyCritical) {
			passed = getCriticalPassed();
			total = getCriticalTotal();
		} else {
			passed = getOverallPassed();
			total = getOverallTotal();
		}
		
		if(total == 0) return 0;
		
		double percentage = (double) passed / total * 100;
		return roundToDecimals(percentage, 1);
	}

	private static double roundToDecimals(double value, int decimals){
		BigDecimal bd = new BigDecimal(Double.toString(value));
		bd = bd.setScale(decimals, BigDecimal.ROUND_HALF_UP);
		return bd.doubleValue();
	}

	public String getDisplayName() {
		return "Robot result";
	}

	public String getSearchUrl() {
		return "robot";
	}

	public RobotSuiteResult getSuite(String name) {
		if(suites == null) return null;
		return suites.get(name);
	}
	
	public Collection<RobotSuiteResult> getSuites(){
		return suites == null ? null : suites.values();
	}
	
	public List<RobotSuiteResult> getAllSuites(){
		List<RobotSuiteResult> allSuites = new ArrayList<RobotSuiteResult>();
		for(RobotSuiteResult suite : getSuites()){
			allSuites.add(suite);
			List<RobotSuiteResult> childSuites = suite.getAllChildSuites();
			if(childSuites != null)
				allSuites.addAll(childSuites);
		}
		return allSuites;
	}
	
	public List<RobotCaseResult> getAllFailedCases(){
		List<RobotCaseResult> allFailedCases = new ArrayList<RobotCaseResult>();
		for(RobotSuiteResult suite : getSuites()){
			List<RobotCaseResult> failedCases = suite.getAllFailedCases();
			allFailedCases.addAll(failedCases);
		}
		return allFailedCases;
	}
	
	public void tally(RobotBuildAction parentAction){
		setParentAction(parentAction);
		failed = 0;
		passed = 0;
		criticalPassed = 0;
		criticalFailed = 0;
		duration = 0;
		
		for(RobotSuiteResult suite : suites.values()){
			suite.tally(parentAction);
			failed += suite.getFailed();
			passed += suite.getPassed();
			criticalFailed += suite.getCriticalFailed();
			criticalPassed += suite.getCriticalPassed();
			duration += suite.getDuration();
		}
	}
	
	public Object getDynamic(String token, StaplerRequest req, StaplerResponse rsp){
		return suites.get(token);
	}
	
	/**
	 * Serves Robot html report via robot url. Shows not found page if file is missing.
	 * @param req
	 * @param rsp
	 * @throws IOException
	 * @throws ServletException
	 * @throws InterruptedException
	 */
	public void doReport(StaplerRequest req, StaplerResponse rsp)
			throws IOException, ServletException, InterruptedException {
		String indexFile = getReportFileName();
		FilePath robotDir = getRobotDir();
		
		if(!new FilePath(robotDir, indexFile).exists()){
			rsp.sendRedirect2("notfound");
			return;
		}
		
		DirectoryBrowserSupport dbs = new DirectoryBrowserSupport(this,
				getRobotDir(), getDisplayName(),
				"folder.gif", false);
		dbs.setIndexFileName(indexFile);
		dbs.generateResponse(req, rsp, this);
	}
	
	/**
	 * Return robot trend graph in the request.
	 * @param req
	 * @param rsp
	 * @throws IOException
	 */
	public void doGraph(StaplerRequest req, StaplerResponse rsp)
			throws IOException {
		if (ChartUtil.awtProblemCause != null) {
			rsp.sendRedirect2(req.getContextPath() + "/images/headless.png");
			return;
		}
		
		Calendar t = getOwner().getTimestamp();

		if (req.checkIfModified(t, rsp))
			return;
		
		Graph g = new RobotGraph(getOwner(), RobotGraphHelper.createDataSetForBuild(getOwner()), Messages.robot_trendgraph_testcases(),
				Messages.robot_trendgraph_builds(), 500, 200);
		g.doPng(req, rsp);
	}
	
	/**
	 * Return robot trend graph in the request.
	 * @param req
	 * @param rsp
	 * @throws IOException
	 */
	public void doDurationGraph(StaplerRequest req, StaplerResponse rsp)
			throws IOException {
		if (ChartUtil.awtProblemCause != null) {
			rsp.sendRedirect2(req.getContextPath() + "/images/headless.png");
			return;
		}
		
		Calendar t = getOwner().getTimestamp();

		if (req.checkIfModified(t, rsp))
			return;
		
		Graph g = new RobotGraph(getOwner(), RobotGraphHelper.createDurationDataSetForBuild(getOwner()), "Duration (ms)",
				Messages.robot_trendgraph_builds(), 500, 200);
		g.doPng(req, rsp);
	}

	private FilePath getRobotDir() {
		FilePath rootDir = new FilePath(getParentAction().getBuild().getRootDir());
		return new FilePath(rootDir, "robot-plugin");
	}

	public String getReportFileName() {
		return "report.html";
	}

	@Override
	public RobotTestObject getParent() {
		return null;
	}

	public long getDuration() {
		return duration;
	}
}