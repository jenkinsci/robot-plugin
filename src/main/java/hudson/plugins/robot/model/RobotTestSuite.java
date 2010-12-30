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
import hudson.plugins.robot.model.base.MetaTabulatedResult;
import hudson.plugins.robot.model.base.TestObject;
import hudson.plugins.robot.model.base.TestResult;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class RobotTestSuite extends MetaTabulatedResult {

	private static final long serialVersionUID = 1L;

	private String name;
	private String source;
	private final Map<String, RobotCaseResult> testCases = new TreeMap<String, RobotCaseResult>();

	// Robot test suites can be nested indefinitely
	private List<RobotTestSuite> children = new ArrayList<RobotTestSuite>();
	private TestResult parent;

	public RobotTestSuite(String name, String source, RobotResult parent) {
		this.name = name;
		this.source = source;
		this.parent = parent;
	}

	private RobotTestSuite(Element suiteRoot) {
		String name = suiteRoot.attributeValue("name");
		this.name = TestObject.safe(name);

		List<Element> nestedSuites = (List<Element>)suiteRoot.elements("suite");
		if (nestedSuites != null && !nestedSuites.isEmpty()) {
			for (Element nestedSuite : nestedSuites) {
				addSuite(new RobotTestSuite(nestedSuite));
			}
		}
		List<Element> tests = (List<Element>)suiteRoot.elements("test");
		if (tests != null && !tests.isEmpty()) {
			for (Element testCase : tests) {
				addCase(new RobotCaseResult(this, testCase));
			}
		}
	}

	public void setChildren(List<RobotTestSuite> children) {
		this.children = children;
	}

	public void setParent(RobotTestSuite parent) {
		this.parent = parent;
	}

	public void addCase(RobotCaseResult robotTestCase) {
		String safeName = safe(robotTestCase.getName());
		testCases.put(safeName, robotTestCase);
	}

	public void addSuite(RobotTestSuite robotTestSuite) {
		robotTestSuite.setParent(this);
		children.add(robotTestSuite);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public Map<String, RobotCaseResult> getTestCases() {
		return testCases;
	}

	public void addTestCase(String name, RobotCaseResult testCase) {
		testCases.put(name, testCase);
	}

	public String getDisplayName() {
		return name;
	}

	@Override
	public List<RobotTestSuite> getChildren() {
		return children;
	}

	@Override
	public boolean hasChildren() {
		return !testCases.isEmpty() || !children.isEmpty();
	}

	@Override
	public TestResult findCorrespondingResult(String id) {
		TestResult testCase = getTestCase(id);
		if (testCase != null) {
			return testCase;
		}
		return null;
	}

	public RobotCaseResult getTestCase(String id) {
		return testCases.get(id);
	}

	@Override
	public AbstractBuild<?, ?> getOwner() {
		return (parent == null ? null : parent.getOwner());
	}

	@Override
	public TestObject getParent() {
		return parent;
	}

	public boolean freeze(RobotResult robotResult) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Collection<RobotCaseResult> getFailedTests() {
		// TODO Auto-generated method stub
		return Collections.emptyList();
	}

	public static List<RobotTestSuite> parse(File reportFile)
			throws DocumentException, IOException {
		List<RobotTestSuite> parsedSuites = new ArrayList<RobotTestSuite>();

		SAXReader reader = new SAXReader();
		Document result = reader.read(reportFile);

		Element root = result.getRootElement();

		if (root.getName().equals("robot")) {
			for (Element suite : (List<Element>) root.elements("suite")) {
				parsedSuites.add(new RobotTestSuite(suite));
			}
		}

		return parsedSuites;
	}

}
