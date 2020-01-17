package hudson.plugins.robot;

import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.google.common.collect.ImmutableSet;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.matrix.MatrixBuild;
import hudson.model.Run;
import hudson.model.TaskListener;

public class RobotStep extends Step {
	
	private static final Logger logger = Logger.getLogger(RobotStep.class.getName());

	private final @Nonnull String outputPath;
	private @CheckForNull String reportFileName;
	private @CheckForNull String logFileName;
	private @CheckForNull String outputFileName;
	private boolean disableArchiveOutput;
	private double passThreshold;
	private double unstableThreshold;
	private @CheckForNull String[] otherFiles;
	private boolean enableCache = true;
	private boolean onlyCritical = true;
	private @CheckForNull String[] includeTags;
	private @CheckForNull String[] excludeTags;

	
	
	/**
	 * Create new Robot step action that runs the tests and generates reports
	 * @param outputPath Path where the Robot report is stored relative to build root
	 */
	@DataBoundConstructor
	public RobotStep(String outputPath) {
		this.outputPath = outputPath;
	}
	
	public String getOutputPath() {
		return this.outputPath;
	}
	
	public String getReportFileName() {
		return this.reportFileName;
	}
	
	public String getLogFileName() {
		return this.logFileName;
	}
	
	public String getOutputFileName() {
		return this.outputFileName;
	}
	
	public boolean getDisableArchiveOutput() {
		return this.disableArchiveOutput;
	}
	
	public double getPassThreshold() {
		return this.passThreshold;
	}
	
	public double getUnstableThreshold() {
		return this.unstableThreshold;
	}
	
	public String getOtherFiles() {
		return StringUtils.join(otherFiles, ",");
	}
	
	public boolean getEnableCache() {
		return this.enableCache;
	}
	
	public boolean getOnlyCritical() {
		return this.onlyCritical;
	}

	public String getIncludeTags() {
		return StringUtils.join(includeTags, ",");
	}

	public String getExcludeTags() {
		return StringUtils.join(excludeTags, ",");
	}	

	@DataBoundSetter
	public void setReportFileName(String reportFileName) {
		this.reportFileName = Util.fixEmpty(reportFileName);
	}
	
	@DataBoundSetter
	public void setLogFileName(String logFileName) {
		this.logFileName = Util.fixEmpty(logFileName);
	}
	
	@DataBoundSetter
	public void setOutputFileName(String outputFileName) {
		this.outputFileName = Util.fixEmpty(outputFileName);
	}
	
	@DataBoundSetter
	public void setDisableArchiveOutput(boolean disableArchiveOutput) {
		this.disableArchiveOutput = disableArchiveOutput;
	}
	
	@DataBoundSetter
	public void setPassThreshold(double passThreshold) {
		this.passThreshold = passThreshold;
	}
	
	@DataBoundSetter
	public void setUnstableThreshold(double unstableThreshold) {
		this.unstableThreshold = unstableThreshold;
	}
	
	@DataBoundSetter
	public void setEnableCache(boolean enableCache) {
		this.enableCache = enableCache;
	}
	
	@DataBoundSetter
	public void setOnlyCritical(boolean onlyCritical) {
		this.onlyCritical = onlyCritical;
	}
	
	@DataBoundSetter
	public void setOtherFiles(String otherFiles) {
		otherFiles = Util.fixEmpty(otherFiles);
		if (otherFiles != null) {
			String[] filemasks = otherFiles.split("[, ]+");
			for (int i = 0; i < filemasks.length; i++){
				filemasks[i] = StringUtils.strip(filemasks[i]);
			}
			this.otherFiles = filemasks;
		}
	}

	@DataBoundSetter
	public void setIncludeTags(String includeTags) {
		includeTags = Util.fixEmpty(includeTags);
		if (includeTags != null) {
			this.includeTags = includeTags.split("[, ]+");
		}
	}

	@DataBoundSetter
	public void setExcludeTags(String excludeTags) {
		excludeTags = Util.fixEmpty(excludeTags);
		if (excludeTags != null) {
			this.excludeTags = excludeTags.split("[, ]+");
		}
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new RobotStepExecution(this, context);
	}
	
	@Extension 
	public static final class DescriptorImpl extends StepDescriptor {
		
        @Override public String getFunctionName() {
            return "robot";
        }

        @Override public String getDisplayName() {
            return "Configure robot framework report collection";
        }

        @Override public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, FilePath.class, Launcher.class, TaskListener.class);
        }
    }
}
