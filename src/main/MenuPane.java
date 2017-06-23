import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.layout.mxStackLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxLayoutManager;

public class MenuPane {
	private int cellWidth = PaneConstants.DEFAULT_CELL_WIDTH;
	private mxGraph menuGraph;
	private mxIGraphModel menuModel;
	private int numOfThreads = -1;

	public MenuPane(List<String> threadNames) {

		this.numOfThreads = threadNames.size();
		menuGraph = new mxGraph();
		menuModel = menuGraph.getModel();

		menuGraph.setCellsEditable(false);
		menuGraph.setCellsSelectable(false);
		menuGraph.setCellsResizable(false);
		menuGraph.setCollapseToPreferredSize(false);

		Map<String, Object> mDefaultstyle = menuGraph.getStylesheet().getDefaultVertexStyle();

		mDefaultstyle.put(mxConstants.STYLE_VERTICAL_ALIGN, "middle");
		mDefaultstyle.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, "white");
		mDefaultstyle.put(mxConstants.STYLE_FONTSIZE, PaneConstants.FONT_SIZE);
		mDefaultstyle.put(mxConstants.STYLE_STARTSIZE, 0);
		mDefaultstyle.put(mxConstants.STYLE_HORIZONTAL, true);
		mDefaultstyle.put(mxConstants.STYLE_FONTCOLOR, "black");
		mDefaultstyle.put(mxConstants.STYLE_STROKECOLOR, "black");
		mDefaultstyle.put(mxConstants.STYLE_FONTSTYLE, mxConstants.FONT_BOLD);
		mDefaultstyle.remove(mxConstants.STYLE_FILLCOLOR);

		Map<String, Object> menuStyle = new HashMap<String, Object>(mDefaultstyle);

		// menu style not foldable
		menuStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_SWIMLANE);
		menuStyle.put(mxConstants.STYLE_STARTSIZE, PaneConstants.SIGN_SIZE + PaneConstants.RANGE_SIZE);
		menuStyle.put(mxConstants.STYLE_HORIZONTAL, false);
		menuStyle.put(mxConstants.STYLE_STROKECOLOR, "none");
		menuStyle.put(mxConstants.STYLE_FOLDABLE, false);
		menuStyle.put(mxConstants.STYLE_SPACING_TOP, PaneConstants.TOP_SPACE);
		menuGraph.getStylesheet().putCellStyle("menu", menuStyle);

		// while folding, the lower cells goes up
		mxLayoutManager menuLayoutMng = new mxLayoutManager(menuGraph) {
			public mxIGraphLayout getLayout(Object parent) {

				if (menuModel.getChildCount(parent) > 0 && menuModel.getStyle(parent) != "menu") {
					return new mxStackLayout(graph, false);
				} else if (menuModel.getChildCount(parent) > 0 && menuModel.getStyle(parent) == "menu") {
					return new mxStackLayout(graph, true);
				}
				return null;
			}

		};

		Object menuParent = menuGraph.getDefaultParent();

		menuModel.beginUpdate();
		try {
			// draw the menu
			// int numOfThreads = threadNames.size();
			mxCell menu = (mxCell) menuGraph.insertVertex(menuParent, null, "Trans.", 0, 0,
					numOfThreads * cellWidth , 0, "menu");
			menu.setConnectable(false);
			System.out.println("menu lane cellWidth = " + menuModel.getGeometry(menu));

			
			for (int i = 0; i < numOfThreads; i++) {

				((mxCell) menuGraph.insertVertex(menu, null, threadNames.get(i) + "\n" + i, 0, 0, cellWidth,
						PaneConstants.CELL_HEIGHT)).setConnectable(false);

			}
		} finally {
			menuModel.endUpdate();

		}
	}

	public void resize(int newCellWidth) {
		this.cellWidth = newCellWidth;
		Object parent = menuGraph.getDefaultParent();
		for (Object obj : menuGraph.getChildCells(parent)) {
			if (obj != null) {
				menuGraph.resizeCell(obj, new mxRectangle(0, 0, numOfThreads * cellWidth + PaneConstants.START_SIZE,
						menuGraph.getCellGeometry(obj).getHeight()));
				for (Object subObj : menuGraph.getChildCells(obj)) {
					if (subObj != null) {
						menuGraph.resizeCell(subObj,
								new mxRectangle(0, 0, cellWidth, menuGraph.getCellGeometry(subObj).getHeight()));
					}
				}
			}
		}
		menuGraph.refresh();
	}

	public mxGraph getGraph() {
		return menuGraph;
	}
}
