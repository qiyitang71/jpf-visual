import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import java.util.LinkedList;
import java.util.List;

import gov.nasa.jpf.util.Pair;
import gov.nasa.jpf.vm.Path;
import gov.nasa.jpf.vm.Transition;

import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.layout.mxStackLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxLayoutManager;

public class DrawErrorTrace extends JPanel {

	private static final long serialVersionUID = 1L;
	private final int dx = 300;
	private final int dy = 45;
	private final int START_SIZE = 30;
	private final int TOP_SPACE = 10;
	private final int LEFT_SPACE = 15;

	// private final int numOfThreads = 5;

	public DrawErrorTrace() {
		super();
	}

	public void drawGraph(Path path) {

		if (path.size() == 0) {
			return; // nothing to publish
		}

		int currTran = 0;
		int prevThread = -1;
		int numOfThreads = -1;
		int start = -1;
		List<Pair<Integer, Integer>> group = new LinkedList<>();
		// first pass of the trace
		for (Transition t : path) {
			int currThread = t.getThreadIndex();
			if (currTran == 0) {
				start = 0;
			}
			if (currTran > 0 && currThread != prevThread) {
				group.add(new Pair<Integer, Integer>(start, currTran - 1));
				start = currTran;
			}

			if (currTran == path.size() - 1) {
				group.add(new Pair<Integer, Integer>(start, currTran));
			}

			prevThread = currThread;
			currTran++;
			numOfThreads = Math.max(numOfThreads, currThread);
		}
		numOfThreads++;
		// System.out.println(numOfThreads);

		int numOfRows = group.size();

		mxGraph graph = new mxGraph() {
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

		mxIGraphModel model = graph.getModel();

		graph.setCellsEditable(false);
		graph.setCellsSelectable(false);
		graph.setCellsResizable(false);
		Map<String, Object> defaultStyle = graph.getStylesheet().getDefaultVertexStyle();
		Map<String, Object> rowStyle = new HashMap<String, Object>(defaultStyle);
		Map<String, Object> menuStyle = new HashMap<String, Object>(defaultStyle);

		// defaultStyle.put(mxConstants.STYLE_SHAPE,
		// mxConstants.SHAPE_SWIMLANE);
		defaultStyle.put(mxConstants.STYLE_VERTICAL_ALIGN, "middle");
		defaultStyle.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, "white");
		defaultStyle.put(mxConstants.STYLE_FONTSIZE, 11);
		defaultStyle.put(mxConstants.STYLE_STARTSIZE, START_SIZE);
		defaultStyle.put(mxConstants.STYLE_HORIZONTAL, true);
		defaultStyle.put(mxConstants.STYLE_FONTCOLOR, "black");
		defaultStyle.put(mxConstants.STYLE_STROKECOLOR, "black");
		defaultStyle.remove(mxConstants.STYLE_FILLCOLOR);

		// menu style not foldable
		menuStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_SWIMLANE);
		menuStyle.put(mxConstants.STYLE_VERTICAL_ALIGN, "middle");
		menuStyle.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, "white");
		menuStyle.put(mxConstants.STYLE_FONTSIZE, 11);
		menuStyle.put(mxConstants.STYLE_STARTSIZE, START_SIZE);
		menuStyle.put(mxConstants.STYLE_HORIZONTAL, false);
		menuStyle.put(mxConstants.STYLE_FONTCOLOR, "black");
		menuStyle.put(mxConstants.STYLE_STROKECOLOR, "black");
		menuStyle.put(mxConstants.STYLE_FOLDABLE, false);
		menuStyle.put(mxConstants.STYLE_SPACING_TOP, TOP_SPACE);

		menuStyle.remove(mxConstants.STYLE_FILLCOLOR);
		graph.getStylesheet().putCellStyle("menu", menuStyle);

		rowStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_SWIMLANE);
		// rowStyle.put(mxConstants.STYLE_VERTICAL_ALIGN, "middle");
		rowStyle.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, "white");
		rowStyle.put(mxConstants.STYLE_FONTSIZE, 11);
		rowStyle.put(mxConstants.STYLE_STARTSIZE, START_SIZE);
		rowStyle.put(mxConstants.STYLE_HORIZONTAL, false);
		rowStyle.put(mxConstants.STYLE_FONTCOLOR, "black");
		rowStyle.put(mxConstants.STYLE_STROKECOLOR, "black");
		rowStyle.put(mxConstants.STYLE_SPACING_TOP, TOP_SPACE);
		rowStyle.remove(mxConstants.STYLE_FILLCOLOR);
		graph.getStylesheet().putCellStyle("row", rowStyle);

		// when folding, the width of the cell won't change
		mxIEventListener foldingHandler = new mxIEventListener() {
			@Override
			public void invoke(Object sender, mxEventObject evt) {
				Object[] cells = (Object[]) evt.getProperty("cells");
				for (int i = 0; i < cells.length; i++) {
					mxGeometry geo = model.getGeometry(cells[i]);
					if (geo.getAlternateBounds() != null) {
						geo.setWidth(geo.getAlternateBounds().getWidth());
						if (graph.isCellCollapsed(cells[i])) {
							Map<String, Object> style = graph.getStylesheet().getStyles().get("row");
							style.put(mxConstants.STYLE_HORIZONTAL, true);
							style.put(mxConstants.STYLE_ALIGN, "left");
							style.put(mxConstants.STYLE_SPACING_LEFT, LEFT_SPACE);
							style.put(mxConstants.STYLE_SWIMLANE_LINE, 0);
							//style.put(mxConstants.STYLE_SPACING_BOTTOM, TOP_SPACE);
							style.remove(mxConstants.STYLE_SPACING_TOP);
							style.put(mxConstants.STYLE_SPACING_TOP, -2);

						} else {
							Map<String, Object> style = graph.getStylesheet().getStyles().get("row");
							style.put(mxConstants.STYLE_HORIZONTAL, false);
							style.put(mxConstants.STYLE_SWIMLANE_LINE, 1);
							style.remove(mxConstants.STYLE_ALIGN);
							//style.put(mxConstants.STYLE_ALIGN, "middle");

							style.put(mxConstants.STYLE_SPACING_TOP, TOP_SPACE);
							style.remove(mxConstants.STYLE_SPACING_LEFT);

						}
					}

				}
			}
		};

		graph.addListener(mxEvent.FOLD_CELLS, foldingHandler);

		// while folding, the lower cells goes up
		mxLayoutManager layoutMng = new mxLayoutManager(graph) {
			public mxIGraphLayout getLayout(Object parent) {

				if (model.getChildCount(parent) > 0 && model.getStyle(parent) != "row"
						&& model.getStyle(parent) != "menu") {
					return new mxStackLayout(graph, false);
				} else if (model.getChildCount(parent) > 0
						&& (model.getStyle(parent) == "row" || model.getStyle(parent) == "menu")) {
					return new mxStackLayout(graph, true);
				}
				return null;
			}

		};

		Object parent = graph.getDefaultParent();

		model.beginUpdate();
		try {
			mxCell menu = (mxCell) graph.insertVertex(parent, null, "Trans.", 0, 0, numOfThreads * dx + START_SIZE, 0,
					"menu");
			menu.setConnectable(false);

			for (int i = 0; i < numOfThreads; i++) {
				if (i == 0) {
					((mxCell) graph.insertVertex(menu, null, "main", 0, 0, dx, dy)).setConnectable(false);
				} else {
					((mxCell) graph.insertVertex(menu, null, "Thread-" + i, 0, 0, dx, dy)).setConnectable(false);
				}
			}

			for (int i = 0; i < numOfRows; i++) {
				int from = group.get(i)._1;
				int to = group.get(i)._2;
				mxCell lane;
				if (from != to) {
					lane = (mxCell) graph.insertVertex(parent, null, from + "-" + to, 0, 0,
							numOfThreads * dx + START_SIZE, 0, "row");
				} else {
					lane = (mxCell) graph.insertVertex(parent, null, from, 0, 0, numOfThreads * dx + START_SIZE, 0,
							"row");
				}
				lane.setConnectable(false);

				int threadIdx = path.get(from).getThreadIndex();
				if (threadIdx > 0) {
					((mxCell) graph.insertVertex(lane, null, "", 0, 0, threadIdx * dx, dy)).setConnectable(false);
				}
				if (from != to) {
					((mxCell) graph.insertVertex(lane, null, "Tr. " + from + "-" + to, 0, 0, dx, dy))
							.setConnectable(false);
				} else {
					((mxCell) graph.insertVertex(lane, null, "Tr. " + from, 0, 0, dx, dy)).setConnectable(false);
				}

			}

			// set the collapsed height i.e. the folded height
			for (Object o : graph.getChildCells(parent)) {
				mxCell cell = (mxCell) o;
				if (cell != null && model.getStyle(cell) == "row") {
					model.getGeometry(cell)
							.setAlternateBounds(new mxRectangle(0, 0, numOfThreads * dx + START_SIZE, 23));
				}
			}

		} finally {
			model.endUpdate();

		}
		mxGraphComponent graphComponent = new mxGraphComponent(graph);
		graphComponent.getGraphHandler().setRemoveCellsFromParent(false);
		this.add(graphComponent);
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame();

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(700, 320);
		DrawErrorTrace et = new DrawErrorTrace();
		JScrollPane scrollPane = new JScrollPane(et);
		frame.getContentPane().add(scrollPane);
		// et.drawGraph();
		frame.setVisible(true);

	}
}
