package hudson.plugins.robot;

import java.util.logging.Logger;

import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

import hudson.FilePath;
import hudson.Launcher;
import hudson.matrix.MatrixBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;

public class RobotStepExecution extends SynchronousNonBlockingStepExecution<Void> {

	private static final Logger logger = Logger.getLogger(RobotStepExecution.class.getName());

	private static final long serialVersionUID = 1L;

	private transient final RobotStep step;
    
    RobotStepExecution(RobotStep step, StepContext context) {
        super(context);
        this.step = step;
    }

    @Override protected Void run() throws Exception {
    	FilePath workspace = getContext().get(FilePath.class);
        workspace.mkdirs();
        RobotPublisher rp = new RobotPublisher(step.getOutputPath(), step.getOutputFileName(), 
                                               step.getDisableArchiveOutput(), step.getReportFileName(), 
                                               step.getLogFileName(), step.getPassThreshold(), step.getUnstableThreshold(), 
                                               step.getOnlyCritical(), step.getOtherFiles(), step.getEnableCache(), 
                                               step.getIncludeTags(), step.getExcludeTags());
    	rp.perform(getContext().get(Run.class), workspace, getContext().get(Launcher.class), getContext().get(TaskListener.class));
    	return null;
    }

}
