package hudson.plugins.robot.tokens;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.robot.RobotBuildAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RobotReportLinkTokenMacroTest {

    private static final String macroName = "ROBOT_REPORTLINK";

    private RobotReportLinkTokenMacro token;
    private AbstractBuild<?, ?> build;
    private TaskListener listener;
    private RobotBuildAction action;

    @BeforeEach
    void setUp() {
        token = new RobotReportLinkTokenMacro();
        build = mock(AbstractBuild.class);
        listener = mock(TaskListener.class);
        action = mock(RobotBuildAction.class);

        when(build.getUrl()).thenReturn("job/robotjob/1/");
        when(action.getUrlName()).thenReturn("robot");
        when(build.getAction(RobotBuildAction.class)).thenReturn(action);

    }

    @Test
    void testAcceptsName() {
        assertTrue(new RobotReportLinkTokenMacro().acceptsMacroName(macroName));
    }

    @Test
    void testTokenConversionWithoutReportLink() throws Exception {
        assertEquals("job/robotjob/1/robot/report/", token.evaluate(build, listener, macroName));
    }

    @Test
    void testTokenConversionWithReportLink() throws Exception {
        when(action.getLogFileLink()).thenReturn("report.html");
        assertEquals("job/robotjob/1/robot/report/report.html", token.evaluate(build, listener, macroName));
    }
}

