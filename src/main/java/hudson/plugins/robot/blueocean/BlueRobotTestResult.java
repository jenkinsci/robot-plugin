package hudson.plugins.robot.blueocean;

import java.util.stream.Collectors;

import hudson.Extension;
import hudson.model.Run;
import hudson.plugins.robot.RobotBuildAction;
import hudson.plugins.robot.model.RobotCaseResult;
import io.jenkins.blueocean.rest.Reachable;
import io.jenkins.blueocean.rest.factory.BlueTestResultFactory;
import io.jenkins.blueocean.rest.hal.Link;
import io.jenkins.blueocean.rest.model.BlueTestResult;

public class BlueRobotTestResult extends BlueTestResult {

	protected final RobotCaseResult result;

	public BlueRobotTestResult(RobotCaseResult result, Link parent) {
		super(parent);
		this.result = result;
	}

	@Override
	public String getName() {
		return result.getDisplayName();
	}

	@Override
	public Status getStatus() {
		return result.isPassed() ? Status.PASSED : Status.FAILED;
	}

	@Override
	public State getTestState() {
		return State.UNKNOWN;
	}

	@Override
	public float getDuration() {
		return result.getDuration();
	}

	@Override
	public String getErrorStackTrace() {
		return result.getStackTrace();
	}

	@Override
	public String getErrorDetails() {
		return result.getErrorMsg() == null ? "" : result.getErrorMsg();
	}

	@Override
	public String getUniqueId() {
		return result.getId();
	}

	@Override
	public int getAge() {
		return result.getAge();
	}

	@Override
	public String getStdErr() {
		return "";
	}

	@Override
	public String getStdOut() {
		return "";
	}

	@Override
	public boolean hasStdLog() {
		return false;
	}

	@Extension(optional = true)
	public static class FactoryImpl extends BlueTestResultFactory {
		@Override
		public Result getBlueTestResults(Run<?,?> run, final Reachable parent) {
			RobotBuildAction action = run.getAction(RobotBuildAction.class);
			if (action == null) {
				return Result.notFound();
			}
			return Result.of(action.getAllTests().stream().map(t-> new BlueRobotTestResult(t, parent.getLink())).collect(Collectors.toList()));
		}
	}
}
