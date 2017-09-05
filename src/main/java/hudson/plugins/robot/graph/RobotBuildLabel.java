package hudson.plugins.robot.graph;

import hudson.model.Run;
import hudson.plugins.robot.RobotConfig;
import hudson.plugins.robot.model.RobotTestObject;

import java.text.SimpleDateFormat;
import java.util.Date;


public class RobotBuildLabel implements Comparable<RobotBuildLabel>
{
    private final int buildNr;
    private final String buildLabel;

	public RobotBuildLabel(RobotTestObject obj, RobotConfig config) {
        Run<?,?> build = obj.getOwner();
        buildNr = build.number;
        buildLabel = formatBuildLabel(config, build.getTime());
    }

    private String formatBuildLabel(RobotConfig config, Date startTime) {
        String pattern = config.getXAxisLabelFormat().replace("$build",""+buildNr);
        return new SimpleDateFormat(pattern).format(startTime);
    }

	@Override
	public int compareTo(RobotBuildLabel that) {
        return this.buildNr-that.buildNr;
	}

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof RobotBuildLabel))    return false;
        RobotBuildLabel that = (RobotBuildLabel) o;
        return buildNr==that.buildNr;
    }

    @Override
    public int hashCode() {
        return buildNr;
    }

    @Override
    public String toString() { return buildLabel; }
}
