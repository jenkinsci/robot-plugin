/*
* Copyright 2008-2014 Nokia Siemens Networks Oyj
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

import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.model.BuildListener;
import hudson.Launcher;

public class RobotResultAggregator extends MatrixAggregator {

	public RobotResultAggregator(MatrixBuild build, Launcher launcher, BuildListener listener){
		super(build, launcher, listener);
	}

	@Override
	public boolean endBuild(){
		AggregatedRobotAction action = new AggregatedRobotAction(build);
		for (MatrixRun run : build.getExactRuns()) {
			RobotBuildAction robotAction = run.getAction(RobotBuildAction.class);
			if (robotAction != null){
				action.addResult(robotAction.getResult());
			}
		}
		build.addAction(action);
		return true;
	}
}
