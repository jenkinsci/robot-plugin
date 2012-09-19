package hudson.plugins.robot.tokens;

import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.plugins.robot.RobotBuildAction;
import hudson.plugins.robot.model.RobotCaseResult;
import hudson.plugins.robot.model.RobotResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.mockito.Mockito;

public class RobotFailedCasesTokenMacroTest extends TestCase {

	private static final String macroName = "ROBOT_FAILEDCASES";
	
	private RobotFailedCasesTokenMacro token;
	private AbstractBuild<?,?> build;
	private TaskListener listener;
	private RobotBuildAction action;
	
	public void setUp(){
		token = new RobotFailedCasesTokenMacro();
		build = Mockito.mock(AbstractBuild.class);
		listener = Mockito.mock(TaskListener.class);
		action = Mockito.mock(RobotBuildAction.class);
		
		RobotResult result = Mockito.mock(RobotResult.class);
		
		List<RobotCaseResult> failedList = new ArrayList<RobotCaseResult>();
		
		RobotCaseResult case1 = Mockito.mock(RobotCaseResult.class);
		Mockito.when(case1.getRelativePackageName(result)).thenReturn("Failcases.subcases.Failure1");
		
		RobotCaseResult case2 = Mockito.mock(RobotCaseResult.class);
		Mockito.when(case2.getRelativePackageName(result)).thenReturn("Morefails.Failure2");
		
		failedList.add(case1);
		failedList.add(case2);
		
		Mockito.when(result.getAllFailedCases()).thenReturn(failedList);
		Mockito.when(action.getResult()).thenReturn(result);
		Mockito.when(build.getAction(RobotBuildAction.class)).thenReturn(action);
	}
	
	public void testAcceptsName(){
		assertTrue(new RobotFailedCasesTokenMacro().acceptsMacroName(macroName));
	}
	
	public void testTokenConversion() throws MacroEvaluationException, IOException, InterruptedException{
		assertEquals("Failcases.subcases.Failure1\nMorefails.Failure2",token.evaluate(build, listener, macroName));
	}
}
