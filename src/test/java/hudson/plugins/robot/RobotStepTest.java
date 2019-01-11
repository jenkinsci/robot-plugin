package hudson.plugins.robot;

import java.io.File;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Test;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;

public class RobotStepTest {

    @Rule 
    public JenkinsRule r = new JenkinsRule();

    @Test 
    public void basics() throws Exception {
        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
        String outputPath = new File("src/test/resources/hudson/plugins/robot").getAbsolutePath();
        String outputFileName = "low_failure_output.xml";
        p.setDefinition(new CpsFlowDefinition("node {robot outputFileName: '"+outputFileName+"', outputPath: '"+outputPath+"'}", true));
        r.assertLogContains("Done publishing Robot results.", r.assertBuildStatusSuccess(p.scheduleBuild2(0)));
    }
}