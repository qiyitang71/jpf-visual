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
import com.mxgraph.view.mxLayoutManager;

public class DrawMenu extends JPanel {

	private static final long serialVersionUID = 1L;
	private final int dx = 300;
	private final int dy = 45;
	private final int START_SIZE = 30;
	private final int TOP_SPACE = 10;
	private final int LEFT_SPACE = 15;
	private final int ALTER_SIZE = 50;
	private final int FONT_SIZE = 12;
	private final int CONTENT_FONT = 12;
	private final int AMPLIFY = 17;
	// private final int numOfThreads = 5;

	public DrawMenu(List<String> threadNames) {
		super();

		mxGraph graph = new mxGraph();

		mxIGraphModel model = graph.getModel();

		graph.setCellsEditable(false);
		graph.setCellsSelectable(false);
		graph.setCellsResizable(false);
		graph.setCollapseToPreferredSize(false);

		Map<String, Object> style = graph.getStylesheet().getDefaultVertexStyle();

		style.put(mxConstants.STYLE_VERTICAL_ALIGN, "middle");
		style.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, "white");
		style.put(mxConstants.STYLE_FONTSIZE, FONT_SIZE);
		style.put(mxConstants.STYLE_STARTSIZE, 0);
		style.put(mxConstants.STYLE_HORIZONTAL, true);
		style.put(mxConstants.STYLE_FONTCOLOR, "black");
		style.put(mxConstants.STYLE_STROKECOLOR, "black");
		style.put(mxConstants.STYLE_FONTSTYLE, mxConstants.FONT_BOLD);
		style.remove(mxConstants.STYLE_FILLCOLOR);

		Map<String, Object> menuStyle = new HashMap<String, Object>(style);

		// menu style not foldable
		menuStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_SWIMLANE);
		menuStyle.put(mxConstants.STYLE_STARTSIZE, START_SIZE);
		menuStyle.put(mxConstants.STYLE_HORIZONTAL, false);
		menuStyle.put(mxConstants.STYLE_FOLDABLE, false);
		menuStyle.put(mxConstants.STYLE_SPACING_TOP, TOP_SPACE);
		graph.getStylesheet().putCellStyle("menu", menuStyle);

		// while folding, the lower cells goes up
		mxLayoutManager layoutMng = new mxLayoutManager(graph) {
			public mxIGraphLayout getLayout(Object parent) {

				if (model.getChildCount(parent) > 0 && model.getStyle(parent) != "menu") {
					return new mxStackLayout(graph, false);
				} else if (model.getChildCount(parent) > 0 && model.getStyle(parent) == "menu") {
					return new mxStackLayout(graph, true);
				}
				return null;
			}

		};

		Object parent = graph.getDefaultParent();

		model.beginUpdate();
		try {
			// draw the menu
			int numOfThreads = threadNames.size();
			mxCell menu = (mxCell) graph.insertVertex(parent, null, "Trans.", 0, 0, numOfThreads * dx + START_SIZE, 0,
					"menu");
			menu.setConnectable(false);

			for (int i = 0; i < numOfThreads; i++) {

				((mxCell) graph.insertVertex(menu, null, threadNames.get(i) + "\n" + i, 0, 0, dx, dy))
						.setConnectable(false);

			}
		} finally {
			model.endUpdate();

		}
		mxGraphComponent graphComponent = new mxGraphComponent(graph);
		graphComponent.getGraphHandler().setRemoveCellsFromParent(false);
		this.add(graphComponent);
		this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

	}

	public static void main(String[] args) {
		JFrame frame = new JFrame();

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(700, 320);
		//DrawMenu et = new DrawMenu(6);
		//JScrollPane scrollPane = new JScrollPane(et);
		//frame.getContentPane().add(scrollPane);
		// et.drawGraph(6);
		frame.setVisible(true);

	}
}
