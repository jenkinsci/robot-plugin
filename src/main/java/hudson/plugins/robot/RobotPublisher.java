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

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.robot.model.RobotResult;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.tasks.test.TestResultAggregator;
import hudson.util.FormValidation;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class RobotPublisher extends Recorder implements Serializable,
		MatrixAggregatable {

	private static final long serialVersionUID = 1L;
	
	protected static final String DEFAULT_REPORT_FILE = "report.html";
	protected static final String FILE_ARCHIVE_DIR = "robot-plugin";

	private static final String DEFAULT_OUTPUT_FILE = "output.xml";
	private static final String DEFAULT_LOG_FILE = "log.html";

	final private String outputPath;
	final private String reportFileName;
	final private String logFileName;
	final private String outputFileName;
	final private double passThreshold;
	final private double unstableThreshold;
	final private String logFileLink;
	private String[] otherFiles;
	
	//Default to true
	private boolean onlyCritical = true;


	/**
	 * Create new publisher for Robot Framework results
	 * 
	 * @param outputPath
	 *            Path to Robot Framework's output files
	 * @param outputFileName
	 *            Name of Robot output xml
	 * @param reportFileName
	 *            Name of Robot report html
	 * @param logFileName
	 *            Name of Robot log html
	 * @param passThreshold
	 *            Threshold of test pass percentage for successful builds
	 * @param unstableThreshold
	 *            Threhold of test pass percentage for unstable builds
	 * @param onlyCritical
	 *            True if only critical tests are included in pass percentage
	 */
	@DataBoundConstructor
	public RobotPublisher(String outputPath, String outputFileName,
			String reportFileName, String logFileName, double passThreshold,
			double unstableThreshold, boolean onlyCritical, String logFileLink, String otherFiles) {
		this.outputPath = outputPath;
		this.outputFileName = outputFileName;
		this.reportFileName = reportFileName;
		this.passThreshold = passThreshold;
		this.unstableThreshold = unstableThreshold;
		this.logFileName = logFileName;
		this.onlyCritical = onlyCritical;
		this.logFileLink = logFileLink;
		
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
	 * Return the filename to be rendered in the job front page
	 * @return null if empty file configured
	 */
	public String getLogFileLink() {
		if(StringUtils.isBlank(logFileLink))
			return null;
		return logFileLink;
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
		
		RobotBuildAction lastBuildAction = roboAction.getLastBuildAction();
		if(lastBuildAction == null)
			return actions;
		
		final int buildNumber = lastBuildAction.getOwner().getNumber();
		final String logFileName = lastBuildAction.getLogFileLink();
		if(logFileName != null){
			actions.add(new AbstractRobotAction(){

				public String getDisplayName() {
					return "Open Latest Robot " + logFileName;
				}

				public String getUrlName() {
					return buildNumber + "/robot/report/" + logFileName;
				}

			});
		}
		return actions;
	}

	protected RobotResult parse(String expandedTestResults, String outputPath, AbstractBuild<?,?> build,
			Launcher launcher, BuildListener listener) throws IOException,
			InterruptedException {
		return new RobotParser().parse(expandedTestResults, outputPath, build, launcher,
				listener);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
		if (build.getResult() != Result.ABORTED) {
			PrintStream logger = listener.getLogger();
			logger.println(Messages.robot_publisher_started());
			logger.println(Messages.robot_publisher_parsing());
			RobotResult result;
			
			try {
				String expandedOutputFileName = build.getEnvironment(listener).expand(getOutputFileName());
				String expandedOutputPath = build.getEnvironment(listener).expand(getOutputPath());
				String expandedReportFileName = build.getEnvironment(listener).expand(getReportFileName());
				String expandedLogFileName = build.getEnvironment(listener).expand(getLogFileName());
				String logFileJavascripts = trimSuffix(expandedLogFileName) + ".js";
				
				result = parse(expandedOutputFileName, expandedOutputPath, build, launcher, listener);
				
				logger.println(Messages.robot_publisher_done());
				logger.println(Messages.robot_publisher_copying());
				
				copyFilesToBuildDir(build, expandedOutputPath, expandedOutputFileName, expandedReportFileName, expandedLogFileName, logFileJavascripts);
				
				if(otherFiles != null) {
					for(String filemask : otherFiles){
						filemask = build.getEnvironment(listener).expand(filemask);
						if(StringUtils.isNotBlank(filemask))
							copyFilesToBuildDir(build, expandedOutputPath, filemask);
					}
				}
				
				logger.println(Messages.robot_publisher_done());
			} catch (Exception e) {
				logger.println(Messages.robot_publisher_fail());
				e.printStackTrace(logger);
				build.setResult(Result.FAILURE);
				return true;
			}

			logger.println(Messages.robot_publisher_assigning());

			RobotBuildAction action = new RobotBuildAction(build, result, FILE_ARCHIVE_DIR, listener,  getLogFileLink());
			build.addAction(action);
			
			if(getLogFileLink() != null) {
				build.addAction(new AbstractRobotAction(){

					public String getDisplayName() {
						return "Open Robot " + getLogFileLink();
					}

					public String getUrlName() {
						return "robot/report/" + getLogFileLink();
					}

				});
			}

			logger.println(Messages.robot_publisher_done());
			logger.println(Messages.robot_publisher_checking());

			Result buildResult = getBuildResult(build, result);
			build.setResult(buildResult);

			logger.println(Messages.robot_publisher_done());
			logger.println(Messages.robot_publisher_finished());
		}
		return true;
	}

	/**
	 * Copy files with given filemasks from input path relative to build into specific build file archive dir
	 * @param build
	 * @param inputPath Base path for copy. Relative to build workspace.
	 * @param filemaskToCopy List of Ant GLOB style filemasks to copy from dirs specified at inputPathMask
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void copyFilesToBuildDir(AbstractBuild<?, ?> build,
			String inputPath, String...filemaskToCopy) throws IOException, InterruptedException {
		FilePath srcDir = new FilePath(build.getWorkspace(), inputPath);
		FilePath destDir = new FilePath(new FilePath(build.getRootDir()),
				FILE_ARCHIVE_DIR);
		for(String filemask : filemaskToCopy){
			flattenDirsCopy(filemask, srcDir, destDir);
		}
	}

	/**
	 * Copy files matching given filemask from all subdirectories to destination directory
	 * @param filemask Ant GLOB style filemask
	 * @param srcDir source dir as string
	 * @param destDir destination dir as string
	 * @throws IOException on copy failure
	 * @throws InterruptedException
	 */
	public static void flattenDirsCopy(String filemask, FilePath srcDir, FilePath destDir) throws IOException, InterruptedException{
		//take care of splitoutput files not copied by the original filemask (e.g. output-001.xml etc.)
		if (StringUtils.isNotBlank(getSuffix(filemask))) filemask = trimSuffix(filemask) + "*" + getSuffix(filemask);
		
		FilePath[] list = srcDir.list(filemask);
			for (FilePath file : list) {
					FilePath destinationFile = new FilePath(destDir, file.getName());
					String destinationFileName = destinationFile.getName();
					int i = 1;
					while(destinationFile.exists()){
						destinationFile = new FilePath(destDir, trimSuffix(destinationFileName) + "(" + i + ")" + getSuffix(destinationFileName));
						i++;
					}
					
					file.copyTo(destinationFile);
			}
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
	 * Determines the build result based on set thresholds. If build is already
	 * failed before the tests it won't be changed to successful.
	 * 
	 * @param build
	 *            Build to be evaluated
	 * @param result
	 *            Results associated to build
	 * @return Result of build
	 */
	protected Result getBuildResult(AbstractBuild<?, ?> build,
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
		return new TestResultAggregator(build, launcher, listener);
	}
}
