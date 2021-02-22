package hudson.plugins.robot.blueocean;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import hudson.model.FreeStyleBuild;
import hudson.plugins.robot.RobotBuildAction;
import hudson.plugins.robot.RobotParser;
import hudson.plugins.robot.model.RobotResult;
import io.jenkins.blueocean.rest.Reachable;
import io.jenkins.blueocean.rest.factory.BlueTestResultFactory.Result;
import io.jenkins.blueocean.rest.hal.Link;
import io.jenkins.blueocean.rest.model.BlueTestResult;

public class BlueRobotTestResultTest {

	private RobotResult result;
	private FreeStyleBuild mockBuild;
	private RobotBuildAction mockAction;
	private Reachable mockReachable;

	@Before
	public void setUp() throws Exception {
		RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("output.xml", null, null);
		result = remoteOperation.invoke(new File(BlueRobotTestResultTest.class.getResource("output.xml").toURI()).getParentFile(), null);
		result.tally(null);
		
		mockBuild = mock(FreeStyleBuild.class);
		mockAction = mock(RobotBuildAction.class);
		mockReachable = mock(Reachable.class);
		doReturn(mockAction).when(mockBuild).getAction(RobotBuildAction.class);
		doReturn(result.getAllCases()).when(mockAction).getAllTests();
		doReturn(new Link("/")).when(mockReachable).getLink();
	}
	
	@Test
	public void testBasic() {
		BlueRobotTestResult.FactoryImpl factory = new BlueRobotTestResult.FactoryImpl();
		Result blueResult = factory.getBlueTestResults(mockBuild, mockReachable);
		for (BlueTestResult tempResult : blueResult.results) {
			String name = tempResult.getName();
			String trace = tempResult.getErrorStackTrace();
			String msg = tempResult.getErrorDetails();
			switch (name) {
				case "Failed Test":
					assertEquals("Fail    This fails!\n", trace);
					assertEquals("This fails!", msg);
					break;
				case "Nested failed test":
					assertEquals("My failed Keyword\n  The real failed keyword\n    Fail    Really fails!\n", trace);
					assertEquals("Really fails!", msg);
					break;
				case "Nested with not first":
					String helper = "Should Be Equal    ${MESSAGE}    Hello, world!\nShould Be Equal    ${MESSAGE}    Hello, world!\n" + 
						"My failed Keyword\n  The real failed keyword\n    Fail    Really fails!\n";
					assertEquals(helper, trace);
					assertEquals("Really fails!", msg);
					break;
				case "Another Test":
					assertEquals("", trace);
					assertEquals("", msg);
					break;
				case "My Test":
					assertEquals("", trace);
					assertEquals("", msg);
					break;
			}
		}
	}

	@Test
	public void testRobot4StackTrace() throws Exception {
		RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("blue_skip.xml", null, null);
		result = remoteOperation.invoke(new File(BlueRobotTestResultTest.class.getResource("blue_skip.xml").toURI()).getParentFile(), null);
		result.tally(null);
		doReturn(result.getAllCases()).when(mockAction).getAllTests();

		BlueRobotTestResult.FactoryImpl factory = new BlueRobotTestResult.FactoryImpl();
		Result blueResult = factory.getBlueTestResults(mockBuild, mockReachable);
		for (BlueTestResult tempResult : blueResult.results) {
			String name = tempResult.getName();
			String trace = tempResult.getErrorStackTrace();
			switch (name) {
				case "Test 8 Will Always Fail":
					assertEquals("Fail\n", trace);
					break;
				case "Test 9 Will Always Fail":
					assertEquals("Fail    Optional failure message\n", trace);
					break;
				default:
					assertEquals("", trace);
			}
		}
	}
}
