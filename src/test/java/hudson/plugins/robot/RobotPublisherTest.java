/*
* Copyright 2008-2011 Nokia Siemens Networks Oyj
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
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
import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.plugins.robot.RobotPublisher;
import hudson.plugins.robot.model.RobotResult;
import junit.framework.TestCase;

public class RobotPublisherTest extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testBlankConfigShouldReturnDefaults() {
		RobotPublisher testable = new RobotPublisher(" "," ", " ", " ", 0, 0, false);
		
		assertEquals("output.xml", testable.getOutputFileName());
		assertEquals("report.html", testable.getReportFileName());
		assertEquals("log.html", testable.getLogFileName());
	}
	
	public void testShouldReturnSuccessWhenThresholdsExceeded() throws Exception{
		boolean onlyCritical = false;
		
		RobotPublisher publisher = new RobotPublisher("","","","",99.9,99,onlyCritical);
		RobotResult mockResult = mock(RobotResult.class);
		AbstractBuild<?,?> mockBuild = mock(FreeStyleBuild.class);
		
		when(mockBuild.getResult()).thenReturn(Result.SUCCESS);
		when(mockResult.getPassPercentage(onlyCritical)).thenReturn(100.0);
		
		assertEquals(Result.SUCCESS, publisher.getBuildResult(mockBuild,mockResult));
	}
	
	public void testShouldFailWhenFailedBuild() throws Exception{
		boolean onlyCritical = false;
		
		RobotPublisher publisher = new RobotPublisher("","","","",0,0,onlyCritical);
		RobotResult mockResult = mock(RobotResult.class);
		AbstractBuild<?,?> mockBuild = mock(FreeStyleBuild.class);
		
		when(mockBuild.getResult()).thenReturn(Result.FAILURE);
		when(mockResult.getPassPercentage(onlyCritical)).thenReturn(100.0);
		
		assertEquals(Result.FAILURE, publisher.getBuildResult(mockBuild,mockResult));
	}
	
	public void testShouldFailWhenUnstableThresholdNotExceeded(){
		boolean onlyCritical = false;
		
		RobotPublisher publisher = new RobotPublisher("","","","",90,50,onlyCritical);
		RobotResult mockResult = mock(RobotResult.class);
		AbstractBuild<?,?> mockBuild = mock(FreeStyleBuild.class);
		
		when(mockBuild.getResult()).thenReturn(Result.SUCCESS);
		when(mockResult.getPassPercentage(onlyCritical)).thenReturn(49.9);
		
		assertEquals(Result.FAILURE, publisher.getBuildResult(mockBuild,mockResult));
	}
	
	public void testShouldBeUnstableWhenPassThresholdNotExceeded(){
		boolean onlyCritical = false;
		
		RobotPublisher publisher = new RobotPublisher("","","","",90,50,onlyCritical);
		RobotResult mockResult = mock(RobotResult.class);
		AbstractBuild<?,?> mockBuild = mock(FreeStyleBuild.class);
		
		when(mockBuild.getResult()).thenReturn(Result.SUCCESS);
		when(mockResult.getPassPercentage(onlyCritical)).thenReturn(89.9);
		
		assertEquals(Result.UNSTABLE, publisher.getBuildResult(mockBuild,mockResult));
	}
	
	public void testShouldBeSuccessWithOnlyCritical(){
		boolean onlyCritical = false;
		
		RobotPublisher publisher = new RobotPublisher("","","","",90,50,onlyCritical);
		RobotResult mockResult = mock(RobotResult.class);
		AbstractBuild<?,?> mockBuild = mock(FreeStyleBuild.class);
		
		when(mockBuild.getResult()).thenReturn(Result.SUCCESS);
		when(mockResult.getPassPercentage(onlyCritical)).thenReturn(90.0);
		
		assertEquals(Result.SUCCESS, publisher.getBuildResult(mockBuild,mockResult));
	}
}
