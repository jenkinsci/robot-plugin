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
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class BlueRobotTestResultTest {

    private RobotResult result;
    private FreeStyleBuild mockBuild;
    private RobotBuildAction mockAction;
    private Reachable mockReachable;

    @BeforeEach
    void setUp() throws Exception {
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
    void testSimpleTrace() {
        BlueTestResult result = getResult("Failed Test");
        assertEquals("Fail    This fails!", result.getErrorStackTrace());
        assertEquals("This fails!", result.getErrorDetails());
    }

    @Test
    void testNestedTrace() {
        BlueTestResult result = getResult("Nested failed test");
        assertEquals("My failed Keyword\n  The real failed keyword\n    Fail    Really fails!", result.getErrorStackTrace());
        assertEquals("Really fails!", result.getErrorDetails());
    }

    @Test
    void testNestedNotFirst() {
        BlueTestResult result = getResult("Nested with not first");
        String helper = """
                Should Be Equal    ${MESSAGE}    Hello, world!
                Should Be Equal    ${MESSAGE}    Hello, world!
                My failed Keyword
                  The real failed keyword
                    Fail    Really fails!""";
        assertEquals(helper, result.getErrorStackTrace());
        assertEquals("Really fails!", result.getErrorDetails());
    }

    @Test
    void testEmptyMessage() {
        BlueTestResult result1 = getResult("Another Test");
        BlueTestResult result2 = getResult("My Test");

        assertEquals("", result1.getErrorStackTrace());
        assertEquals("", result1.getErrorDetails());
        assertEquals("", result2.getErrorStackTrace());
        assertEquals("", result2.getErrorDetails());
    }

    private BlueTestResult getResult(String filterCondition) {
        BlueRobotTestResult.FactoryImpl factory = new BlueRobotTestResult.FactoryImpl();
        Result blueResult = factory.getBlueTestResults(mockBuild, mockReachable);
        return StreamSupport.stream(blueResult.results.spliterator(), false)
                .filter(element -> element.getName().equals(filterCondition))
                .toList().get(0);
    }
}
