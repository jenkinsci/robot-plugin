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
package hudson.plugins.robot.model;

import hudson.plugins.robot.RobotBuildAction;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class RobotSuiteResult extends RobotTestObject {

	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = Logger.getLogger(RobotSuiteResult.class.getName());

	private Map<String, RobotSuiteResult> children;
	private RobotTestObject parent;
	private String name;
	private Map<String, RobotCaseResult> caseResults;
	private String startTime;
	private String endTime;
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
			this.children = new HashMap<String, RobotSuiteResult>();
		int i = 1;
		String originalName = child.getName();
		String checkedSuiteName = originalName;
		while(children.get(checkedSuiteName) != null){
			checkedSuiteName = originalName + "_" + i;
			i++;
		}
		child.setDuplicateSafeName(checkedSuiteName);
		children.put(checkedSuiteName, child);
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
		if(caseResults != null) {
			List<RobotCaseResult> res = new ArrayList(caseResults.values());
			Collections.sort(res, new RobotCaseComparator());
			return res;
		}
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
	 * Get number of passed critical tests
	 * @return
	 */
	public long getCriticalPassed() {
		return criticalPassed;
	}

	/**
	 * Get number of failed critical tests
	 * @return
	 */
	public long getCriticalFailed() {
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
		caseResults.put(caseResult.getDuplicateSafeName(), caseResult);
	}

	public void setStartTime(String startTime){
		this.startTime = startTime;
	}

	public void setEndTime(String endTime){
		this.endTime = endTime;
	}

	@Override
	public long getDuration() {
		if (StringUtils.isEmpty(this.startTime) || StringUtils.isEmpty(this.endTime))
				return duration;

		try{
			return RobotCaseResult.timeDifference(this.startTime, this.endTime);
		} catch (ParseException e){
			LOGGER.warn("Couldn't parse duration for suite " + name);
			return 0;
		}
	}

	public String getDisplayName() {
		return getName();
	}

	public String getSearchUrl() {
		return getDisplayName();
	}

	/**
	 * Get nested suite result by duplicate safe unencoded name
	 * @param name
	 * @return suite result, null if none found
	 */
	public RobotSuiteResult getSuite(String name) {
		if(children == null)
			return null;
		return children.get(name);
	}

	/**
	 * Get case result by duplicate safe unencoded name
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
		if(prevParent instanceof RobotSuiteResult) {
			RobotSuiteResult result = ((RobotSuiteResult)prevParent).getSuite(getDuplicateSafeName());
			if (result==null)
				result = ((RobotSuiteResult)prevParent).getSuite(getOldFormatName());
			return result;
		}
		else if (prevParent instanceof RobotResult) {
			RobotSuiteResult result = ((RobotResult)prevParent).getSuite(getDuplicateSafeName());
			if (result == null)
				result = ((RobotResult)prevParent).getSuite(getOldFormatName());
			return result;
		}
		return null;
	}

	/**
	 * Get suite or case result by url encoded name
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
		Collections.sort(failedCases, new RobotCaseComparator());
		return failedCases;
	}

	/**
	 * Get all cases in this suite and its child suites
	 * @return
	 */
	public List<RobotCaseResult> getAllCases() {
		List<RobotCaseResult> cases = new ArrayList<RobotCaseResult>();
		cases.addAll(getCaseResults());
		for(RobotSuiteResult suite : getChildSuites()){
			cases.addAll(suite.getAllCases());
		}
		return cases;
	}

	/**
	 * Fail all cases because of teardown failure.
	 */
	public void failTeardown() {
		for (RobotCaseResult res: getAllCases())
			res.setPassed(false);
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

		HashMap<String, RobotCaseResult> newCases = new HashMap<String, RobotCaseResult>();
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
			newCases.put(caseResult.getDuplicateSafeName(), caseResult);
		}
		caseResults = newCases;

		HashMap<String, RobotSuiteResult> newSuites = new HashMap<String, RobotSuiteResult>();
		for (RobotSuiteResult suite : getChildSuites()) {
			suite.tally(parentAction);
			failed += suite.getFailed();
			passed += suite.getPassed();
			criticalFailed += suite.getCriticalFailed();
			criticalPassed += suite.getCriticalPassed();
			duration += suite.getDuration();
			newSuites.put(suite.getDuplicateSafeName(), suite);
		}
		children = newSuites;
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
	 * If suites with same name exist, the originals are kept
	 * @param childSuites
	 */
	public void addChildren(Collection<RobotSuiteResult> childSuites) {
		for(RobotSuiteResult child : childSuites){
			addChild(child);
		}
	}

	/**
	 * If cases with same name exist, the originals are kept
	 * @param newCaseResults
	 */
	public void addCaseResults(Collection<RobotCaseResult> newCaseResults) {
		for(RobotCaseResult caseResult : newCaseResults){
			if(caseResults.get(caseResult.getDuplicateSafeName()) == null){
				caseResults.put(caseResult.getDuplicateSafeName(), caseResult);
			}
		}
	}
}
