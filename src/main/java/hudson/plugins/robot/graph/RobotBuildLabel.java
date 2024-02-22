package hudson.plugins.robot.graph;

import hudson.model.Run;
import hudson.plugins.robot.model.RobotTestObject;

import java.text.SimpleDateFormat;
import java.util.Date;


public class RobotBuildLabel implements Comparable<RobotBuildLabel>
{
    private final Run<?,?> run;
    private final String buildLabel;

	public RobotBuildLabel(RobotTestObject obj, String format) {
        run = obj.getOwner();
        buildLabel = formatBuildLabel(format, run.getTime());
    }

    private String formatBuildLabel(String format, Date startTime) {
        String pattern = format.replace("$build",""+run.number);
        pattern = pattern.replace("$display_name", run.getDisplayName());
        return new SimpleDateFormat(pattern).format(startTime);
    }

	@Override
	public int compareTo(RobotBuildLabel that) {
        return this.run.number-that.run.number;
	}

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof RobotBuildLabel))    return false;
        RobotBuildLabel that = (RobotBuildLabel) o;
        return run==that.run;
    }

    @Override
    public int hashCode() {
        return run.hashCode();
    }

    @Override
    public String toString() { return buildLabel; }
}
