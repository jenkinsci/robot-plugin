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

	// Default to true to retain previous behavior, false to ignore skipped tests as part of the total
	@Parameter
	public boolean countSkippedTests = true;

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
			long passed = result.getOverallPassed();
			long total = countSkippedTests ? result.getOverallTotal() : result.getOverallTotal() - result.getOverallSkipped();
			return passed + " / " + total;
		}
		return "";
	}

	@Override
	public boolean acceptsMacroName(String macroName) {
		return macroName.equals("ROBOT_PASSRATIO");
	}

}
