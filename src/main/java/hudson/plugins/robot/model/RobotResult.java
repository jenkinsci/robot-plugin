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

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Api;
import hudson.model.DirectoryBrowserSupport;
import hudson.model.Run;
import hudson.plugins.robot.RobotBuildAction;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;


/**
 * Class representing Robot Framework test results.
 *
 */
@ExportedBean
public class RobotResult extends RobotTestObject {

	private static final long serialVersionUID = 1L;

	private String timeStamp;

	private transient int passed, failed, criticalPassed, criticalFailed;

	//backwards compatibility with old builds
	private transient List<RobotResultStatistics> overallStats;

	private Map<String, RobotSuiteResult> suites;

	/**
	 * Find a testobject in the result tree with id-path
	 * @param id path e.g. "suite/subsuite/testcase"
	 * @return null if not found
	 */
	public RobotTestObject findObjectById(String id){
		if (id.contains("/")) {
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
	@Exported
	public long getCriticalPassed(){
		if(overallStats == null) return criticalPassed;
		if(overallStats.isEmpty()) return 0;
		return overallStats.get(0).getPass();
	}

	/**
	 * Get number of failed critical tests.
	 * @return
	 */
	@Exported
	public long getCriticalFailed(){
		if(overallStats == null) return criticalFailed;
		if( overallStats.isEmpty()) return 0;
		return overallStats.get(0).getFail();
	}

	/**
	 * Get total number of critical tests.
	 * @return
	 */
	@Exported
	public long getCriticalTotal(){
		if(overallStats == null) return criticalFailed + criticalPassed;
		if(overallStats.isEmpty()) return 0;
		return overallStats.get(0).getTotal();
	}

	/**
	 * Get number of all passed tests.
	 * @return
	 */
	@Exported
	public long getOverallPassed(){
		if(overallStats == null) return passed;
		if(overallStats.isEmpty()) return 0;
		return overallStats.get(1).getPass();
	}

	/**
	 * Get number of all failed tests.
	 * @return
	 */
	@Exported
	public long getOverallFailed(){
		if(overallStats == null) return failed;
		if(overallStats.isEmpty()) return 0;
		return overallStats.get(1).getFail();
	}

	/**
	 * Get number of all tests.
	 * @return
	 */
	@Exported
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
	@Exported
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

	@Exported
	public double getPassPercentage(){
		return getPassPercentage(false);
	}

	private static double roundToDecimals(double value, int decimals){
		BigDecimal bd = new BigDecimal(Double.toString(value));
		bd = bd.setScale(decimals, BigDecimal.ROUND_DOWN);
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
		return suites.get(name);
	}

	/**
	 * Add a suite to this result. If suite with same name exists, store this
	 * with sequential numbering
	 * @param suite
	 */
	public void addSuite(RobotSuiteResult suite){
		if(suites == null)
			this.suites = new HashMap<String, RobotSuiteResult>();
		int i = 1;
		String originalName = suite.getName();
		String checkedSuiteName = originalName;
		while(suites.get(checkedSuiteName) != null){
			checkedSuiteName = originalName + "_" + i;
			i++;
		}
		suite.setDuplicateSafeName(checkedSuiteName);
		suites.put(checkedSuiteName, suite);
	}

	/**
	 * Get all top level suites
	 * @return Collection of suiteresults
	 */
	@Exported
	public Collection<RobotSuiteResult> getSuites(){
		if (suites != null)
			return suites.values();
		return Collections.emptyList();
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
			allSuites.addAll(childSuites);
		}
		return allSuites;
	}

	/**
	 * Get all failed test cases related to result.
	 * @return list of test case results
	 */
	@Exported
	public List<RobotCaseResult> getAllFailedCases(){
		List<RobotCaseResult> allFailedCases = new ArrayList<RobotCaseResult>();
		for(RobotSuiteResult suite : getSuites()){
			List<RobotCaseResult> failedCases = suite.getAllFailedCases();
			allFailedCases.addAll(failedCases);
		}
		Collections.sort(allFailedCases, new RobotCaseComparator());
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

		Collection<RobotSuiteResult> newSuites = getSuites();
		HashMap<String, RobotSuiteResult> newMap = new HashMap<String, RobotSuiteResult>(newSuites.size());

		for (RobotSuiteResult suite : newSuites) {
			suite.tally(robotBuildAction);
			failed += suite.getFailed();
			passed += suite.getPassed();
			criticalFailed += suite.getCriticalFailed();
			criticalPassed += suite.getCriticalPassed();
			duration += suite.getDuration();
			newMap.put(suite.getDuplicateSafeName(), suite);
		}
		suites = newMap;
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
		Run<?,?> build = getOwner();
		if (build == null) {
			return null;
		}
		while((build = build.getPreviousBuild()) != null) {
			RobotBuildAction parentAction = build.getAction(getParentAction().getClass());
			if(parentAction != null) {
				return parentAction.getResult();
			}
		}
		return null;
	}

	@Override
	public int getFailed() {
		return (int) getOverallFailed();
	}

	@Override
	public int getPassed() {
		return (int) getOverallPassed();
	}

	public Api getApi() { return new Api(this); }
}
