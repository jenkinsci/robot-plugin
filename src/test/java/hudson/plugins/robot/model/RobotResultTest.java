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
package hudson.plugins.robot.model;

import hudson.Util;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

import junit.framework.TestCase;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;


public class RobotResultTest extends TestCase {

	private RobotResult result;
	
	protected void setUp() throws Exception {
		super.setUp();
		File reportFile = new File(new RobotSuiteResultTest().getClass().getResource("output.xml").toURI());

		FileSet fs = Util.createFileSet(reportFile.getParentFile(), "output.xml");
        DirectoryScanner ds = fs.getDirectoryScanner();
        String[] files = ds.getIncludedFiles();
       

        if(files.length == 0) throw new Exception("No example file found!");
        
		result = new RobotResult(ds);	
		result.tally(null);
	}
	
	public void testShouldParseSuites(){
		RobotSuiteResult suite = result.getSuite("Othercases_&_Testcases");
		assertNotNull(suite);
	}
	
	public void testShouldParseNestedSuites(){
		RobotSuiteResult suite = result.getSuite("Othercases_&_Testcases");
		RobotSuiteResult nestedSuite = suite.getSuite("Testcases");
		assertNotNull(nestedSuite);
	}
	
	public void testShouldParseCases(){
		RobotSuiteResult suite = result.getSuite("Othercases_&_Testcases");
		RobotCaseResult caseResult = suite.getCase("Hello");
		assertNotNull(caseResult);
	}
	
	public void testShouldParseCriticalCases(){
		assertEquals(15, result.getCriticalTotal());
	}
	
	public void testShouldParseOverallCases(){
		assertEquals(17, result.getOverallTotal());
	}
	
	public void testShouldParseFailedCases(){
		assertEquals(8, result.getOverallFailed());
	}
	
	public void testShouldParseFailedCriticalCases(){
		assertEquals(7, result.getCriticalFailed());
	}
	
	public void testShouldReturnAllFailedCases(){
		List<RobotCaseResult> failers = result.getAllFailedCases();
		assertEquals(8, failers.size()
				);
	}
	
	public void testShouldReturnPackageName(){
		RobotSuiteResult suite = (RobotSuiteResult)result.findObjectById("Othercases_&_Testcases/Othercases/3rd_level_cases");
		assertEquals("Othercases & Testcases.Othercases.3rd level cases", suite.getRelativePackageName(result));
	}
	
	public void testShouldReturnSuiteById(){
		RobotSuiteResult suite = (RobotSuiteResult)result.findObjectById("Othercases_&_Testcases/Othercases/3rd_level_cases");
		assertEquals("3rd level cases", suite.getName());
	}
	
	public void testShouldReturnIdForSuite(){
		RobotSuiteResult suite = (RobotSuiteResult)result.findObjectById("Othercases_&_Testcases/Othercases/3rd_level_cases");
		assertEquals("Othercases_&_Testcases/Othercases/3rd_level_cases", suite.getRelativeId(result));
	}
	
	public void testShouldReturnCaseById(){
		RobotCaseResult caseResult = (RobotCaseResult)result.findObjectById("Othercases_&_Testcases/Othercases/3rd_level_cases/Hello3rd");
		assertEquals("Hello3rd", caseResult.getName());
	}
	
	
	public void testShouldParseSplittedOutput() throws Exception, URISyntaxException{
		File reportFile = new File(new RobotSuiteResultTest().getClass().getResource("testfile.xml").toURI());
		
		FileSet fs = Util.createFileSet(reportFile.getParentFile(), "testfile.xml");
        DirectoryScanner ds = fs.getDirectoryScanner();
        String[] files = ds.getIncludedFiles();
       
        if(files.length == 0) throw new Exception("No example file found!");
        
		result = new RobotResult(ds);
		
		RobotSuiteResult suite = result.getSuite("nestedSuites");
		RobotSuiteResult splittedSuite = suite.getSuite("subSuite");
		RobotSuiteResult splittedNestedSuite = splittedSuite.getSuite("Testcases");
		assertNotNull(splittedNestedSuite);
	}
	
	
}
