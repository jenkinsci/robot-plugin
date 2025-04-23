package hudson.plugins.robot.tokens;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.robot.RobotBuildAction;
import hudson.plugins.robot.model.RobotResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RobotPassPercentageTokenMacroTest {

    private static final String macroName = "ROBOT_PASSPERCENTAGE";

    private RobotPassPercentageTokenMacro token;
    private AbstractBuild<?, ?> build;
    private TaskListener listener;
    private RobotBuildAction action;

    @BeforeEach
    void setUp() {
        token = new RobotPassPercentageTokenMacro();
        build = mock(AbstractBuild.class);
        listener = mock(TaskListener.class);
        action = mock(RobotBuildAction.class);

        RobotResult result = mock(RobotResult.class);

        when(result.getPassPercentage(true)).thenReturn(55.0);
        when(result.getPassPercentage(false)).thenReturn(41.0);
        when(action.getResult()).thenReturn(result);
        when(build.getAction(RobotBuildAction.class)).thenReturn(action);
    }

    @Test
    void testAcceptsName() {
        assertTrue(new RobotPassPercentageTokenMacro().acceptsMacroName(macroName));
    }

    @Test
    void testTokenConversionWithAll() throws Exception {
        token.countSkippedTests = false;
        assertEquals("41.0", token.evaluate(build, listener, macroName));
    }
}
