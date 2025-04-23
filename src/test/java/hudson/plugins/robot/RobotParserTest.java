package hudson.plugins.robot;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class RobotParserTest {

    @Test
    void testBasic1() {
        final String dir = ".";
        final String mask = "low_failure_output.xml";
        parse(dir, mask);
    }

    @Test
    void testBasic2() {
        final String dir = ".";
        final String mask = "rebot_output.xml";
        parse(dir, mask);
    }

    @Test
    void testBasic3() {
        final String dir = "blueocean";
        final String mask = "output.xml";
        parse(dir, mask);
    }

    @Test
    void testBasic4() {
        final String dir = "graph";
        final String mask = "output.xml";
        parse(dir, mask);
    }

    @Test
    void testBasic5() {
        final String dir = "model";
        final String mask = "collisions.xml";
        parse(dir, mask);
    }

    @Test
    void testBasic6() {
        final String dir = "model";
        final String mask = "new_critical_output.xml";
        parse(dir, mask);
    }

    @Test
    void testBasic7() {
        final String dir = "model";
        final String mask = "output.xml";
        parse(dir, mask);
    }

    @Test
    void testBasic8() {
        final String dir = "model";
        final String mask = "suite-setup-and-teardown.xml";
        parse(dir, mask);
    }

    @Test
    void testBasic9() {
        final String dir = "model";
        final String mask = "testfile.xml";
        parse(dir, mask);
    }

    @Test
    void testBasic10() {
        final String dir = "model";
        final String mask = "teardown_fail.xml";
        parse(dir, mask);
    }

    @Test
    void testBasic11() {
        final String dir = "model";
        final String mask = "testfile-001.xml";
        parse(dir, mask);
    }

    @Test
    void testBasic12() {
        final String dir = "model";
        final String mask = "testfile-002.xml";
        parse(dir, mask);
    }

    @Test
    void testBasic13() {
        final String dir = "RobotPublisherSystemTest/jobs/robot/workspace";
        final String mask = "output.xml";
        parse(dir, mask);
    }

    @Test
    void testBasic14() {
        final String dir = "RobotPublisherSystemTest/jobs/collisions/workspace";
        final String mask = "output.xml";
        parse(dir, mask);
    }

    @Test
    void testBasic15() {
        final String dir = "RobotPublisherSystemTest/jobs/disable-archive-output-xml/workspace";
        final String mask = "output.xml";
        parse(dir, mask);
    }

    @Test
    void testBasic16() {
        final String dir = "RobotPublisherSystemTest/jobs/dont-copy/workspace";
        final String mask = "output.xml";
        parse(dir, mask);
    }

    @Test
    void testBasic17() {
        final String dir = "RobotPublisherSystemTest/jobs/failingtests/workspace";
        final String mask = "output.xml";
        parse(dir, mask);
    }

    @Test
    void testBasic18() {
        final String dir = "RobotPublisherSystemTest/jobs/robot29output/workspace";
        final String mask = "output.xml";
        parse(dir, mask);
    }

    @Test
    void testBasic19() {
        final String dir = "RobotPublisherSystemTest/jobs/oldrobotbuild/workspace";
        final String mask = "output.xml";
        parse(dir, mask);
    }

    @Test
    void testNested1() {
        final String dir = ".";
        final String mask = "nested_output.xml";
        parse(dir, mask);
    }

    @Test
    void testNested2() {
        final String dir = ".";
        final String mask = "nested_output2.xml";
        parse(dir, mask);
    }

    @Test
    void testRobot4() {
        final String dir = ".";
        final String mask = "robot4_output.xml";
        parse(dir, mask);
    }

    @Test
    void testRobot4Nested() {
        final String dir = ".";
        final String mask = "robot4_nested_output.xml";
        parse(dir, mask);
    }

    @Test
    void testRobot4If() {
        final String dir = ".";
        final String mask = "robot4_if_output.xml";
        parse(dir, mask);
    }

    /**
     * Robot Framework 5.0 introduced TRY-EXCEPT, WHILE,
     * BREAK, and CONTINUE. The output file contains simple
     * test cases which use new features.
     */
    @Test
    void testRobot5TryExceptFinallyWhileContinue() {
        final String dir = "robot5";
        final String mask = "basic_new_features_output.xml";
        parse(dir, mask);
    }

    @Test
    void testEmptyArgTags() {
        final String dir = ".";
        final String mask = "empty_args-output.xml";
        parse(dir, mask);
    }

    @Test
    void testRF7InlineVar() {
        final String dir = "robot7";
        final String mask = "inline_var_output.xml";
        parse(dir, mask);
    }

    private void parse(String dir, String mask) {
        assertDoesNotThrow(() -> {
            File directory = new File(RobotParserTest.class.getResource(dir).toURI());
            RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable(mask, null, null);
            remoteOperation.invoke(directory, null);
        });
    }
}
