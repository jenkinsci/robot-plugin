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
import hudson.model.Run;
import hudson.plugins.robot.Messages;
import hudson.plugins.robot.graph.RobotGraph;
import hudson.plugins.robot.graph.RobotGraphHelper;
import hudson.util.Graph;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class RobotCaseResult extends RobotTestObject{

	private static final Logger LOGGER = Logger.getLogger(RobotCaseResult.class.getName());
	
	private boolean passed;
	private boolean critical;
	private long duration;
	private String errorMsg;
	private String name;
	private RobotSuiteResult parent;
	private int failedSince;
	
	//TODO; dummy constructor remove
	public RobotCaseResult(String name){
		this.name = name;
	}
	
	/**
	 * Create new case result from <testcase> -element
	 * @param parent parent suite object
	 * @param testCase testcase elemen in xml
	 */
	public RobotCaseResult(RobotSuiteResult parent, Element testCase) {
		this.parent = parent;

		this.name = testCase.attributeValue("name");
		
		String critical = testCase.attributeValue("critical");
		this.critical = critical != null ? critical.equalsIgnoreCase("yes") : false;
		
		parse(testCase);
	}

	private void parse(Element testCase) {
		
		Element status = testCase.element("status");
		passed = status.attributeValue("status").equalsIgnoreCase("pass");
		if(!passed){
			errorMsg = status.getTextTrim();
		}
		String start = status.attributeValue("starttime");
		String end = status.attributeValue("endtime");
		
		duration =  timeDifference(start, end);
	}
	
	/**
	 * Difference between string timevalues in format yyyyMMdd HH:mm:ss.SS (Java DateFormat).
	 * Difference is calculated time2 - time1.
	 * @param time1
	 * @param time2
	 * @return
	 */
	protected long timeDifference(String time1, String time2){
		long difference = 0;
		DateFormat format = new SimpleDateFormat("yyyyMMdd HH:mm:ss.SS");
		try {
			Date startDate = format.parse(time1);
			Date endDate = format.parse(time2);
			difference = endDate.getTime() - startDate.getTime();
		} catch (ParseException e) {
			LOGGER.warn("Unable to parse testcase \"" + getName() + "\" start and endtimes", e);
		}
		return difference;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RobotTestObject getParent() {
		return parent;
	}
	
	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	public void setPassed(boolean passed) {
		this.passed = passed;
	}

	public void setCritical(boolean critical) {
		this.critical = critical;
	}

	public String getDisplayName() {
		return getName();
	}

	public String getSearchUrl() {
		return getName();
	}

	public boolean isPassed() {
		return passed;
	}

	public boolean isCritical() {
		return critical;
	}
	
	/**
	 * Gives the buildnumber of the build that this case first failed in
	 * @return number of build
	 */
	public int getFailedSince(){
        if (failedSince == 0 && !isPassed()) {
            RobotCaseResult previous = getPreviousResult();
            if(previous != null && !previous.isPassed())
                this.failedSince = previous.getFailedSince();
            else if (getOwner() != null) {
                this.failedSince = getOwner().getNumber();
            } else {
                LOGGER.warn("trouble calculating getFailedSince. We've got prev, but no owner.");
            }
        }
        return failedSince;
	}
	
	/**
	 * Gives the corresponding caseresult from previous build
	 */
	public RobotCaseResult getPreviousResult(){
		if (parent == null) return null;
		RobotSuiteResult prevParent = parent.getPreviousResult();
		if(prevParent == null) return null;
		return prevParent.getCase(getSafeName());
	}
	
	/**
	 * Gives the run that this case first failed in
	 * @return run object
	 */
	public Run<?,?> getFailedSinceRun() {
    	return getOwner().getParent().getBuildByNumber(getFailedSince());
    }
	
	/**
	 * Get the number of builds this test case has failed for
	 * @return number of builds
	 */
	public int getAge(){
		if(isPassed()) return 0;
		AbstractBuild<?,?> owner = getOwner();
		if(owner != null)
			return getOwner().getNumber() - getFailedSince() + 1;
		else return 0;
	}
	
	/**
	 * Return duration graph of the case in the request.
	 * @param req
	 * @param rsp
	 * @throws IOException
	 */
	public void doDurationGraph(StaplerRequest req, StaplerResponse rsp)
			throws IOException {
		if(!isNeedToGenerate(req, rsp)) return;
		
		Graph g = new RobotGraph(getOwner(), RobotGraphHelper.createDurationDataSetForCase(this), "Duration (ms)",
				Messages.robot_trendgraph_builds(), 500, 200);
		g.doPng(req, rsp);
	}
}
