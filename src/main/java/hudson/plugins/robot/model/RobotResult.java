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

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.DirectoryBrowserSupport;
import hudson.plugins.robot.Messages;
import hudson.plugins.robot.RobotBuildAction;
import hudson.plugins.robot.graph.RobotGraph;
import hudson.plugins.robot.graph.RobotGraphHelper;
import hudson.util.Graph;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
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
	
	private transient int passed, failed, criticalPassed, criticalFailed;
	private transient long duration;

	//backwards compatibility with old builds
	private transient List<RobotResultStatistics> overallStats;

	private Map<String, RobotSuiteResult> suites;	
	
	public RobotResult(DirectoryScanner scanner) throws DocumentException {
		parse(scanner);
	}
	
	/**
	 * Parse robot reports to object tree
	 * @param scanner contains files to be parsed
	 * @throws DocumentException if xml parsing fails
	 */
	public void parse(DirectoryScanner scanner) throws DocumentException {
		suites = new HashMap<String, RobotSuiteResult>();
		String[] files = scanner.getIncludedFiles();
		
		for(String file : files){
				SAXReader reader = new SAXReader();
				File baseDirectory = scanner.getBasedir();
				File reportFile = new File(baseDirectory, file);
				Document resultFile = reader.read(reportFile);
				Element root = resultFile.getRootElement();
				
				timeStamp = root.attributeValue("generated");
				if(timeStamp == null) continue;
				
				//get the potential directories emerging from the use of GLOB filemask accounted in the splitted file parsing
				String dirFromFileGLOB = new File(file).getParent();
				if(dirFromFileGLOB != null)
					baseDirectory = new File(baseDirectory, dirFromFileGLOB.toString());
				
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
	
	/**
	 * Find a testobject in the result tree with id-path
	 * @param id path e.g. "suite/subsuite/testcase"
	 * @return null if not found
	 */
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
	public long getCriticalFailed(){
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

	/**
	 * Set the timestamp of test run
	 * @param timeStamp
	 */
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
		
		if(total == 0) return 100;
		
		double percentage = (double) passed / total * 100;
		return roundToDecimals(percentage, 1);
	}

	private static double roundToDecimals(double value, int decimals){
		BigDecimal bd = new BigDecimal(Double.toString(value));
		bd = bd.setScale(decimals, BigDecimal.ROUND_HALF_UP);
		return bd.doubleValue();
	}

	/**
	 * {@inheritDoc}}
	 */
	public String getDisplayName() {
		return "Robot result";
	}

	/**
	 * {@inheritDoc}
	 */
	public String getSearchUrl() {
		return "robot";
	}

	/**
	 * Get top level suite by name
	 * @param name
	 * @return suite result, null when not found
	 */
	public RobotSuiteResult getSuite(String name) {
		if(suites == null) return null;
		return suites.get(name);
	}
	
	/**
	 * Get all top level suites
	 * @return Collection of suiteresults
	 */
	public Collection<RobotSuiteResult> getSuites(){
		return suites == null ? null : suites.values();
	}
	
	/**
	 * Get all testsuites related to result.
	 * @return List of suiteresults
	 */
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
	
	/**
	 * Get all failed test cases related to result.
	 * @return list of test case results
	 */
	public List<RobotCaseResult> getAllFailedCases(){
		List<RobotCaseResult> allFailedCases = new ArrayList<RobotCaseResult>();
		for(RobotSuiteResult suite : getSuites()){
			List<RobotCaseResult> failedCases = suite.getAllFailedCases();
			allFailedCases.addAll(failedCases);
		}
		return allFailedCases;
	}
	
	/**
	 * Count the totals in result tree and assign parent action.
	 * @param robotBuildAction
	 */
	public void tally(RobotBuildAction robotBuildAction){
		setParentAction(robotBuildAction);
		failed = 0;
		passed = 0;
		criticalPassed = 0;
		criticalFailed = 0;
		duration = 0;
		
		for(RobotSuiteResult suite : getSuites()){
			suite.tally(robotBuildAction);
			failed += suite.getFailed();
			passed += suite.getPassed();
			criticalFailed += suite.getCriticalFailed();
			criticalPassed += suite.getCriticalPassed();
			duration += suite.getDuration();
		}
	}
	
	/**
	 * Return the object represented by url-string
	 * @param token
	 * @param req
	 * @param rsp
	 * @return
	 */
	public Object getDynamic(String token, StaplerRequest req, StaplerResponse rsp){
		return suites.get(token);
	}
	
	/**
	 * Serves Robot html report via robot url. Shows not found page if file is missing. If reportfilename is specified, the report is served (To be compatible with v1.0 builds)
	 * @param req
	 * @param rsp
	 * @throws IOException
	 * @throws ServletException
	 * @throws InterruptedException
	 */
	public DirectoryBrowserSupport doReport(StaplerRequest req, StaplerResponse rsp)
			throws IOException, ServletException, InterruptedException {
		RobotBuildAction parent = getParentAction();
		FilePath robotDir = null;

		if(parent != null)		
			robotDir = parent.getRobotDir();
		
		if(robotDir != null && robotDir.exists()) {
			if(StringUtils.isBlank(parent.getReportFileName()))
				return new DirectoryBrowserSupport(this, robotDir, getDisplayName(), "folder.gif", true);
		}
		rsp.sendRedirect("notfound");
		return null;
	}
	
	/**
	 * Return robot trend graph in the request.
	 * @param req
	 * @param rsp
	 * @throws IOException
	 */
	public void doGraph(StaplerRequest req, StaplerResponse rsp)
			throws IOException {
		if(!isNeedToGenerate(req, rsp)) return;
		
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
		if(!isNeedToGenerate(req, rsp)) return;
		
		Graph g = new RobotGraph(getOwner(), RobotGraphHelper.createDurationDataSetForBuild(getOwner()), "Duration (ms)",
				Messages.robot_trendgraph_builds(), 500, 200);
		g.doPng(req, rsp);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RobotTestObject getParent() {
		return null;
	}

	/**
	 * Get the total duration of the test run
	 * @return duration
	 */
	public long getDuration() {
		return duration;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public RobotResult getPreviousResult(){
		AbstractBuild<?,?> build = getOwner();
        if (build == null) {
            return null;
        }
        while((build = build.getPreviousBuild()) != null) {
            RobotBuildAction parentAction = build.getAction(getParentAction().getClass());
            if(parentAction != null) {
                RobotResult result = parentAction.getResult();
                return result;
            }
        }
        return null;
	}
}