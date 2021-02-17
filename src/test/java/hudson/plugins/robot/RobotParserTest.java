package hudson.plugins.robot;

import java.io.File;

import org.junit.Test;

public class RobotParserTest {

    public void parse(String dir, String mask) {
        try {
            File directory = new File(RobotParserTest.class.getResource(dir).toURI());
            RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable(mask, null, null);
            remoteOperation.invoke(directory, null);
        } catch (Exception e) {
            e.printStackTrace();
            assert (false);
        }
    }

    @Test
    public void testBasic1() {
        final String dir = ".";
        final String mask = "low_failure_output.xml";
        parse(dir, mask);
    }

    @Test
    public void testBasic2() {
        final String dir = ".";
        final String mask = "rebot_output.xml";
        parse(dir, mask);
    }

    @Test
    public void testBasic3() {
        final String dir = "blueocean";
        final String mask = "output.xml";
        parse(dir, mask);
    }

    @Test
    public void testBasic4() {
        final String dir = "graph";
        final String mask = "output.xml";
        parse(dir, mask);
    }

    @Test
    public void testBasic5() {
        final String dir = "model";
        final String mask = "collisions.xml";
        parse(dir, mask);
    }

    @Test
    public void testBasic6() {
        final String dir = "model";
        final String mask = "new_critical_output.xml";
        parse(dir, mask);
    }

    @Test
    public void testBasic7() {
        final String dir = "model";
        final String mask = "output.xml";
        parse(dir, mask);
    }

    @Test
    public void testBasic8() {
        final String dir = "model";
        final String mask = "suite-setup-and-teardown.xml";
        parse(dir, mask);
    }

    @Test
    public void testBasic9() {
        final String dir = "model";
        final String mask = "testfile.xml";
        parse(dir, mask);
    }

    @Test
    public void testBasic10() {
        final String dir = "model";
        final String mask = "teardown_fail.xml";
        parse(dir, mask);
    }

    @Test
    public void testBasic11() {
        final String dir = "model";
        final String mask = "testfile-001.xml";
        parse(dir, mask);
    }

    @Test
    public void testBasic12() {
        final String dir = "model";
        final String mask = "testfile-002.xml";
        parse(dir, mask);
    }

    @Test
    public void testBasic13() {
        final String dir = "RobotPublisherSystemTest/jobs/robot/workspace";
        final String mask = "output.xml";
        parse(dir, mask);
    }

    @Test
    public void testBasic14() {
        final String dir = "RobotPublisherSystemTest/jobs/collisions/workspace";
        final String mask = "output.xml";
        parse(dir, mask);
    }

    @Test
    public void testBasic15() {
        final String dir = "RobotPublisherSystemTest/jobs/disable-archive-output-xml/workspace";
        final String mask = "output.xml";
        parse(dir, mask);
    }

    @Test
    public void testBasic16() {
        final String dir = "RobotPublisherSystemTest/jobs/dont-copy/workspace";
        final String mask = "output.xml";
        parse(dir, mask);
    }

    @Test
    public void testBasic17() {
        final String dir = "RobotPublisherSystemTest/jobs/failingtests/workspace";
        final String mask = "output.xml";
        parse(dir, mask);
    }

    @Test
    public void testBasic18() {
        final String dir = "RobotPublisherSystemTest/jobs/robot29output/workspace";
        final String mask = "output.xml";
        parse(dir, mask);
    }

    @Test
    public void testBasic19() {
        final String dir = "RobotPublisherSystemTest/jobs/oldrobotbuild/workspace";
        final String mask = "output.xml";
        parse(dir, mask);
    }

    @Test
    public void testNested1() {
        final String dir = ".";
        final String mask = "nested_output.xml";
        parse(dir, mask);
    }

    @Test
    public void testNested2() {
        final String dir = ".";
        final String mask = "nested_output2.xml";
        parse(dir, mask);
    }

    @Test
    public void testRobot4() {
        final String dir = ".";
        final String mask = "robot4_output.xml";
        parse(dir, mask);
    }
}
