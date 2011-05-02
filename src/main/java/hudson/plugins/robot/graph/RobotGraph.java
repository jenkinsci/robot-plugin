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
import hudson.util.Graph;
import hudson.util.ShiftedCategoryAxis;

import java.awt.Color;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

public class RobotGraph extends Graph {

	private final String yLabel;
	private final String xLabel;

	private final CategoryDataset categoryDataset;
	private Color[] colors;
	private boolean binaryData;

	public static final int DEFAULT_CHART_WIDTH = 500;
	public static final int DEFAULT_CHART_HEIGHT = 200;

	/**
	 * Construct a new styled trend graph for given dataset
	 * @param owner Build which the graph is associated to
	 * @param categoryDataset Category data for graph
	 * @param yLabel Y label name
	 * @param xLabel X label name
	 * @param chartWidth Chart width in pixels
	 * @param chartHeight Chart height in pixels
	 */
	public RobotGraph(AbstractBuild<?, ?> owner,
			CategoryDataset categoryDataset, String yLabel, String xLabel,
			int chartWidth, int chartHeight, boolean binaryData, Color...colors) {
		super(owner.getTimestamp(), chartWidth, chartHeight);
		this.yLabel = yLabel;
		this.xLabel = xLabel;
		this.categoryDataset = categoryDataset;
		this.colors = colors;
		this.binaryData = binaryData;
	}

	/**
	 * Creates a Robot trend graph
	 * @return the JFreeChart graph object
	 */
	protected JFreeChart createGraph() {

		final JFreeChart chart = ChartFactory.createStackedAreaChart(null,
				null, yLabel, categoryDataset, PlotOrientation.VERTICAL, true,
				true, false);

		final LegendTitle legend = chart.getLegend();
		legend.setPosition(RectangleEdge.RIGHT);

		chart.setBackgroundPaint(Color.white);

		final CategoryPlot plot = (CategoryPlot) chart.getPlot();
		plot.setForegroundAlpha(0.7f);
		plot.setBackgroundPaint(Color.white);
		plot.setRangeGridlinePaint(Color.darkGray);

		final CategoryAxis domainAxis = new ShiftedCategoryAxis(xLabel);
		domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
		domainAxis.setLowerMargin(0.0);
		domainAxis.setUpperMargin(0.0);
		domainAxis.setCategoryMargin(0.0);
		plot.setDomainAxis(domainAxis);

		final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		if(binaryData){
			rangeAxis.setUpperBound(1);
		} else {
			rangeAxis.setAutoRange(true);
			rangeAxis.setAutoRangeMinimumSize(5);
		}
		rangeAxis.setLowerBound(0);


		final CategoryItemRenderer renderer = plot.getRenderer();
	
		for(int i = 0; i < colors.length; i++){
			renderer.setSeriesPaint(i, colors[i]);
		}

		plot.setInsets(new RectangleInsets(5.0, 0, 0, 5.0));

		return chart;
	}
}
