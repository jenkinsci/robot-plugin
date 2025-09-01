package hudson.plugins.robot.tokens;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.plugins.robot.RobotBuildAction;
import jenkins.model.Jenkins;

import java.io.IOException;

import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

@Extension(optional = true)
public class RobotReportLinkTokenMacro extends DataBoundTokenMacro {

	@Override
	public String evaluate(AbstractBuild<?, ?> context, TaskListener listener,
			String macroName) throws MacroEvaluationException, IOException,
			InterruptedException {
		return evaluate(context, context.getWorkspace(), listener, macroName);
	}

	// Required for pipeline
	@Override
	public String evaluate(Run<?, ?> context, FilePath workspace, TaskListener listener, String macroName)
			throws MacroEvaluationException {
		RobotBuildAction action = context.getAction(RobotBuildAction.class);
		if (action!=null){
			String rootURL = (Jenkins.getInstanceOrNull() != null) ? Jenkins.get().getRootUrl() : "";
			if (action.getLogFileLink() == null)
				return rootURL + context.getUrl()+ action.getReportUrlName();
			else
				return rootURL + context.getUrl()+ action.getReportUrlName() + action.getLogFileLink();
		}
		return "";
	}

	@Override
	public boolean acceptsMacroName(String macroName) {
		return macroName.equals("ROBOT_REPORTLINK");
	}

}
