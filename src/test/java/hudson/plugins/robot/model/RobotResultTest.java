/*
* Copyright 2008-2010 Nokia Siemens Networks Oyj
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
package hudson.plugins.robot.model;

import java.util.ArrayList;

import junit.framework.TestCase;

public class RobotResultTest extends TestCase {

	private RobotResult result;
	
	protected void setUp() throws Exception {
		super.setUp();
		result = new RobotResult();
		RobotResultStatistics criticalStats = new RobotResultStatistics();
		criticalStats.setPass(5);
		criticalStats.setFail(4);
		RobotResultStatistics overallStats = new RobotResultStatistics();
		overallStats.setPass(1);
		overallStats.setFail(2);
		ArrayList<RobotResultStatistics> statsByCategory = new ArrayList<RobotResultStatistics>();
		statsByCategory.add(criticalStats);
		statsByCategory.add(overallStats);
		result.setStatsByCategory(statsByCategory);
	}
	
	public void testShouldReturnCriticalPassPercentage(){
		assertEquals(55.6, result.getPassPercentage(true));
	}
	public void testShouldReturnOverallPassPercentage(){
		assertEquals(33.3, result.getPassPercentage(false));
	}

}
