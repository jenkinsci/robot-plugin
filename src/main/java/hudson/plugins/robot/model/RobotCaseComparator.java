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
