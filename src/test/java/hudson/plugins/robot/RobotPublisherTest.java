/*
 * Copyright 2008-2014 Nokia Solutions and Networks Oy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hudson.plugins.robot;

import com.ctc.wstx.exc.WstxLazyException;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.plugins.robot.model.RobotResult;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RobotPublisherTest {
    private final boolean countSkipped = false;

    @Test
    void testBlankConfigShouldReturnDefaults() {
        RobotPublisher testable = getRobotPublisher(0, 0);

        assertEquals("output.xml", testable.getOutputFileName());
        assertEquals("report.html", testable.getReportFileName());
        assertEquals("log.html", testable.getLogFileName());
    }

    @Test
    void testShouldReturnSuccessWhenThresholdsExceeded() {
        RobotPublisher publisher = getRobotPublisher(99.9, 99);
        RobotResult mockResult = mock(RobotResult.class);
        AbstractBuild<?, ?> mockBuild = mock(FreeStyleBuild.class);

        when(mockBuild.getResult()).thenReturn(Result.SUCCESS);
        when(mockResult.getPassPercentage(countSkipped)).thenReturn(100.0);

        assertEquals(Result.SUCCESS, publisher.getBuildResult(mockBuild, mockResult));
    }

    @Test
    void testShouldFailWhenFailedBuild() {
        RobotPublisher publisher = getRobotPublisher(0, 0);
        RobotResult mockResult = mock(RobotResult.class);
        AbstractBuild<?, ?> mockBuild = mock(FreeStyleBuild.class);

        when(mockBuild.getResult()).thenReturn(Result.FAILURE);
        when(mockResult.getPassPercentage(countSkipped)).thenReturn(100.0);

        assertEquals(Result.FAILURE, publisher.getBuildResult(mockBuild, mockResult));
    }

    @Test
    void testShouldFailWhenUnstableThresholdNotExceeded() {
        RobotPublisher publisher = getRobotPublisher(90, 50);
        RobotResult mockResult = mock(RobotResult.class);
        AbstractBuild<?, ?> mockBuild = mock(FreeStyleBuild.class);

        when(mockBuild.getResult()).thenReturn(Result.SUCCESS);
        when(mockResult.getPassPercentage(countSkipped)).thenReturn(49.9);

        assertEquals(Result.FAILURE, publisher.getBuildResult(mockBuild, mockResult));
    }

    @Test
    void testShouldBeUnstableWhenPassThresholdNotExceeded() {
        RobotPublisher publisher = getRobotPublisher(90, 50);
        RobotResult mockResult = mock(RobotResult.class);
        AbstractBuild<?, ?> mockBuild = mock(FreeStyleBuild.class);

        when(mockBuild.getResult()).thenReturn(Result.SUCCESS);
        when(mockResult.getPassPercentage(countSkipped)).thenReturn(89.9);

        assertEquals(Result.UNSTABLE, publisher.getBuildResult(mockBuild, mockResult));
    }

    @Test
    void testShouldBeSuccessWithOnlyCritical() {
        RobotPublisher publisher = getRobotPublisher(90, 50);
        RobotResult mockResult = mock(RobotResult.class);
        AbstractBuild<?, ?> mockBuild = mock(FreeStyleBuild.class);

        when(mockBuild.getResult()).thenReturn(Result.SUCCESS);
        when(mockResult.getPassPercentage(countSkipped)).thenReturn(90.0);

        assertEquals(Result.SUCCESS, publisher.getBuildResult(mockBuild, mockResult));
    }

    @Test
    void testShouldUnstableLowFailures() throws Exception {
        RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("low_failure_output.xml", null, null);
        RobotResult result = remoteOperation.invoke(new File(RobotPublisherTest.class.getResource("low_failure_output.xml").toURI()).getParentFile(), null);
        result.tally(null);

        assertEquals(1, result.getOverallFailed());
        assertEquals(2001, result.getOverallTotal());

        RobotPublisher publisher = getRobotPublisher(100, 0);
        AbstractBuild<?, ?> mockBuild = mock(FreeStyleBuild.class);

        when(mockBuild.getResult()).thenReturn(Result.SUCCESS);

        assertEquals(Result.UNSTABLE, publisher.getBuildResult(mockBuild, result));
    }

    @Test
    void testShouldHandleDurationWithoutTimes() throws Exception {
        RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("rebot_output.xml", null, null);
        RobotResult result = remoteOperation.invoke(new File(RobotPublisherTest.class.getResource("rebot_output.xml").toURI()).getParentFile(), null);
        result.tally(null);

        assertEquals(151, result.getDuration());
    }

    @Test
    void testOutputFileWithXXEShouldThrowException() {
        RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("xxe_output.xml", null, null);
        assertThrows(WstxLazyException.class, () ->
                remoteOperation.invoke(new File(RobotPublisherTest.class.getResource("xxe_output.xml").toURI()).getParentFile(), null));
    }

    private RobotPublisher getRobotPublisher(double passThreshold, double unstableThreshold) {
        return new RobotPublisher(null, "", "", false, "", "", passThreshold, unstableThreshold, countSkipped, "", false, "", false);
    }

}
