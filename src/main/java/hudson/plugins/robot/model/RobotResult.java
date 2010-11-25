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

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Class representing Robot Framework test results.
 *
 */
public class RobotResult implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String timeStamp;
	private List<RobotResultStatistics> overallStats;
	
	//backwards compatibility with old builds
	private transient List<RobotResultStatistics> statsBySuite;
	
	/*
	 * The data structure of passed and failed tests is awkward and fragile but
	 * needs some consideration before migrating so that old builds won't break.
	 */
	
	/**
	 * Get number of passed critical tests.
	 * @return
	 */
	public long getCriticalPassed(){
		if(overallStats.isEmpty()) return 0;
		return overallStats.get(0).getPass();
	}
	
	/**
	 * Get number of failed critical tests.
	 * @return
	 */
	public long getCriticalFail(){
		if(overallStats.isEmpty()) return 0;
		return overallStats.get(0).getFail();
	}
	
	/**
	 * Get total number of critical tests.
	 * @return
	 */
	public long getCriticalTotal(){
		if(overallStats.isEmpty()) return 0;
		return overallStats.get(0).getTotal();
	}
	
	/**
	 * Get number of all passed tests.
	 * @return
	 */
	public long getOverallPassed(){
		if(overallStats.isEmpty()) return 0;
		return overallStats.get(1).getPass();
	}
	
	/**
	 * Get number of all failed tests.
	 * @return
	 */
	public long getOverallFailed(){
		if(overallStats.isEmpty()) return 0;
		return overallStats.get(1).getFail();
	}
	
	/**
	 * Get number of all tests.
	 * @return
	 */
	public long getOverallTotal(){
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
		double percentage = (double) passed / total * 100;
		return roundToDecimals(percentage, 1);
	}

	private static double roundToDecimals(double value, int decimals){
		BigDecimal bd = new BigDecimal(Double.toString(value));
		bd = bd.setScale(decimals, BigDecimal.ROUND_HALF_UP);
		return bd.doubleValue();
	}
}