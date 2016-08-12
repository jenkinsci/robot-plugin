/*
* Copyright 2008-2014 Nokia Solutions and Networks Oy
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
package hudson.plugins.robot.graph;

import hudson.model.Run;
import hudson.plugins.robot.Messages;
import hudson.plugins.robot.model.RobotTestObject;
import hudson.util.ChartUtil;
import hudson.util.Graph;
import hudson.util.ChartUtil.NumberOnlyBuildLabel;
import hudson.util.DataSetBuilder;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

public class RobotGraphHelper {

	private static int SECONDSCALE = 1000;
	private static int MINUTESCALE = 60000;
	private static int HOURSCALE = 3600000;

	/**
	 * Create a test result trend graph. The graph will ignore builds with no robot results.
	 * @param rootObject The dataset will be taken from rootObject backwards.
	 * (i.e. there are no saved robot results in a given build)
	 * @param failedOnly put test failures onto the graph only, to allow focus on test failures
	 * @param maxBuildsToShow This maximum number of build result will be displayed on a graph
	 *                        0 - no limits applied.
	 * @return
	 */
	public static RobotGraph createTestResultsGraphForTestObject(RobotTestObject rootObject,
																 boolean significantData,
																 boolean binarydata,
																 boolean hd,
																 boolean failedOnly,
																 boolean criticalOnly,
																 int maxBuildsToShow) {
		List<Number> values = new ArrayList<Number>();
		List<String> rows = new ArrayList<String>();
		List<NumberOnlyBuildLabel> columns = new ArrayList<NumberOnlyBuildLabel>();

		double lowerbound = 0;
		double upperbound = 0;
		int buildsLeftToShow = maxBuildsToShow > 0? maxBuildsToShow: -1;
		for (RobotTestObject testObject = rootObject;
			 testObject != null && buildsLeftToShow != 0;
			 testObject = testObject.getPreviousResult(), buildsLeftToShow--)
		{
			Number failed =  !criticalOnly ? testObject.getFailed() : testObject.getCriticalFailed();
			Number passed = 0;
			int compareLowerBoundTo;
			if ( failedOnly) {
			    compareLowerBoundTo = failed.intValue();
			} else {
			    passed = !criticalOnly ? testObject.getPassed() : testObject.getCriticalPassed();
			    compareLowerBoundTo = passed.intValue();
			}

			if (significantData){
				if(lowerbound == 0 || lowerbound > compareLowerBoundTo)
					lowerbound =  compareLowerBoundTo;

				if(upperbound < failed.intValue() + passed.intValue())
					upperbound = failed.intValue() + passed.intValue();
			}

			ChartUtil.NumberOnlyBuildLabel label = new ChartUtil.NumberOnlyBuildLabel(
					(Run<?,?>)testObject.getOwner());

			values.add(passed);
			rows.add(Messages.robot_trendgraph_passed());
			columns.add(label);

			values.add(failed);
			rows.add(Messages.robot_trendgraph_failed());
			columns.add(label);
		}

		if(significantData){
			lowerbound = Math.max(0,lowerbound - (1 + upperbound - lowerbound)*0.05);
			upperbound = upperbound + (1 + upperbound - lowerbound)*0.05;
		}
		int graphScale = hd ? 3 : 1;
		return RobotGraph.getRobotGraph(rootObject.getOwner(), createSortedDataset(values, rows, columns), Messages.robot_trendgraph_testcases(),
				Messages.robot_trendgraph_builds(), graphScale, binarydata, lowerbound, upperbound, Color.green, Color.red);
	}

	/**
	 * Create a duration trend graph. The graph will ignore builds with no robot results.
	 * @param rootObject rootObject The dataset will be taken from rootObject backwards.
	 * @return
	 */
	public static RobotGraph createDurationGraphForTestObject(RobotTestObject rootObject, boolean hd, int maxBuildsToShow) {
		DataSetBuilder<String, NumberOnlyBuildLabel> builder = new DataSetBuilder<String, NumberOnlyBuildLabel>();

		int scale = 1;
		int buildsLeftToShow = maxBuildsToShow > 0? maxBuildsToShow: -1;

		List<NumberOnlyBuildLabel> labels = new ArrayList<NumberOnlyBuildLabel>();
		List<Long> durations = new ArrayList<Long>();

		for (RobotTestObject testObject = rootObject;
			 testObject != null && buildsLeftToShow != 0;
			 testObject = testObject.getPreviousResult(), buildsLeftToShow--) {

			scale = getTimeScaleFactor(testObject.getDuration(), scale);
			labels.add(new ChartUtil.NumberOnlyBuildLabel(testObject.getOwner()));
			durations.add(testObject.getDuration());
		}

		for (int i = 0; i < labels.size(); i++) {
			builder.add((double) durations.get(i) / scale, "Duration", labels.get(i));
		}

		int graphScale = hd ? 3 : 1;
		return RobotGraph.getRobotGraph(rootObject.getOwner(), builder.build(), "Duration (" + getTimeScaleString(scale) + ")",
				  Messages.robot_trendgraph_builds(), graphScale, false, 0, 0, Color.cyan);
	}

	private static CategoryDataset createSortedDataset(List<Number> values, List<String> rows, List<NumberOnlyBuildLabel> columns) {
		// Code from DataSetBuilder, reversed row order for passed tests to go
		// first into dataset for nicer order when rendered in chart
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		TreeSet<String> rowSet = new TreeSet<String>(rows);
		TreeSet<ChartUtil.NumberOnlyBuildLabel> colSet = new TreeSet<ChartUtil.NumberOnlyBuildLabel>(
				columns);

		Comparable[] _rows = rowSet.toArray(new Comparable[rowSet.size()]);
		Comparable[] _cols = colSet.toArray(new Comparable[colSet.size()]);

		// insert rows and columns in the right order, reverse rows
		for (int i = _rows.length - 1; i >= 0; i--)
			dataset.setValue(null, _rows[i], _cols[0]);
		for (Comparable c : _cols)
			dataset.setValue(null, _rows[0], c);

		for (int i = 0; i < values.size(); i++)
			dataset.addValue(values.get(i), rows.get(i), columns.get(i));
		return dataset;
	}

	private static int getTimeScaleFactor(float duration, int originalScale){
		int scale = originalScale;
		if (duration > HOURSCALE) {
	    	scale = HOURSCALE;
	    } else if (duration > MINUTESCALE) {
	    	scale = MINUTESCALE;
	    } else if (duration > SECONDSCALE) {
	    	scale = SECONDSCALE;
	    }
		return scale;
	}

	private static String getTimeScaleString(int scale){
		if(scale == SECONDSCALE) return "s";
		else if(scale == MINUTESCALE) return "min";
		else if(scale == HOURSCALE) return "h";
		return "ms";
	}
}
