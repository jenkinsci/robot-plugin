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
import hudson.plugins.robot.model.base.TestObject;
import hudson.plugins.robot.model.base.TestResult;

import java.util.Collection;
import java.util.Comparator;
import java.util.logging.Logger;

import org.dom4j.Element;

public class RobotCaseResult extends TestResult implements Comparable<RobotCaseResult> {
    private static final Logger LOGGER = Logger.getLogger(RobotCaseResult.class.getName());

	private static final long serialVersionUID = 1L;

	private String name;
	private String documentation;
	private String failMessage;
	private String startTime;
	private String endTime;
	private boolean critical = false;
	private boolean passed;
	private Collection<String> tags;

	public RobotCaseResult(RobotTestSuite parent, Element testCaseRoot) {
		String name = testCaseRoot.attributeValue("name");
		this.name = TestObject.safe(name);
		
		this.critical = testCaseRoot.attributeValue("critical").equals("yes");
		
		Element status = testCaseRoot.element("status");
		this.passed = status.attributeValue("status").equals("PASS");
		this.startTime = status.attributeValue("starttime");
		this.endTime = status.attributeValue("endtime");
		this.failMessage = status.getTextTrim();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDocumentation() {
		return documentation;
	}

	public void setDocumentation(String documentation) {
		this.documentation = documentation;
	}

	public String getStatusMessage() {
		return failMessage;
	}

	public void setStatusMessage(String statusMessage) {
		this.failMessage = statusMessage;
	}

	public boolean isCritical() {
		return critical;
	}

	public void setCritical(boolean critical) {
		this.critical = critical;
	}

	public boolean getPassed() {
		return passed;
	}

	public void setPassed(boolean passed) {
		this.passed = passed;
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public String getEndTime() {
		return endTime;
	}

	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}

	public Collection<String> getTags() {
		return tags;
	}

	public void addTag(String tag) {
		tags.add(tag);
	}

	public String getDisplayName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TestResult findCorrespondingResult(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AbstractBuild<?, ?> getOwner() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TestObject getParent() {
		// TODO Auto-generated method stub
		return null;
	}

	public int compareTo(RobotCaseResult arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setParentTestSuite(RobotTestSuite suite) {
		// TODO Auto-generated method stub
		
	}
	
	/**
     * Gets the number of consecutive builds (including this)
     * that this test case has been failing.
     */
    public int getAge() {
        if(isPassed())
            return 0;
        else if (getOwner() != null) {
            return getOwner().getNumber()-getFailedSince()+1;
        } else {
            LOGGER.fine("Trying to get age of a RobotCaseResult without an owner");
            return 0; 
    }
    }
	
	/**
     * For sorting errors by age.
     */
    static final Comparator<RobotCaseResult> BY_AGE = new Comparator<RobotCaseResult>() {
        public int compare(RobotCaseResult lhs, RobotCaseResult rhs) {
            return lhs.getAge()-rhs.getAge();
        }
    };

}
