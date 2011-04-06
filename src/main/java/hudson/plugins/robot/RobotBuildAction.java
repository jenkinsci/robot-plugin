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
package hudson.plugins.robot;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.DirectoryBrowserSupport;
import hudson.plugins.robot.graph.RobotGraph;
import hudson.plugins.robot.graph.RobotGraphHelper;
import hudson.plugins.robot.model.RobotTestObject;
import hudson.plugins.robot.model.RobotResult;
import hudson.util.ChartUtil;
import hudson.util.Graph;

import java.io.IOException;
import java.util.Calendar;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerProxy;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class RobotBuildAction extends AbstractRobotAction implements StaplerProxy {

	private AbstractBuild<?, ?> build;
	private RobotResult result;
	private transient String reportFileName;
	private String outputPath;


	/**
	 * Create new Robot build action
	 * @param build Build which this action is associated to
	 * @param result Robot result
	 * @param outputPath Path where the Robot report is stored relative to build root
	 * @param reportFileName Name of Robot html report file stored
	 */
	public RobotBuildAction(AbstractBuild<?, ?> build, RobotResult result,
			String outputPath) {
		this.build = build;
		this.result = result;
		this.outputPath = outputPath;
	}

	/**
	 * Get build associated to action
	 * @return build object
	 */
	public AbstractBuild<?, ?> getOwner() {
		return build;
	}

	/**
	 * Get Robot result
	 * @return result object
	 */
	public RobotResult getResult() {
		return result;
	}
	
	/**
	 * Get file name for Robot html report.
	 * @return file name as string
	 */
	public String getReportFileName(){
		return reportFileName;
	}
	
	/**
	 * Get ratio of passed tests per total tests. Accounts for all tests run.
	 * @return percent number
	 */
	public double getOverallPassPercentage(){
		return result.getPassPercentage(false);
	}
	
	/**
	 * Get ratio of passed tests per total tests. Accounts for only critical tests run.
	 * @return percent number
	 */
	public double getCriticalPassPercentage() {
		return result.getPassPercentage(true);
	}
	
	/**
	 * Find test object from the results object tree
	 * @param id path e.g. "suite/nestedsuite/testcase"
	 * @return test object
	 */
	public RobotTestObject findObjectById(String id) {
        return getResult().findObjectById(id);
    }
	
	/**
	 * Get the result object which is responsible for UI. If an old project doesn't have it provides buildaction as this.
	 */
	public Object getTarget(){
		if(reportFileName != null) return this;
		return result;
	}

	/**
	 * Serves Robot html report via robot url. Shows not found page if file is missing.
	 * @param req
	 * @param rsp
	 * @throws IOException
	 * @throws ServletException
	 * @throws InterruptedException
	 */
	public void doIndex(StaplerRequest req, StaplerResponse rsp)
			throws IOException, ServletException, InterruptedException {
		String indexFile = getReportFileName();
		FilePath robotDir = getRobotDir();
		
		if(!new FilePath(robotDir, indexFile).exists()){
			rsp.sendRedirect("notfound");
			return;
		}
		
		DirectoryBrowserSupport dbs = new DirectoryBrowserSupport(this,
				getRobotDir(), getDisplayName(),
				"folder.gif", false);
		
		dbs.setIndexFileName(indexFile);
		dbs.generateResponse(req, rsp, this);
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

		Calendar t = build.getTimestamp();

		if (req.checkIfModified(t, rsp))
			return;
		
		Graph g = new RobotGraph(getOwner(), RobotGraphHelper.createDataSetForBuild(getOwner()), Messages.robot_trendgraph_testcases(),
				Messages.robot_trendgraph_builds(), 500, 200);
		g.doPng(req, rsp);
	}
	
	/**
	 * Return path of robot files in build
	 * @return
	 */
	public FilePath getRobotDir() {
		FilePath rootDir = new FilePath(build.getRootDir());
		if (StringUtils.isNotBlank(outputPath))
			return new FilePath(rootDir, outputPath);
        return rootDir;
	}
}