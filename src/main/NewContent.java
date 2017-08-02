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
					mxCell c = (mxCell) cells[i];// swim
					if (!c.getStyle().contains("swim")) {
						return;
					}
					if (graph.isCellCollapsed(cells[i])) {

						// swim's parent is rightCell
						mxCell rightCell = (mxCell) c.getParent();
						double tmpHt = 0;
						if (model.getChildCount(rightCell) > 2) {
							mxCell cell = (mxCell) model.getChildAt(rightCell, 1);
							mxCell cell2 = (mxCell) model.getChildAt(rightCell, 2);

							// System.out.println("visible " + cell.getStyle());

							cell.setVisible(true);
							cell2.setVisible(true);
							tmpHt = cell2.getGeometry().getHeight();
							System.out.println("fold and " + cell.getStyle() + " height = " + tmpHt);

						}
						model.getGeometry(rightCell).setHeight(tmpHt);

						mxCell rowCell = (mxCell) rightCell.getParent();
						model.getGeometry(rowCell).setHeight(tmpHt);

					} else {
						if (model.getChildCount(c.getParent()) > 2) {
							mxCell cell = (mxCell) model.getChildAt(c.getParent(), 1);
							mxCell cell2 = (mxCell) model.getChildAt(c.getParent(), 2);

							cell.setVisible(false);
							cell2.setVisible(false);
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

				if (!lineTable.containsKey(ithRow) || lineTable.get(ithRow).isNoSrc()) {
					lineTable.remove(ithRow);
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
				location.addRowCell(ithRow, rowCell);
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
				location.addRightCell(ithRow, rightCell);

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
				location.addThreadLabel(ithRow, threadRow);

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
					double txtWidth = txtStr.length() * wtPerLine;
					mxCell contentBox = (mxCell) graph.insertVertex(swimCell, "" + ithLine, null, 0, 0, txtWidth, 0,
							"content");
					contentBox.setConnectable(false);
					contentBox.setId("" + ithLine);

					mxCell content = (mxCell) graph.insertVertex(contentBox, "" + ithLine, txtStr, 0, 0, txtWidth,
							htPerLine, "content");
					content.setConnectable(false);
					content.setId("" + ithLine);
					location.addContentCell(ithRow, ithLine, contentBox);
				}

				/**
				 * draw summary line one at a time; hide those lines
				 */
				mxCell summaryBlank = (mxCell) graph.insertVertex(rightCell, null, null, 0, 0, threadIdx * cellWidth, 5,
						"content");
				summaryBlank.setConnectable(false);
				summaryBlank.setId("" + threadIdx);
				summaryBlank.setVisible(false);
				location.addSummaryBlank(ithRow, summaryBlank);

				Map<String, Object> summaryStyle = new HashMap<String, Object>(contentStyle);
				graph.getStylesheet().putCellStyle("summary", summaryStyle);

				mxCell summaryBorder = (mxCell) graph.insertVertex(rightCell, null, null, 0, 0,
						(numOfThreads - threadIdx) * cellWidth, 0, "summary");
				summaryBorder.setConnectable(false);
				summaryBorder.setId("" + threadIdx);
				location.addSummaryBorderCell(ithRow, summaryBorder);

				// Map<String, Object> summaryContentStyle = new HashMap<String,
				// Object>(contentStyle);
				// summaryContentStyle.put(mxConstants.STYLE_SPACING_LEFT,
				// threadIdx * cellWidth);
				// graph.getStylesheet().putCellStyle("summaryContent" + ithRow,
				// summaryContentStyle);

				boolean nonSrcInBetween = false;
				int sumNum = 0;
				SummaryCell prevCell = null;
				String prevTxt = null;

				for (TextLine tl : lineTable.get(ithRow).getList()) {

					if (tl.isSrc()) {
						if (prevTxt != null && nonSrcInBetween && prevTxt.equals(tl.getText())) {
							nonSrcInBetween = false;
							continue;
						}

						double sumWt = tl.getText().length() * wtPerLine;
						mxCell summaryBox = (mxCell) graph.insertVertex(summaryBorder, null, null, 0, 0, sumWt,
								htPerLine, "content");
						summaryBox.setId(tl.getLineNum() + "");
						summaryBox.setConnectable(false);

						mxCell summaryContent = (mxCell) graph.insertVertex(summaryBox, null, tl.getText(), 0, 0, sumWt,
								htPerLine, "content");// "summaryContent" +
														// ithRow);
						summaryContent.setId(tl.getLineNum() + "");
						summaryContent.setConnectable(false);

						sumNum++;
						summaryBox.setVisible(false);

						mxCell summaryDots = (mxCell) graph.insertVertex(summaryBorder, null, "...", 0, 0, 5, htPerLine,
								"content");// "summaryContent"
											// +
											// ithRow);
						summaryDots.setId(-1 + "");
						summaryDots.setConnectable(false);
						summaryDots.setVisible(false);

						SummaryCell sCell = new SummaryCell(summaryBox, threadIdx);

						sCell.setNextDots(summaryDots);

						if (prevCell != null) {
							sCell.setPreviousSummary(prevCell);
							sCell.setPrevSrc(prevCell.getSummary());
							sCell.setPrevDots(prevCell.getNextDots());
							prevCell.setNextSrc(summaryBox);
						}

						if (tl.isFirst()) {
							summaryBox.setVisible(true);
							sCell.setFirst(true);
						}

						location.addSummaryCell(ithRow, tl.getLineNum(), sCell);
						prevTxt = tl.getText();
						prevCell = sCell;

					} else {
						nonSrcInBetween = true;
					}
				}

				// is last line of src code
				prevCell.setLast(true);
				((mxCell) prevCell.getSummary()).setVisible(true);
				mxCell switchLine = (mxCell) graph.insertVertex(summaryBorder, null, null, 0, 0, 5, 0, "switch");
				switchLine.getGeometry().setAlternateBounds(new mxRectangle(0, 0, 0, 0));
				switchLine.setId(ithRow + "");
				switchLine.setConnectable(false);
				switchLine.setVisible(false);
				location.addSwitchCell(ithRow, switchLine);

				int numOfLines = 0;

				if (sumNum > 2) {
					((mxCell) model.getChildAt(summaryBorder, 1)).setVisible(true);
					numOfLines = 3;
				} else {
					numOfLines = sumNum;
				}

				double sumHt = numOfLines * htPerLine + 10;
				model.getGeometry(summaryBorder).setHeight(sumHt);
				summaryBorder.setVisible(false);

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

	public void resize(double newCellWidth) {
		this.cellWidth = newCellWidth;
		// resize row cells
		for (Object rowCell : location.getAllRowCells()) {
			model.getGeometry(rowCell)
					.setWidth(PaneConstants.SIGN_SIZE + PaneConstants.RANGE_SIZE + numOfThreads * cellWidth);
		}

		// resize right cells
		for (Object rightCell : location.getAllRightCells()) {
			model.getGeometry(rightCell).setWidth(PaneConstants.SIGN_SIZE + numOfThreads * cellWidth);
		}

		// resize thread labels
		for (Object threadLabel : location.getAllThreadLabels()) {
			model.getGeometry(threadLabel).setWidth(numOfThreads * cellWidth);
			String st = model.getStyle(threadLabel);
			Map<String, Object> tmpStyle = graph.getStylesheet().getStyles().get(st);
			int threadIdx = Integer.parseInt(((mxCell) threadLabel).getId());
			tmpStyle.put(mxConstants.STYLE_SPACING_LEFT,
					cellWidth * threadIdx + cellWidth / 2 - PaneConstants.LEFT_SPACE / 2);

		}

		// resize summary blanks
		for (Object summaryBlank : location.getAllSummaryBlanks()) {
			int threadIdx = Integer.parseInt(((mxCell) summaryBlank).getId());
			model.getGeometry(summaryBlank).setWidth(threadIdx * cellWidth);
		}

		// resize summary border cells
		for (Object summaryBorder : location.getAllSummaryBorderCells()) {
			int threadIdx = Integer.parseInt(((mxCell) summaryBorder).getId());
			model.getGeometry(summaryBorder).setWidth((numOfThreads - threadIdx) * cellWidth);
		}

		// resize swim cells
		for (Object swimCell : location.getAllSwimCells()) {
			if (!graph.isCellCollapsed(swimCell)) {
				model.getGeometry(swimCell).setWidth(numOfThreads * cellWidth + PaneConstants.SIGN_SIZE);
			} else {
				model.getGeometry(swimCell).getAlternateBounds()
						.setWidth(numOfThreads * cellWidth + PaneConstants.SIGN_SIZE);
			}
			graph.foldCells(!((mxCell) swimCell).isCollapsed(), false, new Object[] { swimCell }, false);
			graph.foldCells(!((mxCell) swimCell).isCollapsed(), false, new Object[] { swimCell }, false);

		}

		graph.refresh();
	}

	public void expand(Set<Pair<Integer, Integer>> set, String color, boolean reset) {
		Set<Integer> rowSet = new HashSet<>();
		for (Pair<Integer, Integer> p : set) {
			rowSet.add(p._1);
		}

		Map<Integer, Set<Integer>> rowLineMap = new HashMap<>();
		for (Pair<Integer, Integer> p : set) {
			int rowNum = p._1;
			System.out.println("rowNum " + p._1);
			System.out.println("lineNum " + p._2);

			if (rowLineMap.containsKey(rowNum)) {
				rowLineMap.get(rowNum).add(p._2);
			} else {
				Set<Integer> newSet = new HashSet<>();
				newSet.add(p._2);
				rowLineMap.put(rowNum, newSet);
			}
		}

		for (int row : rowSet) {
			int htChange = 0;
			System.out.println("row:" + row);
			for (int line : rowLineMap.get(row)) {
				System.out.println("line: " + line);

				Pair<Integer, Integer> p = new Pair<>(row, line);
				TextLine tl = lineTable.get(row).getList().get(line);
				Object contentBox = location.getContentCell(p);
				mxCell content = (mxCell) graph.getChildCells(contentBox)[0];

				SummaryCell sCell = location.getSummaryCell(p);
				mxCell summaryBox = (sCell == null) ? null : (mxCell) sCell.getSummary();
				mxCell summaryContent = (mxCell) model.getChildAt(summaryBox, 0);
				mxCell prevDots = (sCell == null) ? null : (mxCell) sCell.getPrevDots();
				mxCell nextDots = (sCell == null) ? null : (mxCell) sCell.getNextDots();
				mxCell prevSrc = (sCell == null) ? null : (mxCell) sCell.getPrevSrc();
				mxCell nextSrc = (sCell == null) ? null : (mxCell) sCell.getNextSrc();

				if (!reset) {
					if (tl.isHighlightedColor(color)) {
						return;
					}
					graph.foldCells(false, false, new Object[] { location.getSwimCell(row) });
					Map<String, Object> hlStyle = new HashMap<>(graph.getStylesheet().getStyles().get("content"));
					hlStyle.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, color);
					graph.getStylesheet().putCellStyle("highlight" + color, hlStyle);

					// Map<String, Object> sumContentStyle = new HashMap<>(
					// graph.getStylesheet().getStyles().get("content"));
					// sumContentStyle.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR,
					// color);
					// graph.getStylesheet().putCellStyle("sumHL" + row + color,
					// sumContentStyle);

					if (!tl.isHighlighted()) {

						// the detailed content
						content.setStyle("highlight" + color);
						// the summary
						if (sCell != null) {
							if (!summaryBox.isVisible()) {
								summaryBox.setVisible(true);
								htChange++;
							}

							summaryContent.setStyle("highlight" + color);
							// System.out.println(summaryContent.getValue());
							//
							// System.out.println("?summaryBox "
							// + (summaryContent.getParent() ==
							// location.getSummaryCell(row,
							// line).getSummary()));
							//
							// System.out.println("?summaryCell "
							// + (summaryContent.getParent().getParent() ==
							// location.getSummaryBorderCell(row)));
							//
							// System.out.println("?rightCell " +
							// (summaryContent.getParent().getParent()
							// .getParent() == location.getRightCell(row)));

							if (prevSrc != null && prevSrc.isVisible()) {
								if (prevDots.isVisible()) {
									System.out.println();
									prevDots.setVisible(false);
									htChange--;
								}
							}

							if (nextSrc != null && nextSrc.isVisible() && nextDots.isVisible()) {
								nextDots.setVisible(false);
								htChange--;
							} else if (nextSrc != null && !nextSrc.isVisible() && !nextDots.isVisible()) {
								nextDots.setVisible(true);
								htChange++;
							}

						}
					} else {
						// the detailed content
						mxCell colorBlock = (mxCell) graph.insertVertex(contentBox, null, " ", 0, 0, 5, htPerLine,
								"highlight" + color);
						colorBlock.setConnectable(false);

						// the summary
						if (sCell != null) {
							System.out.println("add summary color block " + row + " " + summaryBox.getStyle());
							mxCell sumBlock = (mxCell) graph.insertVertex(summaryBox, null, " ", 0, 0, 5, htPerLine,
									"highlight" + color);
							sumBlock.setConnectable(false);
						}
					}
					tl.setHighlight(color);
				} else {
					// reset
					if (!tl.isHighlightedColor(color)) {
						return;
					}
					tl.resetHighlight(color);

					String newColor = tl.getOneColor();

					// the detailed content
					for (int i = 1; i < model.getChildCount(contentBox); i++) {
						mxCell o = (mxCell) model.getChildAt(contentBox, i);
						if (o.getStyle() != null && (o.getStyle().contains(color)
								|| (newColor != null && o.getStyle().contains(newColor)))) {
							o.removeFromParent();
						}
					}

					if (!tl.isHighlighted()) {
						content.setStyle("content");
					} else {
						content.setStyle("highlight" + newColor);
					}

					// the summary
					if (sCell != null) {
						System.out.println("reset value: " + summaryContent.getValue());
						for (int i = 1; i < model.getChildCount(summaryBox); i++) {
							System.out.println("remove " + i);
							mxCell o = (mxCell) model.getChildAt(summaryBox, i);
							if (o.getStyle() != null && (o.getStyle().contains(color)
									|| (newColor != null && o.getStyle().contains(newColor)))) {
								o.removeFromParent();
							}
						}

						if (!tl.isHighlighted()) {
							if (!sCell.isFirst() && !sCell.isLast()) {
								System.out.println("invisible " + summaryBox.getStyle() + ":" + row + "," + line);

								summaryBox.setVisible(false);
								htChange--;
							}

							summaryContent.setStyle("content");

							// check the previous visible cell and set the dots
							// next to it as visible
							SummaryCell prevCell = sCell.getPreviousSummary();
							while (prevCell != null && !((mxCell) prevCell.getSummary()).isVisible()) {
								prevCell = prevCell.getPreviousSummary();
							}

							if (prevCell != null && !((mxCell) prevCell.getNextDots()).isVisible()) {
								((mxCell) prevCell.getNextDots()).setVisible(true);
								htChange++;
							}

							if (nextSrc != null && nextDots.isVisible()) {
								nextDots.setVisible(false);
								htChange--;
							}
						} else {
							System.out.println("highight with new color");
							summaryContent.setStyle("highlight" + newColor);
						}
					}
				}

				// if (reset && summaryContent != null) {
				// System.out.println("reset: " + row + "," + line);
				// }

			}

			// if (summaryContent != null) {
			mxCell sw = (mxCell) location.getSwitchCell(row);
			if (sw != null) {
				graph.foldCells(!sw.isCollapsed(), false, new Object[] { sw }, false);
				// graph.foldCells(false, false, new Object[] { sw },
				// false);
			} else {
				System.out.println("row not have switch " + row);
			}
			// mxGeometry geometry = (mxGeometry)
			// summaryContent.getGeometry().clone();
			// model.setGeometry(summaryContent, geometry);
			// }
			System.out.println("height change: " + htChange);
			mxCell summaryBorder = (mxCell) location.getSummaryBorderCell(row);
			System.out.println(summaryBorder.getStyle() + " height" + summaryBorder.getGeometry().getHeight());
			if (htChange == 0) {
				continue;
			}
			double tmpHt = htPerLine * htChange + model.getGeometry(summaryBorder).getHeight();

			model.getGeometry(summaryBorder).setHeight(tmpHt);
			if (((mxCell) summaryBorder).isVisible()) {
				model.getGeometry(location.getRightCell(row)).setHeight(tmpHt);
				model.getGeometry(location.getRowCell(row)).setHeight(tmpHt);
				model.getGeometry(location.getSwimCell(row)).setHeight(tmpHt);
			} else {
				mxCell swimCell = (mxCell) location.getSwimCell(row);
				model.getGeometry(swimCell).setAlternateBounds(
						new mxRectangle(0, 0, model.getGeometry(swimCell).getAlternateBounds().getWidth(), tmpHt));
			}

		}

		// graph.refresh();

	}

	public void foldAll(boolean b) {
		for (Object swimCell : location.getAllSwimCells()) {
			graph.foldCells(!((mxCell) swimCell).isCollapsed(), false, new Object[] { swimCell }, false);
			graph.foldCells(b, false, new Object[] { swimCell }, false);
		}
	}

}
