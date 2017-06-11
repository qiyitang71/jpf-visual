import java.util.HashMap;
import java.util.Map;
//import java.util.Set;

import javax.swing.JFrame;
//import javax.swing.text.html.HTMLDocument.Iterator;
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
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxLayoutManager;

public class ErrorTrace extends JPanel {

	private static final long serialVersionUID = 1L;
	private final int dx = 100;
	private final int dy = 45;
	private final int START_SIZE = 15;

	// private final int foldHeight = 15;
	// public boolean isHorizontalLayout(mxCell cell){
	// if()
	// }

	public ErrorTrace() {
		super();
		mxGraph graph = new mxGraph();
		mxIGraphModel model = graph.getModel();
		Map<String, Object> defaultStyle = graph.getStylesheet().getDefaultVertexStyle();
		Map<String, Object> rowStyle = new HashMap<String, Object>(defaultStyle);

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

		// rowStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_SWIMLANE);
		rowStyle.put(mxConstants.STYLE_VERTICAL_ALIGN, "middle");
		rowStyle.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, "white");
		rowStyle.put(mxConstants.STYLE_FONTSIZE, 11);
		rowStyle.put(mxConstants.STYLE_STARTSIZE, START_SIZE);
		rowStyle.put(mxConstants.STYLE_HORIZONTAL, false);
		rowStyle.put(mxConstants.STYLE_FONTCOLOR, "black");
		rowStyle.put(mxConstants.STYLE_STROKECOLOR, "black");
		rowStyle.remove(mxConstants.STYLE_FILLCOLOR);
		graph.getStylesheet().putCellStyle("row", rowStyle);

		// Set<String> keySet = style.keySet();
		// java.util.Iterator<String> iter = keySet.iterator();
		// while(iter.hasNext()){
		// String str = iter.next();
		// System.out.println(str + ", " + style.get(str));
		// }

		// when folding, the width of the cell won't change

		mxIEventListener foldingHandler = new mxIEventListener() {
			@Override
			public void invoke(Object sender, mxEventObject evt) {
				Object[] cells = (Object[]) evt.getProperty("cells");
				for (int i = 0; i < cells.length; i++) {
					mxGeometry geo = model.getGeometry(cells[i]);
					if (geo.getAlternateBounds() != null) {
						geo.setWidth(geo.getAlternateBounds().getWidth());
					}
				}
			}
		};

		graph.addListener(mxEvent.FOLD_CELLS, foldingHandler);

		// while folding, the lower cells goes up
		mxStackLayout layoutHorizontal = new mxStackLayout(graph, true);
		mxStackLayout layoutVertical = new mxStackLayout(graph, false);

		mxLayoutManager layoutMng = new mxLayoutManager(graph) {
			public mxIGraphLayout getLayout(Object parent) {
				if (model.getChildCount(parent) > 0 && model.getStyle(parent) != "row") {
					return layoutVertical;
				} else if (model.getStyle(parent) == "row") {
					return layoutHorizontal;
				}
				return null;
			}
		};

		Object parent = graph.getDefaultParent();

		model.beginUpdate();
		try {
			int numOfCols = 6;

			mxCell menu = (mxCell) graph.insertVertex(parent, null, "", 0, 0, numOfCols * dx + START_SIZE, 0, "row");
			menu.setConnectable(false);

			mxCell menuCell1 = (mxCell) graph.insertVertex(menu, null, "Trans.", 0, 0, dx, dy);
			menuCell1.setConnectable(false);

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

			mxCell lane1 = (mxCell) graph.insertVertex(parent, null, "", 0, 0, numOfCols * dx + START_SIZE, 0, "row");
			lane1.setConnectable(false);

			mxCell tranNum1 = (mxCell) graph.insertVertex(lane1, null, "0-5", 0, 0, dx, dy);
			tranNum1.setConnectable(false);

			mxCell thread0 = (mxCell) graph.insertVertex(lane1, null, "Tr. 0-5", 0, 0, dx, dy);
			thread0.setConnectable(false);

			mxCell lane2 = (mxCell) graph.insertVertex(parent, null, "", 0, 0, numOfCols * dx + START_SIZE, 0, "row");
			lane2.setConnectable(false);

			mxCell tranNum2 = (mxCell) graph.insertVertex(lane2, null, "6-7", 0, 0, dx, dy);
			tranNum2.setConnectable(false);

			mxCell thread01 = (mxCell) graph.insertVertex(lane2, null, "", dx, 0, dx, dy);
			thread01.setConnectable(false);

			mxCell thread1 = (mxCell) graph.insertVertex(lane2, null, "Tr. 6-7", 0, 0, dx, dy);
			thread1.setConnectable(false);

			mxCell lane3 = (mxCell) graph.insertVertex(parent, null, "", 0, 0, numOfCols * dx + START_SIZE, 0, "row");
			lane3.setConnectable(false);

			mxCell tranNum3 = (mxCell) graph.insertVertex(lane3, null, "8-10", 0, 0, dx, dy);
			tranNum3.setConnectable(false);

			mxCell thread02 = (mxCell) graph.insertVertex(lane3, null, "", dx, 0, dx * 2, dy);
			thread02.setConnectable(false);

			mxCell thread2 = (mxCell) graph.insertVertex(lane3, null, "Tr. 8-10", 0, 0, dx, dy);
			thread2.setConnectable(false);

			mxCell lane4 = (mxCell) graph.insertVertex(parent, null, "", 0, 0, numOfCols * dx + START_SIZE, 0, "row");
			lane4.setConnectable(false);

			mxCell tranNum4 = (mxCell) graph.insertVertex(lane4, null, "11-12", 0, 0, dx, dy);
			tranNum4.setConnectable(false);

			mxCell thread03 = (mxCell) graph.insertVertex(lane4, null, "", dx, 0, dx * 3, dy);
			thread03.setConnectable(false);

			mxCell thread3 = (mxCell) graph.insertVertex(lane4, null, "Tr. 11-12", 0, 0, dx, dy);
			thread3.setConnectable(false);

			mxCell lane5 = (mxCell) graph.insertVertex(parent, null, "", 0, 0, numOfCols * dx + START_SIZE, 0, "row");
			lane5.setConnectable(false);

			mxCell tranNum5 = (mxCell) graph.insertVertex(lane5, null, "13-15", 0, 0, dx, dy);
			tranNum5.setConnectable(false);

			mxCell thread04 = (mxCell) graph.insertVertex(lane5, null, "", dx, 0, dx * 4, dy);
			thread04.setConnectable(false);

			mxCell thread4 = (mxCell) graph.insertVertex(lane5, null, "Tr. 13-15", 0, 0, dx, dy);
			thread4.setConnectable(false);

			mxCell lane6 = (mxCell) graph.insertVertex(parent, null, "", 0, 0, numOfCols * dx + START_SIZE, 0, "row");
			lane6.setConnectable(false);

			mxCell tranNum6 = (mxCell) graph.insertVertex(lane6, null, "16-17", 0, 0, dx, dy);
			tranNum6.setConnectable(false);

			mxCell thread05 = (mxCell) graph.insertVertex(lane6, null, "", 0, 0, dx, dy);
			thread05.setConnectable(false);

			mxCell thread5 = (mxCell) graph.insertVertex(lane6, null, "Tr. 16-17", 0, 0, dx, dy);
			thread5.setConnectable(false);

			mxCell lane7 = (mxCell) graph.insertVertex(parent, null, "", 0, 0, numOfCols * dx + START_SIZE, 0, "row");
			lane7.setConnectable(false);

			mxCell tranNum7 = (mxCell) graph.insertVertex(lane7, null, "18", 0, 0, dx, dy);
			tranNum7.setConnectable(false);

			mxCell thread06 = (mxCell) graph.insertVertex(lane7, null, "", 0, 0, dx * 3, dy);
			thread06.setConnectable(false);

			mxCell thread6 = (mxCell) graph.insertVertex(lane7, null, "Tr. 18", 0, 0, dx, dy);
			thread6.setConnectable(false);

			mxCell lane8 = (mxCell) graph.insertVertex(parent, null, "", 0, 0, numOfCols * dx + START_SIZE, 0, "row");
			lane8.setConnectable(false);

			mxCell tranNum8 = (mxCell) graph.insertVertex(lane8, null, "19", 0, 0, dx, dy);
			tranNum8.setConnectable(false);

			mxCell thread07 = (mxCell) graph.insertVertex(lane8, null, "", 0, 0, dx * 2, dy);
			thread07.setConnectable(false);

			mxCell thread7 = (mxCell) graph.insertVertex(lane8, null, "Tr. 19", 0, 0, dx, dy);
			thread7.setConnectable(false);

			mxCell lane9 = (mxCell) graph.insertVertex(parent, null, "", 0, 0, numOfCols * dx + START_SIZE, 0, "row");
			lane9.setConnectable(false);

			mxCell tranNum9 = (mxCell) graph.insertVertex(lane9, null, "20", 0, 0, dx, dy);
			tranNum9.setConnectable(false);

			mxCell thread08 = (mxCell) graph.insertVertex(lane9, null, "", 0, 0, dx * 2, dy);
			thread08.setConnectable(false);

			mxCell thread8 = (mxCell) graph.insertVertex(lane9, null, "Tr. 20", 0, 0, dx, dy);
			thread8.setConnectable(false);

			mxCell lane10 = (mxCell) graph.insertVertex(parent, null, "", 0, 0, numOfCols * dx + START_SIZE, 0, "row");
			lane10.setConnectable(false);

			mxCell tranNum10 = (mxCell) graph.insertVertex(lane10, null, "21", 0, 0, dx, dy);
			tranNum10.setConnectable(false);

			mxCell thread09 = (mxCell) graph.insertVertex(lane10, null, "", 0, 0, dx * 4, dy);
			thread09.setConnectable(false);

			mxCell thread9 = (mxCell) graph.insertVertex(lane10, null, "Tr. 21", 0, 0, dx, dy);
			thread9.setConnectable(false);

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
