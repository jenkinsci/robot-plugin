/*
* Copyright 2008-2011 Nokia Siemens Networks Oyj
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
	 * Create a pass/fail trend graph. The graph will ignore builds with no robot results.
	 * @param rootObject The dataset will be taken from rootObject backwards.
	 * (i.e. there are no saved robot results in a given build)
	 * @return
	 */
	public static Graph createDataSetForTestObject(RobotTestObject rootObject, boolean significantData, boolean binarydata, boolean hd) {
		List<Number> values = new ArrayList<Number>();
		List<String> rows = new ArrayList<String>();
		List<NumberOnlyBuildLabel> columns = new ArrayList<NumberOnlyBuildLabel>();

		int lowerbound = 0;
		int upperbound = 0;
		for (RobotTestObject testObject = rootObject; testObject != null; testObject = testObject.getPreviousResult()) {
			Number failed = testObject.getFailed();
			Number passed = testObject.getPassed();

			if (!significantData){
				if(lowerbound == 0 || lowerbound > failed.intValue() + passed.intValue())
					lowerbound = failed.intValue() + passed.intValue();

				if(upperbound < failed.intValue() + passed.intValue())
					upperbound = failed.intValue() + passed.intValue();
			}

			ChartUtil.NumberOnlyBuildLabel label = new ChartUtil.NumberOnlyBuildLabel(
					testObject.getOwner());

			values.add(passed);
			rows.add(Messages.robot_trendgraph_passed());
			columns.add(label);

			values.add(failed);
			rows.add(Messages.robot_trendgraph_failed());
			columns.add(label);
		}

		if(!significantData){
			lowerbound = (int)(lowerbound * 0.9);
			upperbound = (int)Math.ceil(upperbound * 1.1);
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
	public static Graph createDurationGraphForTestObject(RobotTestObject rootObject, boolean hd) {
		DataSetBuilder<String, NumberOnlyBuildLabel> builder = new DataSetBuilder<String, NumberOnlyBuildLabel>();

		int scale = 1;
		for (RobotTestObject testObject = rootObject; testObject != null; testObject = testObject.getPreviousResult()){
			scale = getTimeScaleFactor(testObject.getDuration(), scale);
		}

		for (RobotTestObject testObject = rootObject; testObject != null; testObject = testObject.getPreviousResult()){
			ChartUtil.NumberOnlyBuildLabel label = new ChartUtil.NumberOnlyBuildLabel(
					testObject.getOwner());
			builder.add((double)testObject.getDuration() / scale, "Duration", label);
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
