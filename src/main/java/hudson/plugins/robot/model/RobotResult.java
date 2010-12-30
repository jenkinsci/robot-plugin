/*
* Copyright 2008-2010 Nokia Siemens Networks Oyj
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package hudson.plugins.robot.model;

import hudson.model.AbstractBuild;
import hudson.plugins.robot.RobotResultAction;
import hudson.plugins.robot.model.base.AbstractTestResultAction;
import hudson.plugins.robot.model.base.MetaTabulatedResult;
import hudson.plugins.robot.model.base.TestObject;
import hudson.plugins.robot.model.base.TestResult;
import hudson.util.IOException2;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.apache.tools.ant.DirectoryScanner;
import org.dom4j.DocumentException;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Class representing Robot Framework test results.
 *
 */
public class RobotResult extends MetaTabulatedResult implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(RobotResult.class.getName());

	private static final long serialVersionUID = 1L;
	
	private String timeStamp;
	private RobotResultStatistics criticalStatistics;
	private RobotResultStatistics overallStatistics;
	
	//backwards compatibility with old builds
	private transient List<RobotResultStatistics> statsBySuite;
	private transient List<RobotResultStatistics> overallStats;
	
    private transient TestObject parent;

	private transient AbstractTestResultAction parentAction;

	private transient int totalTests;
	private transient int totalCriticalTests;
	private float duration;

	private transient List<RobotCaseResult> failedTests;
	private transient List<TestResult> failedCriticalTests;

	private final Map<String, RobotTestSuite> suites = new TreeMap<String, RobotTestSuite>();
	
	/*
	 * The data structure of passed and failed tests is awkward and fragile but
	 * needs some consideration before migrating so that old builds won't break.
	 */
	

	/**
	 * Empty constructor for dummy result
	 */
	public RobotResult() {
	}
	
	public RobotResult(DirectoryScanner resultFiles) throws IOException {
		parse(resultFiles);
	}

	public void parse(DirectoryScanner files) throws IOException{
		String[] includedFiles = files.getIncludedFiles();
        File baseDir = files.getBasedir();

        for (String value : includedFiles) {
            File reportFile = new File(baseDir, value);
                if(reportFile.length()==0) {
                	//TODO; react to empty files
                } else {
                    parse(reportFile);
                }
        }
	}

	private void add(RobotTestSuite suite) {
        suites.put(suite.getSafeName(), suite);
        duration += suite.getDuration();
    }

    /**
     * Parses an additional report file.
     */
    public void parse(File reportFile) throws IOException {
        try {
            for (RobotTestSuite suite : RobotTestSuite.parse(reportFile))
                add(suite);
        } catch (RuntimeException e) {
            throw new IOException2("Failed to read " + reportFile, e);
        } catch (DocumentException e) {
            if (!reportFile.getPath().endsWith(".xml")) {
                throw new IOException2("Failed to read " + reportFile + "\n" +
                    "Is your configuration matching too many files?", e);
            } else {
                throw new IOException2("Failed to read " + reportFile, e);
            }
        }
    }

	/**
	 * Get number of passed critical tests.
	 * @return
	 */
	public long getCriticalPassed(){
		if(overallStats == null) return criticalStatistics.getPass();
		return overallStats.get(0).getPass();
	}
	
	/**
	 * Get number of failed critical tests.
	 * @return
	 */
	public long getCriticalFail(){
		if(overallStats == null) return criticalStatistics.getFail();
		return overallStats.get(0).getFail();
	}
	
	/**
	 * Get total number of critical tests.
	 * @return
	 */
	public long getCriticalTotal(){
		if(overallStats == null) return criticalStatistics.getTotal();
		return overallStats.get(0).getTotal();
	}
	
	/**
	 * Get number of all passed tests.
	 * @return
	 */
	public long getOverallPassed(){
		if(overallStats == null) return overallStatistics.getPass();
		return overallStats.get(1).getPass();
	}
	
	/**
	 * Get number of all failed tests.
	 * @return
	 */
	public long getOverallFailed(){
		if(overallStats == null) return overallStatistics.getFail();
		return overallStats.get(1).getFail();
	}
	
	/**
	 * Get number of all tests.
	 * @return
	 */
	public long getOverallTotal(){
		if(overallStats == null) return overallStatistics.getTotal();
		return overallStats.get(1).getTotal();
	}
	
	public void setCriticalStatistics(RobotResultStatistics stats) {
		this.criticalStatistics = stats;
	}

	public void setOverallStatistics(RobotResultStatistics stats) {
		this.overallStatistics = stats;
	}

	/**
	 * Get the timestamp of the original test run
	 * @return timestamp in Robot format as string
	 */
	public String getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}

	/**
	 * Returns pass percentage of passed tests per total tests.
	 * @param onlyCritical true if only critical tests are to be calculated
	 * @return Percentage value rounded to 1 decimal
	 */
	public double getPassPercentage(boolean onlyCritical) {
		int passed, total;
		if(onlyCritical) {
			passed = totalCriticalTests - failedCriticalTests.size();
			total = totalCriticalTests;
		} else {
			//TODO; dummy implementation
			passed = 100; //totalTests - failedTests.size();
			total = 100; //totalTests;
		}
		double percentage = (double) passed / total * 100;
		return roundToDecimals(percentage, 1);
	}

	private static double roundToDecimals(double value, int decimals){
		BigDecimal bd = new BigDecimal(Double.toString(value));
		bd = bd.setScale(decimals, BigDecimal.ROUND_HALF_UP);
		return bd.doubleValue();
	}


	@Override
	public Collection<? extends TestResult> getFailedTests() {
		return failedTests;
	}

	@Override
	public Collection<RobotTestSuite> getChildren() {
		return suites.values();
	}

	@Override
	public boolean hasChildren() {
		return !suites.isEmpty();
	}
	
	 public Map<String, RobotTestSuite> getSuites() {
	        return suites;
	  }
	 
	 @Override
	    public String getName() {
	        return "robot";
	    }

	@Override
	public TestResult findCorrespondingResult(String id) {
		if (getId().equals(id) || (id == null)) {
            return this;
        }
        
        String firstElement = null;
        String subId = null;
        int sepIndex = id.indexOf('/');
        if (sepIndex < 0) {
            firstElement = id;
            subId = null;
        } else {
            firstElement = id.substring(0, sepIndex);
            subId = id.substring(sepIndex + 1);
            if (subId.length() == 0) {
                subId = null;
            }
        }

        String packageName = null;
        if (firstElement.equals(getId())) {
            sepIndex = subId.indexOf('/');
            if (sepIndex < 0) {
                packageName = subId;
                subId = null; 
            } else {
                packageName = subId.substring(0, sepIndex);
                subId = subId.substring(sepIndex + 1);
            }
        } else {
            packageName = firstElement;
            subId = null; 
        }
        RobotTestSuite child = getSuite(packageName);
        if (child != null) {
            if (subId != null) {
                return child.findCorrespondingResult(subId);
            } else {
                return child;
            }
        } else {
            return null;
    }
	}
	

	@Override
	public AbstractBuild<?, ?> getOwner() {
	        return (parentAction == null ? null : parentAction.owner);
	}

	@Override
	public TestObject getParent() {
		return this.parent;
	}
	
	
	public void setParent(TestObject parent){
		this.parent = parent;
	}
	
	 @Override
     public void setParentAction(AbstractTestResultAction action) {
        this.parentAction = action;
        tally();
     }
	
	public RobotTestSuite getSuite(String name){
		return suites.get(name);
	}
	
	@Override
    public Object getDynamic(String token, StaplerRequest req, StaplerResponse rsp) {
        if (token.equals(getId())) {
            return this;
        }
        
        RobotTestSuite result = suites.get(token);
        if (result != null) {
        	return result;
        } else {
        	return super.getDynamic(token, req, rsp);
        }
    }
	
	   /**
     * Recount my children.
     */
    @Override
    public void tally() {
        failedTests = new ArrayList<RobotCaseResult>();
        failedCriticalTests = new ArrayList<TestResult>();
        
        totalTests = 0;
        totalCriticalTests = 0;

        for (RobotTestSuite suite : suites.values()) {
            suite.setParent(this); // kluge to prevent double-counting the results
            Collection<RobotCaseResult> cases = suite.getTestCases().values();

            for (RobotCaseResult curCase: cases) {
                curCase.setParentAction(this.parentAction);
                curCase.setParentTestSuite(suite);
                curCase.tally();
            }
            suite.tally();
            failedTests.addAll(suite.getFailedTests());
            totalTests += suite.getTotalCount();
        }
    }

	public void freeze(RobotResultAction parent) {
		this.parentAction = parent;

        for (RobotTestSuite suite : suites.values()) {
            if(!suite.freeze(this))      // this is disturbing: has-a-parent is conflated with has-been-counted
                continue;

            totalTests += suite.getTestCases().values().size();
            for(RobotCaseResult testCase : suite.getTestCases().values()) {
               if(!testCase.isPassed())
                    failedTests.add(testCase);
            }
        }

       Collections.sort(failedTests, RobotCaseResult.BY_AGE);	
	}

	public int getTotalCriticalCount() {
		return totalCriticalTests;
	}

	public int getFailedCriticalCount() {
		if(failedCriticalTests != null)
			return failedCriticalTests.size();
		return 0;
	}

	public String getDisplayName() {
		return "Robot result";
	}
}