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
package hudson.plugins.robot.model;

import java.util.Comparator;

public class RobotCaseComparator implements Comparator<RobotCaseResult> {

	@Override
	public int compare(RobotCaseResult result1, RobotCaseResult result2) {
		if (!result1.isPassed()) {
			if (result2.isPassed())
				return -1;
			if (result1.isCritical()) {
				if (!result2.isCritical())
					return -1;
			} else if (result2.isCritical())
				return 1;
		} else if (!result2.isPassed())
			return 1;
		return result1.getRelativePackageName(result1).compareTo(result2.getRelativePackageName(result2));
	}
}
