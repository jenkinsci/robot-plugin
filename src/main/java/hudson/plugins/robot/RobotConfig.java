package hudson.plugins.robot;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public class RobotConfig extends GlobalConfiguration {

	private boolean robotResultsColumnEnabled = true;

	public RobotConfig() {
		load();
	}

	@Override
	public boolean configure(StaplerRequest req, JSONObject o) throws FormException {
		// Get Robot Framework section
		o = o.getJSONObject("robotFramework");

		robotResultsColumnEnabled = o.getBoolean("robotResultsColumnEnabled");

		save();
		return super.configure(req, o);
	}

	public boolean isRobotResultsColumnEnabled() {
		return robotResultsColumnEnabled;
	}

	public void setRobotResultsColumnEnabled(boolean robotResultsColumnEnabled) {
		this.robotResultsColumnEnabled = robotResultsColumnEnabled;
	}
}
