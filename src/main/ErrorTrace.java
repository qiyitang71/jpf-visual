import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

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

public class ErrorTrace extends JPanel {

	private static final long serialVersionUID = 1L;
	private final int dx = 100;
	private final int dy = 45;
	private final int START_SIZE = 15;
	private final int numOfThreads = 5;

	public ErrorTrace() {
		super();
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
		// graph.setCellsSelectable(false);
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
							style.put(mxConstants.STYLE_SPACING_LEFT, START_SIZE);
							style.put(mxConstants.STYLE_SWIMLANE_LINE, 0);
						} else {
							Map<String, Object> style = graph.getStylesheet().getStyles().get("row");
							style.put(mxConstants.STYLE_HORIZONTAL, false);
							style.put(mxConstants.STYLE_SWIMLANE_LINE, 1);
							style.remove(mxConstants.STYLE_ALIGN);
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

			mxCell menuCell2 = (mxCell) graph.insertVertex(menu, null, "main", 0, 0, dx, dy);
			menuCell2.setConnectable(false);

			mxCell menuCell3 = (mxCell) graph.insertVertex(menu, null, "Thread-1", 0, 0, dx, dy);
			menuCell3.setConnectable(false);

			mxCell menuCell4 = (mxCell) graph.insertVertex(menu, null, "Thread-2", 0, 0, dx, dy);
			menuCell4.setConnectable(false);

			mxCell menuCell5 = (mxCell) graph.insertVertex(menu, null, "Thread-3", 0, 0, dx, dy);
			menuCell5.setConnectable(false);

			mxCell menuCell6 = (mxCell) graph.insertVertex(menu, null, "Thread-4", 0, 0, dx, dy);
			menuCell6.setConnectable(false);

			mxCell lane1 = (mxCell) graph.insertVertex(parent, null, "0-5", 0, 0, numOfThreads * dx + START_SIZE, 0,
					"row");
			lane1.setConnectable(false);

			mxCell thread0 = (mxCell) graph.insertVertex(lane1, null, "Tr. 0-5", 0, 0, dx, dy);
			thread0.setConnectable(false);

			mxCell lane2 = (mxCell) graph.insertVertex(parent, null, "6-7", 0, 0, numOfThreads * dx + START_SIZE, 0,
					"row");
			lane2.setConnectable(false);

			mxCell thread01 = (mxCell) graph.insertVertex(lane2, null, "", dx, 0, dx, dy);
			thread01.setConnectable(false);

			mxCell thread1 = (mxCell) graph.insertVertex(lane2, null, "Tr. 6-7", 0, 0, dx, dy);
			thread1.setConnectable(false);

			mxCell lane3 = (mxCell) graph.insertVertex(parent, null, "8-10", 0, 0, numOfThreads * dx + START_SIZE, 0,
					"row");
			lane3.setConnectable(false);

			mxCell thread02 = (mxCell) graph.insertVertex(lane3, null, "", dx, 0, dx * 2, dy);
			thread02.setConnectable(false);

			mxCell thread2 = (mxCell) graph.insertVertex(lane3, null, "Tr. 8-10", 0, 0, dx, dy);
			thread2.setConnectable(false);

			mxCell lane4 = (mxCell) graph.insertVertex(parent, null, "11-12", 0, 0, numOfThreads * dx + START_SIZE, 0,
					"row");
			lane4.setConnectable(false);

			mxCell thread03 = (mxCell) graph.insertVertex(lane4, null, "", dx, 0, dx * 3, dy);
			thread03.setConnectable(false);

			mxCell thread3 = (mxCell) graph.insertVertex(lane4, null, "Tr. 11-12", 0, 0, dx, dy);
			thread3.setConnectable(false);

			mxCell lane5 = (mxCell) graph.insertVertex(parent, null, "13-15", 0, 0, numOfThreads * dx + START_SIZE, 0,
					"row");
			lane5.setConnectable(false);

			mxCell thread04 = (mxCell) graph.insertVertex(lane5, null, "", dx, 0, dx * 4, dy);
			thread04.setConnectable(false);

			mxCell thread4 = (mxCell) graph.insertVertex(lane5, null, "Tr. 13-15", 0, 0, dx, dy);
			thread4.setConnectable(false);

			mxCell lane6 = (mxCell) graph.insertVertex(parent, null, "16-17", 0, 0, numOfThreads * dx + START_SIZE, 0,
					"row");
			lane6.setConnectable(false);

			mxCell thread05 = (mxCell) graph.insertVertex(lane6, null, "", 0, 0, dx, dy);
			thread05.setConnectable(false);

			mxCell thread5 = (mxCell) graph.insertVertex(lane6, null, "Tr. 16-17", 0, 0, dx, dy);
			thread5.setConnectable(false);

			mxCell lane7 = (mxCell) graph.insertVertex(parent, null, "18", 0, 0, numOfThreads * dx + START_SIZE, 0,
					"row");
			lane7.setConnectable(false);

			mxCell thread06 = (mxCell) graph.insertVertex(lane7, null, "", 0, 0, dx * 3, dy);
			thread06.setConnectable(false);

			mxCell thread6 = (mxCell) graph.insertVertex(lane7, null, "Tr. 18", 0, 0, dx, dy);
			thread6.setConnectable(false);

			mxCell lane8 = (mxCell) graph.insertVertex(parent, null, "19", 0, 0, numOfThreads * dx + START_SIZE, 0,
					"row");
			lane8.setConnectable(false);

			mxCell thread07 = (mxCell) graph.insertVertex(lane8, null, "", 0, 0, dx * 2, dy);
			thread07.setConnectable(false);

			mxCell thread7 = (mxCell) graph.insertVertex(lane8, null, "Tr. 19", 0, 0, dx, dy);
			thread7.setConnectable(false);

			mxCell lane9 = (mxCell) graph.insertVertex(parent, null, "20", 0, 0, numOfThreads * dx + START_SIZE, 0,
					"row");
			lane9.setConnectable(false);

			mxCell thread08 = (mxCell) graph.insertVertex(lane9, null, "", 0, 0, dx * 2, dy);
			thread08.setConnectable(false);

			mxCell thread8 = (mxCell) graph.insertVertex(lane9, null, "Tr. 20", 0, 0, dx, dy);
			thread8.setConnectable(false);

			mxCell lane10 = (mxCell) graph.insertVertex(parent, null, "21", 0, 0, numOfThreads * dx + START_SIZE, 0,
					"row");
			lane10.setConnectable(false);

			mxCell thread09 = (mxCell) graph.insertVertex(lane10, null, "", 0, 0, dx * 4, dy);
			thread09.setConnectable(false);

			mxCell thread9 = (mxCell) graph.insertVertex(lane10, null, "Tr. 21", 0, 0, dx, dy);
			thread9.setConnectable(false);

			//the height is small when folded
			for (Object o : graph.getChildCells(parent)) {
				mxCell cell = (mxCell) o;
				if (cell != null && model.getStyle(cell) == "row") {
					model.getGeometry(cell)
							.setAlternateBounds(new mxRectangle(0, 0, numOfThreads * dx + START_SIZE, START_SIZE));
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
		ErrorTrace et = new ErrorTrace();
		JScrollPane scrollPane = new JScrollPane(et);

		frame.getContentPane().add(scrollPane);
		frame.setVisible(true);

	}
}
