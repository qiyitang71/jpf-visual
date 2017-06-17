import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
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
import com.mxgraph.view.mxGraphView;
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
	private final int CONTENT_FONT = 12;
	private final int AMPLIFY = 17;
	private int numOfThreads = -1;
	private List<String> threadNames = null;

	public DrawErrorTrace() {
		super();
	}

	public int getNumberOfThreads() {
		return this.numOfThreads;
	}

	public List<String> getThreadNames() {
		return new ArrayList<>(threadNames);
	}

	public void drawGraph(Path path) {

		/**
		 * deal with the trace
		 */

		if (path.size() == 0) {
			return; // nothing to publish
		}

		int currTran = 0;
		int prevThread = -1;
		int start = -1;

		List<Pair<Integer, Integer>> group = new LinkedList<>(); // group the
																	// transition
		threadNames = new ArrayList<>(); // range
		// first pass of the trace
		for (Transition t : path) {
			int currThread = t.getThreadIndex();
			if (threadNames.size() == currThread) {
				threadNames.add(t.getThreadInfo().getName());
			}
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
			tempStr.deleteCharAt(tempStr.length() - 1);
			detailList.add(tempStr.toString());
			heightList.add(height);

		}

		/**
		 * begin draw table contents
		 */
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
		graph.setCellsResizable(true);
		// graph.setCollapseToPreferredSize(false);

		Map<String, Object> style = graph.getStylesheet().getDefaultVertexStyle();

		style.put(mxConstants.STYLE_VERTICAL_ALIGN, "middle");
		style.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, "white");
		style.put(mxConstants.STYLE_FONTSIZE, FONT_SIZE);
		style.put(mxConstants.STYLE_STARTSIZE, 0);
		style.put(mxConstants.STYLE_HORIZONTAL, true);
		style.put(mxConstants.STYLE_FONTCOLOR, "black");
		style.put(mxConstants.STYLE_STROKECOLOR, "black");
		style.remove(mxConstants.STYLE_FILLCOLOR);

		Map<String, Object> textStyle = new HashMap<String, Object>(style);
		textStyle.put(mxConstants.STYLE_STARTSIZE, START_SIZE);
		graph.getStylesheet().putCellStyle("text", textStyle);

		// Map<String, Object> Style = new HashMap<String,
		// Object>(defaultStyle);

		Map<String, Object> contentStyle = new HashMap<String, Object>(textStyle);
		//contentStyle.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_TOP);
		contentStyle.put(mxConstants.STYLE_ALIGN, "left");
		contentStyle.put(mxConstants.STYLE_SPACING_LEFT, LEFT_SPACE);
		//contentStyle.put(mxConstants.STYLE_SPACING_TOP, 1);
		contentStyle.put(mxConstants.STYLE_STROKECOLOR, "none");

		graph.getStylesheet().putCellStyle("content", contentStyle);

		Map<String, Object> rowStyle = new HashMap<String, Object>(textStyle);
		rowStyle.put(mxConstants.STYLE_HORIZONTAL, false);
		rowStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_SWIMLANE);
		rowStyle.put(mxConstants.STYLE_FOLDABLE, true);
		rowStyle.put(mxConstants.STYLE_SPACING_TOP, TOP_SPACE);
		graph.getStylesheet().putCellStyle("row", rowStyle);

		Map<String, Object> labelStyle = new HashMap<String, Object>(rowStyle);
		labelStyle.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_LEFT);
		labelStyle.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_BOTTOM);
		labelStyle.put(mxConstants.STYLE_FONTSIZE, CONTENT_FONT);
		labelStyle.put(mxConstants.STYLE_FONTSTYLE, mxConstants.FONT_BOLD);
		labelStyle.put(mxConstants.STYLE_HORIZONTAL, true);
		labelStyle.put(mxConstants.STYLE_SWIMLANE_LINE, 0);
		labelStyle.put(mxConstants.STYLE_FOLDABLE, false);
		graph.getStylesheet().putCellStyle("label", labelStyle);

		// when folding, the width of the cell won't change
		// the orientation STYLE_HORIZONTAL will change
		// the content of rows will change
		mxIEventListener foldingHandler = new mxIEventListener() {
			@Override
			public void invoke(Object sender, mxEventObject evt) {
				Object[] cells = (Object[]) evt.getProperty("cells");
				for (int i = 0; i < cells.length; i++) {
					mxGeometry geo = model.getGeometry(cells[i]);
					if (geo.getAlternateBounds() != null) {
						geo.setWidth(geo.getAlternateBounds().getWidth());
						String str = model.getStyle(cells[i]);
						if (graph.isCellCollapsed(cells[i])) {

							// set the style when folded
							// cells[i] must be of style "rowi"
							mxCell c = (mxCell) cells[i];
							int idx = Integer.parseInt(c.getId());
							int from = group.get(idx)._1;
							int threadIdx = path.get(from).getThreadIndex();

							Map<String, Object> style = graph.getStylesheet().getStyles().get(str);
							style.put(mxConstants.STYLE_HORIZONTAL, true);
							style.put(mxConstants.STYLE_ALIGN, "left");
							style.put(mxConstants.STYLE_SPACING_LEFT, threadIdx * dx + START_SIZE + LEFT_SPACE);
							style.put(mxConstants.STYLE_SWIMLANE_LINE, 0);

							String s = detailList.get(idx);
							s = s.replaceAll("gov.nasa.jpf.vm.*?\\n", "").replaceAll("\\[.*?\\n", "");
							String[] strs = s.split("\\n");
							String first;
							String last;
							if (strs.length > 0) {
								first = strs[0].replaceAll("^.*?:.*?:", "");
								StringBuilder sb = new StringBuilder().append(Left.format(first, 20));
								if (strs.length > 1) {
									last = strs[strs.length - 1].replaceAll("^.*?:.*?:", "");

									if (strs.length > 2) {
										sb.append("\n...");
									}
									sb.append("\n" + Left.format(last, 20));
								}
								c.setValue(sb.toString());
							}
						} else if (!str.equals("menu")) {
							// set back the style of the expanded cell
							Map<String, Object> style = graph.getStylesheet().getStyles().get(str);
							style.put(mxConstants.STYLE_HORIZONTAL, false);
							style.put(mxConstants.STYLE_SWIMLANE_LINE, 1);
							style.remove(mxConstants.STYLE_ALIGN);
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

				if (model.getChildCount(parent) > 0 && model.getStyle(parent) != "row") {
					return new mxStackLayout(graph, false);
				} else if (model.getChildCount(parent) > 0 && model.getStyle(parent) == "row") {
					return new mxStackLayout(graph, true);
				}
				return null;
			}

		};

		Object parent = graph.getDefaultParent();

		model.beginUpdate();
		try {

			// show the details
			for (int i = 0; i < numOfRows; i++) {
				int from = group.get(i)._1;
				int to = group.get(i)._2;
				mxCell lane;

				// set the id of lane to be the group idx
				Map<String, Object> tmpStyle = new HashMap<>(rowStyle);
				graph.getStylesheet().putCellStyle("row" + i, tmpStyle);

				if (from != to) {
					lane = (mxCell) graph.insertVertex(parent, "" + i, from + "-" + to, 0, 0,
							numOfThreads * dx + START_SIZE, 0, "row" + i);
				} else {
					lane = (mxCell) graph.insertVertex(parent, "" + i, from, 0, 0, numOfThreads * dx + START_SIZE, 0,
							"row" + i);
				}
				lane.setId("" + i);
				lane.setConnectable(false);

				// lane.setCollapsed(true);

				int threadIdx = path.get(from).getThreadIndex();
				Map<String, Object> tmpLabel = new HashMap<String, Object>(labelStyle);
				tmpLabel.put(mxConstants.STYLE_SPACING_LEFT, dx * threadIdx + dx / 2 - LEFT_SPACE / 2);
				graph.getStylesheet().putCellStyle("label" + i, tmpLabel);

				int htPerLine = mxUtils.getFontMetrics(mxUtils.getFont(tmpLabel)).getHeight();
				int currHt = heightList.get(i) * htPerLine;

				mxCell labelRow = (mxCell) graph.insertVertex(lane, null, "" + threadIdx, 0, 0, numOfThreads * dx,
						START_SIZE + currHt, "label" + i);
				labelRow.setConnectable(false);

				mxCell content = (mxCell) graph.insertVertex(labelRow, null, detailList.get(i), 0,
						0, numOfThreads * dx, currHt, "content");
				content.setConnectable(false);

			}

			// set the collapsed height i.e. the folded height
			for (Object o : graph.getChildCells(parent)) {
				mxCell cell = (mxCell) o;
				if (cell != null && cell.getId() != null) {
					model.getGeometry(cell)
							.setAlternateBounds(new mxRectangle(0, 0, numOfThreads * dx + START_SIZE, ALTER_SIZE));
					graph.foldCells(true, false, new Object[] { cell }, true);
				}
			}

		} finally {
			model.endUpdate();

		}
		mxGraphComponent graphComponent = new mxGraphComponent(graph);
		graphComponent.getGraphHandler().setRemoveCellsFromParent(false);
		// graphComponent.setAutoScroll(false);
		// graphComponent.setAutoscrolls(false);
		graphComponent.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		graphComponent.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

		// graphComponent.setVerticalScrollBar(null);

		this.add(graphComponent);
		this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

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
