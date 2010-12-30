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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.dom4j.DocumentException;

import junit.framework.TestCase;

public class RobotTestSuiteTest extends TestCase {
	
	private List<RobotTestSuite> suites;

	public void setUp() throws Exception{
		File reportFile = new File(getClass().getResource("output.xml").toURI());
		suites = RobotTestSuite.parse(reportFile);

	}
	
	public void testShouldParseSuites() throws DocumentException, IOException, URISyntaxException {
			assertEquals(2, suites.size());
	}
	
	public void testShouldParseNestedSuites(){
		List<RobotTestSuite> nestedSuites = suites.get(0).getChildren();
		RobotTestSuite firstNested = nestedSuites.get(0);
		assertEquals("Othercases", firstNested.getName());
	}
	
	public void testShouldParseThirdLevelSuites(){
		List<RobotTestSuite> nestedSuites = suites.get(0).getChildren();
		RobotTestSuite firstNested = nestedSuites.get(0);
	
		//Go another level deeper
		nestedSuites = firstNested.getChildren();
		firstNested = nestedSuites.get(0);
		
		assertEquals("3rd level cases", firstNested.getName());
	}
	
	public void testShouldParseTestCases(){
		RobotTestSuite suite = suites.get(0);
		RobotCaseResult testCase = suite.getTestCase("Hello");
		assertNotNull(testCase);
	}

}
