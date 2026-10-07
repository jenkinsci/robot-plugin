package hudson.plugins.robot;

import hudson.plugins.robot.model.RobotResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RobotParserTest {

    @BeforeEach
    void turnRecoveryOn() {
        System.setProperty(RecoveringXMLStreamReader.RECOVER_PARTIAL_OUTPUT, "true");
    }

    @AfterEach
    void leaveTheDefaultAsItIs() {
        System.clearProperty(RecoveringXMLStreamReader.RECOVER_PARTIAL_OUTPUT);
    }

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

    @Test
    void testTruncatedOutput() {
        final String dir = ".";
        final String mask = "truncated_output.xml";
        RobotResult result = parse(dir, mask);
        result.tally(null);
        assertEquals(2, result.getOverallTotal());
        assertEquals(1, result.getOverallPassed());
        assertEquals(1, result.getOverallFailed());
        assertEquals("Crashed Test", result.getAllFailedCases().get(0).getName());
        assertNotNull(result.getParseError());
    }

    @Test
    void testTruncatedBetweenTests() {
        final String dir = ".";
        final String mask = "truncated_between_tests_output.xml";
        RobotResult result = parse(dir, mask);
        result.tally(null);
        assertEquals(1, result.getOverallTotal());
        assertEquals(1, result.getOverallPassed());
        assertEquals(0, result.getOverallFailed());
        assertNotNull(result.getParseError());
    }

    /** Robot fails every test in a suite whose teardown fails. */
    @Test
    void testTruncatedSuiteTeardown() {
        final String dir = ".";
        final String mask = "truncated_teardown_output.xml";
        RobotResult result = parse(dir, mask);
        result.tally(null);
        assertEquals(1, result.getOverallTotal());
        assertEquals(0, result.getOverallPassed());
        assertEquals(1, result.getOverallFailed());
        assertNotNull(result.getParseError());
    }

    /** The pass percentage of zero tests is 100, so the parse error is the only thing that fails the build. */
    @Test
    void testTruncatedBeforeFirstSuite() {
        final String dir = ".";
        final String mask = "truncated_before_suite_output.xml";
        RobotResult result = parse(dir, mask);
        result.tally(null);
        assertEquals(0, result.getOverallTotal());
        assertEquals(100, result.getPassPercentage(), 0.01);
        assertNotNull(result.getParseError());
    }

    /**
     * The truncation points include the middle of multi-byte characters and
     * the middle of entities. This file contains failing tests.
     */
    @Test
    void testEveryTruncationPoint(@TempDir Path workspace) throws Exception {
        sweep(workspace, "rebot_output.xml");
    }

    /** Every test in this file passed, so a parse error is the only failure a truncated copy can report. */
    @Test
    void testEveryTruncationPointOfPassingRun(@TempDir Path workspace) throws Exception {
        sweep(workspace, "robot7/inline_var_output.xml");
    }

    @Test
    void testEveryTruncationPointOfNestedRun(@TempDir Path workspace) throws Exception {
        sweep(workspace, "model/output.xml");
    }

    /**
     * XMLStreamReader implementations differ in when they report the text of an
     * element, so this test is worth running with more than one. The test
     * classpath provides Woodstox. Jenkins provides the JDK implementation
     * unless the woodstox-core-api plugin is installed. Run with
     * -Djavax.xml.stream.XMLInputFactory=com.sun.xml.internal.stream.XMLInputFactoryImpl
     * to test the JDK implementation.
     * <p>
     * Before the end of the robot start tag no element has been read, so parsing
     * must throw. The parser stops reading at the statistics element, so a
     * truncation after the statistics start tag has no parse error. A longer
     * truncation contains every complete test of a shorter one, so the number of
     * reported tests cannot decrease.
     */
    private void sweep(Path workspace, String resource) throws Exception {
        byte[] whole = Files.readAllBytes(Path.of(RobotParserTest.class.getResource(resource).toURI()));
        //ISO-8859-1 maps each byte to one char, so an offset in text is the same offset in whole
        String text = new String(whole, StandardCharsets.ISO_8859_1);
        int readable = text.indexOf('>', text.indexOf("<robot")) + 1;
        int complete = text.indexOf("<statistics>") + "<statistics>".length();
        List<String> broken = new ArrayList<>();
        long reported = 0;
        for (int length = 0; length <= whole.length; length++) {
            Files.write(workspace.resolve("output.xml"), Arrays.copyOf(whole, length));
            RobotParser.RobotParserCallable parser = new RobotParser.RobotParserCallable("output.xml", null, null);
            try {
                RobotResult result = parser.invoke(workspace.toFile(), null);
                result.tally(null);
                if (length < readable)
                    broken.add(length + ": read as " + result.getOverallTotal() + " tests instead of failing");
                else if (length < complete && result.getParseError() == null)
                    broken.add(length + ": read short without saying so");
                else if (length >= complete && result.getParseError() != null)
                    broken.add(length + ": said it read short when it had not");
                else if (result.getOverallTotal() < reported)
                    broken.add(length + ": " + result.getOverallTotal() + " tests after " + reported + " were reported");
                reported = Math.max(reported, result.getOverallTotal());
            } catch (Exception failed) {
                if (length >= readable)
                    broken.add(length + ": " + failed.getCause());
            }
        }
        assertTrue(broken.isEmpty(), () -> broken.size() + " of " + whole.length + " truncation points of "
                + resource + " misbehave: " + String.join(" | ", broken.subList(0, Math.min(10, broken.size()))));
    }

    @Test
    void testSplitSuiteReadShort() {
        final String dir = ".";
        final String mask = "truncated_split_output.xml";
        RobotResult result = parse(dir, mask);
        result.tally(null);
        assertEquals(2, result.getOverallTotal());
        assertEquals(1, result.getOverallPassed());
        assertEquals(1, result.getOverallFailed());
        assertEquals("Crashed Test", result.getAllFailedCases().get(0).getName());
        assertTrue(result.getParseError().startsWith("truncated_split_output-001.xml"));
    }

    @Test
    void testFileCutOffIsRefusedByDefault() throws Exception {
        System.clearProperty(RecoveringXMLStreamReader.RECOVER_PARTIAL_OUTPUT);
        File directory = new File(RobotParserTest.class.getResource(".").toURI());
        RobotParser.RobotParserCallable remoteOperation =
                new RobotParser.RobotParserCallable("truncated_output.xml", null, null);
        assertThrows(IOException.class, () -> remoteOperation.invoke(directory, null));
    }

    private RobotResult parse(String dir, String mask) {
        return assertDoesNotThrow(() -> {
            File directory = new File(RobotParserTest.class.getResource(dir).toURI());
            RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable(mask, null, null);
            return remoteOperation.invoke(directory, null);
        });
    }
}
