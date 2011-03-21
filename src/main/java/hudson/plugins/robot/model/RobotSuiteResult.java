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

import hudson.model.AbstractBuild;
import hudson.plugins.robot.Messages;
import hudson.plugins.robot.RobotBuildAction;
import hudson.plugins.robot.graph.RobotGraph;
import hudson.plugins.robot.graph.RobotGraphHelper;
import hudson.util.ChartUtil;
import hudson.util.Graph;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class RobotSuiteResult extends RobotTestObject {
	
	private Map<String, RobotSuiteResult> children;
	private RobotTestObject parent;
	private String name;
	private Map<String, RobotCaseResult> caseResults;
	private File baseDirectory;
	private long duration;
	private int failed;
	private int passed;
	private int criticalPassed;
	private int criticalFailed;


	public RobotSuiteResult(String name){
		this.name = name;
	}
	
	public RobotSuiteResult(RobotTestObject parent, Element suite, File baseDirectory) throws DocumentException {
		this.name = suite.attributeValue("name");
		this.parent = parent;
		this.baseDirectory = baseDirectory;
		if (suite.attributeValue("src") != null) {
			parseExternalFile(suite);
		} else {
			parseChildren(suite);
		}
	}

	private void parseExternalFile(Element suite) throws DocumentException {
		File externalFile = new File(baseDirectory, suite.attributeValue("src"));
		SAXReader reader = new SAXReader();
		Document splittedOutput = reader.read(externalFile);
		Element rootElement = splittedOutput.getRootElement().element("suite");
		parseChildren(rootElement);
	}

	private void parseChildren(Element suite) throws DocumentException {
		
		for (Element nestedSuite : (List<Element>) suite.elements("suite")) {
			
			RobotSuiteResult suiteResult = new RobotSuiteResult(this, nestedSuite,
					baseDirectory);
			addChild(suiteResult);
		}

		for (Element testCase : (List<Element>) suite.elements("test")) {
			RobotCaseResult caseResult = new RobotCaseResult(this, testCase);
			addCaseResult(caseResult);
		}
	}

	/**
	 * Adds a nested suite to this suite. If a suite exists with the same name
	 * it will be overwritten with this one.
	 * 
	 * @param child
	 */
	public void addChild(RobotSuiteResult child) {
		if (children == null)
			children = new HashMap<String, RobotSuiteResult>();
		children.put(safe(child.getName()), child);
	}

	/**
	 * Get the immediate child suites of this suite
	 * @return
	 */
	public Collection<RobotSuiteResult> getChildSuites() {
		return children == null ? null : children.values();
	}

	/**
	 * Get the parent object of this suite in tree
	 */
	public RobotTestObject getParent() {
		return parent;
	}

	/**
	 * Get the name of this suite
	 */
	public String getName(){
		return name;
	}
	
	/**
	 * Get all case results belonging to this suite
	 * @return
	 */
	public Collection<RobotCaseResult> getCaseResults() {
		return caseResults == null ? null : caseResults.values();
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
		if (caseResults == null)
			caseResults = new HashMap<String, RobotCaseResult>();
		caseResults.put(safe(caseResult.getName()), caseResult);
	}

	public String getDisplayName() {
		return getName();
	}

	public String getSearchUrl() {
		return getDisplayName();
	}

	/**
	 * Get suite result by safe name
	 * @param name
	 * @return
	 */
	public RobotSuiteResult getSuite(String name) {
		if (children == null)
			return null;
		return children.get(name);
	}

	/**
	 * Get case result by safe name
	 * @param name
	 * @return
	 */
	public RobotCaseResult getCase(String name) {
		if (caseResults == null)
			return null;
		return caseResults.get(name);
	}
	
	/**
	 * Get build that this result belongs to
	 */
	public AbstractBuild<?,?> getOwner(){
		return getParentAction().getBuild();
	}

	@Override
	public RobotSuiteResult getPreviousResult(){
		if (parent == null) return null;
		RobotTestObject prevParent = parent.getPreviousResult();
		if(prevParent instanceof RobotSuiteResult)
			return ((RobotSuiteResult)prevParent).getSuite(safe(getName()));
		else if (prevParent instanceof RobotResult) {
			return ((RobotResult)prevParent).getSuite(safe(getName()));
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
		if (children != null){
			for (RobotSuiteResult suite : children.values()) {
				allChildSuites.add(suite);
				List<RobotSuiteResult> childSuites = suite.getAllChildSuites();
				allChildSuites.addAll(childSuites);
			}
		}
		return allChildSuites;
	}
	
	/**
	 * Get all failed cases below this suite
	 * @return
	 */
	public List<RobotCaseResult> getAllFailedCases() {
		List<RobotCaseResult> failedCases = new ArrayList<RobotCaseResult>();
		if(caseResults != null) {
			for(RobotCaseResult caseResult : caseResults.values()){
				if(!caseResult.isPassed()) failedCases.add(caseResult);
			}
		}
		if(children != null){
			for(RobotSuiteResult suite : children.values()){
				failedCases.addAll(suite.getAllFailedCases());
			}
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

		if(caseResults != null) {
			for(RobotCaseResult caseResult : caseResults.values()) {
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
		}
		
		if (children != null) {
			for (RobotSuiteResult suite : children.values()) {
				suite.tally(parentAction);
				failed += suite.getFailed();
				passed += suite.getPassed();
				criticalFailed += suite.getCriticalFailed();
				criticalPassed += suite.getCriticalPassed();
				duration += suite.getDuration();
			}
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
		if (ChartUtil.awtProblemCause != null) {
			rsp.sendRedirect2(req.getContextPath() + "/images/headless.png");
			return;
		}
		
		Calendar t = getOwner().getTimestamp();

		if (req.checkIfModified(t, rsp))
			return;
		
		Graph g = new RobotGraph(getOwner(), RobotGraphHelper.createDataSetForSuite(this), Messages.robot_trendgraph_testcases(),
				Messages.robot_trendgraph_builds(), 500, 200);
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
		if (ChartUtil.awtProblemCause != null) {
			rsp.sendRedirect2(req.getContextPath() + "/images/headless.png");
			return;
		}
		
		Calendar t = getOwner().getTimestamp();

		if (req.checkIfModified(t, rsp))
			return;
		
		Graph g = new RobotGraph(getOwner(), RobotGraphHelper.createDurationDataSetForSuite(this), "Duration (ms)",
				Messages.robot_trendgraph_builds(), 500, 200);
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
			else{
				child.addChildren(child.getChildSuites());
				child.addCaseResults(child.getCaseResults());
			}
		}
	}

	/**
	 * If cases with same name exist, the originals are kept
	 * @param caseResults
	 */
	public void addCaseResults(Collection<RobotCaseResult> caseResults) {
		for(RobotCaseResult caseResult : caseResults){
			if(this.caseResults.get(caseResult) == null){
				this.caseResults.put(caseResult.getSafeName(), caseResult);
			}
		}
	}
}
