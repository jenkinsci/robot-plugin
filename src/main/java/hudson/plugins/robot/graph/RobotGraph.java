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
import hudson.util.Graph;
import hudson.util.ShiftedCategoryAxis;

import java.awt.Color;
import java.awt.Font;

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
	private double lowerBound;
	private double upperBound;
	private int fontSize;
	private boolean preview;

	public static final int DEFAULT_CHART_WIDTH = 500;
	public static final int DEFAULT_CHART_HEIGHT = 200;
	public static final int DEFAULT_FONT_SIZE = 12;

	/**
	 * Construct a new styled trend graph for given dataset
	 * @param owner Build which the graph is associated to
	 * @param categoryDataset Category data for graph
	 * @param yLabel Y label name
	 * @param xLabel X label name
	 * @param scale the size 1 is graph of default size. This is multiplied by scale.
	 * @param binaryData binary data
	 * @param colors Used colors
	 * @param lowerBound Lower bound for the graph
	 * @param upperBound Upper bound for the graph
	 * @param preview True if preview is enabled
	 * @return Graph
	 */
	public static RobotGraph getRobotGraph(Run<?, ?> owner,
			CategoryDataset categoryDataset, String yLabel, String xLabel,
			double scale, boolean preview, boolean binaryData, double lowerBound, double upperBound, Color...colors) {
		int width = (int)(scale * RobotGraph.DEFAULT_CHART_WIDTH);
		int heigth = (int)(scale * RobotGraph.DEFAULT_CHART_HEIGHT);
		int fontSize = (int)(Math.sqrt(scale) * RobotGraph.DEFAULT_FONT_SIZE);  // use sqrt to scale font slower
		return new RobotGraph(owner, categoryDataset, yLabel, xLabel, width, heigth, fontSize, preview, binaryData, lowerBound, upperBound, colors);
	}

	/**
	 * Construct a new styled trend graph for given dataset
	 * @param owner Build which the graph is associated to
	 * @param categoryDataset Category data for graph
	 * @param yLabel Y label name
	 * @param xLabel X label name
	 * @param chartWidth Chart width in pixels
	 * @param chartHeight Chart height in pixels
	 * @param fontSize Chart font size
	 * @param binaryData binary data
	 * @param colors Used colors
	 * @param lowerBound Lower bound for the graph
	 * @param upperBound Upper bound for the graph
	 * @param preview True if preview is enabled
	 */
	private RobotGraph(Run<?, ?> owner,
			CategoryDataset categoryDataset, String yLabel, String xLabel,
			int chartWidth, int chartHeight, int fontSize, boolean preview,boolean binaryData, double lowerBound, double upperBound, Color...colors) {
		super(owner.getTimestamp(), chartWidth, chartHeight);
		this.yLabel = yLabel;
		this.xLabel = xLabel;
		this.categoryDataset = categoryDataset;
		this.colors = colors;
		this.binaryData = binaryData;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		this.fontSize = fontSize;
		this.preview = preview;
	}

	public CategoryDataset getDataset(){
		return categoryDataset;
	}

	/**
	 * Creates a Robot trend graph
	 * @return the JFreeChart graph object
	 */
	protected JFreeChart createGraph() {

		final JFreeChart chart = ChartFactory.createStackedAreaChart(null,null, preview ? null : yLabel, categoryDataset, PlotOrientation.VERTICAL, !preview,true, false);
		chart.setBackgroundPaint(Color.white);

		final Font font = new Font("Dialog", Font.PLAIN, fontSize);
		setLegend(chart, font);
		final CategoryPlot plot = initPlot(chart);

		setXaxis(font, plot);
		setYaxis(font, plot);

		final CategoryItemRenderer renderer = plot.getRenderer();

		for(int i = 0; i < colors.length; i++){
			renderer.setSeriesPaint(i, colors[i]);
		}

		RectangleInsets bounds = preview ? RectangleInsets.ZERO_INSETS : new RectangleInsets(15.0, 0, 0, 5.0);
		plot.setInsets(bounds);

		return chart;
	}

	private CategoryPlot initPlot(JFreeChart chart) {
		final CategoryPlot plot = (CategoryPlot) chart.getPlot();
		plot.setForegroundAlpha(0.7f);
		plot.setBackgroundPaint(Color.white);
		plot.setRangeGridlinePaint(Color.darkGray);
		plot.setRangeGridlinesVisible(!preview);
		plot.setOutlineVisible(!preview);
		return plot;
	}

	private void setLegend(JFreeChart chart, Font font) {
		if (preview) chart.clearSubtitles();
		else {
			final LegendTitle legend = chart.getLegend();
			legend.setPosition(RectangleEdge.RIGHT);
			legend.setItemFont(font);
		}
	}

	private void setYaxis(Font font, CategoryPlot plot) {
		final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		if (preview) {
			rangeAxis.setVisible(false);
		} else {
			rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
			if (binaryData) {
				rangeAxis.setUpperBound(1);
			} else if (upperBound != 0) {
				rangeAxis.setUpperBound(upperBound);
			} else {
				rangeAxis.setAutoRange(true);
			}
			rangeAxis.setLowerBound(lowerBound);
			rangeAxis.setLabelFont(font);
			rangeAxis.setTickLabelFont(font);
		}
	}

	private void setXaxis(Font font, CategoryPlot plot) {
		CategoryAxis domainAxis;
		if (preview) {
			domainAxis = new CategoryAxis();
			domainAxis.setVisible(false);
		} else {
			domainAxis = new ShiftedCategoryAxis(xLabel);
			domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
			domainAxis.setLowerMargin(0.0);
			domainAxis.setUpperMargin(0.0);
			domainAxis.setCategoryMargin(0.0);
			domainAxis.setLabelFont(font);
			domainAxis.setTickLabelFont(font);
		}
		plot.setDomainAxis(domainAxis);
	}
}
