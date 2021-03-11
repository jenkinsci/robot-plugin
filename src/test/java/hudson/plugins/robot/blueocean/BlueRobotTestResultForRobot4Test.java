package hudson.plugins.robot.blueocean;

import static org.junit.Assert.assertEquals;
import hudson.model.FreeStyleBuild;
import hudson.plugins.robot.RobotBuildAction;
import hudson.plugins.robot.RobotParser;
import hudson.plugins.robot.model.RobotResult;
import io.jenkins.blueocean.rest.Reachable;
import io.jenkins.blueocean.rest.factory.BlueTestResultFactory.Result;
import io.jenkins.blueocean.rest.hal.Link;
import io.jenkins.blueocean.rest.model.BlueTestResult;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class BlueRobotTestResultForRobot4Test {
    private RobotResult result;
    private FreeStyleBuild mockBuild;
    private RobotBuildAction mockAction;
    private Reachable mockReachable;

    @Before
    public void setUp() throws Exception {
        RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("blue_skip.xml", null, null);
        result = remoteOperation.invoke(new File(BlueRobotTestResultTest.class.getResource("blue_skip.xml").toURI()).getParentFile(), null);
        result.tally(null);

        mockBuild = mock(FreeStyleBuild.class);
        mockAction = mock(RobotBuildAction.class);
        mockReachable = mock(Reachable.class);
        doReturn(mockAction).when(mockBuild).getAction(RobotBuildAction.class);
        doReturn(result.getAllCases()).when(mockAction).getAllTests();
        doReturn(new Link("/")).when(mockReachable).getLink();
    }

    @Test
    public void testSimpleStacktrace(){
        BlueTestResult result = getResult("Test 8 Will Always Fail");
        assertEquals("Fail\n", result.getErrorStackTrace());
    }

    @Test
    public void testStacktraceWithArguments(){
        BlueTestResult result = getResult("Test 9 Will Always Fail");
        assertEquals("Fail    Optional failure message\n", result.getErrorStackTrace());
    }

    @Test
    public void testStacktraceIsEmpty(){
        BlueTestResult result;
        for(String testCase : Arrays.asList("Test 1 Will Always Pass",
                                            "Test 2 Will Always Pass",
                                            "Test 3 Will Always Pass",
                                            "Test 4 Will Always Pass But Is Skipped",
                                            "Test 5 Will Always Fail But Is Skipped",
                                            "Test 6 Will Always Fail But Is Skipped",
                                            "Test 7 Will Always Fail But Is Skipped",
                                            "Test 10 Will Always Fail But Is Skipped On Failure",
                                            "Test 11 Will Always Fail But Is Skipped On Failure",
                                            "Test 12 Will Always Pass But Is Skipped On Failure")){
            result = getResult(testCase);
            assertEquals("", result.getErrorStackTrace());
        }
    }

    @Test
    public void testForLoopStackTrace() throws Exception {
        BlueTestResult result = getResult("For Loop Failure");
        String helper = "FOR IN RANGE\n  Log    ${x}\n  Nested Keyword    ${x}    ${arg}\n  Run Keyword If    ${x}==1    Fail\n" +
                "  Log    ${x}\n  Nested Keyword    ${x}    ${arg}\n  Run Keyword If    ${x}==1    Fail\nEND\n";
        assertEquals(helper, result.getErrorStackTrace());
    }

    @Test
    public void testIfElseStackTrace() throws Exception {
        BlueTestResult result = getResult("If Else Failure");
        String helper = "Set Variable    mikki hiiri\nIF\n  Fail\nELSE\n  Nested Keyword    ${var}    ${arg}\n  Fail    ${var}\nEND\n";
        assertEquals(helper, result.getErrorStackTrace());
    }

    @Test
    public void testIfFailure() throws Exception {
        BlueTestResult result = getResult("If Failure");
        String helper = "Set Variable    mikki hiiri\nIF\n  Nested Keyword    ${var}    ${arg}\n  Fail\nEND\n";
        assertEquals(helper, result.getErrorStackTrace());
    }

    private BlueTestResult getResult(String filterCondition){
        BlueRobotTestResult.FactoryImpl factory = new BlueRobotTestResult.FactoryImpl();
        Result blueResult = factory.getBlueTestResults(mockBuild, mockReachable);
        return StreamSupport.stream(blueResult.results.spliterator(), false)
                .filter(element -> element.getName().equals(filterCondition))
                .collect(Collectors.toList()).get(0);
    }


}
