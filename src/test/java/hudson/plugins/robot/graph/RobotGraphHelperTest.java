/*
* Copyright 2008-2014 Nokia Solutions and Networks Oy
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
package hudson.plugins.robot.graph;

import hudson.model.FreeStyleBuild;
import hudson.plugins.robot.RobotParser;
import hudson.plugins.robot.model.RobotResult;
import junit.framework.TestCase;

import java.io.File;
import java.util.GregorianCalendar;

import static org.mockito.Mockito.*;

public class RobotGraphHelperTest extends TestCase {

	private static final String xLabelFormat = "#$build";

	private RobotResult mockResult1;
	private RobotResult mockResult2;

	protected void setUp() throws Exception {
		super.setUp();

		RobotParser.RobotParserCallable remoteOperation = new RobotParser.RobotParserCallable("output.xml", null, null);
		RobotResult result = remoteOperation.invoke(new File(RobotGraphHelperTest.class.getResource("output.xml").toURI()).getParentFile(), null);
		result.tally(null);

		// Mocked builds to play as owners of test results
		FreeStyleBuild mockBuild1 = mock(FreeStyleBuild.class);
		FreeStyleBuild mockBuild2 = mock(FreeStyleBuild.class);
		when(mockBuild2.compareTo(mockBuild1)).thenReturn(1);
		when(mockBuild1.compareTo(mockBuild2)).thenReturn(-1);

		// This is to pass hudson.util.Graph constructor
		GregorianCalendar c = new GregorianCalendar();
		c.setTimeInMillis(0L);
		when(mockBuild1.getTimestamp()).thenReturn(c);
		when(mockBuild2.getTimestamp()).thenReturn(c);
		when(mockBuild1.getTime()).thenReturn(c.getTime());
		when(mockBuild2.getTime()).thenReturn(c.getTime());
		when(mockBuild1.getDisplayName()).thenReturn("1.2.3");
		when(mockBuild2.getDisplayName()).thenReturn("3.2.1");

		// set up some results chains
		mockResult1 = spy(result);
		doReturn(null).when(mockResult1).getPreviousResult();
		doReturn(mockBuild1).when(mockResult1).getOwner();

		mockResult2 = spy(result);
		doReturn(mockResult1).when(mockResult2).getPreviousResult();
		doReturn(mockBuild2).when(mockResult2).getOwner();
	}

	public void testShouldLimitResultsGraphDataSet() throws Exception {
		RobotGraph limitedResultsGraph = RobotGraphHelper.createTestResultsGraphForTestObject(
				mockResult2, false, false, false, false, xLabelFormat,1);

		assertEquals(1, limitedResultsGraph.getDataset().getColumnCount());
	}

	public void testShouldReturnAllResultsGraphDataIfNotLimited() throws Exception {
		RobotGraph notlimitedResultsGraph = RobotGraphHelper.createTestResultsGraphForTestObject(
				mockResult2, false, false, false, false, xLabelFormat,0);

		assertEquals(2, notlimitedResultsGraph.getDataset().getColumnCount());
	}

	public void testShouldReturnAllResultsGraphDataIfLimitIsBiggerThanDataAmount() throws Exception {
		RobotGraph notlimitedResultsGraph = RobotGraphHelper.createTestResultsGraphForTestObject(
				mockResult2, false, false, false, false, xLabelFormat,10);

		assertEquals(2, notlimitedResultsGraph.getDataset().getColumnCount());
	}

	public void testShouldShowCustomLabel() throws Exception {
		RobotGraph customLabelGraph = RobotGraphHelper.createTestResultsGraphForTestObject(
				mockResult2, false, false, false, false, "$display_name",0);

		assertEquals("3.2.1", customLabelGraph.getDataset().getColumnKey(0).toString());
		assertEquals("1.2.3", customLabelGraph.getDataset().getColumnKey(1).toString());
	}

	public void testShouldLimitDurationGraphDataSet() throws Exception {
		RobotGraph limitedResultsGraph = RobotGraphHelper.createDurationGraphForTestObject(
				mockResult2, false, 1,xLabelFormat,false);

		assertEquals(1, limitedResultsGraph.getDataset().getColumnCount());
	}

	public void testShouldReturnAllDurationGraphDataIfNotLimited() throws Exception {
		RobotGraph notlimitedResultsGraph = RobotGraphHelper.createDurationGraphForTestObject(
				mockResult2, false, 0, xLabelFormat,false);

		assertEquals(2, notlimitedResultsGraph.getDataset().getColumnCount());
	}

	public void testShouldReturnAllDurationDataIfLimitIsBiggerThanDataAmount() throws Exception {
		RobotGraph notlimitedResultsGraph = RobotGraphHelper.createDurationGraphForTestObject(
				mockResult2, false, 10, xLabelFormat,false);

		assertEquals(2, notlimitedResultsGraph.getDataset().getColumnCount());
	}

}
