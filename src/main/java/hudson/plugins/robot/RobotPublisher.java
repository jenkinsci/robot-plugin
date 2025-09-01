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
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletException;

import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import jenkins.model.ArtifactManager;

public class RobotPublisher extends Recorder implements Serializable,
        MatrixAggregatable, SimpleBuildStep {

    @Serial
    private static final long serialVersionUID = 1L;

    protected static final String DEFAULT_REPORT_FILE = "report.html";
    protected static final String DEFAULT_ARCHIVE_DIR = "robot-plugin";
    protected static final String DEFAULT_JENKINS_ARCHIVE_DIR = "archive";

    private static final String DEFAULT_OUTPUT_FILE = "output.xml";
    private static final String DEFAULT_LOG_FILE = "log.html";

    final private String archiveDirName;
    final private String outputPath;
    final private String reportFileName;
    final private String logFileName;
    final private String outputFileName;
    final private boolean disableArchiveOutput;
    final private double passThreshold;
    final private double unstableThreshold;
    private String[] otherFiles;
    final private String overwriteXAxisLabel;
    final private boolean enableCache;
    final private boolean useArtifactManager;

    //Default to true
    private boolean countSkippedTests = false;

    /**
     * Create new publisher for Robot Framework results
     *
     * @param archiveDirName       Name of Archive dir
     * @param outputPath           Path to Robot Framework's output files
     * @param outputFileName       Name of Robot output xml
     * @param disableArchiveOutput Disable Archiving output xml file to server
     * @param reportFileName       Name of Robot report html
     * @param logFileName          Name of Robot log html
     * @param passThreshold        Threshold of test pass percentage for successful builds
     * @param unstableThreshold    Threshold of test pass percentage for unstable builds
     * @param otherFiles           Other files to be saved
     * @param enableCache          True if caching is used
     * @param useArtifactManager   True if Artifact Manager is used
     */
    @DataBoundConstructor
    public RobotPublisher(String archiveDirName, String outputPath, String outputFileName,
                          boolean disableArchiveOutput, String reportFileName, String logFileName,
                          double passThreshold, double unstableThreshold,
                          boolean countSkippedTests, String otherFiles, boolean enableCache, String overwriteXAxisLabel, boolean useArtifactManager) {
        this.archiveDirName = archiveDirName;
        this.outputPath = outputPath;
        this.outputFileName = outputFileName;
        this.disableArchiveOutput = disableArchiveOutput;
        this.reportFileName = reportFileName;
        this.passThreshold = passThreshold;
        this.unstableThreshold = unstableThreshold;
        this.logFileName = logFileName;
        this.countSkippedTests = countSkippedTests;
        this.enableCache = enableCache;
        this.overwriteXAxisLabel = overwriteXAxisLabel;
        this.useArtifactManager = useArtifactManager;

        if (otherFiles != null) {
            String[] filemasks = otherFiles.split(",");
            for (int i = 0; i < filemasks.length; i++) {
                filemasks[i] = StringUtils.strip(filemasks[i]);
            }
            this.otherFiles = filemasks;
        }
    }

    /**
     * Gets the name of archive dir. Reverts to default if empty or
     * whitespace.
     *
     * @return the name of archive dir
     */
    public String getArchiveDirName() {
        if (StringUtils.isBlank(archiveDirName))
            return DEFAULT_ARCHIVE_DIR;
        return archiveDirName;
    }

    /**
     * Gets the output path of Robot files
     *
     * @return the output path of Robot files
     */
    public String getOutputPath() {
        return outputPath;
    }

    /**
     * Gets the name of output xml file. Reverts to default if empty or
     * whitespace.
     *
     * @return the name of output xml file
     */
    public String getOutputFileName() {
        if (StringUtils.isBlank(outputFileName))
            return DEFAULT_OUTPUT_FILE;
        return outputFileName;
    }

    /**
     * Get the value of disable Archive of output xml checkbox
     *
     * @return the value of disable Archive of output xml checkbox
     */
    public boolean getDisableArchiveOutput() {
        return disableArchiveOutput;
    }

    /**
     * Gets the name of report html file. Reverts to default if empty or
     * whitespace.
     *
     * @return the name of report html file
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
     * @return the name of log html file
     */
    public String getLogFileName() {
        if (StringUtils.isBlank(logFileName))
            return DEFAULT_LOG_FILE;
        return logFileName;
    }

    /**
     * Gets the test pass percentage threshold for successful builds.
     *
     * @return the test pass percentage threshold for successful builds
     */
    public double getPassThreshold() {
        return passThreshold;
    }

    /**
     * Gets the test pass percentage threshold for unstable builds.
     *
     * @return the test pass percentage threshold for unstable builds
     */
    public double getUnstableThreshold() {
        return unstableThreshold;
    }

    /**
     * Gets if skipped tests should be counted in the thresholds.
     *
     * @return true if skipped tests should be counted in the thresholds
     */
    public boolean getCountSkippedTests() {
        return countSkippedTests;
    }

    /**
     * Gets value of enableCache
     *
     * @return true if cache is enabled
     */
    public boolean getEnableCache() {
        return enableCache;
    }

    /**
     * Gets the comma separated list of other filemasks to copy into build dir
     *
     * @return List of files as string
     */
    public String getOtherFiles() {
        return StringUtils.join(otherFiles, ",");
    }

    /**
     * Gets the value of overwriteXAxisLabel
     *
     * @return X axis label for the trend
     */
    public String getOverwriteXAxisLabel() {
        return overwriteXAxisLabel;
    }

    /**
     * Gets value of useArtifactManager
     *
     * @return true if Artifact Manager is used
     */
    public boolean getUseArtifactManager() {
        return useArtifactManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Action> getProjectActions(AbstractProject<?, ?> project) {
        Collection<Action> actions = new ArrayList<>();
        RobotProjectAction roboAction = new RobotProjectAction(project);
        actions.add(roboAction);
        return actions;
    }

    protected RobotResult parse(String expandedTestResults, String expandedLogFileName, String expandedReportFileName, String outputPath, Run<?, ?> build, FilePath workspace,
                                Launcher launcher, TaskListener listener) throws IOException,
            InterruptedException {
        return new RobotParser().parse(expandedTestResults, outputPath, build, workspace, expandedLogFileName, expandedReportFileName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressFBWarnings(value = "DCN_NULLPOINTER_EXCEPTION",
            justification = "Lower risk to suppress the warning than to stop catching the null pointer exception")
    public void perform(Run<?, ?> build, @NonNull FilePath workspace, @NonNull EnvVars buildEnv, @NonNull Launcher launcher, @NonNull TaskListener listener) throws InterruptedException, IOException {
        if (build.getResult() != Result.ABORTED) {
            PrintStream logger = listener.getLogger();
            logger.println(Messages.robot_publisher_started());
            logger.println(Messages.robot_publisher_parsing());
            RobotResult result;

            try {
                String expandedOutputFileName = buildEnv.expand(getOutputFileName());
                String expandedOutputPath = buildEnv.expand(getOutputPath());
                String expandedReportFileName = buildEnv.expand(getReportFileName());
                String expandedLogFileName = buildEnv.expand(getLogFileName());
                String logFileJavascripts = trimSuffix(expandedLogFileName) + ".js";

                result = parse(expandedOutputFileName, expandedLogFileName, expandedReportFileName, expandedOutputPath, build, workspace, launcher, listener);
                logger.println(Messages.robot_publisher_done());

                // Check if log and report files exist
                FilePath outputDir = new FilePath(workspace, expandedOutputPath);
                if (outputDir.list(expandedLogFileName).length == 0) {
                    logger.println(Messages.robot_publisher_file_not_found() + " " + expandedLogFileName);
                }
                if (outputDir.list(expandedReportFileName).length == 0) {
                    logger.println(Messages.robot_publisher_file_not_found() + " " + expandedReportFileName);
                }

                if (!DEFAULT_JENKINS_ARCHIVE_DIR.equalsIgnoreCase(getArchiveDirName())) {
                    logger.println(Messages.robot_publisher_copying());
                    //Save configured Robot files (including split output) to destination dir
                    copyFilesToDestination(build, workspace, expandedOutputPath, StringUtils.join(modifyMasksforSplittedOutput(new String[]{expandedReportFileName, expandedLogFileName, logFileJavascripts}), ","), launcher, listener);

                    if (!getDisableArchiveOutput()) {
                        copyFilesToDestination(build, workspace, expandedOutputPath, StringUtils.join(modifyMasksforSplittedOutput(new String[]{expandedOutputFileName}), ","), launcher, listener);
                    }

                    //Save other configured files to destination dir
                    if (StringUtils.isNotBlank(getOtherFiles())) {
                        String filemask = buildEnv.expand(getOtherFiles());
                        copyFilesToDestination(build, workspace, expandedOutputPath, filemask, launcher, listener);
                    }
                    logger.println(Messages.robot_publisher_done());
                }

                logger.println(Messages.robot_publisher_assigning());

                String label = buildEnv.expand(overwriteXAxisLabel);
                RobotBuildAction action = new RobotBuildAction(build, result, getArchiveDirName(), listener,
                        expandedReportFileName, expandedLogFileName, enableCache, label, countSkippedTests, useArtifactManager);
                build.addAction(action);

                // set RobotProjectAction as project action
                Job<?, ?> job = build.getParent();
                if (job != null) {
                    RobotProjectAction projectAction = new RobotProjectAction(job);
                    try {
                        job.addOrReplaceAction(projectAction);
                    } catch (UnsupportedOperationException | NullPointerException e) {
                        // it is possible that the action collection is an unmodifiable collection
                        // NullPointerException is thrown if a freestyle job runs
                    }

                    logger.println(Messages.robot_publisher_done());
                    logger.println(Messages.robot_publisher_checking());

                    Result buildResult = getBuildResult(build, result);
                    build.setResult(buildResult);

                    logger.println(Messages.robot_publisher_done());
                    logger.println(Messages.robot_publisher_finished());
                }

            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                logger.println(Messages.robot_publisher_fail());
                e.printStackTrace(logger);
                build.setResult(Result.FAILURE);
            }
        }
    }

    /**
     * Copy files with given filemasks from input path relative to build into
     * local build archive dir or artifact manager destination
     *
     * @param build          The Jenkins run
     * @param workspace      Build workspace
     * @param inputPath      Base path for copy. Relative to build workspace.
     * @param filemaskToCopy List of Ant GLOB style filemasks to copy from dirs specified at inputPathMask
     * @param launcher       A way to start processes
     * @param listener       A place to send output
     * @throws IOException          thrown exception
     * @throws InterruptedException thrown exception
     */
    public void copyFilesToDestination(Run<?, ?> build, FilePath workspace, String inputPath, String filemaskToCopy,
                                       Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        if (getUseArtifactManager()) {
            archiveFilesToDestination(build, workspace, inputPath, filemaskToCopy, launcher, listener);
        } else {
            copyFilesToBuildDir(build, workspace, inputPath, filemaskToCopy);
        }
    }

    /**
     * Copy files with given filemasks from input path relative to build into local build file archive dir
     *
     * @param build          The Jenkins run
     * @param inputPath      Base path for copy. Relative to build workspace.
     * @param filemaskToCopy List of Ant GLOB style filemasks to copy from dirs specified at inputPathMask
     * @param workspace      Build workspace
     * @throws IOException          thrown exception
     * @throws InterruptedException thrown exception
     */
    public void copyFilesToBuildDir(Run<?, ?> build, FilePath workspace,
                                    String inputPath, String filemaskToCopy) throws IOException, InterruptedException {
        FilePath srcDir = new FilePath(workspace, inputPath);
        FilePath destDir = new FilePath(new FilePath(build.getRootDir()), getArchiveDirName());
        srcDir.copyRecursiveTo(filemaskToCopy, destDir);
    }

    /**
     * Copy files with given filemasks from input path relative to Artifact Manager
     *
     * @param build                 The Jenkins run
     * @param workspace             Build workspace
     * @param inputPath             Base path for copy. Relative to build workspace.
     * @param artifactsFilemask     List of Ant GLOB style filemasks to copy from dirs specified at inputPathMask
     * @param launcher              A way to start processes
     * @param listener              A place to send output
     * @throws IOException          thrown exception
     * @throws InterruptedException thrown exception
     */
    public void archiveFilesToDestination(Run<?, ?> build, FilePath workspace, String inputPath, String artifactsFilemask,
                                          Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        FilePath srcDir = new FilePath(workspace, inputPath);
        FilePath[] artifactFiles = srcDir.list(artifactsFilemask);

        Map<String, String> artifacts = new HashMap<>();
        for (FilePath file : artifactFiles) {
            // Use relative path as artifact name
            String pathInArchiveArea = getRelativePath(srcDir, file);
            String pathInWorkspaceArea = getRelativePath(workspace, file);
            artifacts.put(pathInArchiveArea, pathInWorkspaceArea);
        }

        // This will automatically use the configured artifact manager (S3, etc.)
        ArtifactManager artifactManager = build.pickArtifactManager();
        artifactManager.archive(srcDir, launcher, (BuildListener)listener, artifacts);
        if (artifacts.isEmpty()) {
            listener.getLogger().println("No artifacts to archive");
        } else {
            for (Map.Entry<String,String> artifact : artifacts.entrySet()) {
                listener.getLogger().println("The artifact to archive: " + artifact.getKey() + "->" + artifact.getValue());
            }
        }
    }

    private String getRelativePath(FilePath path1, FilePath path2) {
        Path javaPath1 = Paths.get(path1.getRemote());
        Path javaPath2 = Paths.get(path2.getRemote());
        Path relativeJavaPath = javaPath1.relativize(javaPath2);

        return relativeJavaPath.toString();
    }

    /**
     * Return filename without file suffix.
     *
     * @param filename Filename with suffix
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
     *
     * @param filename Filename with suffix
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
     *
     * @param filemasks Files to be masked with wildcards
     * @return Updated array of filemasks
     */
    private static String[] modifyMasksforSplittedOutput(String[] filemasks) {
        for (int i = 0; i < filemasks.length; i++) {
            filemasks[i] = trimSuffix(filemasks[i]) + "*" + getSuffix(filemasks[i]);
        }
        return filemasks;
    }

    /**
     * Determines the build result based on set thresholds. If build is already
     * failed before the tests it won't be changed to successful.
     *
     * @param build  Build to be evaluated
     * @param result Results associated to build
     * @return Result of build
     */
    protected Result getBuildResult(Run<?, ?> build,
                                    RobotResult result) {
        if (build.getResult() != Result.FAILURE) {
            double passPercentage = result.getPassPercentage(countSkippedTests);
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
         * @param value Value to be checked for the threshold
         * @return OK if value is within threshold
         * @throws IOException      thrown exception
         * @throws ServletException thrown exception
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
         * @param value Value to be checked for the threshold
         * @return OK if value is within threshold
         * @throws IOException      thrown exception
         * @throws ServletException thrown exception
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
                return doubleValue <= 100 && doubleValue >= 0;
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
