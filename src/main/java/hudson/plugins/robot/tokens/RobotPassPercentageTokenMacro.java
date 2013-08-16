package hudson.plugins.robot.tokens;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.plugins.robot.RobotBuildAction;
import hudson.plugins.robot.model.RobotResult;

import java.io.IOException;

import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

@Extension(optional = true)
public class RobotPassPercentageTokenMacro extends DataBoundTokenMacro {

	/**
	 * If true return only pass percentage of critical tests
	 */
	@Parameter
	public boolean onlyCritical;
	
	@Override
	public String evaluate(AbstractBuild<?, ?> context, TaskListener listener,
			String macroName) throws MacroEvaluationException, IOException,
			InterruptedException {
		RobotBuildAction action = context.getAction(RobotBuildAction.class);
		
		if (action!=null){
			RobotResult result = action.getResult();
			return String.valueOf(result.getPassPercentage(onlyCritical));
		}
		
		return "";
	}

	@Override
	public boolean acceptsMacroName(String macroName) {
		return macroName.equals("ROBOT_PASSPERCENTAGE");
	}

}
