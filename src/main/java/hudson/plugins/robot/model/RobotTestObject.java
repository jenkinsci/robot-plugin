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

import hudson.model.AbstractBuild;
import hudson.model.AbstractModelObject;
import hudson.model.Run;
import hudson.plugins.robot.RobotBuildAction;
import hudson.plugins.robot.graph.RobotGraphHelper;
import hudson.util.ChartUtil;
import hudson.util.Graph;

import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public abstract class RobotTestObject extends AbstractModelObject implements Serializable{

	private static final long serialVersionUID = -3191755290679194469L;

	private transient RobotBuildAction parentAction;

	public RobotBuildAction getParentAction() {
		return parentAction;
	}

	public void setParentAction(RobotBuildAction parentAction) {
		this.parentAction = parentAction;
	}

	public abstract String getName();

	public abstract  RobotTestObject getParent();

	private String duplicateSafeName;
	protected transient long duration;

	private String logFile;
	private String reportFile;
	private String id;

	public String getId() {
		return id != null ? id : "";
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getLogFile() {
		return logFile;
	}

	public void setLogFile(String logFileName) {
		this.logFile = logFileName;
	}

	public String getReportFile() {
		return reportFile;
	}

	public void setReportFile(String reportFileName) {
		this.reportFile = reportFileName;
	}

	public boolean getHasLog() {
		return this.logFile != null && !"".equals(this.logFile);
	}

	public boolean getHasReport() {
		return this.reportFile != null && !"".equals(this.reportFile);
	}

	/**
	 * Generates the full packagename
	 * @return
	 */
	public String getRelativePackageName(RobotTestObject thisObject) {
		StringBuilder sb = new StringBuilder(getName());
		String parentPackage = getRelativeParent(thisObject);
		if (! "".equals(parentPackage)) {
			sb.insert(0, parentPackage);
		}
		return sb.toString();
	}

	public String getRelativeParent(RobotTestObject thisObject) {
		StringBuilder sb = new StringBuilder();
		RobotTestObject parent = getParent();
		if(parent != null && !parent.equals(thisObject)){
			String parentPackage = parent.getRelativePackageName(thisObject);
			if(StringUtils.isNotBlank(parentPackage)){
				sb.insert(0, ".");
				sb.insert(0, parentPackage);
			}
		}
		return sb.toString();
	}

	/**
	 * Get path in tree relative to given TestObject
	 * @return
	 */
	public String getRelativeId(RobotTestObject thisObject){
		StringBuilder sb = new StringBuilder(urlEncode(getDuplicateSafeName()));

		RobotTestObject parent = getParent();
		if(parent != null && !parent.equals(thisObject)){
			String parentId = parent.getRelativeId(thisObject);
			if(StringUtils.isNotBlank(parentId)){
				sb.insert(0, "/");
				sb.insert(0, parentId);
			}
		}
		return sb.toString();
	}

	public String urlEncode(String name) {
		return hudson.Util.rawEncode(name);
	}

	/**
	 * Return the build that this result belongs to.
	 * @return Build object. Null if no build.
	 */
	public Run<?,?> getOwner(){
		return getParentAction() == null ? null : getParentAction().getOwner();
	}

	/**
	 * Get the corresponding result object from a given build
	 * @param build
	 * @return
	 */
	public RobotTestObject getResultInBuild(AbstractBuild<?,?> build) {
		parentAction = build.getAction(RobotBuildAction.class);
		String id = getRelativeId(getParentAction().getResult());
		return (parentAction == null) ? null : parentAction.findObjectById(id);
	}

	public String getDuplicateSafeName() {
		if (duplicateSafeName != null)
			return duplicateSafeName;
		return getName();
	}

	public void setDuplicateSafeName(String name) {
		duplicateSafeName = name;
	}

	/**
	 * The name format used in case hashmap was changed in commit
	 * 59c8eec3e16f28baf1288848fccbac228bbe4748, July 2013. This method allows
	 * accessing the tests from old saved results.
	 * TODO: Remove this after a year or so?
	 */
	protected String getOldFormatName() {
		return getName().replaceAll("[/\\ :;#?]", "_");
	}

	/**
	 * Figure out if there's been changes since last request.
	 * @param req
	 * @param rsp
	 * @return
	 * @throws IOException
	 */
	protected boolean isNeedToGenerate(StaplerRequest req, StaplerResponse rsp)
			throws IOException {
		if (ChartUtil.awtProblemCause != null) {
			rsp.sendRedirect2(req.getContextPath() + "/images/headless.png");
			return false;
		}

		Calendar t = getOwner().getTimestamp();

		if (req.checkIfModified(t, rsp))
			return false;
		return true;
	}

	/**
	 * Get duration of this testobject run
	 * @return
	 */
	public long getDuration() {
		return duration;
	}

	/**
	 * Wrapper for calling formatting from jelly
	 * @return
	 */
	public String getHumanReadableDuration(){
		return DurationFormatUtils.formatDurationHMS(getDuration());
	}

	/**
	 * Get difference in of duration to given test object
	 * @param comparable another testobject which to compare to
	 * @return time difference in human readable format
	 */
	public String getDurationDiff(RobotTestObject comparable){
		long duration = getDuration();
		long diff = duration;
		if (comparable != null)
			diff = duration - comparable.getDuration();
		if (diff == 0) return "\u00B10";
		else if (diff > 0) return "+" + DurationFormatUtils.formatDurationHMS(Math.abs(diff));
		else return "-" + DurationFormatUtils.formatDurationHMS(Math.abs(diff));
	}

	public abstract RobotTestObject getPreviousResult();

	public abstract int getFailed();

	public abstract int getPassed();

	public abstract long getCriticalPassed();

	public abstract long getCriticalFailed();

	/**
	 * Return robot trend graph in the request.
	 * @param req
	 * @param rsp
	 * @throws IOException
	 */
	public void doGraph(StaplerRequest req, StaplerResponse rsp)
			throws IOException {
		if(!isNeedToGenerate(req, rsp)) return;
		Graph g = RobotGraphHelper.createTestResultsGraphForTestObject(this,
				Boolean.valueOf(req.getParameter("zoomSignificant")),
				false, Boolean.valueOf(req.getParameter("hd")),
				Boolean.valueOf(req.getParameter("failedOnly")),
				Boolean.valueOf(req.getParameter("criticalOnly")),
				Integer.valueOf(req.getParameter("maxBuildsToShow")));
		g.doPng(req, rsp);
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
		Graph g = RobotGraphHelper.createDurationGraphForTestObject(this,
				req.hasParameter("hd"),
 				Integer.valueOf(req.getParameter("maxBuildsToShow")));
		g.doPng(req, rsp);
	}


}
