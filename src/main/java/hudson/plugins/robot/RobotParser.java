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
package hudson.plugins.robot;

import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.plugins.robot.model.RobotResult;
import hudson.remoting.VirtualChannel;
import hudson.tasks.junit.Messages;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.dom4j.DocumentException;

public class RobotParser {

	public RobotResult parse(String testResultLocations, AbstractBuild build,
			Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {
		RobotResult result = build.getWorkspace().act(
				new RobotParserCallable(testResultLocations));
		return result;
	}

	private static final class RobotParserCallable implements
			FilePath.FileCallable<RobotResult> {

		private final String testResultLocations;

		private RobotParserCallable(String testResultLocations) {
			this.testResultLocations = testResultLocations;
		}

		public RobotResult invoke(File ws, VirtualChannel channel)
				throws IOException {

			FileSet setInWorkspace = Util
					.createFileSet(ws, testResultLocations);
			DirectoryScanner resultScanner = setInWorkspace
					.getDirectoryScanner();

			String[] files = resultScanner.getIncludedFiles();
			if (files.length == 0) {
				throw new AbortException(
						Messages.JUnitResultArchiver_NoTestReportFound()); // TODO;
																			// message...
			}

			try{
			RobotResult result = new RobotResult(resultScanner);
			return result;
			} catch (DocumentException e){
				throw new IOException(e.getMessage());
			}

		}
	}

}
