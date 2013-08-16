package hudson.plugins.robot.tokens;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;
import hudson.plugins.robot.RobotBuildAction;

import java.io.IOException;

import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

@Extension(optional = true)
public class RobotReportLinkTokenMacro extends DataBoundTokenMacro {

	@Override
	public String evaluate(AbstractBuild<?, ?> context, TaskListener listener,
			String macroName) throws MacroEvaluationException, IOException,
			InterruptedException {
		RobotBuildAction action = context.getAction(RobotBuildAction.class);
		if (action!=null){
			String rootURL = (Hudson.getInstance() != null) ? Hudson.getInstance().getRootUrl() : "";
			if (action.getLogFileLink() == null)
				return rootURL + context.getUrl()+ action.getUrlName() + "/report/";
			else
				return rootURL + context.getUrl()+ action.getUrlName() + "/report/" + action.getLogFileLink();
		}
		return "";
	}

	@Override
	public boolean acceptsMacroName(String macroName) {
		return macroName.equals("ROBOT_REPORTLINK");
	}

}
