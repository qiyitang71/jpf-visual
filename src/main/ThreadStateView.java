import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;

import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;

import gov.nasa.jpf.util.Pair;
import gov.nasa.jpf.vm.Path;

public class ThreadStateView extends JComponent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private double cellWidth = 0;
	private mxIGraphModel model;
	private Object parent;
	private mxGraph graph;

	private Map<String, Object> swimStyle;
	private Map<String, Object> contentStyle;

	private int numOfThreads = -1;
	private Path path;
	private List<Pair<Integer, Integer>> group = new ArrayList<>();
	private Map<Integer, TextLineList> lineTable;
	private double htPerLine;
	private int numOfRows = -1;

	public ThreadStateView(double width, int nThreads, Path p, List<Pair<Integer, Integer>> grp,
			Map<Integer, TextLineList> lt) {
		this.cellWidth = width;
		this.lineTable = lt;
		this.numOfThreads = nThreads;
		this.group = grp;
		this.path = p;
		this.cellWidth = width;
		this.numOfRows = group.size();

		// create graph
		this.graph = new mxGraph() {
			public mxRectangle getStartSize(Object swimlane) {
				mxRectangle result = new mxRectangle();
				mxCellState state = view.getState(swimlane);
				Map<String, Object> temp = getCellStyle(swimlane);

				Map<String, Object> style = (temp != null) ? temp : state.getStyle();

				if (style != null) {
					double size = mxUtils.getDouble(style, mxConstants.STYLE_STARTSIZE, mxConstants.DEFAULT_STARTSIZE);

					if (mxUtils.isTrue(style, mxConstants.STYLE_HORIZONTAL, true)) {
						result.setHeight(size);
					} else {
						result.setWidth(size);
					}
				}

				return result;
			}
		};

		// get the model
		this.model = graph.getModel();

		graph.setCellsEditable(false);
		graph.setCellsResizable(false);
		graph.setCollapseToPreferredSize(false);

		setStyles();
		// this.htPerLine =
		// mxUtils.getFontMetrics(mxUtils.getFont(graph.getStylesheet().getStyles().get("content")))
		// .getHeight() + 5;
		// installFoldingHandler();
		// setLayoutManager();
		drawTable();

	}

	protected void drawTable() {
		parent = graph.getDefaultParent();

		model.beginUpdate();
		try {
			// show the details
			for (int row = 0; row < numOfRows; row++) {

				if (!lineTable.containsKey(row) || lineTable.get(row).isNoSrc()) {
					lineTable.remove(row);
					continue;
				}

				TextLineList lineList = lineTable.get(row);
				double currHt = (lineList.getHeight() + 1) * htPerLine + 5 + 10;

				/**
				 * The big box around the first row with black border
				 */
				// mxCell rowCell = (mxCell) drawRowCell(row);

				/**
				 * The transition range no border
				 */
				// drawRangeCell(row, rowCell);

				/**
				 * The big box outside the swimlane
				 */
				// mxCell rightCell = (mxCell) drawRightCell(row, rowCell);

				/**
				 * The swimlane
				 */
				// mxCell swimCell = (mxCell) drawSwimCell(row, rightCell,
				// currHt);

			}

		} finally {
			model.endUpdate();
		}

		// foldAll(true);
	}

	protected void setStyles() {
		Map<String, Object> style = graph.getStylesheet().getDefaultVertexStyle();

		style.put(mxConstants.STYLE_VERTICAL_ALIGN, "middle");
		style.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, "white");
		style.put(mxConstants.STYLE_FONTSIZE, PaneConstants.FONT_SIZE);
		style.put(mxConstants.STYLE_STARTSIZE, 0);
		style.put(mxConstants.STYLE_HORIZONTAL, true);
		style.put(mxConstants.STYLE_FONTCOLOR, "black");
		style.put(mxConstants.STYLE_STROKECOLOR, "black");
		style.put(mxConstants.STYLE_ALIGN, "left");
		style.put(mxConstants.STYLE_FOLDABLE, false);
		style.put(mxConstants.STYLE_FILL_OPACITY, 0);
		style.put(mxConstants.STYLE_FILLCOLOR, "none");

		Map<String, Object> switchStyle = new HashMap<>(style);
		switchStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_SWIMLANE);
		switchStyle.put(mxConstants.STYLE_STROKECOLOR, "none");
		graph.getStylesheet().putCellStyle("switch", switchStyle);

		Map<String, Object> rightStyle = new HashMap<>(style);
		graph.getStylesheet().putCellStyle("right", rightStyle);

		Map<String, Object> borderStyle = new HashMap<>(style);
		graph.getStylesheet().putCellStyle("border", borderStyle);

		Map<String, Object> rangeStyle = new HashMap<String, Object>(style);
		rangeStyle.put(mxConstants.STYLE_SPACING_LEFT, PaneConstants.LEFT_SPACE);
		rangeStyle.put(mxConstants.STYLE_STROKECOLOR, "none");
		graph.getStylesheet().putCellStyle("range", rangeStyle);

		swimStyle = new HashMap<String, Object>(style);
		swimStyle.remove(mxConstants.STYLE_VERTICAL_ALIGN);
		swimStyle.put(mxConstants.STYLE_HORIZONTAL, false);
		swimStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_SWIMLANE);
		swimStyle.put(mxConstants.STYLE_FOLDABLE, true);
		swimStyle.put(mxConstants.STYLE_STROKECOLOR, "none");
		swimStyle.put(mxConstants.STYLE_STARTSIZE, PaneConstants.SIGN_SIZE);
		swimStyle.put(mxConstants.STYLE_SPACING_TOP, PaneConstants.TOP_SPACE);
		swimStyle.put(mxConstants.STYLE_SWIMLANE_LINE, 0);
		graph.getStylesheet().putCellStyle("swim", swimStyle);

		contentStyle = new HashMap<String, Object>(style);
		contentStyle.put(mxConstants.STYLE_STROKECOLOR, "none");
		contentStyle.put(mxConstants.STYLE_OPACITY, 0);
		contentStyle.put(mxConstants.STYLE_FILL_OPACITY, 0);
		contentStyle.put(mxConstants.STYLE_SPACING_TOP, PaneConstants.TOP_SPACE);
		contentStyle.remove(mxConstants.STYLE_VERTICAL_ALIGN);
		contentStyle.put(mxConstants.STYLE_FONTFAMILY, "Courier");
		graph.getStylesheet().putCellStyle("content", contentStyle);
	}

	public JComponent getComponent() {
		return this;
	}

}
