package hudson.plugins.robot.tokens;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.robot.RobotBuildAction;
import hudson.plugins.robot.model.RobotResult;
import junit.framework.TestCase;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.mockito.Mockito;

import java.io.IOException;

public class RobotTotalTokenMacroTest extends TestCase {

	private static final String macroName = "ROBOT_TOTAL";
	private RobotTotalTokenMacro token;
	private AbstractBuild<?,?> build;
	private TaskListener listener;
	
	public void setUp(){
		token = new RobotTotalTokenMacro();
		build = Mockito.mock(AbstractBuild.class);
		listener = Mockito.mock(TaskListener.class);
		
		RobotBuildAction action = Mockito.mock(RobotBuildAction.class);
		RobotResult result = Mockito.mock(RobotResult.class);
		
		Mockito.when(result.getOverallTotal()).thenReturn(6l);
		Mockito.when(result.getCriticalTotal()).thenReturn(5l);
		Mockito.when(action.getResult()).thenReturn(result);
		Mockito.when(build.getAction(RobotBuildAction.class)).thenReturn(action);
	}
	
	public void testAcceptsName(){
		assertTrue(token.acceptsMacroName(macroName));
	}
	
	public void testTokenConversionWithCritical() throws MacroEvaluationException, IOException, InterruptedException{
		token.onlyCritical = true;
		assertEquals("5",token.evaluate(build, listener, macroName));
	}
	
	public void testTokenConversionWithAll() throws MacroEvaluationException, IOException, InterruptedException{
		token.onlyCritical = false;
		assertEquals("6",token.evaluate(build, listener, macroName));
	}
}
