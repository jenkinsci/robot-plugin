package hudson.plugins.robot.blueocean;

import hudson.model.FreeStyleBuild;
import hudson.plugins.robot.RobotBuildAction;
import hudson.plugins.robot.RobotParser;
import hudson.plugins.robot.model.RobotResult;
import io.jenkins.blueocean.rest.Reachable;
import io.jenkins.blueocean.rest.factory.BlueTestResultFactory.Result;
import io.jenkins.blueocean.rest.hal.Link;
import io.jenkins.blueocean.rest.model.BlueTestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class BlueRobotTestResultForRobot4Test {

    private RobotResult result;
    private FreeStyleBuild mockBuild;
    private RobotBuildAction mockAction;
    private Reachable mockReachable;

    @BeforeEach
    void setUp() throws Exception {
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
    void testSimpleStacktrace() {
        BlueTestResult result = getResult("Test 8 Will Always Fail");
        assertEquals("Fail", result.getErrorStackTrace());
    }

    @Test
    void testStacktraceWithArguments() {
        BlueTestResult result = getResult("Test 9 Will Always Fail");
        assertEquals("Fail    Optional failure message", result.getErrorStackTrace());
    }

    @Test
    void testStacktraceIsEmpty() {
        BlueTestResult result;
        for (String testCase : Arrays.asList("Test 1 Will Always Pass",
                "Test 2 Will Always Pass",
                "Test 3 Will Always Pass",
                "Test 4 Will Always Pass But Is Skipped",
                "Test 5 Will Always Fail But Is Skipped",
                "Test 6 Will Always Fail But Is Skipped",
                "Test 7 Will Always Fail But Is Skipped",
                "Test 10 Will Always Fail But Is Skipped On Failure",
                "Test 11 Will Always Fail But Is Skipped On Failure",
                "Test 12 Will Always Pass But Is Skipped On Failure")) {
            result = getResult(testCase);
            assertEquals("", result.getErrorStackTrace());
        }
    }

    @Test
    void testForLoopStackTrace() {
        BlueTestResult result = getResult("For Loop Failure");
        String helper = """
                FOR IN RANGE
                  Log    ${x}
                  Nested Keyword    ${x}
                    Log    ${arg}
                  Run Keyword If    ${x}==1    Fail
                  Log    ${x}
                  Nested Keyword    ${x}
                    Log    ${arg}
                  Run Keyword If    ${x}==1    Fail
                    Fail
                END""";
        assertEquals(helper, result.getErrorStackTrace());
    }

    @Test
    void testIfElseStackTrace() {
        BlueTestResult result = getResult("If Else Failure");
        String helper = "Set Variable    mikki hiiri\nIF\n  Fail\nELSE\n  Nested Keyword    ${var}\n    Log    ${arg}\n  Fail    ${var}\nEND";
        assertEquals(helper, result.getErrorStackTrace());
    }

    @Test
    void testIfFailure() {
        BlueTestResult result = getResult("If Failure");
        String helper = "Set Variable    mikki hiiri\nIF\n  Nested Keyword    ${var}\n    Log    ${arg}\n  Fail\nEND";
        assertEquals(helper, result.getErrorStackTrace());
    }

    private BlueTestResult getResult(String filterCondition) {
        BlueRobotTestResult.FactoryImpl factory = new BlueRobotTestResult.FactoryImpl();
        Result blueResult = factory.getBlueTestResults(mockBuild, mockReachable);
        return StreamSupport.stream(blueResult.results.spliterator(), false)
                .filter(element -> element.getName().equals(filterCondition))
                .toList().get(0);
    }


}
