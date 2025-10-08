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

import hudson.model.Run;
import hudson.plugins.robot.RobotConfig;
import hudson.plugins.robot.graph.RobotGraphHelper;
import hudson.util.Graph;

import java.io.IOException;
import java.io.Serial;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

public class RobotCaseResult extends RobotTestObject{

	@Serial
	private static final long serialVersionUID = -8075680639442547520L;

	private static final Logger LOGGER = Logger.getLogger(RobotCaseResult.class.getName());

	private boolean passed;
	private boolean skipped;
	private String errorMsg;
	private String name;
	private String description;
	private String starttime;
	private String endtime;
	private double elapsedtime;
	private List<String> tags;
	private String stackTrace;

	private RobotSuiteResult parent;
	private int failedSince;
	private int skippedSince;

	/**
	 * Difference between string timevalues in format yyyyMMdd HH:mm:ss.SS (Java DateFormat).
	 * Difference is calculated time2 - time1.
	 * @param time1 Start time
	 * @param time2 End time
	 * @return Elapsed time from start to end
	 * @throws ParseException thrown exception
	 */
	public static long timeDifference(String time1, String time2) throws ParseException {
		long difference = 0;
		String dateFormat = "yyyyMMdd HH:mm:ss.SS";
		DateFormat format = new SimpleDateFormat(dateFormat);

		Date startDate = format.parse(time1);
		Date endDate = format.parse(time2);
		difference = endDate.getTime() - startDate.getTime();

		return difference;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RobotTestObject getParent() {
		return parent;
	}

	public void setParent(RobotSuiteResult parent){
		this.parent = parent;
	}

	@Override
	public long getDuration() {
		if (duration != 0)
			return duration;
		if (elapsedtime != 0)
			return Double.valueOf(elapsedtime * 1000).longValue(); // convert seconds to milliseconds
		if (StringUtils.isNotEmpty(this.endtime)) {
			try {
				return timeDifference(this.starttime, this.endtime);
			} catch (ParseException e) {
				LOGGER.warn("Couldn't parse duration for test case " + name);
			}
		}
		return 0;
	}

	public String getStarttime() {
		return starttime;
	}

	public void setStarttime(String starttime) {
		this.starttime = starttime;
	}

	public String getEndtime() {
		return endtime;
	}

	public void setEndtime(String endtime) {
		this.endtime = endtime;
	}

	public double getElapsedtime() {
		return elapsedtime;
	}

	public void setElapsedTime(String elapsed) {
		this.elapsedtime = Double.parseDouble(elapsed);
	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	public String getStackTrace() {
		return stackTrace;
	}

	public void setStackTrace(String stackTrace) {
		this.stackTrace = stackTrace;
	}

	public void setPassed(boolean passed) {
		this.passed = passed;
	}

	public void setSkipped(boolean skipped) {
		this.skipped = skipped;
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

	public boolean isSkipped() {
		return skipped;
	}

	public List<String> getTags(){
		if(tags == null)
			return new ArrayList<>();
		return tags;
	}

	public String getCommaSeparatedTags(){
		List<String> tags = getTags();
		if (tags.isEmpty())
			return "";
		else {
			StringBuilder buf = new StringBuilder();
			for (String tag: tags)
				buf.append(tag+", ");
			String result = buf.toString();
			return result.substring(0, result.length()-2);
		}
	}

	public void addTags(List<String> taglist){
		if(tags == null)
			tags = new ArrayList<>();
		tags.addAll(taglist);
	}

	/**
	 * Gives the buildnumber of the build that this case first failed in
	 * @return number of build
	 */
	public int getFailedSince(){
		if (failedSince == 0 && (!isPassed() && !isSkipped())) {
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

	public int getSkippedSince() {
		if (skippedSince == 0 && isSkipped()) {
			RobotCaseResult previous = getPreviousResult();
			if(previous != null && previous.isSkipped())
				this.skippedSince = previous.getSkippedSince();
			else if (getOwner() != null) {
				this.skippedSince = getOwner().getNumber();
			} else {
				LOGGER.warn("trouble calculating getSkippedSince. We've got prev, but no owner.");
			}
		}
		return skippedSince;
	}

	public void setSkippedSince(int skippedSince) {
		this.skippedSince = skippedSince;
	}

	public void setFailedSince(int failedSince) {
		this.failedSince = failedSince;
	}

	/**
	 * Gives the corresponding caseresult from previous build
	 * @return Previous result
	 */
	public RobotCaseResult getPreviousResult(){
		if (parent == null) return null;
		RobotSuiteResult prevParent = parent.getPreviousResult();
		if(prevParent == null) return null;
		RobotCaseResult result = prevParent.getCase(getDuplicateSafeName());
		if (result==null)
			result = prevParent.getCase(getOldFormatName());
		return result;
	}

	/**
	 * Gives the run that this case first failed in
	 * @return run object
	 */
	public Run<?,?> getFailedSinceRun() {
		return getOwner().getParent().getBuildByNumber(getFailedSince());
	}

	/**
	 * Gives the run that this case first skipped in
	 * @return run object
	 */
	public Run<?,?> getSkippedSinceRun() {
		return getOwner().getParent().getBuildByNumber(getSkippedSince());
	}

	/**
	 * Get the number of builds this test case has failed/skipped for
	 * @return number of builds
	 */
	public int getAge(){
		if(isPassed()) return 0;
		Run<?,?> owner = getOwner();
		if(owner != null) {
			int previousStatusBuild = isSkipped() ? getSkippedSince() : getFailedSince();
			return getOwner().getNumber() - previousStatusBuild + 1;
		}
		else return 0;
	}

	/**
	 * Return duration graph of the case in the request.
	 * @param req StaplerRequest
	 * @param rsp StaplerResponse
	 * @throws IOException thrown exception
	 */
	public void doGraph(StaplerRequest2 req, StaplerResponse2 rsp)
			throws IOException {
		if(!isNeedToGenerate(req, rsp)) return;
		String label = getParentAction().getxAxisLabel();
		String labelFormat = StringUtils.isBlank(label) ? RobotConfig.getInstance().getXAxisLabelFormat() : label;
		Graph g = RobotGraphHelper.createTestResultsGraphForTestObject(this, false, true,
				  Boolean.parseBoolean(req.getParameter("hd")), false, labelFormat,
				  Integer.parseInt(req.getParameter("maxBuildsToShow")));
		g.doPng(req, rsp);
	}

	@Override
	public int getFailed() {
		if(isPassed() || isSkipped()) return 0;
		return 1;
	}

	@Override
	public int getPassed() {
		if(isPassed()) return 1;
		return 0;
	}

	@Override
	public int getSkipped() {
		if (isSkipped()) return 1;
		return 0;
	}
}
