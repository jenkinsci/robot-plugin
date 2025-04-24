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

class RobotFailTokenMacroTest {

    private static final String macroName = "ROBOT_FAILED";
    private RobotFailTokenMacro token;
    private AbstractBuild<?, ?> build;
    private TaskListener listener;

    @BeforeEach
    void setUp() {
        token = new RobotFailTokenMacro();
        build = mock(AbstractBuild.class);
        listener = mock(TaskListener.class);

        RobotBuildAction action = mock(RobotBuildAction.class);
        RobotResult result = mock(RobotResult.class);

        when(result.getOverallFailed()).thenReturn(6L);
        when(action.getResult()).thenReturn(result);
        when(build.getAction(RobotBuildAction.class)).thenReturn(action);
    }

    @Test
    void testAcceptsName() {
        assertTrue(token.acceptsMacroName(macroName));
    }

    @Test
    void testTokenConversionWithAll() throws Exception {
        assertEquals("6", token.evaluate(build, listener, macroName));
    }
}
