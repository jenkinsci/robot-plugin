package hudson.plugins.robot.view;

import hudson.Extension;
import hudson.model.Descriptor;
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

	public long getPass(Job job){
		RobotResult lastRobotResult = getLastRobotResult(job);
		if(lastRobotResult != null){
			return lastRobotResult.getOverallPassed();
		}
		return 0;
	}

	public long getTotal(Job job){
		RobotResult lastRobotResult = getLastRobotResult(job);
		if(lastRobotResult != null){
			return lastRobotResult.getOverallTotal();
		}
		return 0;
	}

	public double getPassPercent(Job job) {
		RobotResult lastRobotResult = getLastRobotResult(job);
		if (lastRobotResult==null) return 100;
		return lastRobotResult.getPassPercentage();
	}

	public String getRobotPath(Job job) {
		Run build = job.getLastCompletedBuild();
		int lastBuildNr = build==null? 1 : build.number;
		return "/job/"+job.getName()+"/"+lastBuildNr+"/robot/";
	}

	public String getLogUrl(Job job) {
		return getRobotPath(job)+"report/log.html";
	}

	public String getTrendUrl(Job job) {
		return getRobotPath(job)+"durationGraph?maxBuildsToShow="+getBuildsToShowInResultsColumn();
	}

	public String getTrendHdUrl(Job job) {
		return getTrendUrl(job) + "&hd";
	}

	public String getTrendPreviewUrl(Job job) {
		return getTrendUrl(job) + "&preview";
	}

	private RobotResult getLastRobotResult(Job job){
		Run build = job.getLastCompletedBuild();
		if(build != null) {
			RobotBuildAction action = (RobotBuildAction)build.getAction(RobotBuildAction.class);
			if(action != null) {
				return action.getResult();
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
