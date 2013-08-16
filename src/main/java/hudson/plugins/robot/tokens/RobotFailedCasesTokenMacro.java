package hudson.plugins.robot.tokens;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.plugins.robot.RobotBuildAction;
import hudson.plugins.robot.model.RobotCaseResult;
import hudson.plugins.robot.model.RobotResult;

import java.io.IOException;

import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

@Extension(optional = true)
public class RobotFailedCasesTokenMacro extends DataBoundTokenMacro {

	@Override
	public String evaluate(AbstractBuild<?, ?> context, TaskListener listener,
			String macroName) throws MacroEvaluationException, IOException,
			InterruptedException {
		RobotBuildAction action = context.getAction(RobotBuildAction.class);
		if (action!=null){
			RobotResult result = action.getResult();
			StringBuilder builder = new StringBuilder();
			
			String newline = "";
			for (RobotCaseResult failedCase : result.getAllFailedCases()){
				builder.append(newline).append(failedCase.getRelativePackageName(result));
				newline = "\n";
			}
			
			return builder.toString();
		}
		return "";
	}

	@Override
	public boolean acceptsMacroName(String macroName) {
		return macroName.equals("ROBOT_FAILEDCASES");
	}

}
