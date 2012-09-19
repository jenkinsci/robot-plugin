package hudson.plugins.robot.tokens;

import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.plugins.robot.RobotBuildAction;
import hudson.plugins.robot.model.RobotResult;

import java.io.IOException;

import junit.framework.TestCase;

import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.mockito.Mockito;

public class RobotPassRatioTokenMacroTest extends TestCase {

	private static final String macroName = "ROBOT_PASSRATIO";
	private RobotPassRatioTokenMacro token;
	private AbstractBuild<?,?> build;
	private TaskListener listener;
	
	public void setUp(){
		token = new RobotPassRatioTokenMacro();
		build = Mockito.mock(AbstractBuild.class);
		listener = Mockito.mock(TaskListener.class);
		
		RobotBuildAction action = Mockito.mock(RobotBuildAction.class);
		RobotResult result = Mockito.mock(RobotResult.class);
		
		Mockito.when(result.getOverallPassed()).thenReturn(6l);
		Mockito.when(result.getOverallTotal()).thenReturn(13l);
		Mockito.when(result.getCriticalPassed()).thenReturn(5l);
		Mockito.when(result.getCriticalTotal()).thenReturn(5l);
		Mockito.when(action.getResult()).thenReturn(result);
		Mockito.when(build.getAction(RobotBuildAction.class)).thenReturn(action);
	}
	
	public void testAcceptsName(){
		assertTrue(new RobotPassRatioTokenMacro().acceptsMacroName(macroName));
	}
	
	public void testTokenConversionWithCritical() throws MacroEvaluationException, IOException, InterruptedException{
		token.onlyCritical = true;
		assertEquals("5 / 5",token.evaluate(build, listener, macroName));
	}
	
	public void testTokenConversionWithAll() throws MacroEvaluationException, IOException, InterruptedException{
		token.onlyCritical = false;
		assertEquals("6 / 13",token.evaluate(build, listener, macroName));
	}
}
