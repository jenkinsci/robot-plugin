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
import hudson.model.Api;
import hudson.model.DirectoryBrowserSupport;
import hudson.model.Run;
import hudson.plugins.robot.RobotBuildAction;

import java.io.IOException;
import java.io.Serial;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;


/**
 * Class representing Robot Framework test results.
 *
 */
@ExportedBean
public class RobotResult extends RobotTestObject {

	@Serial
	private static final long serialVersionUID = 1L;

	private String timeStamp;

	private transient int passed, failed, skipped, criticalPassed, criticalFailed;

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
			String childId = id.substring(id.indexOf("/")+1);
			RobotSuiteResult suite = suites.get(suiteName);
			return suite.findObjectById(childId);
		} else return null;
	}

	@Override
	public String getName() {
		return "";
	}

	@Override
	public String getDescription() {
		return "";
	}

	/**
	 * Get number of passed critical tests.
	 * @return number of passed critical tests
	 */
	@Deprecated
	@Exported
	public long getCriticalPassed(){
		return this.getOverallPassed();
	}

	/**
	 * Get number of failed critical tests.
	 * @return number of failed critical tests
	 */
	@Deprecated
	@Exported
	public long getCriticalFailed(){
		return this.getOverallFailed();
	}

	/**
	 * Get total number of critical tests.
	 * @return total number of critical tests
	 */
	@Deprecated
	@Exported
	public long getCriticalTotal(){
		return this.getOverallTotal();
	}

	/**
	 * Get number of all passed tests.
	 * @return number of all passed tests
	 */
	@Exported
	public long getOverallPassed(){
		if(overallStats == null) return passed;
		if(overallStats.isEmpty()) return 0;
		return overallStats.get(1).getPass();
	}

	/**
	 * Get number of all failed tests.
	 * @return number of all failed tests
	 */
	@Exported
	public long getOverallFailed(){
		if(overallStats == null) return failed;
		if(overallStats.isEmpty()) return 0;
		return overallStats.get(1).getFail();
	}

	/**
	 * Get number of all skipped tests.
	 * @return number of all skipped tests
	 */
	@Exported
	public long getOverallSkipped(){
		if(overallStats == null) return skipped;
		if(overallStats.isEmpty()) return 0;
		return overallStats.get(1).getSkip();
	}

	/**
	 * Get number of all tests.
	 * @return number of all tests
	 */
	@Exported
	public long getOverallTotal(){
		if(overallStats == null) return failed + passed + skipped;
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
	 * Get the timestamp of the original test run.
	 * @return timestamp of the original test run
	 */
	@Exported
	public String getTimeStamp() {
		return timeStamp;
	}

	/**
	 * Set the timestamp of test run.
	 * @param timeStamp The wanted timestamp.
	 */
	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}

	/**
	 * Returns pass percentage of passed tests per total tests.
	 * @param countSkipped true if skipped tests should be included in calculating total tests
	 * @return Percentage value rounded to 1 decimal
	 */
	public double getPassPercentage(boolean countSkipped) {
		long passed, total;
		passed = getOverallPassed();
		total = getOverallTotal();
		if (!countSkipped)
			total -= getOverallSkipped();

		if(total == 0) return 100;

		double percentage = (double) passed / total * 100;
		return roundToDecimals(percentage, 1);
	}

	@Exported
	public double getPassPercentage(){
		return getPassPercentage(false);
	}

	@Exported
	public double getSkipPercentage() {
		double percentage = (double) getSkipped() / getOverallTotal() * 100;
		return roundToDecimals(percentage, 1);
	}

	private static double roundToDecimals(double value, int decimals){
		BigDecimal bd = new BigDecimal(Double.toString(value));
		bd = bd.setScale(decimals, RoundingMode.DOWN);
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
	 * @param name suite name
	 * @return suite result, null when not found
	 */
	public RobotSuiteResult getSuite(String name) {
		return suites.get(name);
	}

	/**
	 * Add a suite to this result. If suite with same name exists, store this
	 * with sequential numbering
	 * @param suite RobotSuiteResult to add
	 */
	public void addSuite(RobotSuiteResult suite){
		if(suites == null)
			this.suites = new HashMap<>();
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
	public Collection<RobotSuiteResult> getSuites(){
		if (suites != null)
			return suites.values();
		return Collections.emptyList();
	}

	@Exported
	public List<String> getExecutedSuites() {
		List<String> executedSuites = new ArrayList<>();
		for (RobotSuiteResult robotSuiteResult : this.getAllSuites()) {
			RobotTestObject rto = robotSuiteResult.getParent();
			String name = robotSuiteResult.getName();
			while (rto != null && !rto.getName().isEmpty()) {
				name = rto.getName()+"."+name;
				rto = rto.getParent();
			}
			executedSuites.add(name);
		}
		return executedSuites;
	}


	/**
	 * Get all testsuites related to result.
	 * @return List of suiteresults
	 */
	public List<RobotSuiteResult> getAllSuites(){
		List<RobotSuiteResult> allSuites = new ArrayList<>();
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
	public List<RobotCaseResult> getAllFailedCases(){
		List<RobotCaseResult> allFailedCases = new ArrayList<>();
		for(RobotSuiteResult suite : getSuites()){
			List<RobotCaseResult> failedCases = suite.getAllFailedCases();
			allFailedCases.addAll(failedCases);
		}
		allFailedCases.sort(new RobotCaseComparator());
		return allFailedCases;
	}

	/**
	 * Get all passed test cases related to result.
	 * @return list of test case results
	 */
	public List<RobotCaseResult> getAllPassedCases(){
		List<RobotCaseResult> allPassedCases = new ArrayList<>();
		for(RobotSuiteResult suite : getSuites()){
			List<RobotCaseResult> passedCases = suite.getAllPassedCases();
			allPassedCases.addAll(passedCases);
		}
		allPassedCases.sort(new RobotCaseComparator());
		return allPassedCases;
	}

	/**
	 * Get all skipped test cases related to result.
	 * @return list of test case results
	 */
	public List<RobotCaseResult> getAllSkippedCases(){
		List<RobotCaseResult> allSkippedCases = new ArrayList<>();
		for(RobotSuiteResult suite : getSuites()){
			List<RobotCaseResult> skippedCases = suite.getAllSkippedCases();
			allSkippedCases.addAll(skippedCases);
		}
		allSkippedCases.sort(new RobotCaseComparator());
		return allSkippedCases;
	}

	/**
	 * Get all failed test case names related to result.
	 * @return list of test case names as strings
	 */
	@Exported
	public List<String> getFailedCases() {
		List<String> failedCases = new ArrayList<>();
		for (RobotCaseResult robotCaseResult : this.getAllFailedCases()) {
			String name = this.getCaseName(robotCaseResult);
			failedCases.add(name);
		}
		return failedCases;
	}

	/**
	 * Get all passed test case names related to result.
	 * @return list of test case names as strings
	 */
	@Exported
	public List<String> getPassedCases() {
		List<String> passedCases = new ArrayList<>();
		for (RobotCaseResult robotCaseResult : this.getAllPassedCases()) {
			String name = this.getCaseName(robotCaseResult);
			passedCases.add(name);
		}
		return passedCases;
	}

	/**
	 * Get all skipped test case names related to result.
	 * @return list of test case names as strings
	 */
	@Exported
	public List<String> getSkippedCases() {
		List<String> skippedCases = new ArrayList<>();
		for (RobotCaseResult robotCaseResult : this.getAllSkippedCases()) {
			String name = this.getCaseName(robotCaseResult);
			skippedCases.add(name);
		}
		return skippedCases;
	}

	private String getCaseName(RobotCaseResult robotCaseResult) {
		RobotTestObject rto = robotCaseResult.getParent();
		String name = robotCaseResult.getName();
		while (rto != null && !rto.getName().isEmpty()) {
			name = rto.getName()+"."+name;
			rto = rto.getParent();
		}
		return name;
	}

	/**
	 * Count the totals in result tree and assign parent action.
	 * @param robotBuildAction The action to be used as the base
	 */
	public void tally(RobotBuildAction robotBuildAction){
		setParentAction(robotBuildAction);
		failed = 0;
		passed = 0;
		skipped = 0;
		criticalPassed = 0;
		criticalFailed = 0;
		duration = 0;

		Collection<RobotSuiteResult> newSuites = getSuites();
		HashMap<String, RobotSuiteResult> newMap = new HashMap<>(newSuites.size());

		for (RobotSuiteResult suite : newSuites) {
			suite.tally(robotBuildAction);
			failed += suite.getFailed();
			passed += suite.getPassed();
			skipped += suite.getSkipped();
			criticalFailed += suite.getFailed();
			criticalPassed += suite.getPassed();
			duration += suite.getDuration();
			newMap.put(suite.getDuplicateSafeName(), suite);
		}
		suites = newMap;
	}

	/**
	 * Return the object represented by url-string
	 * @param token Token
	 * @param req StaplerRequest
	 * @param rsp StaplerResponse
	 * @return object represented by url-string
	 */
	public Object getDynamic(String token, StaplerRequest2 req, StaplerResponse2 rsp){
		return suites.get(token);
	}

	/**
	 * Serves Robot html report via robot url. Shows not found page if file is missing. If reportfilename is specified, the report is served (To be compatible with v1.0 builds)
	 * @param req StaplerRequest
	 * @param rsp StaplerResponse
	 * @throws IOException thrown exception
	 * @throws ServletException thrown exception
	 * @throws InterruptedException thrown exception
	 * @return DirectoryBrowserSupport for the report or null
	 */
	public DirectoryBrowserSupport doReport(StaplerRequest2 req, StaplerResponse2 rsp)
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

	@Override
	public int getSkipped() {
		return (int)getOverallSkipped();
	}

	public Api getApi() { return new Api(this); }

	public List<RobotCaseResult> getAllCases() {
		List<RobotCaseResult> allCases = new ArrayList<>();
		for (RobotSuiteResult suite : getSuites()) {
			List<RobotCaseResult> cases = suite.getAllCases();
			allCases.addAll(cases);
		}
		allCases.sort(new RobotCaseComparator());
		return allCases;
	}
}
