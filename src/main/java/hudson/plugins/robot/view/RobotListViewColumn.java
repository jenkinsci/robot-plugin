package hudson.plugins.robot.view;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.robot.RobotBuildAction;
import hudson.plugins.robot.RobotConfig;
import hudson.plugins.robot.model.RobotResult;
import hudson.views.ListViewColumn;

import org.kohsuke.stapler.DataBoundConstructor;

import javax.inject.Inject;

public class RobotListViewColumn extends ListViewColumn {

	@DataBoundConstructor
	public RobotListViewColumn(){
	}

	@Override
	public String getColumnCaption() {
		return getDescriptor().getDisplayName();
	}

	public long getPass(Item job){
		RobotResult lastRobotResult = getLastRobotResult(job);
		if(lastRobotResult != null){
			return lastRobotResult.getOverallPassed();
		}
		return 0;
	}

	public long getTotal(Item job){
		RobotResult lastRobotResult = getLastRobotResult(job);
		if(lastRobotResult != null){
			return lastRobotResult.getOverallTotal();
		}
		return 0;
	}

	public long getSkipped(Item job) {
		RobotResult lastRobotResult = getLastRobotResult(job);
		if (lastRobotResult != null) {
			return lastRobotResult.getOverallSkipped();
		}
		return 0;
	}

	public double getPassPercent(Item job) {
		RobotResult lastRobotResult = getLastRobotResult(job);
		if (lastRobotResult==null) return 100;
		return lastRobotResult.getPassPercentage();
	}

	public double getSkipPercent(Item job) {
		RobotResult lastRobotResult = getLastRobotResult(job);
		if (lastRobotResult == null) return 0;
		return lastRobotResult.getSkipPercentage();
	}

	public String getRobotPath(Item job) {
		if (job instanceof Job) {
			Run<?,?> build = ((Job<?,?>)job).getLastCompletedBuild();
			int lastBuildNr = build==null? 1 : build.number;
			return job.getShortUrl() + lastBuildNr+ "/robot/";
		}
		return null;
	}

	public String getLogUrl(Item job) {
		return getRobotPath(job)+"report/log.html";
	}

	public String getTrendUrl(Item job) {
		return getRobotPath(job)+"durationGraph?maxBuildsToShow="+getBuildsToShowInResultsColumn();
	}

	public String getTrendHdUrl(Item job) {
		return getTrendUrl(job) + "&hd";
	}

	public String getTrendPreviewUrl(Item job) {
		return getTrendUrl(job) + "&preview";
	}

	private RobotResult getLastRobotResult(Item job){
		if (job instanceof Job) {
			Run<?,?> build = ((Job<?,?>)job).getLastCompletedBuild();
			if(build != null) {
				RobotBuildAction action = (RobotBuildAction)build.getAction(RobotBuildAction.class);
				if(action != null) {
					return action.getResult();
				}
			}
		}
		return null;
	}

	public int getBuildsToShowInResultsColumn() {
		return ((DescriptorImpl) this.getDescriptor()).getBuildsToShowInResultsColumn();
	}

	public boolean isRobotResultsColumnEnabled() {
		return ((DescriptorImpl) this.getDescriptor()).isRobotResultsColumnEnabled();
	}

	@Extension
	public static final class DescriptorImpl extends Descriptor<ListViewColumn>{

		@Inject
		private RobotConfig globalConfig;

		@Override
		public String getDisplayName() {
			return "Robot pass/fail";
		}

		public boolean isRobotResultsColumnEnabled() {
			return globalConfig.isRobotResultsColumnEnabled();
		}
		public int getBuildsToShowInResultsColumn() {
			return globalConfig.getBuildsToShowInResultsColumn();
		}
	}
}
