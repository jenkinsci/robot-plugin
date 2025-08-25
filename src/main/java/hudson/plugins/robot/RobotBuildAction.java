/*
* Copyright 2008-2014 Nokia Solutions and Networks Oy
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*	http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package hudson.plugins.robot;

import hudson.FilePath;
import hudson.XmlFile;
import hudson.model.*;
import hudson.plugins.robot.graph.RobotGraphHelper;
import hudson.plugins.robot.model.RobotTestObject;
import hudson.plugins.robot.model.RobotCaseResult;
import hudson.plugins.robot.model.RobotResult;
import hudson.plugins.robot.model.RobotSuiteResult;

import hudson.tasks.test.AbstractTestResultAction;
import hudson.util.ChartUtil;
import hudson.util.Graph;
import hudson.util.HeapSpaceStringConverter;
import hudson.util.XStream2;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerProxy;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import com.thoughtworks.xstream.XStream;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(value="UG_SYNC_SET_UNSYNC_GET", justification="Methods are synchronized")
public class RobotBuildAction extends AbstractTestResultAction<RobotBuildAction> implements StaplerProxy {

	private static final Logger logger = Logger.getLogger(RobotBuildAction.class.getName());
	private static final XStream XSTREAM = new XStream2();

	private transient WeakReference<RobotResult> resultReference;
	private transient String reportFileName;
	private String outputPath;
	private String logFileLink;
	private String logHtmlLink;
	private final boolean enableCache;
	private Run<?, ?> build;
	private RobotResult result;
	private String xAxisLabel;

	private boolean countSkippedTests;
	private boolean useArtifactManager;

	static {
		XSTREAM.alias("result",RobotResult.class);
		XSTREAM.alias("suite",RobotSuiteResult.class);
		XSTREAM.alias("case",RobotCaseResult.class);
		XSTREAM.registerConverter(new HeapSpaceStringConverter(),100);
	}

	/**
	 * Create new Robot build action
	 * @param build Build which this action is associated to
	 * @param result Robot result
	 * @param outputPath Path where the Robot report is stored relative to build root
	 * @param logFileLink Link for the log file
	 * @param logHtmlLink  Link for the log html
	 * @param listener TaskListener from Jenkins
	 * @param enableCache Whether we want to enable caching or not
	 */
	public RobotBuildAction(Run<?, ?> build, RobotResult result,
			String outputPath, TaskListener listener, String logFileLink, String logHtmlLink, boolean enableCache, String xAxisLabel,
							boolean countSkippedTests, boolean useArtifactManager) {
		super();
		super.onAttached(build);
		this.build = build;
		this.outputPath = outputPath;
		this.logFileLink = logFileLink;
		this.logHtmlLink = logHtmlLink;
		this.enableCache = enableCache;
		this.xAxisLabel = xAxisLabel;
		this.countSkippedTests = countSkippedTests;
		this.useArtifactManager = useArtifactManager;
		setResult(result, listener);
	}

	/**
	 * Get build associated to action
	 * @return build object
	 */
	public Run<?, ?> getOwner() {
		return build;
	}

	/**
	 * Get filename to link to
	 * @return null if no filename specified
	 */
	public String getLogFileLink(){
		return logFileLink;
	}

	public String getLogHtmlLink(){
		return logHtmlLink;
	}

	/**
	 * Loads new data to {@link RobotResult}.
	 * @param listener TaskListener from Jenkins
	 * @param result RobotResult to set
	 */
	synchronized public void setResult(RobotResult result, TaskListener listener) {
		result.tally(this);
		try {
			getDataFile().write(result);
		} catch (IOException e) {
			if (listener != null) {
				e.printStackTrace(listener.fatalError("Failed to save the Robot test result"));
			}
		}

		cacheRobotResult(result);
	}

	/**
	 * Returns Robotresult. If not in memory loads it from disk.
	 */
	synchronized public RobotResult getResult() {
		RobotResult returnable;

		if (result != null) return result;

		if (resultReference == null) {
			returnable = load();
			cacheRobotResult(returnable);
		} else {
			returnable = resultReference.get();
		}

		if (returnable == null) {
			returnable = load();
			cacheRobotResult(returnable);
		}
		return returnable;
	}

	private XmlFile getDataFile() {
		return new XmlFile(XSTREAM, new File(getOwner().getRootDir(), "robot_results.xml"));
	}

	private void cacheRobotResult(RobotResult result) {
		if (enableCache) {
			resultReference = new WeakReference<>(result);
		}
	}

	/**
	 * Loads a {@link RobotResult} from disk.
	 */
	private RobotResult load() {
		RobotResult loadedResult;
		try {
			loadedResult = (RobotResult)getDataFile().read();
		} catch (IOException e) {
			logger.log(Level.WARNING, "Couldn't load " + getDataFile(),e);
			return null;
		}
		loadedResult.tally(this);
		return loadedResult;
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
		return getResult().getPassPercentage(false);
	}

	/**
	 * Get ratio of passed tests per total tests. Accounts for all tests run and skipped.
	 * @return percent number
	 */
	public double getPassPercentageWithSkipped() {
		return getResult().getPassPercentage(true);
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
		return getResult();
	}

	public String getxAxisLabel() {
		return xAxisLabel;
	}

	/**
	 * Serves Robot html report via robot url. Shows not found page if file is missing.
	 * @param req StaplerRequest
	 * @param rsp StaplerResponse
	 * @throws IOException thrown exception
	 * @throws ServletException thrown exception
	 * @throws InterruptedException thrown exception
	 */
	public void doIndex(StaplerRequest2 req, StaplerResponse2 rsp)
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
	 * @param req StaplerRequest
	 * @param rsp StaplerResponse
	 * @throws IOException thrown exception
	 */
	public void doGraph(StaplerRequest2 req, StaplerResponse2 rsp)
			throws IOException {
		// TODO: When is this executed?
		if (ChartUtil.awtProblemCause != null) {
			rsp.sendRedirect2(req.getContextPath() + "/images/headless.png");
			return;
		}

		Calendar t = build.getTimestamp();

		if (req.checkIfModified(t, rsp))
			return;

		String maxBuildsReq = req.getParameter("maxBuildsToShow");
		if (maxBuildsReq == null || maxBuildsReq.isEmpty())
			maxBuildsReq = "0"; // show all builds by default

		String labelFormat = StringUtils.isBlank(xAxisLabel) ? RobotConfig.getInstance().getXAxisLabelFormat() : xAxisLabel;
		Graph g = RobotGraphHelper.createTestResultsGraphForTestObject(getResult(),
				Boolean.parseBoolean(req.getParameter("zoomSignificant")), false,
				Boolean.parseBoolean(req.getParameter("hd")),
				Boolean.parseBoolean(req.getParameter("failedOnly")),
				labelFormat,
				Integer.parseInt(maxBuildsReq));
		g.doPng(req, rsp);
	}

	/**
	 * Return path of robot files in build
	 * @return path of robot files in build
	 */
	public FilePath getRobotDir() {
		FilePath rootDir = new FilePath(build.getRootDir());
		if (StringUtils.isNotBlank(outputPath))
			return new FilePath(rootDir, outputPath);
		return rootDir;
	}

	@Override
	public int getFailCount() {
		return (int) getResult().getOverallFailed();
	}

	@Override
	public int getTotalCount() {
		return (int) getResult().getOverallTotal();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getIconFileName() {
		return "/plugin/robot/robot.png";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDisplayName() {
		return Messages.robot_sidebar_link();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getUrlName() {
		return "robot";
	}

	public String getReportUrlName() {
		return getUseArtifactManager() ? "artifact/" : getUrlName() + "/report/";
	}

	public List<RobotCaseResult> getAllTests() {
		return getResult().getAllCases();
	}

	public boolean isCountSkippedTests() {
		return countSkippedTests;
	}

	public void setCountSkippedTests(boolean countSkippedTests) {
		this.countSkippedTests = countSkippedTests;
	}

	public boolean getUseArtifactManager() {
		return useArtifactManager;
	}
}
