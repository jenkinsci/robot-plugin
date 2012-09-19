package hudson.plugins.robot.tokens;

import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.plugins.robot.RobotBuildAction;

import java.io.IOException;

import junit.framework.TestCase;

import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.mockito.Mockito;

public class RobotReportLinkTokenMacroTest extends TestCase {

	private static final String macroName = "ROBOT_REPORTLINK";
	
	private RobotReportLinkTokenMacro token;
	private AbstractBuild<?,?> build;
	private TaskListener listener; 
	private RobotBuildAction action;
	
	public void setUp(){
		token = new RobotReportLinkTokenMacro();
		build = Mockito.mock(AbstractBuild.class);
		listener = Mockito.mock(TaskListener.class);
		action = Mockito.mock(RobotBuildAction.class);

		Mockito.when(build.getUrl()).thenReturn("job/robotjob/1/");		
		Mockito.when(action.getUrlName()).thenReturn("robot");
		Mockito.when(build.getAction(RobotBuildAction.class)).thenReturn(action);
		
	}
	
	public void testAcceptsName(){
		assertTrue(new RobotReportLinkTokenMacro().acceptsMacroName(macroName));
	}
	
	public void testTokenConversionWithoutReportLink() throws MacroEvaluationException, IOException, InterruptedException{
		assertEquals("job/robotjob/1/robot/report/",token.evaluate(build, listener, macroName));
	}
	
	public void testTokenConversionWithReportLink() throws MacroEvaluationException, IOException, InterruptedException{
		Mockito.when(action.getLogFileLink()).thenReturn("report.html");
		assertEquals("job/robotjob/1/robot/report/report.html", token.evaluate(build, listener, macroName));
	}
}

