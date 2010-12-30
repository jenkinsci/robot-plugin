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
package hudson.plugins.robot;

import hudson.XmlFile;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.plugins.robot.model.RobotResult;
import hudson.plugins.robot.model.RobotCaseResult;
import hudson.plugins.robot.model.RobotTestSuite;
import hudson.plugins.robot.model.base.TestAction;
import hudson.plugins.robot.model.base.TestObject;
import hudson.util.HeapSpaceStringConverter;
import hudson.util.XStream2;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.StaplerProxy;

import com.thoughtworks.xstream.XStream;

public class RobotResultAction extends
		hudson.plugins.robot.model.base.AbstractTestResultAction<RobotResultAction> implements StaplerProxy {

	private static final Logger logger = Logger
			.getLogger(RobotResultAction.class.getName());

	private static final XStream XSTREAM = new XStream2();

	static {
		XSTREAM.alias("result", RobotResult.class);
		XSTREAM.alias("suite", RobotTestSuite.class);
		XSTREAM.alias("case", RobotCaseResult.class);
		XSTREAM.registerConverter(new HeapSpaceStringConverter(), 100);
	}

	private transient WeakReference<RobotResult> result;

	private int failedTests;
	private int failedCriticalTests;
	private Integer totalTests;
	private Integer totalCriticalTests;
	private List<Data> testData = new ArrayList<Data>();

	protected RobotResultAction(AbstractBuild<?, ?> owner, RobotResult result,
			BuildListener listener) {
		super(owner);
		setResult(result, listener);
	}
	
	//TODO; Critical vs. all tests!

	public synchronized void setResult(RobotResult newResult,
			BuildListener listener) {
		newResult.freeze(this);

		totalTests = newResult.getTotalCount();
		failedTests = newResult.getFailCount();
		totalCriticalTests = newResult.getTotalCriticalCount();
		failedCriticalTests = newResult.getFailedCriticalCount();

		try {
			getDatafile().write(result);
		} catch (IOException e) {
			e.printStackTrace(listener
					.fatalError("Couldn't write Robot result"));
		}
		
		this.result = new WeakReference<RobotResult>(newResult);
	}

	private XmlFile getDatafile() {
		return new XmlFile(XSTREAM, new File(owner.getRootDir(),
				"robotresult.xml"));
	}

	public synchronized RobotResult getResult() {
		RobotResult loadedResult;
		if (result == null) {
			loadedResult = load();
			result = new WeakReference<RobotResult>(loadedResult);
		} else {
			loadedResult = result.get();
		}
		
		if(loadedResult == null) {
			loadedResult = load();
			result = new WeakReference<RobotResult>(loadedResult);
		}
		if (totalTests == null) {
			totalTests = loadedResult.getTotalCount();
			failedTests = loadedResult.getFailCount();
			totalCriticalTests = loadedResult.getTotalCriticalCount();
			failedCriticalTests = loadedResult.getFailedCriticalCount();
		}
		return loadedResult;
	}

	private RobotResult load() {
		RobotResult loadedResult;
		try {
			loadedResult = (RobotResult) getDatafile().read();
		} catch (IOException e) {
			logger.log(Level.WARNING, "Failed to load " + getDatafile(), e);
			loadedResult = new RobotResult(); // return a dummy
		}
		loadedResult.freeze(this);
		return loadedResult;
	}

	@Override
	public int getFailCount() {
		if (totalTests == null)
			getResult();
		return failedTests;
	}

	@Override
	public int getTotalCount() {
		if (totalTests == null)
			getResult();
		return totalTests;
	}

	public Object getTarget() {
		return getResult();
	}

	public Object readResolve() {
		super.readResolve(); // let it do the post-deserialization work
		if (testData == null) {
			testData = new ArrayList<Data>();
		}

		return this;
	}

	public List<TestAction> getActions(TestObject object) {
		List<TestAction> result = new ArrayList<TestAction>();
		if (testData != null) {
			for (Data data : testData) {
				result.addAll(data.getTestAction(object));
			}
		}
		return Collections.unmodifiableList(result);

	}

	public void setData(List<Data> testData) {
		this.testData = testData;
	    }

	    /**
	     * Resolves {@link TestAction}s for the given {@link TestObject}.
	     *
	     * <p>
	     * This object itself is persisted as a part of {@link AbstractBuild}, so it needs to be XStream-serializable.
	     *
	     * @see TestDataPublisher
	     */
	    public static abstract class Data {
	    	/**
	    	 * Returns all TestActions for the testObject.
	         * 
	         * @return
	         *      Can be empty but never null. The caller must assume that the returned list is read-only.
	    	 */
	    	public abstract List<? extends TestAction> getTestAction(TestObject testObject);
	    }

}
