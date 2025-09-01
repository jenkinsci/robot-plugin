package hudson.plugins.robot.tokens;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.robot.RobotBuildAction;
import hudson.plugins.robot.model.RobotResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RobotReportLinkTokenMacroTest {

    private static final String macroName = "ROBOT_REPORTLINK";

    private RobotReportLinkTokenMacro token;
    private AbstractBuild<?, ?> build;
    private TaskListener listener;
    private RobotResult result;
    private RobotBuildAction action;

    @BeforeEach
    void setUp() {
        token = new RobotReportLinkTokenMacro();
        build = mock(AbstractBuild.class);
        listener = mock(TaskListener.class);
        result = mock(RobotResult.class);

        when(build.getRootDir()).thenReturn(new File("."));
        when(build.getUrl()).thenReturn("job/robotjob/1/");
    }

    @Test
    void testAcceptsName() {
        assertTrue(new RobotReportLinkTokenMacro().acceptsMacroName(macroName));
    }

    @Test
    void testLocalTokenConversionWithoutLogLink() throws Exception {
        action = new RobotBuildAction(build, result, "", listener, null, null, false, "", false, false);
        when(build.getAction(RobotBuildAction.class)).thenReturn(action);
        assertEquals("job/robotjob/1/robot/report/", token.evaluate(build, listener, macroName));
    }

    @Test
    void testLocalTokenConversionWithLogLink() throws Exception {
        action = new RobotBuildAction(build, result, "", listener, "log.html", null, false, "", false, false);
        when(build.getAction(RobotBuildAction.class)).thenReturn(action);
        assertEquals("job/robotjob/1/robot/report/log.html", token.evaluate(build, listener, macroName));
    }

    @Test
    void testArtifactTokenConversionWithoutLogLink() throws Exception {
        action = new RobotBuildAction(build, result, "", listener, null, null, false, "", false, true);
        when(build.getAction(RobotBuildAction.class)).thenReturn(action);
        assertEquals("job/robotjob/1/artifact/", token.evaluate(build, listener, macroName));
    }

    @Test
    void testArtifactTokenConversionWithLogLink() throws Exception {
        action = new RobotBuildAction(build, result, "", listener, "log.html", null, false, "", false, true);
        when(build.getAction(RobotBuildAction.class)).thenReturn(action);
        assertEquals("job/robotjob/1/artifact/log.html", token.evaluate(build, listener, macroName));
    }
}

