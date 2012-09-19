package hudson.plugins.robot.tokens;

import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.plugins.robot.RobotBuildAction;
import hudson.plugins.robot.model.RobotResult;

import java.io.IOException;

import junit.framework.TestCase;

import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.mockito.Mockito;

public class RobotPassPercentageTokenMacroTest extends TestCase {

	private static final String macroName = "ROBOT_PASSPERCENTAGE";
	
	private RobotPassPercentageTokenMacro token;
	private AbstractBuild<?,?> build;
	private TaskListener listener;
	private RobotBuildAction action;
	
	public void setUp(){
		token = new RobotPassPercentageTokenMacro();		
		build = Mockito.mock(AbstractBuild.class);
		listener = Mockito.mock(TaskListener.class);
		action = Mockito.mock(RobotBuildAction.class);
		
		RobotResult result = Mockito.mock(RobotResult.class);
		
		Mockito.when(result.getPassPercentage(true)).thenReturn(55.0);
		Mockito.when(result.getPassPercentage(false)).thenReturn(41.0);
		Mockito.when(action.getResult()).thenReturn(result);
		Mockito.when(build.getAction(RobotBuildAction.class)).thenReturn(action);
	}
	
	public void testAcceptsName(){
		assertTrue(new RobotPassPercentageTokenMacro().acceptsMacroName(macroName));
	}
	
	public void testTokenConversionWithOnlyCritical() throws MacroEvaluationException, IOException, InterruptedException{
		token.onlyCritical = true;
		assertEquals("55.0",token.evaluate(build, listener, macroName));
	}
	
	public void testTokenConversionWithAll() throws MacroEvaluationException, IOException, InterruptedException{
		token.onlyCritical = false;
		assertEquals("41.0",token.evaluate(build, listener, macroName));
	}
}
