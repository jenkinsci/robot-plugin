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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;


public class RobotProjectActionTest extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testShouldNotDisplayGraph() {
		FreeStyleProject p = mock(FreeStyleProject.class);
		when(p.getBuilds()).thenReturn(new ArrayList<FreeStyleBuild>());
		
		RobotProjectAction action = new RobotProjectAction(p);
		assertFalse(action.isDisplayGraph());
	}
	
	public void testShouldDisplayGraph() throws Exception {
		FreeStyleProject p = mock(FreeStyleProject.class);
		List<FreeStyleBuild> mockList = new ArrayList<FreeStyleBuild>();
		mockList.add(new FreeStyleBuild(p));
		when(p.getBuilds()).thenReturn(mockList);
		
		RobotProjectAction action = new RobotProjectAction(p);
		assertTrue(action.isDisplayGraph());
	}

	public void testShouldGetLastBuildAction(){
		FreeStyleProject p = mock(FreeStyleProject.class);
		
		FreeStyleBuild lastBuild = mock(FreeStyleBuild.class);
		FreeStyleBuild buildWithAction = mock(FreeStyleBuild.class);
		RobotBuildAction buildAction = new RobotBuildAction(null,null,"","");
		
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
