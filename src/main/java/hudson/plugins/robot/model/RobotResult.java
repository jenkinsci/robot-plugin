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

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.AbstractModelObject;
import hudson.model.DirectoryBrowserSupport;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.tools.ant.DirectoryScanner;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;


/**
 * Class representing Robot Framework test results.
 *
 */
public class RobotResult extends AbstractModelObject {

	private static final long serialVersionUID = 1L;
	
	private String timeStamp;
	private AbstractBuild<?,?> owner;
	
	int passed, failed, criticalPassed, criticalFailed;

	//backwards compatibility with old builds
	private List<RobotResultStatistics> overallStats;
	private transient List<RobotResultStatistics> statsBySuite;

	private Map<String, RobotSuiteResult> suites;

	private transient ArrayList<RobotCaseResult> failedTests;
	
	
	public RobotResult(DirectoryScanner scanner){
		parse(scanner);
	}
	
	public void parse(DirectoryScanner scanner) {
		suites = new HashMap<String, RobotSuiteResult>();
		String[] files = scanner.getIncludedFiles();
		File baseDirectory = scanner.getBasedir();
		
		for(String file : files){
			SAXReader reader = new SAXReader();
			try {
				File reportFile = new File(baseDirectory, file);
				Document resultFile = reader.read(reportFile);
				Element root = resultFile.getRootElement();
				
				timeStamp = root.attributeValue("generated");
				for(Element suite : (List<Element>) root.elements("suite")){
					RobotSuiteResult suiteResult = new RobotSuiteResult(suite, baseDirectory, "");
					suites.put(suiteResult.getSafeName(), suiteResult);
				}
			} catch (DocumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		tally();
	}
	
	public AbstractBuild<?, ?> getOwner() {
		return owner;
	}

	public void setOwner(AbstractBuild<?, ?> owner) {
		this.owner = owner;
	}
	
	/*
	 * The data structure of passed and failed tests is awkward and fragile but
	 * needs some consideration before migrating so that old builds won't break.
	 */

	/**
	 * Get number of passed critical tests.
	 * @return
	 */
	public long getCriticalPassed(){
		if(overallStats == null) return criticalPassed;
		if(overallStats.isEmpty()) return 0;
		return overallStats.get(0).getPass();
	}
	
	/**
	 * Get number of failed critical tests.
	 * @return
	 */
	public long getCriticalFail(){
		if(overallStats == null) return criticalFailed;
		if( overallStats.isEmpty()) return 0;
		return overallStats.get(0).getFail();
	}
	
	/**
	 * Get total number of critical tests.
	 * @return
	 */
	public long getCriticalTotal(){
		if(overallStats == null) return criticalFailed + criticalPassed;
		if(overallStats.isEmpty()) return 0;
		return overallStats.get(0).getTotal();
	}
	
	/**
	 * Get number of all passed tests.
	 * @return
	 */
	public long getOverallPassed(){
		if(overallStats == null) return passed;
		if(overallStats.isEmpty()) return 0;
		return overallStats.get(1).getPass();
	}
	
	/**
	 * Get number of all failed tests.
	 * @return
	 */
	public long getOverallFailed(){
		if(overallStats == null) return failed;
		if(overallStats.isEmpty()) return 0;
		return overallStats.get(1).getFail();
	}
	
	/**
	 * Get number of all tests.
	 * @return
	 */
	public long getOverallTotal(){
		if(overallStats == null) return failed + passed;
		if(overallStats.isEmpty()) return 0;
		return overallStats.get(1).getTotal();
	}
	
	/**
	 * Get pass/fail stats by category.
	 * @return List containing 'critical tests' and 'all tests'
	 */
	public List<RobotResultStatistics> getStatsByCategory() {
		return overallStats;
	}

	public void setStatsByCategory(List<RobotResultStatistics> statsByCategory) {
		this.overallStats = statsByCategory;
	}

	/**
	 * Get the timestamp of the original test run
	 * @return
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
		long passed, total;
		if(onlyCritical) {
			passed = getCriticalPassed();
			total = getCriticalTotal();
		} else {
			passed = getOverallPassed();
			total = getOverallTotal();
		}
		
		if(total == 0) return 0;
		
		double percentage = (double) passed / total * 100;
		return roundToDecimals(percentage, 1);
	}

	private static double roundToDecimals(double value, int decimals){
		BigDecimal bd = new BigDecimal(Double.toString(value));
		bd = bd.setScale(decimals, BigDecimal.ROUND_HALF_UP);
		return bd.doubleValue();
	}

	public String getDisplayName() {
		return "Robot result";
	}

	public String getSearchUrl() {
		return "robot";
	}

	public RobotSuiteResult getSuite(String name) {
		if(suites == null) return null;
		return suites.get(name);
	}
	
	public Collection<RobotSuiteResult> getSuites(){
		return suites == null ? null : suites.values();
	}
	
	public void tally(){
		failed = 0;
		passed = 0;
		criticalPassed = 0;
		criticalFailed = 0;
		failedTests = new ArrayList<RobotCaseResult>();
		
		for(RobotSuiteResult suite : suites.values()){
			suite.tally();
			failed += suite.getFailed();
			passed += suite.getPassed();
			criticalFailed += suite.getCriticalFailed();
			criticalPassed += suite.getCriticalPassed();
		}
	}
	
	public Object getDynamic(String token, StaplerRequest req, StaplerResponse rsp){
		return suites.get(token);
	}
	
	/**
	 * Serves Robot html report via robot url. Shows not found page if file is missing.
	 * @param req
	 * @param rsp
	 * @throws IOException
	 * @throws ServletException
	 * @throws InterruptedException
	 */
	public void doReport(StaplerRequest req, StaplerResponse rsp)
			throws IOException, ServletException, InterruptedException {
		String indexFile = getReportFileName();
		FilePath robotDir = getRobotDir();
		
		if(!new FilePath(robotDir, indexFile).exists()){
			rsp.forward(this, "notfound.jelly", req);
			return;
		}
		
		DirectoryBrowserSupport dbs = new DirectoryBrowserSupport(this,
				getRobotDir(), getDisplayName(),
				"folder.gif", false);
		dbs.setIndexFileName(indexFile);
		dbs.generateResponse(req, rsp, this);
	}

	private FilePath getRobotDir() {
		FilePath rootDir = new FilePath(getOwner().getRootDir());
		return new FilePath(rootDir, "robot");
	}

	public String getReportFileName() {
		return "report.html";
	}
}