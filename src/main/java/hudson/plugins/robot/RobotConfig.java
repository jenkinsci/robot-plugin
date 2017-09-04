package hudson.plugins.robot;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public class RobotConfig extends GlobalConfiguration {

	private boolean robotResultsColumnEnabled = true;
	private int buildsToShowInResultsColumn = 15;
	private String xAxisLabelFormat = "#$build";

	public RobotConfig() {
		load();
	}

	@Override
	public boolean configure(StaplerRequest req, JSONObject o) throws FormException {
		// Get Robot Framework section
		o = o.getJSONObject("robotFramework");

		robotResultsColumnEnabled = o.getBoolean("robotResultsColumnEnabled");
		buildsToShowInResultsColumn = o.getInt("buildsToShowInResultsColumn");
		xAxisLabelFormat = o.getString("xAxisLabelFormat");

		save();
		return super.configure(req, o);
	}

	public int getBuildsToShowInResultsColumn() {
		return buildsToShowInResultsColumn;
	}
	public boolean isRobotResultsColumnEnabled() {
		return robotResultsColumnEnabled;
	}
	public String getXAxisLabelFormat() { return xAxisLabelFormat; }

	public void setBuildsToShowInResultsColumn(int buildsToShowInResultsColumn) {
		this.buildsToShowInResultsColumn = buildsToShowInResultsColumn;
	}

	public void setRobotResultsColumnEnabled(boolean robotResultsColumnEnabled) {
		this.robotResultsColumnEnabled = robotResultsColumnEnabled;
	}

	public void setXAxisLabelFormat(String xAxisLabelFormat) {
		this.xAxisLabelFormat = xAxisLabelFormat;
	}

}
