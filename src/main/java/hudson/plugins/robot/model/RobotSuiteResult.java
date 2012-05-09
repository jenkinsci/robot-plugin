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

import hudson.plugins.robot.Messages;
import hudson.plugins.robot.RobotBuildAction;
import hudson.plugins.robot.graph.RobotGraph;
import hudson.plugins.robot.graph.RobotGraphHelper;
import hudson.util.Graph;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class RobotSuiteResult extends RobotTestObject {

	private static final long serialVersionUID = 1L;
	
	private Map<String, RobotSuiteResult> children;
	private RobotTestObject parent;
	private String name;
	private Map<String, RobotCaseResult> caseResults;
	private transient long duration;
	private transient int failed;
	private transient int passed;
	private transient int criticalPassed;
	private transient int criticalFailed;
	
	
	/**
	 * Adds a nested suite to this suite. If a suite exists with the same name
	 * it will be overwritten with this one.
	 * 
	 * @param child
	 */
	public void addChild(RobotSuiteResult child) {
		if(children == null) 
			children = new HashMap<String, RobotSuiteResult>();
		children.put(child.getSafeName(), child);
	}

	/**
	 * Get the immediate child suites of this suite
	 * @return
	 */
	public Collection<RobotSuiteResult> getChildSuites() {
		if (children != null)
			return children.values();
		return Collections.emptyList();
	}

	/**
	 * Get the parent object of this suite in tree
	 */
	public RobotTestObject getParent() {
		return parent;
	}
	
	public void setParent(RobotTestObject parent){
		this.parent = parent;
	}

	/**
	 * Get the name of this suite
	 */
	public String getName(){
		return name;
	}
	
	public void setName(String name){
		this.name = name;
	}

	/**
	 * Get all case results belonging to this suite
	 * @return
	 */
	public Collection<RobotCaseResult> getCaseResults() {
		if(caseResults != null)
			return caseResults.values();
		return Collections.emptyList();
	}

	/**
	 * Get number of all failed tests
	 * @return
	 */
	public int getFailed() {
		return failed;
	}

	/**
	 * Get number of all passed tests
	 * @return
	 */
	public int getPassed() {
		return passed;
	}

	/**
	 * Get number of all tests
	 * @return
	 */
	public int getTotal() {
		return passed + failed;
	}

	/**
	 * Get duration of this testsuite run
	 * @return
	 */
	public long getDuration() {
		return duration;
	}

	/**
	 * Get number of passed critical tests
	 * @return
	 */
	public int getCriticalPassed() {
		return criticalPassed;
	}

	/**
	 * Get number of failed critical tests
	 * @return
	 */
	public int getCriticalFailed() {
		return criticalFailed;
	}

	/**
	 * Get number of all critical tests
	 * @return
	 */
	public int getCriticalTotal() {
		return criticalPassed + criticalFailed;
	}

	/**
	 * Adds a test case result to this suite. If a case exists with the same
	 * name it will be overwritten with this one.
	 * 
	 * @param caseResult
	 */
	public void addCaseResult(RobotCaseResult caseResult) {
		if(caseResults == null)
			this.caseResults = new HashMap<String, RobotCaseResult>();
		caseResults.put(caseResult.getSafeName(), caseResult);
	}

	public String getDisplayName() {
		return getName();
	}

	public String getSearchUrl() {
		return getDisplayName();
	}

	/**
	 * Get nested suite result by safe name
	 * @param name
	 * @return suite result, null if none found
	 */
	public RobotSuiteResult getSuite(String name) {
		if(children == null)
			return null;
		return children.get(name);
	}

	/**
	 * Get case result by safe name
	 * @param name
	 * @return case result, null if none found
	 */
	public RobotCaseResult getCase(String name) {
		if(caseResults == null)
			return null;
		return caseResults.get(name);
	}

	/**
	 * {@inheritDoc}
	 */
	public RobotSuiteResult getPreviousResult(){
		if (parent == null) return null;
		RobotTestObject prevParent = parent.getPreviousResult();
		if(prevParent instanceof RobotSuiteResult)
			return ((RobotSuiteResult)prevParent).getSuite(getSafeName());
		else if (prevParent instanceof RobotResult) {
			return ((RobotResult)prevParent).getSuite(getSafeName());
		}
		return null;
	}

	/**
	 * Get suite or case result by safe name
	 * @param token
	 * @param req
	 * @param rsp
	 * @return
	 */
	public Object getDynamic(String token, StaplerRequest req,
			StaplerRequest rsp) {
		if ((token) == null)
			return this;
		if (getCase(token) != null)
			return getCase(token);
		return getSuite(token);
	}

	/**
	 * Get all children of this suite
	 * @return
	 */
	public List<RobotSuiteResult> getAllChildSuites() {
		List<RobotSuiteResult> allChildSuites = new ArrayList<RobotSuiteResult>();
		for (RobotSuiteResult suite : getChildSuites()) {
			allChildSuites.add(suite);
			List<RobotSuiteResult> childSuites = suite.getAllChildSuites();
			allChildSuites.addAll(childSuites);
		}

		return allChildSuites;
	}

	/**
	 * Get all failed cases in this suite and its child suites
	 * @return
	 */
	public List<RobotCaseResult> getAllFailedCases() {
		List<RobotCaseResult> failedCases = new ArrayList<RobotCaseResult>();
		for(RobotCaseResult caseResult : getCaseResults()){
			if(!caseResult.isPassed()) failedCases.add(caseResult);
		}
		for(RobotSuiteResult suite : getChildSuites()){
			failedCases.addAll(suite.getAllFailedCases());
		}

		return failedCases;
	}

	/**
	 * Count total values from children and set same parentaction to all
	 * @param parentAction
	 */
	public void tally(RobotBuildAction parentAction) {
		setParentAction(parentAction);
		failed = 0;
		passed = 0;
		criticalPassed = 0;
		criticalFailed = 0;
		duration = 0;

		for(RobotCaseResult caseResult : getCaseResults()) {
			if(caseResult.isPassed()) {
				if(caseResult.isCritical()) criticalPassed++;
				passed++;
			} else {
				if(caseResult.isCritical()) criticalFailed++;
				failed++;
			}
			duration += caseResult.getDuration();
			caseResult.setParentAction(parentAction);
		}


		for (RobotSuiteResult suite : getChildSuites()) {
			suite.tally(parentAction);
			failed += suite.getFailed();
			passed += suite.getPassed();
			criticalFailed += suite.getCriticalFailed();
			criticalPassed += suite.getCriticalPassed();
			duration += suite.getDuration();
		}
	}

	/**
	 * Get object by path in tree
	 * @param id
	 * @return
	 */
	public RobotTestObject findObjectById(String id) {
		if(id.indexOf("/") >= 0){
			String suiteName = id.substring(0, id.indexOf("/"));
			String childId = id.substring(id.indexOf("/")+1, id.length());
			RobotSuiteResult suite = children.get(suiteName);
			return suite.findObjectById(childId);
		} else if(getSuite(id) != null){
			return getSuite(id);
		} else return getCase(id);
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

		Graph g = new RobotGraph(getOwner(), RobotGraphHelper.createDataSetForSuite(this), Messages.robot_trendgraph_testcases(),
				Messages.robot_trendgraph_builds(), 500, 200, false, Color.green, Color.red);
		g.doPng(req, rsp);
	}

	/**
	 * Return duration graph for the suite in the request.
	 * @param req
	 * @param rsp
	 * @throws IOException
	 */
	public void doDurationGraph(StaplerRequest req, StaplerResponse rsp)
	throws IOException {
		if(!isNeedToGenerate(req, rsp)) return;

		Graph g = new RobotGraph(getOwner(), RobotGraphHelper.createDurationDataSetForSuite(this), "Duration (ms)",
				Messages.robot_trendgraph_builds(), 500, 200, false, Color.cyan);
		g.doPng(req, rsp);
	}

	/**
	 * If suites with same name exist, the originals are kept
	 * @param childSuites
	 */
	public void addChildren(Collection<RobotSuiteResult> childSuites) {
		for(RobotSuiteResult child : childSuites){
			if(children.get(child.getSafeName()) == null)
				children.put(child.getSafeName(), child);
		}
	}

	/**
	 * If cases with same name exist, the originals are kept
	 * @param caseResults
	 */
	public void addCaseResults(Collection<RobotCaseResult> newCaseResults) {
		for(RobotCaseResult caseResult : newCaseResults){
			if(caseResults.get(caseResult.getSafeName()) == null){
				caseResults.put(caseResult.getSafeName(), caseResult);
			}
		}
	}
}
