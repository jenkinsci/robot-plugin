package hudson.plugins.robot.tokens;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.robot.RobotBuildAction;
import hudson.plugins.robot.model.RobotCaseResult;
import hudson.plugins.robot.model.RobotResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RobotFailedCasesTokenMacroTest {

    private static final String macroName = "ROBOT_FAILEDCASES";

    private RobotFailedCasesTokenMacro token;
    private AbstractBuild<?, ?> build;
    private TaskListener listener;
    private RobotBuildAction action;

    @BeforeEach
    void setUp() {
        token = new RobotFailedCasesTokenMacro();
        build = mock(AbstractBuild.class);
        listener = mock(TaskListener.class);
        action = mock(RobotBuildAction.class);

        RobotResult result = mock(RobotResult.class);

        List<RobotCaseResult> failedList = new ArrayList<>();

        RobotCaseResult case1 = mock(RobotCaseResult.class);
        when(case1.getRelativePackageName(result)).thenReturn("Failcases.subcases.Failure1");
        when(case1.getErrorMsg()).thenReturn("Case1 failed");

        RobotCaseResult case2 = mock(RobotCaseResult.class);
        when(case2.getRelativePackageName(result)).thenReturn("Morefails.Failure2");
        when(case2.getErrorMsg()).thenReturn("Case2 failed");

        failedList.add(case1);
        failedList.add(case2);

        when(result.getAllFailedCases()).thenReturn(failedList);
        when(action.getResult()).thenReturn(result);
        when(build.getAction(RobotBuildAction.class)).thenReturn(action);
    }

    @Test
    void testAcceptsName() {
        assertTrue(new RobotFailedCasesTokenMacro().acceptsMacroName(macroName));
    }

    @Test
    void testTokenConversionWithoutMessages() throws Exception {
        assertEquals("Failcases.subcases.Failure1\nMorefails.Failure2", token.evaluate(build, listener, macroName));
    }

    @Test
    void testTokenConversionWithMessages() throws Exception {
        token.addErrorMessages = true;
        assertEquals("Failcases.subcases.Failure1: Case1 failed\nMorefails.Failure2: Case2 failed", token.evaluate(build, listener, macroName));
    }
}
