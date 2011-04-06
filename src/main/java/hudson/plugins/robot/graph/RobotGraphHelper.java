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

import hudson.model.AbstractBuild;
import hudson.plugins.robot.Messages;
import hudson.plugins.robot.RobotBuildAction;
import hudson.plugins.robot.model.RobotCaseResult;
import hudson.plugins.robot.model.RobotSuiteResult;
import hudson.util.ChartUtil;
import hudson.util.ChartUtil.NumberOnlyBuildLabel;
import hudson.util.DataSetBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

public class RobotGraphHelper {

	/**
	 * Create a dataset for the trend graph.
	 * @param project
	 * @return
	 */
	public static CategoryDataset createDataSetForBuild(AbstractBuild<?,?> build) {
		List<Number> values = new ArrayList<Number>();
		List<String> rows = new ArrayList<String>();
		List<NumberOnlyBuildLabel> columns = new ArrayList<NumberOnlyBuildLabel>();

		for (; build != null; build = build
				.getPreviousBuild()) {
			RobotBuildAction action = build.getAction(RobotBuildAction.class);

			Number failed = 0, passed = 0;
			if (action != null && action.getResult() != null) {
				failed = action.getResult().getOverallFailed();
				passed = action.getResult().getOverallPassed();
			}

			// default 'zero value' must be set over zero to circumvent
			// JFreeChart stacked area rendering problem with zero values
			if (failed.intValue() < 1)
				failed = 0.01f;
			if (passed.intValue() < 1)
				passed = 0.01f;

			ChartUtil.NumberOnlyBuildLabel label = new ChartUtil.NumberOnlyBuildLabel(
					build);

			values.add(passed);
			rows.add(Messages.robot_trendgraph_passed());
			columns.add(label);

			values.add(failed);
			rows.add(Messages.robot_trendgraph_failed());
			columns.add(label);
		}

		return createSortedDataset(values, rows, columns);
	}
	
	/**
	 * Create a dataset for the trend graph.
	 * @param project
	 * @return
	 */
	public static CategoryDataset createDurationDataSetForBuild(AbstractBuild<?,?> build) {
		DataSetBuilder<String, NumberOnlyBuildLabel> builder = new DataSetBuilder<String, NumberOnlyBuildLabel>();

		for (; build != null; build = build
				.getPreviousBuild()) {
			RobotBuildAction action = build.getAction(RobotBuildAction.class);

			float duration = 0;
			if (action != null && action.getResult() != null) {
				duration = action.getResult().getDuration();
			}

			// default 'zero value' must be set over zero to circumvent
			// JFreeChart stacked area rendering problem with zero values
			if (duration < 1)
				duration = 0.01f;

			ChartUtil.NumberOnlyBuildLabel label = new ChartUtil.NumberOnlyBuildLabel(
					build);
			
			builder.add(duration, "duration", label);
		}
		
		return builder.build();
	}
	
	public static CategoryDataset createDurationDataSetForSuite(RobotSuiteResult suite) {
		DataSetBuilder<String, NumberOnlyBuildLabel> builder = new DataSetBuilder<String, NumberOnlyBuildLabel>();

		for (; suite != null; suite = (RobotSuiteResult)suite.getPreviousResult()) {
			float duration = 0;
			duration = suite.getDuration();

				// default 'zero value' must be set over zero to circumvent
				// JFreeChart stacked area rendering problem with zero values
				if (duration < 1)
					duration = 0.01f;

				ChartUtil.NumberOnlyBuildLabel label = new ChartUtil.NumberOnlyBuildLabel(
						suite.getOwner());
				
				builder.add(duration, "duration", label);
		}
		
		return builder.build();
	}
	
	public static CategoryDataset createDurationDataSetForCase(RobotCaseResult caseResult) {
		DataSetBuilder<String, NumberOnlyBuildLabel> builder = new DataSetBuilder<String, NumberOnlyBuildLabel>();

		for (; caseResult != null; caseResult = caseResult.getPreviousResult()) {
			float duration = 0;
			duration = caseResult.getDuration();

				// default 'zero value' must be set over zero to circumvent
				// JFreeChart stacked area rendering problem with zero values
				if (duration < 1)
					duration = 0.01f;

				ChartUtil.NumberOnlyBuildLabel label = new ChartUtil.NumberOnlyBuildLabel(
						caseResult.getOwner());
				
				builder.add(duration, "duration", label);
		}
		
		return builder.build();
	}

	public static CategoryDataset createDataSetForSuite(RobotSuiteResult suite) {
		List<Number> values = new ArrayList<Number>();
		List<String> rows = new ArrayList<String>();
		List<NumberOnlyBuildLabel> columns = new ArrayList<NumberOnlyBuildLabel>();

		for (; suite != null; suite = (RobotSuiteResult)suite.getPreviousResult()) {
			Number failed = 0, passed = 0;
				failed = suite.getFailed();
				passed = suite.getPassed();

			// default 'zero value' must be set over zero to circumvent
			// JFreeChart stacked area rendering problem with zero values
			if (failed.intValue() < 1)
				failed = 0.01f;
			if (passed.intValue() < 1)
				passed = 0.01f;

			ChartUtil.NumberOnlyBuildLabel label = new ChartUtil.NumberOnlyBuildLabel(
					suite.getOwner());

			values.add(passed);
			rows.add(Messages.robot_trendgraph_passed());
			columns.add(label);

			values.add(failed);
			rows.add(Messages.robot_trendgraph_failed());
			columns.add(label);
		}
		return createSortedDataset(values, rows, columns);
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
}
