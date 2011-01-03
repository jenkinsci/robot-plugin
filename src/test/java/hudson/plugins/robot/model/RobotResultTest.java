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

import hudson.Util;

import java.io.File;
import java.net.URISyntaxException;

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
	
	
	//TODO; generate new test files
	public void testShouldParseSplittedOutput() throws Exception, URISyntaxException{
		File reportFile = new File(new RobotSuiteResultTest().getClass().getResource("testfile.xml").toURI());
		
		FileSet fs = Util.createFileSet(reportFile.getParentFile(), "testfile.xml");
        DirectoryScanner ds = fs.getDirectoryScanner();
        String[] files = ds.getIncludedFiles();
       
        if(files.length == 0) throw new Exception("No example file found!");
        
		result = new RobotResult(ds);
		
		RobotSuiteResult suite = result.getSuite("Suite");
		RobotSuiteResult splittedSuite = suite.getSuite("Splitted");
		RobotSuiteResult splittedNestedSuite = splittedSuite.getSuite("Splittednested");
		assertNotNull(splittedNestedSuite);
	}
}
