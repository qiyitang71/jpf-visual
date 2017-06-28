import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.layout.mxStackLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.util.mxUtils;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxLayoutManager;

import gov.nasa.jpf.util.Left;
import gov.nasa.jpf.util.Pair;
import gov.nasa.jpf.vm.Path;

public class ContentPane {
	private int cellWidth = PaneConstants.DEFAULT_CELL_WIDTH;
	private mxGraph graph;
	private mxIGraphModel model;

	private int numOfThreads = -1;
	private Path path;
	private List<Pair<Integer, Integer>> group = new ArrayList<>();
	// private List<String> detailList = new ArrayList<>();
	// private List<Integer> heightList = new ArrayList<>();

	public ContentPane(int numOfThreads, Path path, List<Pair<Integer, Integer>> group, List<String> detailList,
			List<Integer> heightList) {
		this.numOfThreads = numOfThreads;
		this.group = group;
		this.path = path;

		int numOfRows = group.size();
		/**
		 * begin draw table contents
		 */
		graph = new mxGraph() {
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

		model = graph.getModel();

		graph.setCellsEditable(false);
		graph.setCellsSelectable(false);
		graph.setCellsResizable(true);
		// graph.setCollapseToPreferredSize(false);

		Map<String, Object> style = graph.getStylesheet().getDefaultVertexStyle();

		style.put(mxConstants.STYLE_VERTICAL_ALIGN, "middle");
		style.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, "white");
		style.put(mxConstants.STYLE_FONTSIZE, PaneConstants.FONT_SIZE);
		style.put(mxConstants.STYLE_STARTSIZE, 0);
		style.put(mxConstants.STYLE_HORIZONTAL, true);
		style.put(mxConstants.STYLE_FONTCOLOR, "black");
		style.put(mxConstants.STYLE_STROKECOLOR, "black");
		style.remove(mxConstants.STYLE_FILLCOLOR);

		Map<String, Object> textStyle = new HashMap<String, Object>(style);
		textStyle.put(mxConstants.STYLE_STARTSIZE, PaneConstants.START_SIZE);
		graph.getStylesheet().putCellStyle("text", textStyle);

		Map<String, Object> contentStyle = new HashMap<String, Object>(textStyle);
		// contentStyle.put(mxConstants.STYLE_VERTICAL_ALIGN,
		// mxConstants.ALIGN_TOP);
		contentStyle.put(mxConstants.STYLE_ALIGN, "left");
		contentStyle.put(mxConstants.STYLE_SPACING_LEFT, PaneConstants.LEFT_SPACE);
		// contentStyle.put(mxConstants.STYLE_SPACING_TOP, 1);
		contentStyle.put(mxConstants.STYLE_STROKECOLOR, "none");

		graph.getStylesheet().putCellStyle("content", contentStyle);

		Map<String, Object> rowStyle = new HashMap<String, Object>(textStyle);
		rowStyle.put(mxConstants.STYLE_HORIZONTAL, false);
		rowStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_SWIMLANE);
		rowStyle.put(mxConstants.STYLE_FOLDABLE, true);
		rowStyle.put(mxConstants.STYLE_STARTSIZE, PaneConstants.SIGN_SIZE);
		rowStyle.put(mxConstants.STYLE_SPACING_TOP, PaneConstants.TOP_SPACE);
		rowStyle.put(mxConstants.STYLE_SWIMLANE_LINE, 0);

		graph.getStylesheet().putCellStyle("row", rowStyle);

		Map<String, Object> labelStyle = new HashMap<String, Object>(rowStyle);
		labelStyle.put(mxConstants.STYLE_STARTSIZE, PaneConstants.START_SIZE);
		labelStyle.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_LEFT);
		labelStyle.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_BOTTOM);
		labelStyle.put(mxConstants.STYLE_FONTSIZE, PaneConstants.CONTENT_FONT);
		labelStyle.put(mxConstants.STYLE_FONTSTYLE, mxConstants.FONT_BOLD);
		labelStyle.put(mxConstants.STYLE_HORIZONTAL, true);
		labelStyle.put(mxConstants.STYLE_SWIMLANE_LINE, 0);
		labelStyle.put(mxConstants.STYLE_FOLDABLE, false);
		graph.getStylesheet().putCellStyle("label", labelStyle);

		Map<String, Object> rangeStyle = new HashMap<String, Object>(style);
		rangeStyle.put(mxConstants.STYLE_STROKECOLOR, "none");
		graph.getStylesheet().putCellStyle("range", rangeStyle);

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
						geo.setWidth(geo.getWidth());
						String str = model.getStyle(cells[i]);
						if (graph.isCellCollapsed(cells[i])) {
							// set the style when folded
							// cells[i] must be of style "rowi"
							mxCell c = (mxCell) cells[i];
							int idx = Integer.parseInt(c.getId());
							int from = group.get(idx)._1;
							int to = group.get(idx)._2;

							int threadIdx = path.get(from).getThreadIndex();
							Map<String, Object> style = graph.getStylesheet().getStyles().get(str);
							style.put(mxConstants.STYLE_STARTSIZE, PaneConstants.START_SIZE);
							style.put(mxConstants.STYLE_HORIZONTAL, true);
							style.put(mxConstants.STYLE_ALIGN, "left");
							style.put(mxConstants.STYLE_SPACING_LEFT, threadIdx * cellWidth + PaneConstants.SIGN_SIZE
									+ PaneConstants.RANGE_SIZE + PaneConstants.LEFT_SPACE);
							// style.put(mxConstants.STYLE_SWIMLANE_LINE, 0);
							style.put(mxConstants.STYLE_SPACING_TOP,
									PaneConstants.START_SIZE - PaneConstants.TOP_SPACE);
							String s = detailList.get(idx);
							s = s.replaceAll("gov.nasa.jpf.vm.*?\\n", "").replaceAll("\\[.*?\\n", "");
							String[] strs = s.split("\\n");
							String first;
							String last;
							StringBuilder sb = new StringBuilder();
							if (from != to) {
								sb.append("Tr. " + from + "-" + to);
							} else {
								sb.append("Tr. " + from);
							}
							if (strs.length > 0) {
								first = strs[0].replaceAll("^.*?:.*?:", "");
								sb.append("\n" + Left.format(first, 20));
								if (strs.length > 1) {
									last = strs[strs.length - 1].replaceAll("^.*?:.*?:", "");

									if (strs.length > 2) {
										sb.append("\n...");
									}
									sb.append("\n" + Left.format(last, 20));
								}

								c.setValue(sb.toString());

							}
						} else {
							// set back the style of the expanded cell
							Map<String, Object> style = graph.getStylesheet().getStyles().get(str);
							style.put(mxConstants.STYLE_HORIZONTAL, false);
							// style.put(mxConstants.STYLE_SWIMLANE_LINE, 1);
							style.remove(mxConstants.STYLE_ALIGN);
							style.put(mxConstants.STYLE_SPACING_TOP, PaneConstants.TOP_SPACE);
							style.remove(mxConstants.STYLE_SPACING_LEFT);
							style.put(mxConstants.STYLE_STARTSIZE, PaneConstants.SIGN_SIZE);

							mxCell c = (mxCell) cells[i];
							c.setValue("");
						}
					}

				}
			}
		};

		graph.addListener(mxEvent.FOLD_CELLS, foldingHandler);

		// while folding, the lower cells goes up
		mxLayoutManager layoutMng = new mxLayoutManager(graph) {
			public mxIGraphLayout getLayout(Object parent) {
				String st = model.getStyle(parent);
				if (model.getChildCount(parent) > 0 && ((st == null) || (st != null) && (!st.contains("row")))) {
					return new mxStackLayout(graph, false);
				} else if (model.getChildCount(parent) > 0 && (st != null) && st.contains("row")) {
					return new mxStackLayout(graph, true);
				}
				return null;
			}

		};

		Object parent = graph.getDefaultParent();

		model.beginUpdate();
		try {

			// show the details
			for (int ithRow = 0; ithRow < numOfRows; ithRow++) {
				int from = group.get(ithRow)._1;
				int to = group.get(ithRow)._2;

				// set the id of lane to be the group idx
				Map<String, Object> tmpStyle = new HashMap<>(rowStyle);
				graph.getStylesheet().putCellStyle("row" + ithRow, tmpStyle);

				mxCell lane = (mxCell) graph.insertVertex(parent, "" + ithRow, null, 0, 0,
						numOfThreads * cellWidth + PaneConstants.RANGE_SIZE + PaneConstants.SIGN_SIZE, 0,
						"row" + ithRow);

				lane.setId("" + ithRow);
				lane.setConnectable(false);

				int threadIdx = path.get(from).getThreadIndex();
				Map<String, Object> tmpLabel = new HashMap<String, Object>(labelStyle);
				tmpLabel.put(mxConstants.STYLE_SPACING_LEFT,
						cellWidth * threadIdx + cellWidth / 2 - PaneConstants.LEFT_SPACE / 2);
				graph.getStylesheet().putCellStyle("label" + ithRow, tmpLabel);

				int htPerLine = mxUtils.getFontMetrics(mxUtils.getFont(tmpLabel)).getHeight();
				int currHt = heightList.get(ithRow) * htPerLine + 5;

				mxCell range;

				if (from != to) {
					range = (mxCell) graph.insertVertex(lane, null, from + "⋮" + to, 0, 0, PaneConstants.RANGE_SIZE,
							PaneConstants.START_SIZE + currHt, "range");
				} else {
					range = (mxCell) graph.insertVertex(lane, null, from, 0, 0, PaneConstants.RANGE_SIZE,
							PaneConstants.START_SIZE + currHt, "range");
				}
				mxCell labelRow = (mxCell) graph.insertVertex(lane, null, "" + threadIdx, 0, 0,
						numOfThreads * cellWidth, PaneConstants.START_SIZE + currHt, "label" + ithRow);
				labelRow.setConnectable(false);

				/**
				 * draw the line one at a time
				 */
				String[] contentLines = detailList.get(ithRow).split("\\n");
				for (int ithLine = 0; ithLine < contentLines.length; ithLine++) {
					mxCell content = (mxCell) graph.insertVertex(labelRow, "" + ithLine, contentLines[ithLine], 0, 0,
							numOfThreads * cellWidth, htPerLine, "content");
					content.setConnectable(false);
					content.setId("" + ithLine);
				}
				// mxCell content = (mxCell) graph.insertVertex(labelRow, null,
				// detailList.get(ithRow), 0, 0,
				// numOfThreads * cellWidth, currHt, "content");
				// content.setConnectable(false);

			}

			// set the collapsed height i.e. the folded height
			for (Object o : graph.getChildCells(parent)) {
				mxCell cell = (mxCell) o;
				if (cell != null && cell.getId() != null) {
					model.getGeometry(cell)
							.setAlternateBounds(new mxRectangle(0, 0,
									numOfThreads * cellWidth + PaneConstants.SIGN_SIZE + PaneConstants.RANGE_SIZE,
									PaneConstants.ALTER_SIZE));
					graph.foldCells(true, false, new Object[] { cell }, true);
				}
			}
		} finally {
			model.endUpdate();
		}
	}

	public mxGraph getGraph() {
		return graph;
	}

	public void resize(int newCellWidth) {
		this.cellWidth = newCellWidth;
		Object parent = graph.getDefaultParent();
		for (Object objLane : graph.getChildCells(parent)) {
			if (objLane != null) {
				String str = model.getStyle(objLane);
				Map<String, Object> style = graph.getStylesheet().getStyles().get(str);
				mxCell c = (mxCell) objLane;
				int idx = Integer.parseInt(c.getId());
				int from = group.get(idx)._1;
				int threadIdx = path.get(from).getThreadIndex();
				graph.resizeCell(objLane,
						new mxRectangle(0, 0,
								numOfThreads * cellWidth + PaneConstants.RANGE_SIZE + PaneConstants.SIGN_SIZE,
								graph.getCellGeometry(objLane).getHeight()));
				// System.out.println(numOfThreads * cellWidth +
				// PaneConstants.RANGE_SIZE + PaneConstants.SIGN_SIZE);
				model.getGeometry(objLane).getAlternateBounds()
						.setWidth(numOfThreads * cellWidth + PaneConstants.RANGE_SIZE + PaneConstants.SIGN_SIZE);
				if (graph.isCellCollapsed(objLane)) {
					style.put(mxConstants.STYLE_SPACING_LEFT, threadIdx * cellWidth + PaneConstants.SIGN_SIZE
							+ PaneConstants.RANGE_SIZE + PaneConstants.LEFT_SPACE);
				}
				for (Object objLabel : graph.getChildCells(objLane)) {
					if (objLabel != null && model.getStyle(objLabel) != "range") {
						String tmpStr = model.getStyle(objLabel);
						Map<String, Object> tmpStyle = graph.getStylesheet().getStyles().get(tmpStr);
						tmpStyle.put(mxConstants.STYLE_SPACING_LEFT,
								cellWidth * threadIdx + cellWidth / 2 - PaneConstants.LEFT_SPACE / 2);
						graph.resizeCell(objLabel, new mxRectangle(0, 0, numOfThreads * cellWidth,
								graph.getCellGeometry(objLabel).getHeight()));
						for (Object objContent : graph.getChildCells(objLabel)) {
							if (objContent != null) {
								graph.resizeCell(objContent, new mxRectangle(0, 0, numOfThreads * cellWidth,
										graph.getCellGeometry(objContent).getHeight()));

							}
						}
					}
				}
			}
		}
		graph.refresh();
	}

	public void expand(Set<Pair<Integer, Integer>> set, String color) {
		// System.out.println("xxxx");

		Set<Integer> expandedRows = new HashSet<>();
		for (Pair<Integer, Integer> p : set) {
			expandedRows.add(p._1);
		}
		Map<Integer, Set<Integer>> map = new HashMap<>();
		for (Pair<Integer, Integer> p : set) {
			int rowNum = p._1;
			if (map.containsKey(rowNum)) {
				map.get(rowNum).add(p._2);
			} else {
				Set<Integer> newSet = new HashSet<>();
				newSet.add(p._2);
				map.put(rowNum, newSet);
			}
		}

		Object parent = graph.getDefaultParent();
		for (Object o : graph.getChildCells(parent)) {
			mxCell cell = (mxCell) o;
			// System.out.println(cell.getId());

			if (cell != null && cell.getId() != null) {
				// System.out.println("row");
				int id = Integer.parseInt(cell.getId());

				// System.out.println("origi = " + model.getGeometry(cell));
				// System.out.println("alter = " +
				// model.getGeometry(cell).getAlternateBounds());

				if (expandedRows.contains(id)) {
					graph.foldCells(false, false, new Object[] { cell }, true);
					for (Object rowChild : graph.getChildCells(cell)) {
						mxCell rowChildCell = (mxCell) rowChild;
						if (rowChildCell.getStyle() != "range") {
							for (Object contentObj : graph.getChildCells(rowChildCell)) {
								mxCell contentCell = (mxCell) contentObj;
								if (contentCell != null && contentCell.getId() != null) {
									int cid = Integer.parseInt(contentCell.getId());
									// System.out.println("hl in progress" +
									// cid);
									// System.out.println("map contains: " +
									// map.get(id));
									if (map.get(id).contains(cid)) {
										Map<String, Object> hlStyle = new HashMap<>(
												graph.getStylesheet().getStyles().get(contentCell.getStyle()));
										hlStyle.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, color);
										graph.getStylesheet().putCellStyle("highlight"+color, hlStyle);
										contentCell.setStyle("highlight"+color);
									}

								}
							}

						}

					}

				} 

				// System.out.println("alter2 = " +
				// model.getGeometry(cell).getAlternateBounds());
			}
		}
		graph.refresh();
	}

	public void foldAll() {
		// set the collapsed height i.e. the folded height
		Object parent = graph.getDefaultParent();
		for (Object o : graph.getChildCells(parent)) {
			mxCell cell = (mxCell) o;
			if (cell != null && cell.getId() != null) {
				graph.foldCells(true, false, new Object[] { cell }, true);
				/**
				 * reset the highlight
				 */
				for (Object rowChild : graph.getChildCells(cell)) {
					mxCell rowChildCell = (mxCell) rowChild;
					if (rowChildCell.getStyle() != "range") {
						for (Object contentObj : graph.getChildCells(rowChildCell)) {
							mxCell contentCell = (mxCell) contentObj;
							if (contentCell != null && contentCell.getId() != null) {
								contentCell.setStyle("content");
							}
						}
					}
				}
			}
		}
		graph.refresh();

	}

}
