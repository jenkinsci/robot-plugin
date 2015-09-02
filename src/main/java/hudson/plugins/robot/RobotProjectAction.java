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
package hudson.plugins.robot;

import hudson.model.Action;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.util.ChartUtil;

import java.io.IOException;
import java.util.Calendar;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class RobotProjectAction implements Action {

	private AbstractProject<?, ?> project;

	/**
	 * Create new Robot project action
	 * @param project Project which this action will be applied to
	 */
	public RobotProjectAction(AbstractProject<?, ?> project) {
		this.project = project;
	}

	/**
	 * Get associated project.
	 * @return
	 */
	public AbstractProject<?, ?> getProject() {
		return project;
	}

	/**
	 * Returns true if there are any builds in the associated project.
	 * @return
	 */
	public boolean isDisplayGraph() {
		if (getLastBuildAction() != null)
			return true;

		return false;
	}

	/**
	 * Return the action of last build associated with robot
	 * @return null if action not found or no build
	 */
	public Action getLastBuildAction(){
		AbstractBuild<?, ?> lastBuild = getLastBuildWithRobot();
		if(lastBuild != null){
			RobotBuildAction action = (RobotBuildAction)lastBuild.getAction(RobotBuildAction.class);
			if (action == null)
				return lastBuild.getAction(AggregatedRobotAction.class);
			return action;
		}
		else return null;
	}

	/**
	 * Return robot trend graph in the request.
	 * @param req
	 * @param rsp
	 * @throws IOException
	 * @throws ServletException
	 */
	public void doGraph(StaplerRequest req, StaplerResponse rsp)
			throws IOException, ServletException {
		if (ChartUtil.awtProblemCause != null) {
			rsp.sendRedirect2(req.getContextPath() + "/images/headless.png");
			return;
		}

		Calendar t = project.getLastCompletedBuild().getTimestamp();

		if (req.checkIfModified(t, rsp))
			return;

		AbstractBuild<?,?> lastBuild = getLastBuildWithRobot();
		rsp.sendRedirect2("../" + lastBuild.getNumber() + "/" + getUrlName()
			+ "/graph?zoomSignificant="+Boolean.valueOf(req.getParameter("zoomSignificant"))
			+ "&hd="+Boolean.valueOf(req.getParameter("hd"))
			+ "&failedOnly="+Boolean.valueOf(req.getParameter("failedOnly"))
			+ "&criticalOnly="+Boolean.valueOf(req.getParameter("criticalOnly"))
			+ "&maxBuildsToShow="+Integer.valueOf(req.getParameter("maxBuildsToShow")));

	}

	/**
	 * Show Robot html report of the latest build. If no builds are associated with Robot, returns info page.
	 * @param req
	 * @param rsp
	 * @throws IOException
	 */
	public void doIndex(StaplerRequest req, StaplerResponse rsp)
			throws IOException {
		AbstractBuild<?,?> lastBuild = getLastBuildWithRobot();
		if (lastBuild == null) {
			rsp.sendRedirect2("nodata");
		} else {
			int buildNumber = lastBuild.getNumber();
			rsp.sendRedirect2("../" + buildNumber + "/" + getUrlName());
		}
	}


	private AbstractBuild<?, ?> getLastBuildWithRobot() {
		AbstractBuild<?, ?> lastBuild = (AbstractBuild<?, ?>) project
				.getLastBuild();
		while (lastBuild != null
				&& (lastBuild.getAction(RobotBuildAction.class) == null && lastBuild.getAction(AggregatedRobotAction.class) == null)) {
			lastBuild = lastBuild.getPreviousBuild();
		}
		return lastBuild;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getIconFileName() {
		return "/plugin/robot/robot.png";
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDisplayName() {
		return Messages.robot_sidebar_link();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getUrlName() {
		return "robot";
	}
}
