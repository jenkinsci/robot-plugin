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

import hudson.FilePath;
import hudson.plugins.robot.model.RobotResult;
import hudson.plugins.robot.model.RobotResultStatistics;
import hudson.remoting.VirtualChannel;
import hudson.util.Digester2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

/**
 * A parser for reading Robot Framework XML output into RobotResult object.
 * Reads pass/fail statistics for critical and all tests from XML part:
 * <robot>
 * ...
 *   <total>
 *    <stat pass="" fail="">Critical tests</stat>
 *    <stat pass="" fail="">All tests</stat>
 *   </total>
 * ...
 * </robot>
 * 
 */
public class RobotParser implements FilePath.FileCallable<RobotResult> {

	private static final long serialVersionUID = 1L;
	
	/*
	 * (non-Javadoc)
	 * @see hudson.FilePath.FileCallable#invoke(java.io.File, hudson.remoting.VirtualChannel)
	 */
	public RobotResult invoke(File outputFile, VirtualChannel channel) throws IOException {
		RobotResult result = null;

		Digester digester = new Digester2();
		digester.setValidating(false);
		digester.setClassLoader(RobotParser.class.getClassLoader());

		digester.addObjectCreate("robot", RobotResult.class);
		digester.addSetProperties("robot", "generated", "timeStamp");
		
		digester.addObjectCreate("robot/statistics/total", ArrayList.class);
		
		digester.addObjectCreate("robot/statistics/total/stat", RobotResultStatistics.class);	
		digester.addSetProperties("robot/statistics/total/stat", "pass", "pass");
		digester.addSetNext("robot/statistics/total/stat", "add");
		
		digester.addSetNext("robot/statistics/total", "setStatsByCategory");

		try {
			result = (RobotResult) digester.parse(outputFile);
		} catch (SAXException e) {
			throw new IOException(e.getMessage());
		}
		return result;
	}
}