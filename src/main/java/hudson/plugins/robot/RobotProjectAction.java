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
package hudson.plugins.robot;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.robot.graph.RobotGraph;
import hudson.plugins.robot.graph.RobotGraphHelper;
import hudson.util.ChartUtil;
import hudson.util.Graph;

import java.io.IOException;
import java.util.Calendar;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class RobotProjectAction  extends AbstractRobotAction {

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
		if (project.getBuilds().size() > 0) 
			return true;
		
		return false;
	}
	
	/**
	 * Return the action of last build associated with robot
	 * @return
	 */
	public RobotBuildAction getLastBuildAction(){
		AbstractBuild<?, ?> lastBuild = getLastBuildWithRobot();
		if(lastBuild != null)
			return lastBuild.getAction(RobotBuildAction.class);
		else return null;
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

		Calendar t = project.getLastCompletedBuild().getTimestamp();

		if (req.checkIfModified(t, rsp))
			return;
		
		Graph g = new RobotGraph(project.getLastBuild(), RobotGraphHelper.createDataSetForProject(project), Messages.robot_trendgraph_testcases(),
				Messages.robot_trendgraph_builds(), 500, 200);
		g.doPng(req, rsp);
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
				&& lastBuild.getAction(RobotBuildAction.class) == null) {
			lastBuild = lastBuild.getPreviousBuild();
		}
		return lastBuild;
	}
}
