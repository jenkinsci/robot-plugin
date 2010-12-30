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
package hudson.plugins.robot.parser;
import hudson.plugins.robot.model.RobotResult;

import java.io.File;

import junit.framework.TestCase;


public class RobotParserTest extends TestCase {
	
	private RobotResult parserResult;


	protected void setUp() throws Exception {
		super.setUp();
		RobotParser parser = new RobotParser();
		File testOutputFile = new File(getClass().getResource("output.xml").toURI());
		parserResult = parser.invoke(testOutputFile, null);
	}

	public void testShouldParseTimestamp(){
		assertEquals("20100629 11:08:54.230", parserResult.getTimeStamp());
	}
	
	public void testShouldParseTestName(){
		//assertEquals("Othercases & Testcases", parserResult.getTestSuites().get(0).getName());
	}
	
	public void testShouldParseAllFailedTests(){
		assertEquals(6, parserResult.getOverallFailed());
	}
	
	public void testShouldParseCriticalPassedTests(){
		assertEquals(4, parserResult.getCriticalPassed());
	}

	public void testShouldParseCriticalFailedTests(){
		assertEquals(6, parserResult.getCriticalFail());
	}
	
	public void testShouldParseTestSuites(){
		//assertEquals(2, parserResult.getTestSuites().size());
	}
}
