import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.layout.mxStackLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxLayoutManager;

import gov.nasa.jpf.util.Pair;
import gov.nasa.jpf.vm.Path;

public class NewContent {
	private double cellWidth = 0;// PaneConstants.DEFAULT_CELL_WIDTH;
	private mxGraph graph;
	private mxIGraphModel model;

	private int numOfThreads = -1;
	private Path path;
	private List<Pair<Integer, Integer>> group = new ArrayList<>();
	private Map<Integer, TextLineList> lineTable;
	private double htPerLine;
	private double wtPerLine = 7;

	private LocationInGraph location = new LocationInGraph();

	public NewContent(double width, int nThreads, Path p, List<Pair<Integer, Integer>> grp,
			Map<Integer, TextLineList> lt) {
		this.lineTable = lt;
		this.numOfThreads = nThreads;
		this.group = grp;
		this.path = p;
		this.cellWidth = width;
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
		graph.setCellsSelectable(true);
		graph.setCellsResizable(false);
		// graph.setCellsLocked(value);

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

		Map<String, Object> rightStyle = new HashMap<>(style);
		graph.getStylesheet().putCellStyle("right", rightStyle);

		Map<String, Object> borderStyle = new HashMap<>(style);
		graph.getStylesheet().putCellStyle("border", borderStyle);

		Map<String, Object> rangeStyle = new HashMap<String, Object>(style);
		rangeStyle.put(mxConstants.STYLE_SPACING_LEFT, PaneConstants.LEFT_SPACE);
		rangeStyle.put(mxConstants.STYLE_STROKECOLOR, "none");
		graph.getStylesheet().putCellStyle("range", rangeStyle);

		Map<String, Object> swimStyle = new HashMap<String, Object>(style);
		swimStyle.remove(mxConstants.STYLE_VERTICAL_ALIGN);
		swimStyle.put(mxConstants.STYLE_HORIZONTAL, false);
		swimStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_SWIMLANE);
		swimStyle.put(mxConstants.STYLE_FOLDABLE, true);
		swimStyle.put(mxConstants.STYLE_STROKECOLOR, "none");
		swimStyle.put(mxConstants.STYLE_STARTSIZE, PaneConstants.SIGN_SIZE);
		swimStyle.put(mxConstants.STYLE_SPACING_TOP, PaneConstants.TOP_SPACE);
		swimStyle.put(mxConstants.STYLE_SWIMLANE_LINE, 0);
		graph.getStylesheet().putCellStyle("swim", swimStyle);

		Map<String, Object> contentStyle = new HashMap<String, Object>(style);
		contentStyle.put(mxConstants.STYLE_STROKECOLOR, "none");
		contentStyle.put(mxConstants.STYLE_OPACITY, 0);
		contentStyle.put(mxConstants.STYLE_FILL_OPACITY, 0);
		contentStyle.put(mxConstants.STYLE_SPACING_TOP, PaneConstants.TOP_SPACE);
		contentStyle.remove(mxConstants.STYLE_VERTICAL_ALIGN);
		contentStyle.put(mxConstants.STYLE_FONTFAMILY, "Courier");
		graph.getStylesheet().putCellStyle("content", contentStyle);

		mxIEventListener foldingHandler = new mxIEventListener() {
			@Override
			public void invoke(Object sender, mxEventObject evt) {
				Object[] cells = (Object[]) evt.getProperty("cells");
				for (int i = 0; i < cells.length; i++) {
					if (graph.isCellCollapsed(cells[i])) {
						mxCell c = (mxCell) cells[i];// swim
						// swim's parent is rightCell
						mxCell rightCell = (mxCell) c.getParent();
						double tmpHt = 0;
						if (model.getChildCount(rightCell) > 1) {
							mxCell cell = (mxCell) model.getChildAt(rightCell, 1);
							cell.setVisible(true);
							tmpHt = cell.getGeometry().getHeight();
						}

						model.getGeometry(rightCell).setHeight(tmpHt);

						mxCell rowCell = (mxCell) rightCell.getParent();
						model.getGeometry(rowCell).setHeight(tmpHt);

					} else {
						mxCell c = (mxCell) cells[i];// swim
						if (model.getChildCount(c.getParent()) > 1) {
							mxCell cell = (mxCell) model.getChildAt(c.getParent(), 1);
							cell.setVisible(false);
						}
						// swim's parent is rightCell
						mxCell rightCell = (mxCell) c.getParent();
						int ithRow = Integer.parseInt(rightCell.getId());
						htPerLine = mxUtils.getFontMetrics(mxUtils.getFont(contentStyle)).getHeight() + 5;
						double currHt = (lineTable.get(ithRow).getHeight() + 1) * htPerLine + 7 + 10;
						model.getGeometry(rightCell).setHeight(currHt);

						mxCell rowCell = (mxCell) rightCell.getParent();
						model.getGeometry(rowCell).setHeight(currHt);
					}
				}
				graph.refresh();
			}
		};
		graph.addListener(mxEvent.FOLD_CELLS, foldingHandler);

		@SuppressWarnings("unused")
		mxLayoutManager layoutMng = new mxLayoutManager(graph) {
			public mxIGraphLayout getLayout(Object parent) {
				String st = model.getStyle(parent);

				if (model.getChildCount(parent) > 0 && (st == null || st.contains("swim") || st == "summary")) {
					// vertical
					return new mxStackLayout(graph, false);
				} else {
					// horizontal
					return new mxStackLayout(graph, true);
				}
			}

		};

		Object parent = graph.getDefaultParent();

		model.beginUpdate();
		try {
			// show the details
			for (int ithRow = 0; ithRow < numOfRows; ithRow++) {

				if (lineTable.get(ithRow).isNoSrc()) {
					continue;
				}

				int from = group.get(ithRow)._1;
				int to = group.get(ithRow)._2;

				TextLineList lineList = lineTable.get(ithRow);
				htPerLine = mxUtils.getFontMetrics(mxUtils.getFont(contentStyle)).getHeight() + 5;
				double currHt = (lineList.getHeight() + 1) * htPerLine + 5 + 10;

				/**
				 * The big box around the first row with black border
				 */
				mxCell rowCell = (mxCell) graph.insertVertex(parent, null, null, 0, 0,
						PaneConstants.SIGN_SIZE + PaneConstants.RANGE_SIZE + numOfThreads * cellWidth, 0, "border");
				rowCell.setConnectable(false);

				/**
				 * The transition range no border
				 */
				String rangeStr = null;
				if (from != to) {
					rangeStr = from + "-" + to;
				} else {
					rangeStr = "" + from;
				}
				mxCell rangeCell = (mxCell) graph.insertVertex(rowCell, null, rangeStr, 0, 0, PaneConstants.RANGE_SIZE,
						PaneConstants.START_SIZE, "range");
				rangeCell.setConnectable(false);

				/**
				 * The big box outside the swimlane
				 */
				mxCell rightCell = (mxCell) graph.insertVertex(rowCell, null, null, 0, 0,
						PaneConstants.SIGN_SIZE + numOfThreads * cellWidth, 0, "right");
				rightCell.setId("" + ithRow);
				rightCell.setConnectable(false);

				/**
				 * The swimlane
				 */
				// set the id of lane to be the group idx
				Map<String, Object> swimStyleI = new HashMap<>(swimStyle);
				graph.getStylesheet().putCellStyle("swim" + ithRow, swimStyleI);
				mxCell swimCell = (mxCell) graph.insertVertex(rightCell, null, null, 0, 0,
						numOfThreads * cellWidth + PaneConstants.SIGN_SIZE, currHt, "swim" + ithRow);
				swimCell.setConnectable(false);
				swimCell.setId("" + ithRow);
				location.addSwimCell(ithRow, swimCell);
				graph.foldCells(true, false, new Object[] { swimCell }, true);

				/**
				 * The thread label
				 */
				int threadIdx = path.get(from).getThreadIndex();
				Map<String, Object> threadLabel = new HashMap<String, Object>(contentStyle);
				threadLabel.put(mxConstants.STYLE_SPACING_LEFT,
						cellWidth * threadIdx + cellWidth / 2 - PaneConstants.LEFT_SPACE / 2);
				graph.getStylesheet().putCellStyle("thread" + ithRow, threadLabel);

				mxCell threadRow = (mxCell) graph.insertVertex(swimCell, null, "" + threadIdx, 0, 0,
						numOfThreads * cellWidth, htPerLine + 10, "thread" + ithRow);
				threadRow.setId("" + threadIdx);
				threadRow.setConnectable(false);

				/**
				 * draw the detail line one at a time
				 */
				List<TextLine> txtLines = lineList.getList();

				// String javaCode = detailList.get(ithRow);

				// String[] contentLines = javaCode.split("\\n");
				for (int ithLine = 0; ithLine < txtLines.size(); ithLine++) {
					TextLine txt = txtLines.get(ithLine);
					String txtStr = txt.getText();
					if (txt.isSrc()) {
						txtStr = " " + txt.getLocationString() + ": " + txtStr;
					}
					double txtWidth = numOfThreads * cellWidth;
					mxCell contentBox = (mxCell) graph.insertVertex(swimCell, "" + ithLine, null, 0, 0, txtWidth, 0,
							"content");
					contentBox.setConnectable(false);
					contentBox.setId("" + ithLine);

					txtWidth = Math.min(txtStr.length() * wtPerLine, txtWidth);
					mxCell content = (mxCell) graph.insertVertex(contentBox, "" + ithLine, txtStr, 0, 0, txtWidth,
							htPerLine, "content");
					content.setConnectable(false);
					content.setId("" + ithLine);
					location.addContentCell(ithRow, ithLine, contentBox);
				}

				/**
				 * draw summary line one at a time; hide those lines
				 */
				Map<String, Object> summaryStyle = new HashMap<String, Object>(contentStyle);
				graph.getStylesheet().putCellStyle("summary", summaryStyle);
				mxCell summaryCell = (mxCell) graph.insertVertex(rightCell, null, null, 0, 0, numOfThreads * cellWidth,
						0, "summary");
				summaryCell.setConnectable(false);
				summaryCell.setId("" + threadIdx);

				Map<String, Object> summaryContentStyle = new HashMap<String, Object>(contentStyle);
				summaryContentStyle.put(mxConstants.STYLE_SPACING_LEFT, threadIdx * cellWidth);
				graph.getStylesheet().putCellStyle("summaryContent" + ithRow, summaryContentStyle);

				boolean srcInBetween = false;
				int sumNum = 0;
				SummaryCell prevCell = null;
				for (TextLine tl : lineTable.get(ithRow).getList()) {
					
//					if(tl.isSrc()){
//						double sumWt = Math.min(tl.getText().length() * wtPerLine + threadIdx * cellWidth,
//								numOfThreads * cellWidth);
//						mxCell summaryBox = (mxCell) graph.insertVertex(summaryCell, null, null, 0, 0,
//								numOfThreads * cellWidth, htPerLine, "content");
//						summaryBox.setId(tl.getLineNum() + "");
//						summaryBox.setConnectable(false);
//						
//						mxCell summaryContent = (mxCell) graph.insertVertex(summaryBox, null, tl.getText(), 0, 0, sumWt,
//								htPerLine, "summaryContent" + ithRow);
//						summaryContent.setId(tl.getLineNum() + "");
//						summaryContent.setConnectable(false);
//						
//						sumNum++;
//						
//						
//						mxCell summaryDots = (mxCell) graph.insertVertex(summaryCell, null, "...", 0, 0,
//								numOfThreads * cellWidth, htPerLine, "summaryContent" + ithRow);
//						summaryDots.setId(-1 + "");
//						summaryDots.setConnectable(false);
//						
//						SummaryCell sCell= new SummaryCell(summaryBox);
//						
//						sCell.setNextDots(summaryDots);
//						
//						if(prevCell != null){
//							sCell.setPrevSrc(prevCell.getSummary());
//							sCell.setPrevDots(prevCell.getNextDots());
//							prevCell.setNextSrc(summaryBox);
//						}
//						
//						prevCell = sCell;
//						
//					}
					
					if (tl.isFirst() || tl.isLast()) {
						double sumWt = Math.min(tl.getText().length() * wtPerLine + threadIdx * cellWidth,
								numOfThreads * cellWidth);
						if (srcInBetween) {
							mxCell summaryContent = (mxCell) graph.insertVertex(summaryCell, null, "...", 0, 0,
									numOfThreads * cellWidth, htPerLine, "summaryContent" + ithRow);
							summaryContent.setId(-1 + "");
							summaryContent.setConnectable(false);
							sumNum++;
							srcInBetween = false;
						}
						mxCell summaryBox = (mxCell) graph.insertVertex(summaryCell, null, null, 0, 0,
								numOfThreads * cellWidth, htPerLine, "content");
						summaryBox.setId(tl.getLineNum() + "");
						summaryBox.setConnectable(false);
						mxCell summaryContent = (mxCell) graph.insertVertex(summaryBox, null, tl.getText(), 0, 0, sumWt,
								htPerLine, "summaryContent" + ithRow);
						summaryContent.setId(tl.getLineNum() + "");
						summaryContent.setConnectable(false);
						sumNum++;
					} else if (tl.isSrc()) {
						srcInBetween = true;
					}
				}
				double sumHt = sumNum * htPerLine + 10;
				model.getGeometry(summaryCell).setHeight(sumHt);
				summaryCell.setVisible(false);

				model.getGeometry(swimCell).setAlternateBounds(new mxRectangle(0, 0, PaneConstants.SIGN_SIZE, sumHt));
			}
		} finally {
			model.endUpdate();
		}

		foldAll(true);
	}

	public mxGraph getGraph() {
		return graph;
	}

	private void resizeSwimCell(Object swimCell) {
		if (!graph.isCellCollapsed(swimCell)) {
			model.getGeometry(swimCell).setWidth(numOfThreads * cellWidth + PaneConstants.SIGN_SIZE);
		} else {
			model.getGeometry(swimCell).getAlternateBounds()
					.setWidth(numOfThreads * cellWidth + PaneConstants.SIGN_SIZE);
		}
		for (Object swimContent : graph.getChildCells(swimCell)) {
			model.getGeometry(swimContent).setWidth(numOfThreads * cellWidth);
			String st = model.getStyle(swimContent);
			if (st.contains("thread")) {
				Map<String, Object> tmpStyle = graph.getStylesheet().getStyles().get(st);
				int threadIdx = Integer.parseInt(((mxCell) swimContent).getId());
				tmpStyle.put(mxConstants.STYLE_SPACING_LEFT,
						cellWidth * threadIdx + cellWidth / 2 - PaneConstants.LEFT_SPACE / 2);
			}
		}
	}

	private void resizeSummaryCell(Object summaryCell) {
		int threadIdx = Integer.parseInt(((mxCell) summaryCell).getId());
		model.getGeometry(summaryCell).setWidth(numOfThreads * cellWidth);
		for (Object summaryBox : graph.getChildCells(summaryCell)) {
			String tmpStr = model.getStyle(summaryBox);
			if (tmpStr.contains("summary")) {
				Map<String, Object> tmpStyle = graph.getStylesheet().getStyles().get(tmpStr);
				tmpStyle.put(mxConstants.STYLE_SPACING_LEFT, threadIdx * cellWidth);
			}
			model.getGeometry(summaryBox).setWidth(numOfThreads * cellWidth);
			for (Object summaryContent : graph.getChildCells(summaryBox)) {
				String tmpStr2 = model.getStyle(summaryContent);
				Map<String, Object> tmpStyle = graph.getStylesheet().getStyles().get(tmpStr2);
				tmpStyle.put(mxConstants.STYLE_SPACING_LEFT, threadIdx * cellWidth);

			}
		}
	}

	private void resizeRightCell(Object rightCell) {
		model.getGeometry(rightCell).setWidth(PaneConstants.SIGN_SIZE + numOfThreads * cellWidth);
		for (int swi = 0; swi < model.getChildCount(rightCell); swi++) {
			Object swimCell = model.getChildAt(rightCell, swi);
			if (model.getStyle(swimCell).contains("swim")) {
				resizeSwimCell(swimCell);
			} else {
				resizeSummaryCell(swimCell);
			}
		}
	}

	public void resize(double newCellWidth) {
		this.cellWidth = newCellWidth;
		Object parent = graph.getDefaultParent();
		for (Object rowCell : graph.getChildCells(parent)) {
			model.getGeometry(rowCell)
					.setWidth(PaneConstants.SIGN_SIZE + PaneConstants.RANGE_SIZE + numOfThreads * cellWidth);
			for (Object rightCell : graph.getChildCells(rowCell)) {
				if (!model.getStyle(rightCell).contains("range")) {
					resizeRightCell(rightCell);
				}
			}
		}
		graph.refresh();
	}

	private void highlightContentBox(Object contentBox, boolean reset, String color, TextLine tl) {
		for (int cb1 = 0; cb1 < model.getChildCount(contentBox); cb1++) {
			// content and color block
			mxCell contentCell = (mxCell) model.getChildAt(contentBox, cb1);
			if (((String) contentCell.getValue()).length() > 1) {
				// the content
				if (reset) {
					tl.resetHighlight(color);
					if (contentCell.getStyle().equals("highlight" + color)) {
						if (!tl.isHighlighted()) {
							contentCell.setStyle("content");
						} else {
							contentCell.setStyle("highlight" + tl.getOneColor());
						}
					}
				} else {
					if (tl.isHighlightedColor(color)) {
						break;
					}
					Map<String, Object> hlStyle = new HashMap<>(graph.getStylesheet().getStyles().get("content"));
					hlStyle.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, color);
					graph.getStylesheet().putCellStyle("highlight" + color, hlStyle);
					if (!tl.isHighlighted()) {
						contentCell.setStyle("highlight" + color);
					} else {
						// add a color block
						mxCell colorBlock = (mxCell) graph.insertVertex(contentBox, null, " ", 0, 0, 5, htPerLine,
								"highlight" + color);
						colorBlock.setConnectable(false);
					}
					tl.setHighlight(color);
				}

			} else {
				// the color block
				// reset
				if (reset && contentCell.getStyle().contains(color)) {
					model.remove(contentCell);
				}
			}
		}
	}

	private void expandSwimLane(Set<Integer> expandedRows, Map<Integer, Set<Integer>> map, int ithRow, boolean reset,
			Object swimCell, String color) {
		if (expandedRows.contains(ithRow)) {
			if (!reset) {
				// not reset, expand
				graph.foldCells(false, false, new Object[] { swimCell }, true);
			}
			for (Object contentBox : graph.getChildCells(swimCell)) {
				// deal with content lines
				if (model.getStyle(contentBox) == "content") {
					int lineNum = Integer.parseInt(((mxCell) contentBox).getId());

					if (map.get(ithRow).contains(lineNum)) {

						TextLine tl = lineTable.get(ithRow).getList().get(lineNum);
						if (!tl.isSrc()) {
							continue;
						}

						if (!reset && tl.isHighlightedColor(color)) {
							continue;
						}

						highlightContentBox(contentBox, reset, color, tl);
					}
				}
			}
		}
	}

	private void highlightSummaryCell(Set<Integer> expandedRows, Map<Integer, Set<Integer>> map, int ithRow,
			boolean reset, Object summaryCell, String color, Object rightCell, Object rowCell) {
		if (expandedRows.contains(ithRow)) {
			
			long t1 = System.nanoTime();
			int threadIdx = Integer.parseInt(((mxCell) summaryCell).getId());
			Map<String, Object> sumContent = graph.getStylesheet().getStyles().get("summaryContent" + ithRow);
			Map<String, Object> sumContentStyle = new HashMap<>(sumContent);

			sumContentStyle.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, color);
			graph.getStylesheet().putCellStyle("sumhighlight" + ithRow + color, sumContentStyle);

			// remove all the contents
			while (((mxCell) summaryCell).getChildCount() != 0) {
				((mxCell) summaryCell).remove(0);
			}

			// summaryCell
			int summaryLineNum = 0;
			boolean srcInBetween = false;
			boolean cgInBetween = false;
			String lastLine = null;
			String styleStr = "summaryContent" + ithRow;

			
			long t2 = System.nanoTime();
			for (TextLine tl : lineTable.get(ithRow).getList()) {
				if (!tl.isSrc()) {
					cgInBetween = true;
					continue;
				}
				double sumWt = Math.min(tl.getText().length() * wtPerLine + threadIdx * cellWidth,
						numOfThreads * cellWidth);
				int lineNum = tl.getLineNum();
				if (tl.isFirst() || tl.isLast()) {
					if (srcInBetween) {
						mxCell summaryContent = (mxCell) graph.insertVertex(summaryCell, null, "...", 0, 0,
								numOfThreads * cellWidth, htPerLine, styleStr);
						summaryContent.setConnectable(false);
						summaryContent.setId("-1");
						summaryLineNum++;
						srcInBetween = false;
					} else if (cgInBetween && lastLine != null && lastLine.equals(tl.getText())) {
						cgInBetween = false;
						srcInBetween = false;
						continue;
					}

					if (tl.isHighlighted()) {
						mxCell summaryBox = (mxCell) graph.insertVertex(summaryCell, null, null, 0, 0,
								numOfThreads * cellWidth, htPerLine, "content");
						summaryBox.setId(lineNum + "");
						summaryBox.setConnectable(false);

						mxCell summaryContent = (mxCell) graph.insertVertex(summaryBox, null, tl.getText(), 0, 0, sumWt,
								htPerLine, "sumhighlight" + ithRow + tl.getOneColor());
						summaryContent.setId(lineNum + "");
						summaryContent.setConnectable(false);

						for (String c : tl.getAllHighlight()) {
							if (!c.equals(tl.getOneColor())) {
								mxCell sumBlock = (mxCell) graph.insertVertex(summaryBox, null, " ", 0, 0, 5, htPerLine,
										"highlight" + c);
								sumBlock.setConnectable(false);
							}
						}

					} else {
						mxCell summaryBox = (mxCell) graph.insertVertex(summaryCell, null, null, 0, 0,
								numOfThreads * cellWidth, htPerLine, "content");
						summaryBox.setId(lineNum + "");
						summaryBox.setConnectable(false);
						mxCell summaryContent = (mxCell) graph.insertVertex(summaryBox, null, tl.getText(), 0, 0, sumWt,
								htPerLine, styleStr);
						summaryContent.setId(lineNum + "");
						summaryContent.setConnectable(false);
					}
					summaryLineNum++;
					lastLine = tl.getText();
				} else if (tl.isHighlighted()) {
					long y1 = System.nanoTime();
					if (srcInBetween) {
						mxCell summaryContent = (mxCell) graph.insertVertex(summaryCell, null, "...", 0, 0,
								numOfThreads * cellWidth, htPerLine, styleStr);
						summaryContent.setId("-1");
						summaryContent.setConnectable(false);
						summaryLineNum++;
						srcInBetween = false;
					} else if (cgInBetween && lastLine != null && lastLine.equals(tl.getText())) {

						cgInBetween = false;
						srcInBetween = false;
						continue;
					}

					mxCell summaryBox = (mxCell) graph.insertVertex(summaryCell, null, null, 0, 0,
							numOfThreads * cellWidth, htPerLine, "content");
					summaryBox.setId(lineNum + "");
					summaryBox.setConnectable(false);

					mxCell summaryContent = (mxCell) graph.insertVertex(summaryBox, null, tl.getText(), 0, 0, sumWt,
							htPerLine, "sumhighlight" + ithRow + tl.getOneColor());
					summaryContent.setId(lineNum + "");
					summaryContent.setConnectable(false);

					for (String c : tl.getAllHighlight()) {
						if (!c.equals(tl.getOneColor())) {
							mxCell sumBlock = (mxCell) graph.insertVertex(summaryBox, null, " ", 0, 0, 5, htPerLine,
									"highlight" + c);
							sumBlock.setConnectable(false);
						}
					}
					lastLine = tl.getText();
					summaryLineNum++;
					

					long y2 = System.nanoTime();
					System.out.println("Y: " + (y2 - y1));

				} else if (tl.isSrc()) {
					srcInBetween = true;
				} else if (tl.isCG()) {
					cgInBetween = true;
				}
			}
			double tmpHt = htPerLine * summaryLineNum + 10;
			long t3 = System.nanoTime();

			model.getGeometry(summaryCell).setHeight(tmpHt);
			if (((mxCell) summaryCell).isVisible()) {
				model.getGeometry(rightCell).setHeight(tmpHt);
				model.getGeometry(rowCell).setHeight(tmpHt);
			}
			
			long t4 = System.nanoTime();
			System.out.println("x: " + (t2 - t1) + ", " + (t3 - t2) + ", " + (t4- t3) );
		}
	}

	public void expand(Set<Pair<Integer, Integer>> set, String color, boolean reset) {
		// for (Pair<Integer, Integer> p : set) {
		// int row = p._1;
		// int line = p._2;
		// TextLine tl = lineTable.get(row).getList().get(line);
		// Object contentBox = location.getContentCell(p);
		// if (!reset) {
		// graph.foldCells(false, false, new Object[] {
		// location.getSwimCell(row) });
		// Map<String, Object> hlStyle = new
		// HashMap<>(graph.getStylesheet().getStyles().get("content"));
		// hlStyle.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, color);
		// graph.getStylesheet().putCellStyle("highlight" + color, hlStyle);
		// if (!tl.isHighlighted()) {
		// mxCell content = (mxCell) graph.getChildCells(contentBox)[0];
		// content.setStyle("highlight" + color);
		// } else {
		// mxCell colorBlock = (mxCell) graph.insertVertex(contentBox, null, "
		// ", 0, 0, 5, htPerLine,
		// "highlight" + color);
		// colorBlock.setConnectable(false);
		// }
		// tl.resetHighlight(color);
		// } else {
		// tl.resetHighlight(color);
		// for (int i = 1; i < model.getChildCount(contentBox); i++) {
		// mxCell o = (mxCell) model.getChildAt(contentBox, i);
		// if (o.getStyle().equals("highlight" + color)) {
		// o.removeFromParent();
		// }
		// }
		//
		// if (!tl.isHighlighted()) {
		// mxCell content = (mxCell) graph.getChildCells(contentBox)[0];
		// content.setStyle("content");
		// }
		// }
		// }
		long t1 = System.nanoTime();
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
		
		long t11 = System.nanoTime();

		for (Object rowCell : graph.getChildCells(parent)) {
			for (Object rightCell : graph.getChildCells(rowCell)) {
				if (model.getStyle(rightCell) != "range") {
					int ithRow = Integer.parseInt(((mxCell) rightCell).getId());
					for (int swimi = 0; swimi < model.getChildCount(rightCell); swimi++) {
						Object swimCell = model.getChildAt(rightCell, swimi);
						if (model.getStyle(swimCell).contains("swim")) {

							// swimCell

							expandSwimLane(expandedRows, map, ithRow, reset, swimCell, color);

						} else {
							// summary cell -- summaryCell
							// redraw summary cell
							// may be slow and delayed

							highlightSummaryCell(expandedRows, map, ithRow, reset, swimCell, color, rightCell, rowCell);
						}
					}
				}
			}
		}
		long t2 = System.nanoTime();
		graph.refresh();
		long t3 = System.nanoTime();
		System.out.println((t11 - t1) + "," + (t2 - t11) + ", " + (t3 - t2));
		// resize(cellWidth);
	}

	public void foldAll(boolean b) {
		Object parent = graph.getDefaultParent();
		for (Object rowCell : graph.getChildCells(parent)) {
			for (Object rightCell : graph.getChildCells(rowCell)) {
				if (model.getStyle(rightCell) != "range") {
					for (Object swimCell : graph.getChildCells(rightCell)) {
						if (model.getStyle(swimCell).contains("swim")) {
							graph.foldCells(b, false, new Object[] { swimCell }, true);
						}
					}
				}
			}
		}
	}

}
