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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.dom4j.Element;

import org.junit.Test;


public class RobotSuiteResultTest {
	

	@Test
	public void shouldAcceptMultipleSuitesAsChildren(){
		RobotSuiteResult result = new RobotSuiteResult("");
		
		//Robot sibling suites should have unique names by definition (directories / files in file system)
		RobotSuiteResult child = new RobotSuiteResult("child");
		RobotSuiteResult child2 = new RobotSuiteResult("child2");
		
		result.addChild(child);
		result.addChild(child2);
		
		assertEquals(2, result.getChildSuites().size());
	}
	
	@Test
	public void shouldReturnNullIfNoChildren(){
		RobotSuiteResult result = new RobotSuiteResult("");
		assertNull(result.getChildSuites());
	}
	
	@Test
	public void shouldReturnChildSuiteByName(){
		RobotSuiteResult result = new RobotSuiteResult("");
		RobotSuiteResult child = new RobotSuiteResult("child");
		result.addChild(child);
		
		assertEquals(child, result.getSuite("child"));
	}
	
	@Test
	public void shouldReturnTestCaseByName(){
		RobotSuiteResult result = new RobotSuiteResult("");
		RobotCaseResult caseResult = new RobotCaseResult("case1");
		
		result.addCaseResult(caseResult);
		assertEquals(caseResult, result.getCase("case1"));
	}
	
	@Test
	public void shouldReturnNullIfNoParent() {
		RobotSuiteResult rootResult = new RobotSuiteResult("");
		assertNull(rootResult.getParent());
	}
	
	@Test
	public void shouldAcceptMultipleCaseResults() {
		RobotSuiteResult result = new RobotSuiteResult("");
		RobotCaseResult caseResult = new RobotCaseResult("case1");
		RobotCaseResult caseResult2 = new RobotCaseResult("case2");
		
		result.addCaseResult(caseResult);
		result.addCaseResult(caseResult2);
		
		assertEquals(2, result.getCaseResults().size());
	}
	
	@Test
	public void shouldReturnNullIfNoTestCases() {
		RobotSuiteResult result = new RobotSuiteResult("");
		assertNull(result.getCaseResults());
	}
}