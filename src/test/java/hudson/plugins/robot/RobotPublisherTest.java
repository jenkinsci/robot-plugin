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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import com.ctc.wstx.exc.WstxLazyException;
import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.plugins.robot.model.RobotResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RobotPublisherTest {
	private final boolean onlyCritical = false;
	private final boolean countSkipped = false;

	@Before
	public void setUp() throws Exception {
	}

	private RobotPublisher getRobotPublisher(double passThreshold, double unstableThreshold) {
		return new RobotPublisher(null, "", "", false, "", "", passThreshold, unstableThreshold, onlyCritical, countSkipped, "", false, "");
	}

	@Test
	public void testBlankConfigShouldReturnDefaults() {
		RobotPublisher testable = getRobotPublisher(0, 0);

		Assert.assertEquals("output.xml", testable.getOutputFileName());
		Assert.assertEquals("report.html", testable.getReportFileName());
		Assert.assertEquals("log.html", testable.getLogFileName());
	}

	@Test
	public void testShouldReturnSuccessWhenThresholdsExceeded() throws Exception {
		RobotPublisher publisher = getRobotPublisher(99.9, 99);
		RobotResult mockResult = mock(RobotResult.class);
		AbstractBuild<?,?> mockBuild = mock(FreeStyleBuild.class);

		when(mockBuild.getResult()).thenReturn(Result.SUCCESS);
		when(mockResult.getPassPercentage(onlyCritical)).thenReturn(100.0);

		Assert.assertEquals(Result.SUCCESS, publisher.getBuildResult(mockBuild, mockResult));
	}

	@Test
	public void testShouldFailWhenFailedBuild() throws Exception{
		RobotPublisher publisher = getRobotPublisher(0, 0);
		RobotResult mockResult = mock(RobotResult.class);
		AbstractBuild<?,?> mockBuild = mock(FreeStyleBuild.class);

		when(mockBuild.getResult()).thenReturn(Result.FAILURE);
		when(mockResult.getPassPercentage(onlyCritical)).thenReturn(100.0);

		Assert.assertEquals(Result.FAILURE, publisher.getBuildResult(mockBuild, mockResult));
	}

	@Test
	public void testShouldFailWhenUnstableThresholdNotExceeded(){
		RobotPublisher publisher = getRobotPublisher(90, 50);
		RobotResult mockResult = mock(RobotResult.class);
		AbstractBuild<?,?> mockBuild = mock(FreeStyleBuild.class);

		when(mockBuild.getResult()).thenReturn(Result.SUCCESS);
		when(mockResult.getPassPercentage(onlyCritical)).thenReturn(49.9);

		Assert.assertEquals(Result.FAILURE, publisher.getBuildResult(mockBuild, mockResult));
	}

	@Test
	public void testShouldBeUnstableWhenPassThresholdNotExceeded(){
		RobotPublisher publisher = getRobotPublisher(90, 50);
		RobotResult mockResult = mock(RobotResult.class);
		AbstractBuild<?,?> mockBuild = mock(FreeStyleBuild.class);

		when(mockBuild.getResult()).thenReturn(Result.SUCCESS);
		when(mockResult.getPassPercentage(onlyCritical)).thenReturn(89.9);

		Assert.assertEquals(Result.UNSTABLE, publisher.getBuildResult(mockBuild, mockResult));
	}

	@Test
	public void testShouldBeSuccessWithOnlyCritical(){
		RobotPublisher publisher = getRobotPublisher(90, 50);
		RobotResult mockResult = mock(RobotResult.class);
		AbstractBuild<?,?> mockBuild = mock(FreeStyleBuild.class);

		when(mockBuild.getResult()).thenReturn(Result.SUCCESS);
		when(mockResult.getPassPercentage(onlyCritical)).thenReturn(90.0);

		Assert.assertEquals(Result.SUCCESS, publisher.getBuildResult(mockBuild, mockResult));
	}

	@Test
	public void testShouldUnstableLowFailures() throws Exception{
		RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("low_failure_output.xml", null, null);
		RobotResult result = remoteOperation.invoke(new File(RobotPublisherTest.class.getResource("low_failure_output.xml").toURI()).getParentFile(), null);
		result.tally(null);

		Assert.assertEquals(1, result.getOverallFailed());
		Assert.assertEquals(2001, result.getOverallTotal());

		RobotPublisher publisher = getRobotPublisher(100, 0);
		AbstractBuild<?,?> mockBuild = mock(FreeStyleBuild.class);

		when(mockBuild.getResult()).thenReturn(Result.SUCCESS);

		Assert.assertEquals(Result.UNSTABLE, publisher.getBuildResult(mockBuild, result));
	}
	
	@Test
	public void testShouldHandleDurationWithoutTimes() throws Exception {
		RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("rebot_output.xml", null, null);
		RobotResult result = remoteOperation.invoke(new File(RobotPublisherTest.class.getResource("rebot_output.xml").toURI()).getParentFile(), null);
		result.tally(null);

		Assert.assertEquals(151, result.getDuration());
	}

	@Test(expected = WstxLazyException.class)
	public void testOutputFileWithXXEShouldThrowException() throws URISyntaxException, IOException {
		RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("xxe_output.xml", null, null);
		RobotResult result = remoteOperation.invoke(new File(RobotPublisherTest.class.getResource("xxe_output.xml").toURI()).getParentFile(), null);
	}

}
