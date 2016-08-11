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

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.model.*;
import hudson.plugins.robot.model.RobotResult;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.ServletException;

import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class RobotPublisher extends Recorder implements Serializable,
		MatrixAggregatable, SimpleBuildStep {

	private static final long serialVersionUID = 1L;

	protected static final String DEFAULT_REPORT_FILE = "report.html";
	protected static final String FILE_ARCHIVE_DIR = "robot-plugin";

	private static final String DEFAULT_OUTPUT_FILE = "output.xml";
	private static final String DEFAULT_LOG_FILE = "log.html";

	final private String outputPath;
	final private String reportFileName;
	final private String logFileName;
	final private String outputFileName;
	final private boolean disableArchiveOutput;
	final private double passThreshold;
	final private double unstableThreshold;
	private String[] otherFiles;
	final private boolean enableCache;

	//Default to true
	private boolean onlyCritical = true;


	/**
	 * Create new publisher for Robot Framework results
	 *
	 * @param outputPath
	 *			Path to Robot Framework's output files
	 * @param outputFileName
	 *			Name of Robot output xml
	 * @param disableArchiveOutput
	 *			Disable Archiving output xml file to server
	 * @param reportFileName
	 *			Name of Robot report html
	 * @param logFileName
	 *			Name of Robot log html
	 * @param passThreshold
	 *			Threshold of test pass percentage for successful builds
	 * @param unstableThreshold
	 *			Threhold of test pass percentage for unstable builds
	 * @param onlyCritical
	 *			True if only critical tests are included in pass percentage
	 */
	@DataBoundConstructor
	public RobotPublisher(String outputPath, String outputFileName,
						  boolean disableArchiveOutput, String reportFileName, String logFileName,
						  double passThreshold, double unstableThreshold,
						  boolean onlyCritical, String otherFiles, boolean enableCache) {
		this.outputPath = outputPath;
		this.outputFileName = outputFileName;
		this.disableArchiveOutput = disableArchiveOutput;
		this.reportFileName = reportFileName;
		this.passThreshold = passThreshold;
		this.unstableThreshold = unstableThreshold;
		this.logFileName = logFileName;
		this.onlyCritical = onlyCritical;
		this.enableCache = enableCache;

		String[] filemasks = otherFiles.split(",");
		for (int i = 0; i < filemasks.length; i++){
			filemasks[i] = StringUtils.strip(filemasks[i]);
		}
		this.otherFiles = filemasks;
	}

	/**
	 * Gets the output	 path of Robot files
	 *
	 * @return
	 */
	public String getOutputPath() {
		return outputPath;
	}

	/**
	 * Gets the name of output xml file. Reverts to default if empty or
	 * whitespace.
	 *
	 * @return
	 */
	public String getOutputFileName() {
		if (StringUtils.isBlank(outputFileName))
			return DEFAULT_OUTPUT_FILE;
		return outputFileName;
	}
	/*
	* Get the value of disable Archive of output xml checkbox
	*
	*
	* @return
	*/
	public boolean getDisableArchiveOutput() {
		return disableArchiveOutput;
		}

	/**
	 * Gets the name of report html file. Reverts to default if empty or
	 * whitespace.
	 *
	 * @return
	 */
	public String getReportFileName() {
		if (StringUtils.isBlank(reportFileName))
			return DEFAULT_REPORT_FILE;
		return reportFileName;
	}

	/**
	 * Gets the name of log html file. Reverts to default if empty or
	 * whitespace.
	 *
	 * @return
	 */
	public String getLogFileName() {
		if (StringUtils.isBlank(logFileName))
			return DEFAULT_LOG_FILE;
		return logFileName;
	}

	/**
	 * Gets the test pass percentage threshold for successful builds.
	 *
	 * @return
	 */
	public double getPassThreshold() {
		return passThreshold;
	}

	/**
	 * Gets the test pass percentage threshold for unstable builds.
	 *
	 * @return
	 */
	public double getUnstableThreshold() {
		return unstableThreshold;
	}

	/**
	 * Gets if only critical tests should be accounted for the thresholds.
	 *
	 * @return
	 */
	public boolean getOnlyCritical() {
		return onlyCritical;
	}


	/**
	 * Gets the comma separated list of other filemasks to copy into build dir
	 * @return List of files as string
	 */
	public String getOtherFiles() {
		return StringUtils.join(otherFiles, ",");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<Action> getProjectActions(AbstractProject<?, ?> project) {
		Collection<Action> actions = new ArrayList<Action>();
		RobotProjectAction roboAction = new RobotProjectAction(project);
		actions.add(roboAction);
		return actions;
	}

	protected RobotResult parse(String expandedTestResults, String outputPath, Run<?,?> build, FilePath workspace,
			Launcher launcher, TaskListener listener) throws IOException,
			InterruptedException {
		return new RobotParser().parse(expandedTestResults, outputPath, build, workspace, getLogFileName(), getReportFileName());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		if (build.getResult() != Result.ABORTED) {
			PrintStream logger = listener.getLogger();
			logger.println(Messages.robot_publisher_started());
			logger.println(Messages.robot_publisher_parsing());
			RobotResult result;

			try {
				EnvVars buildEnv = build.getEnvironment(listener);
				String expandedOutputFileName = buildEnv.expand(getOutputFileName());
				String expandedOutputPath = buildEnv.expand(getOutputPath());
				String expandedReportFileName = buildEnv.expand(getReportFileName());
				String expandedLogFileName = buildEnv.expand(getLogFileName());
				String logFileJavascripts = trimSuffix(expandedLogFileName) + ".js";

				result = parse(expandedOutputFileName, expandedOutputPath, build, workspace, launcher, listener);

				logger.println(Messages.robot_publisher_done());
				logger.println(Messages.robot_publisher_copying());

				//Save configured Robot files (including split output) to build dir
				copyFilesToBuildDir(build, workspace, expandedOutputPath, StringUtils.join(modifyMasksforSplittedOutput(new String[]{expandedReportFileName, expandedLogFileName, logFileJavascripts}), ","));

				if (!getDisableArchiveOutput()){
					copyFilesToBuildDir(build, workspace, expandedOutputPath, StringUtils.join(modifyMasksforSplittedOutput(new String[]{expandedOutputFileName}), ","));
				}

				//Save other configured files to build dir
				if(StringUtils.isNotBlank(getOtherFiles())) {
					String filemask = buildEnv.expand(getOtherFiles());
					copyFilesToBuildDir(build, workspace, expandedOutputPath, filemask);
				}

				logger.println(Messages.robot_publisher_done());
			} catch (Exception e) {
				logger.println(Messages.robot_publisher_fail());
				e.printStackTrace(logger);
				build.setResult(Result.FAILURE);
				return;
			}

			logger.println(Messages.robot_publisher_assigning());

			RobotBuildAction action = new RobotBuildAction(build, result, FILE_ARCHIVE_DIR, listener, getReportFileName(), getLogFileName(), enableCache);
			build.addAction(action);

			logger.println(Messages.robot_publisher_done());
			logger.println(Messages.robot_publisher_checking());

			Result buildResult = getBuildResult(build, result);
			build.setResult(buildResult);

			logger.println(Messages.robot_publisher_done());
			logger.println(Messages.robot_publisher_finished());
		}
	}

	/**
	 * Copy files with given filemasks from input path relative to build into specific build file archive dir
	 * @param build
	 * @param inputPath Base path for copy. Relative to build workspace.
	 * @param filemaskToCopy List of Ant GLOB style filemasks to copy from dirs specified at inputPathMask
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void copyFilesToBuildDir(Run<?, ?> build, FilePath workspace,
			String inputPath, String filemaskToCopy) throws IOException, InterruptedException {
		FilePath srcDir = new FilePath(workspace, inputPath);
		FilePath destDir = new FilePath(new FilePath(build.getRootDir()),
				FILE_ARCHIVE_DIR);
		srcDir.copyRecursiveTo(filemaskToCopy, destDir);
	}

	/**
	 * Return filename without file suffix.
	 * @param filename
	 * @return filename as string
	 */
	public static String trimSuffix(String filename) {
		int index = filename.lastIndexOf('.');
		if (index > 0) {
			filename = filename.substring(0, index);
		}
		return filename;
	}

	/**
	 * Return file suffix from string.
	 * @param filename
	 * @return file suffix as string
	 */
	public static String getSuffix(String filename) {
		int index = filename.lastIndexOf('.');
		if (index > 0) {
			return filename.substring(index);
		}
		return "";
	}

	/**
	 * Add wildcard to filemasks between name and file extension in order to copy split output
	 * e.g. output-001.xml, output-002.xml etc.
	 * @param filemasks
	 * @return
	 */
	private static String[] modifyMasksforSplittedOutput(String[] filemasks){
		for (int i = 0; i < filemasks.length; i++){
			filemasks[i] = trimSuffix(filemasks[i]) + "*" + getSuffix(filemasks[i]);
		}
		return filemasks;
	}

	/**
	 * Determines the build result based on set thresholds. If build is already
	 * failed before the tests it won't be changed to successful.
	 *
	 * @param build
	 *			Build to be evaluated
	 * @param result
	 *			Results associated to build
	 * @return Result of build
	 */
	protected Result getBuildResult(Run<?, ?> build,
			RobotResult result) {
		if (build.getResult() != Result.FAILURE) {
			double passPercentage = result.getPassPercentage(onlyCritical);
			if (passPercentage < getUnstableThreshold()) {
				return Result.FAILURE;
			} else if (passPercentage < getPassThreshold()) {
				return Result.UNSTABLE;
			}
			return Result.SUCCESS;
		}
		return Result.FAILURE;
	}

	/**
	 * Descriptor for the publisher
	 */
	@Extension
	public static final class DescriptorImpl extends
			BuildStepDescriptor<Publisher> {

		/**
		 * {@inheritDoc}
		 */
		@SuppressWarnings("rawtypes")
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getDisplayName() {
			return Messages.robot_description();
		}

		/**
		 * Validates the unstable threshold input field.
		 *
		 * @param value
		 * @return
		 * @throws IOException
		 * @throws ServletException
		 */
		public FormValidation doCheckUnstableThreshold(
				@QueryParameter String value) throws IOException,
				ServletException {
			if (isPercentageValue(value))
				return FormValidation.ok();
			else
				return FormValidation.error(Messages
						.robot_config_percentvalidation());
		}

		/**
		 * Validates the pass threshold input field.
		 *
		 * @param value
		 * @return
		 * @throws IOException
		 * @throws ServletException
		 */
		public FormValidation doCheckPassThreshold(@QueryParameter String value)
				throws IOException, ServletException {
			if (isPercentageValue(value))
				return FormValidation.ok();
			else
				return FormValidation.error(Messages
						.robot_config_percentvalidation());
		}

		private boolean isPercentageValue(String value) {
			try {
				double doubleValue = Double.parseDouble(value);
				if (doubleValue <= 100 && doubleValue >= 0)
					return true;
				else
					return false;
			} catch (NumberFormatException e) {
				return false;
			}
		}
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	public MatrixAggregator createAggregator(MatrixBuild build,
			Launcher launcher, BuildListener listener) {
		return new RobotResultAggregator(build, launcher, listener);
	}
}
