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
import hudson.model.Job;
import hudson.model.Run;
import hudson.util.ChartUtil;

import java.io.IOException;
import java.util.Calendar;

import jakarta.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

public class RobotProjectAction implements Action {

	private Job<?, ?> project;

	/**
	 * Create new Robot project action
	 * @param project Project which this action will be applied to
	 */
	public RobotProjectAction(Job<?, ?> project) {
		this.project = project;
	}

	/**
	 * Get associated project.
	 * @return the project
	 */
	public Job<?, ?> getProject() {
		return project;
	}

	/**
	 * Returns true if there are any builds in the associated project.
	 * @return true if there are any builds in the associated project.
	 */
	public boolean isDisplayGraph() {
        return getLastBuildAction() != null;
    }

	/**
	 * Return the action of last build associated with robot
	 * @return null if action not found or no build
	 */
	public Action getLastBuildAction(){
		Run<?, ?> lastBuild = getLastBuildWithRobot();
		if(lastBuild != null){
			RobotBuildAction action = lastBuild.getAction(RobotBuildAction.class);
			if (action == null)
				return lastBuild.getAction(AggregatedRobotAction.class);
			return action;
		}
		else return null;
	}

	/**
	 * Return robot trend graph in the request.
	 * @param req StaplerRequest
	 * @param rsp StaplerResponse
	 * @throws IOException thrown exception
	 * @throws ServletException thrown exception
	 */
	public void doGraph(StaplerRequest2 req, StaplerResponse2 rsp)
			throws IOException, ServletException {
		if (ChartUtil.awtProblemCause != null) {
			rsp.sendRedirect2(req.getContextPath() + "/images/headless.png");
			return;
		}

		Calendar t = project.getLastCompletedBuild().getTimestamp();

		if (req.checkIfModified(t, rsp))
			return;

		Run<?,?> lastBuild = getLastBuildWithRobot();
		rsp.sendRedirect2("../" + lastBuild.getNumber() + "/" + getUrlName()
			+ "/graph?zoomSignificant="+Boolean.valueOf(req.getParameter("zoomSignificant"))
			+ "&hd="+Boolean.valueOf(req.getParameter("hd"))
			+ "&failedOnly="+Boolean.valueOf(req.getParameter("failedOnly"))
			+ "&criticalOnly="+Boolean.valueOf(req.getParameter("criticalOnly"))
			+ "&maxBuildsToShow="+Integer.valueOf(req.getParameter("maxBuildsToShow")));

	}

	/**
	 * Show Robot html report of the latest build. If no builds are associated with Robot, returns info page.
	 * @param req StaplerRequest
	 * @param rsp StaplerResponse
	 * @throws IOException thrown exception
	 */
	public void doIndex(StaplerRequest2 req, StaplerResponse2 rsp)
			throws IOException {
		Run<?,?> lastBuild = getLastBuildWithRobot();
		if (lastBuild == null) {
			rsp.sendRedirect2("nodata");
		} else {
			int buildNumber = lastBuild.getNumber();
			rsp.sendRedirect2("../" + buildNumber + "/" + getUrlName());
		}
	}


	private Run<?, ?> getLastBuildWithRobot() {
		Run<?, ?> lastBuild = project.getLastBuild();
		while (lastBuild != null
				&& (lastBuild.getAction(RobotBuildAction.class) == null
				&& lastBuild.getAction(AggregatedRobotAction.class) == null)) {
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
