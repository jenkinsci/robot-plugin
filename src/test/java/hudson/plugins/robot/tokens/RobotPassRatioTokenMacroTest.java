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

class RobotPassRatioTokenMacroTest {

    private static final String macroName = "ROBOT_PASSRATIO";
    private RobotPassRatioTokenMacro token;
    private AbstractBuild<?, ?> build;
    private TaskListener listener;

    @BeforeEach
    void setUp() {
        token = new RobotPassRatioTokenMacro();
        build = mock(AbstractBuild.class);
        listener = mock(TaskListener.class);

        RobotBuildAction action = mock(RobotBuildAction.class);
        RobotResult result = mock(RobotResult.class);

        when(result.getOverallPassed()).thenReturn(6L);
        when(result.getOverallTotal()).thenReturn(13L);
        when(action.getResult()).thenReturn(result);
        when(build.getAction(RobotBuildAction.class)).thenReturn(action);
    }

    @Test
    void testAcceptsName() {
        assertTrue(new RobotPassRatioTokenMacro().acceptsMacroName(macroName));
    }

    @Test
    void testTokenConversionWithAll() throws Exception {
        assertEquals("6 / 13", token.evaluate(build, listener, macroName));
    }
}
