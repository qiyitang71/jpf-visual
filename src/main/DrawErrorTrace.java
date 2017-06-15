import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import java.util.LinkedList;
import java.util.List;

import gov.nasa.jpf.util.Left;
import gov.nasa.jpf.util.Pair;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.Path;
import gov.nasa.jpf.vm.Step;
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
	private final int ALTER_SIZE = 50;
	private final int FONT_SIZE = 11;
	private final int CONTENT_FONT = 9;
	private final int AMPLIFY = 17;
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

		List<Pair<Integer, Integer>> group = new LinkedList<>(); // group the
																	// transition
																	// range
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

		int numOfRows = group.size();

		// second pass of the path
		List<String> detailList = new ArrayList<>();
		List<Integer> heightList = new ArrayList<>();

		for (Pair<Integer, Integer> p : group) {
			int from = p._1;
			int to = p._2;
			int height = 0;
			StringBuilder tempStr = new StringBuilder();

			for (int i = from; i <= to; i++) {
				Transition t = path.get(i);
				String lastLine = null;
				int nNoSrc = 0;
				tempStr.append(t.getChoiceGenerator() + "\n");
				height++;
				for (Step s : t) {
					String line = s.getLineString();
					if (line != null) {
						String src = line.replaceAll("/\\*.*?\\*/", "").replaceAll("//.*$", "")
								.replaceAll("/\\*.*$", "").replaceAll("^.*?\\*/", "").replaceAll("\\*.*$", "").trim();

						if (!line.equals(lastLine) && src.length() > 1) {
							if (nNoSrc > 0) {
								tempStr.append(" [" + nNoSrc + " insn w/o sources]" + "\n");
								height++;
							}
							tempStr.append(" ");
							tempStr.append(Left.format(s.getLocationString(), 30));
							tempStr.append(": ");
							tempStr.append(src + "\n");
							height++;
							nNoSrc = 0;
						}
					} else { // no source
						nNoSrc++;
					}
					lastLine = line;
				}

			}
			detailList.add(tempStr.toString());
			heightList.add(height);

		}

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

		// defaultStyle.put(mxConstants.STYLE_SHAPE,
		// mxConstants.SHAPE_SWIMLANE);
		defaultStyle.put(mxConstants.STYLE_VERTICAL_ALIGN, "middle");
		// defaultStyle.put(mxConstants.STYLE_ALIGN, "left");
		// defaultStyle.put(mxConstants.STYLE_SPACING_LEFT, LEFT_SPACE);
		// defaultStyle.put(mxConstants.STYLE_VERTICAL_ALIGN,
		// mxConstants.ALIGN_TOP);
		defaultStyle.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, "white");
		defaultStyle.put(mxConstants.STYLE_FONTSIZE, 11);
		defaultStyle.put(mxConstants.STYLE_STARTSIZE, START_SIZE);
		defaultStyle.put(mxConstants.STYLE_HORIZONTAL, true);
		defaultStyle.put(mxConstants.STYLE_FONTCOLOR, "black");
		defaultStyle.put(mxConstants.STYLE_STROKECOLOR, "black");
		defaultStyle.remove(mxConstants.STYLE_FILLCOLOR);

		Map<String, Object> contentStyle = new HashMap<String, Object>(defaultStyle);
		contentStyle.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_TOP);
		contentStyle.put(mxConstants.STYLE_ALIGN, "left");
		contentStyle.put(mxConstants.STYLE_SPACING_LEFT, LEFT_SPACE);
		contentStyle.put(mxConstants.STYLE_SPACING_TOP, 7);

		graph.getStylesheet().putCellStyle("content", contentStyle);

		Map<String, Object> menuStyle = new HashMap<String, Object>(defaultStyle);
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

		Map<String, Object> rowStyle = new HashMap<String, Object>(defaultStyle);

		rowStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_SWIMLANE);
		// rowStyle.put(mxConstants.STYLE_VERTICAL_ALIGN,
		// mxConstants.ALIGN_TOP);
		rowStyle.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, "white");
		rowStyle.put(mxConstants.STYLE_FONTSIZE, 11);
		rowStyle.put(mxConstants.STYLE_STARTSIZE, START_SIZE);
		rowStyle.put(mxConstants.STYLE_HORIZONTAL, false);
		rowStyle.put(mxConstants.STYLE_FONTCOLOR, "black");
		rowStyle.put(mxConstants.STYLE_STROKECOLOR, "black");
		rowStyle.put(mxConstants.STYLE_SPACING_TOP, TOP_SPACE);
		rowStyle.remove(mxConstants.STYLE_FILLCOLOR);
		graph.getStylesheet().putCellStyle("row", rowStyle);

		Map<String, Object> labelStyle = new HashMap<String, Object>(defaultStyle);
		labelStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_SWIMLANE);
		labelStyle.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, "white");
		labelStyle.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_LEFT);
		labelStyle.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_BOTTOM);
		labelStyle.put(mxConstants.STYLE_FONTSIZE, CONTENT_FONT);
		labelStyle.put(mxConstants.STYLE_STARTSIZE, START_SIZE);
		labelStyle.put(mxConstants.STYLE_FONTCOLOR, "black");
		labelStyle.put(mxConstants.STYLE_STROKECOLOR, "black");
		labelStyle.remove(mxConstants.STYLE_FILLCOLOR);
		labelStyle.put(mxConstants.STYLE_HORIZONTAL, true);
		labelStyle.put(mxConstants.STYLE_SWIMLANE_LINE, 0);
		labelStyle.put(mxConstants.STYLE_FOLDABLE, false);

		graph.getStylesheet().putCellStyle("label", labelStyle);
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
							// set the style when folded
							mxCell c = (mxCell) cells[i];
							int idx = Integer.parseInt(c.getId());
							int from = group.get(idx)._1;
							int threadIdx = path.get(from).getThreadIndex();

							Map<String, Object> style = graph.getStylesheet().getStyles().get("row");
							style.put(mxConstants.STYLE_HORIZONTAL, true);
							style.put(mxConstants.STYLE_ALIGN, "left");
							style.put(mxConstants.STYLE_SPACING_LEFT, threadIdx * dx + START_SIZE + LEFT_SPACE);
							style.put(mxConstants.STYLE_SWIMLANE_LINE, 0);
							// style.put(mxConstants.STYLE_SPACING_BOTTOM,
							// TOP_SPACE);
							// style.remove(mxConstants.STYLE_SPACING_TOP);
							// style.put(mxConstants.STYLE_SPACING_TOP, -2);

							String s = detailList.get(idx);
							s = s.replaceAll("gov.nasa.jpf.vm.*?\\n", "").replaceAll("\\[.*?\\n", "");
							String[] strs = s.split("\\n");
							String first;
							String last;
							if (strs.length > 1) {
								first = strs[0].replaceAll("^.*?:.*?:", "");
								last = strs[strs.length - 1].replaceAll("^.*?:.*?:", "");
								c.setValue(Left.format(first, 20) + "\n...\n" + Left.format(last, 20));
							} else if (strs.length == 1) {
								first = strs[0].replaceAll("^.*?:.*?:", "");
								c.setValue(Left.format(first, 20));
							}
							// c.setValue(c.getId());
						} else {
							// set back the style of the expanded cell
							Map<String, Object> style = graph.getStylesheet().getStyles().get("row");
							style.put(mxConstants.STYLE_HORIZONTAL, false);
							style.put(mxConstants.STYLE_SWIMLANE_LINE, 1);
							style.remove(mxConstants.STYLE_ALIGN);
							// style.put(mxConstants.STYLE_ALIGN, "middle");
							style.put(mxConstants.STYLE_SPACING_TOP, TOP_SPACE);
							style.remove(mxConstants.STYLE_SPACING_LEFT);

							mxCell c = (mxCell) cells[i];
							Integer idx = Integer.parseInt(c.getId());
							int from = group.get(idx)._1;
							int to = group.get(idx)._2;
							if (from != to) {
								c.setValue(from + "-" + to);
							} else {
								c.setValue(from);
							}
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
			// draw the menu
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

			// show the details
			for (int i = 0; i < numOfRows; i++) {
				int from = group.get(i)._1;
				int to = group.get(i)._2;
				mxCell lane;

				// set the id of lane to be the group idx
				if (from != to) {
					lane = (mxCell) graph.insertVertex(parent, "" + i, from + "-" + to, 0, 0,
							numOfThreads * dx + START_SIZE, 0, "row");
				} else {
					lane = (mxCell) graph.insertVertex(parent, "" + i, from, 0, 0, numOfThreads * dx + START_SIZE, 0,
							"row");
				}
				lane.setId("" + i);
				lane.setConnectable(false);

				int threadIdx = path.get(from).getThreadIndex();
				Map<String, Object> tmpLabel = new HashMap<String, Object>(labelStyle);
				tmpLabel.put(mxConstants.STYLE_SPACING_LEFT, dx * threadIdx + dx / 2);
				graph.getStylesheet().putCellStyle("label" + i, tmpLabel);
				int currHt = heightList.get(i) * AMPLIFY;
				mxCell labelRow = (mxCell) graph.insertVertex(lane, null, "" + threadIdx, 0, 0, numOfThreads * dx,
						START_SIZE + currHt, "label" + i);
				labelRow.setConnectable(false);

				mxCell content = (mxCell) graph.insertVertex(labelRow, null, detailList.get(i), 0, 0, numOfThreads * dx,
						currHt, "content");
				content.setConnectable(false);
			}

			// set the collapsed height i.e. the folded height
			for (Object o : graph.getChildCells(parent)) {
				mxCell cell = (mxCell) o;
				if (cell != null && model.getStyle(cell) == "row") {
					model.getGeometry(cell)
							.setAlternateBounds(new mxRectangle(0, 0, numOfThreads * dx + START_SIZE, ALTER_SIZE));
					//graph.foldCells(true, false, new Object[] { cell }, true);
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
