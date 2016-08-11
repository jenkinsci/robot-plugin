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
package hudson.plugins.robot;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.robot.model.RobotResult;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import org.mockito.Mockito;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


public class RobotProjectActionTest extends TestCase {

	File tmpDir = new File(System.getProperty("java.io.tmpdir"));
	File robotFile = new File(tmpDir, "robot_results.xml");

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws SecurityException {
		if(robotFile.exists())
			robotFile.delete();
	}

	public void testShouldNotDisplayGraph() throws IOException {
		FreeStyleProject p = mock(FreeStyleProject.class);
		FreeStyleBuild build = new FreeStyleBuild(p);
		FreeStyleBuild build2 = spy(build);
		when(p.getLastBuild()).thenReturn(build2);
		doReturn(null).when(build2).getPreviousBuild();

		RobotProjectAction action = new RobotProjectAction(p);
		assertFalse(action.isDisplayGraph());
	}

	public void testShouldDisplayGraph() throws IOException {
		FreeStyleProject p = mock(FreeStyleProject.class);
		FreeStyleBuild build = mock(FreeStyleBuild.class);
		when(build.getProject()).thenReturn(p);
		when(build.getRootDir()).thenReturn(tmpDir);
		RobotResult result = mock(RobotResult.class);
		RobotBuildAction buildAction = new RobotBuildAction(build, result, "", null, null, null, false);
		when(build.getAction(RobotBuildAction.class)).thenReturn(buildAction);
		when(p.getLastBuild()).thenReturn(build);

		RobotProjectAction action = new RobotProjectAction(p);
		assertTrue(action.isDisplayGraph());
	}

	public void testShouldGetLastBuildAction() throws IOException{
		FreeStyleProject p = mock(FreeStyleProject.class);

		FreeStyleBuild lastBuild = mock(FreeStyleBuild.class);
		FreeStyleBuild buildWithAction = mock(FreeStyleBuild.class);
		when(buildWithAction.getProject()).thenReturn(p);
		when(buildWithAction.getRootDir()).thenReturn(tmpDir);
		RobotResult result = mock(RobotResult.class);
		RobotBuildAction buildAction = new RobotBuildAction(buildWithAction, result, "", null, null, null, false);
		when(buildWithAction.getAction(RobotBuildAction.class)).thenReturn(buildAction);

		when(p.getLastBuild()).thenReturn(lastBuild);
		when(lastBuild.getPreviousBuild()).thenReturn(buildWithAction);

		RobotProjectAction projectAction = new RobotProjectAction(p);
		assertEquals(buildAction, projectAction.getLastBuildAction());
	}

	public void testShouldReturnNullWhenNoActions() {
		FreeStyleProject p = mock(FreeStyleProject.class);

		FreeStyleBuild lastBuild = mock(FreeStyleBuild.class);
		FreeStyleBuild firstBuild = mock(FreeStyleBuild.class);

		when(p.getLastBuild()).thenReturn(lastBuild);
		when(lastBuild.getPreviousBuild()).thenReturn(firstBuild);

		RobotProjectAction projectAction = new RobotProjectAction(p);
		assertNull(projectAction.getLastBuildAction());
	}
}
