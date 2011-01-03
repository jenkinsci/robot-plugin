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

import hudson.model.AbstractModelObject;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.kohsuke.stapler.StaplerRequest;

public class RobotSuiteResult extends AbstractModelObject {

	private Map<String, RobotSuiteResult> children;
	private RobotSuiteResult parent;
	private String name;
	private Map<String, RobotCaseResult> caseResults;
	private File baseDirectory;
	private String packageName;
	private int failed;
	private int passed;
	private int criticalPassed;
	private int criticalFailed;

	//Dummy result
	public RobotSuiteResult(String name) {
		this.name = name;
	}

	public RobotSuiteResult(Element suite, File baseDirectory,
			String packageName) throws DocumentException {
		this.baseDirectory = baseDirectory;
		this.name = suite.attributeValue("name");
		this.packageName = packageName;
		if (suite.attributeValue("src") != null) {
			parseExternalFile(suite);
		} else {
			parseChildren(suite);
		}
	}

	private void parseExternalFile(Element suite) throws DocumentException {
		File externalFile = new File(baseDirectory, suite.attributeValue("src"));
		SAXReader reader = new SAXReader();
		Document splittedOutput = reader.read(externalFile);
		Element rootElement = splittedOutput.getRootElement().element("suite");
		parseChildren(rootElement);
	}

	private void parseChildren(Element suite) throws DocumentException {
		for (Element nestedSuite : (List<Element>) suite.elements("suite")) {
			String separator = StringUtils.isBlank(packageName) ? "" : ".";
			String newPackageName = packageName + separator + name;
			RobotSuiteResult suiteResult = new RobotSuiteResult(nestedSuite,
					baseDirectory, newPackageName);
			addChild(suiteResult);
		}

		for (Element testCase : (List<Element>) suite.elements("test")) {
			RobotCaseResult caseResult = new RobotCaseResult(testCase);
			addCaseResult(caseResult);
		}
	}

	/**
	 * Adds a nested suite to this suite. If a suite exists with the same name
	 * it will be overwritten with this one.
	 * 
	 * @param child
	 */
	public void addChild(RobotSuiteResult child) {
		if (children == null)
			children = new HashMap<String, RobotSuiteResult>();
		children.put(safe(child.getName()), child);
	}

	public Collection<RobotSuiteResult> getChildSuites() {
		return children == null ? null : children.values();
	}

	public RobotSuiteResult getParent() {
		return parent;
	}

	public void setParent(RobotSuiteResult parent) {
		this.parent = parent;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public Collection<RobotCaseResult> getCaseResults() {
		return caseResults == null ? null : caseResults.values();
	}

	public String getPackageName() {
		return packageName;
	}

	public int getFailed() {
		return failed;
	}

	public int getPassed() {
		return passed;
	}

	public int getCriticalPassed() {
		return criticalPassed;
	}

	public int getCriticalFailed() {
		return criticalFailed;
	}

	/**
	 * Adds a test case result to this suite. If a case exists with the same
	 * name it will be overwritten with this one.
	 * 
	 * @param caseResult
	 */
	public void addCaseResult(RobotCaseResult caseResult) {
		if (caseResults == null)
			caseResults = new HashMap<String, RobotCaseResult>();
		caseResults.put(safe(caseResult.getName()), caseResult);
	}

	public String getDisplayName() {
		// TODO; check
		return name;
	}

	public String getSearchUrl() {
		// TODO; check
		return getDisplayName();
	}

	public RobotSuiteResult getSuite(String name) {
		if (children == null)
			return null;
		return children.get(name);
	}

	public RobotCaseResult getCase(String name) {
		if (caseResults == null)
			return null;
		return caseResults.get(name);
	}

	public Object getDynamic(String token, StaplerRequest req,
			StaplerRequest rsp) {
		if ((token) == null)
			return this;
		if (getCase(token) != null)
			return getCase(token);
		return getSuite(token);
	}

	public Map<String, RobotSuiteResult> getAllChildSuites() {
		Map<String, RobotSuiteResult> childSuites = new HashMap<String, RobotSuiteResult>();
		if (children == null)
			return childSuites;
		for (RobotSuiteResult suite : children.values()) {
			childSuites.putAll(suite.getAllChildSuites());
			String suitePath = getSafeName() + "." + suite.getSafeName();
			childSuites.put(suitePath, suite);
		}

		return childSuites;
	}

	private String safe(String unsafeName) {
		return unsafeName.replace("/", "_").replace("\\", "_")
		.replace(":", "_").replace(".", "_").replace(" ", "_");
	}

	public String getSafeName() {
		return safe(name);
	}

	public void tally() {
		failed = 0;
		passed = 0;
		criticalPassed = 0;
		criticalFailed = 0;

		if(caseResults != null) {
			for(RobotCaseResult caseResult : caseResults.values()) {
				if(caseResult.isPassed()) {
					if(caseResult.isCritical()) criticalPassed++;
					passed++;
				} else {
					if(caseResult.isCritical()) criticalFailed++;
					failed++;
				}
			}
		}
		
		if (children != null) {
			for (RobotSuiteResult suite : children.values()) {
				suite.tally();
				failed += suite.getFailed();
				passed += suite.getPassed();
				criticalFailed += suite.getCriticalFailed();
				criticalPassed += suite.getCriticalPassed();
			}
		}
	}
}
