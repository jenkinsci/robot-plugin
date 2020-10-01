package hudson.plugins.robot.tokens;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.plugins.robot.RobotBuildAction;
import hudson.plugins.robot.model.RobotResult;

import java.io.IOException;

import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

@Extension(optional = true)
public class RobotPassRatioTokenMacro extends DataBoundTokenMacro {

	@Parameter
	public boolean onlyCritical;

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
		if(action!=null){
			RobotResult result = action.getResult();
			if(onlyCritical)
				return result.getCriticalPassed() + " / " + result.getCriticalTotal();
			else
				return result.getOverallPassed() + " / " + result.getOverallTotal();
		}
		return "";
	}

	@Override
	public boolean acceptsMacroName(String macroName) {
		return macroName.equals("ROBOT_PASSRATIO");
	}

}
