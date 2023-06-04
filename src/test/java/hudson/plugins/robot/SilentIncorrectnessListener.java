package hudson.plugins.robot;

import org.htmlunit.IncorrectnessListener;

/*
 * get rid of verbose warnings in system tests
 */
public class SilentIncorrectnessListener implements IncorrectnessListener {
	
    public void notify(String message, Object origin) {
        // do nothing.
    }
    
}
