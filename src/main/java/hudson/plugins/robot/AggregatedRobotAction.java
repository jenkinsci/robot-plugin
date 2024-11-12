/*
* Copyright 2008-2014 Nokia Siemens Networks Oyj
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

import hudson.matrix.MatrixBuild;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Run;
import hudson.plugins.robot.graph.RobotGraphHelper;
import hudson.plugins.robot.model.RobotResult;
import hudson.util.ChartUtil;
import hudson.util.Graph;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.util.Calendar;

public class AggregatedRobotAction implements Action {

	private AggregatedRobotResult aggregatedResult;
	private MatrixBuild build;

	public AggregatedRobotAction(MatrixBuild build){
		this.build = build;
	}

	public void addResult(RobotResult result) {
		if (aggregatedResult == null) aggregatedResult = new AggregatedRobotResult(this);
		aggregatedResult.addResult(result);
	}

	public AggregatedRobotResult getResult() {
		return aggregatedResult;
	}

	public AbstractBuild getOwner() {
		return build;
	}

	public static RobotBuildAction getChildBuildAction(Run run){
		return run.getAction(RobotBuildAction.class);
	}

	public double getOverallPassPercentage() {
		return aggregatedResult.getPassPercentage(false);
	}

	public double getCriticalPassPercentage() {
		return aggregatedResult.getPassPercentage(true);
	}

	public String getIconFileName() {
		return "/plugin/robot/robot.png";
	}

	public String getDisplayName() {
		return Messages.robot_sidebar_link();
	}

	public String getUrlName() {
		return "robot";
	}

	/**
	 * Return robot trend graph in the request.
	 * @param req The used StaplerRequest
	 * @param rsp The used StaplerResponse
	 * @throws IOException thrown exception
	 */
	public void doGraph(StaplerRequest req, StaplerResponse rsp)
			throws IOException {
		if (ChartUtil.awtProblemCause != null) {
			rsp.sendRedirect2(req.getContextPath() + "/images/headless.png");
			return;
		}

		Calendar t = build.getTimestamp();

		if (req.checkIfModified(t, rsp))
			return;
		String label = getChildBuildAction(build).getxAxisLabel();
		String labelFormat = StringUtils.isBlank(label) ? RobotConfig.getInstance().getXAxisLabelFormat() : label;
		Graph g = RobotGraphHelper.createTestResultsGraphForTestObject(getResult(),
				Boolean.valueOf(req.getParameter("zoomSignificant")),
				false, Boolean.valueOf(req.getParameter("hd")),
				Boolean.valueOf(req.getParameter("failedOnly")),
				Boolean.valueOf(req.getParameter("criticalOnly")),
				labelFormat,
				Integer.parseInt(req.getParameter("maxBuildsToShow")));
		g.doPng(req, rsp);
	}

	public static class AggregatedRobotResult extends RobotResult {

		private static final long serialVersionUID = 1L;
		private final transient AggregatedRobotAction parent;
		private int passed, failed, skipped;

		public AggregatedRobotResult(AggregatedRobotAction parent) {
			this.parent = parent;
		}

		public void addResult(RobotResult result) {
			failed += result.getOverallFailed();
			passed += result.getOverallPassed();
			skipped += result.getOverallSkipped();
		}

		@Deprecated
		@Override
		public long getCriticalPassed() {
			return this.getOverallPassed();
		}

		@Deprecated
		@Override
		public long getCriticalFailed() {
			return this.getOverallFailed();
		}

		@Deprecated
		@Override
		public long getCriticalTotal() {
			return this.getOverallTotal();
		}

		@Override
		public long getOverallPassed(){
			return passed;
		}

		@Override
		public long getOverallFailed() {
			return failed;
		}

		@Override
		public long getOverallSkipped() {
			return  skipped;
		}

		@Override
		public long getOverallTotal() {
			return failed + passed + skipped;
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
			return (int) getOverallSkipped();
		}

		@Override
		public AbstractBuild getOwner() {
			return parent.getOwner();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public RobotResult getPreviousResult(){
			AbstractBuild<?,?> build = getOwner();
			if (build == null) {
				return null;
			}
			while((build = build.getPreviousBuild()) != null) {
				AggregatedRobotAction parentAction = build.getAction(AggregatedRobotAction.class);
				if(parentAction != null) {
					RobotResult result = parentAction.getResult();
					return result;
				}
			}
			return null;
		}
	}
}
