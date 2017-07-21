
//import java.awt.BorderLayout;
//import java.awt.Color;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.BitSet;
//import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
//import javax.swing.text.JTextComponent;

import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.mxGraphOutline;
//import com.mxgraph.swing.view.mxCellEditor;
//import com.mxgraph.util.mxConstants;
//import com.mxgraph.util.mxUtils;
//import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;

import gov.nasa.jpf.util.Pair;
import gov.nasa.jpf.vm.Path;

public class ErrorTablePane extends JPanel implements ComponentListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private mxGraphComponent graphComponent;
	private int numOfThreads = -1;
	// private ContentPane content;
	private NewContent content;

	private MenuPane menu;
	private JSplitPane splitPane;

	public ErrorTablePane() {
		// super();
		this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		graphComponent = new mxGraphComponent(new mxGraph());
		graphComponent.getGraphHandler().setRemoveCellsFromParent(false);
		graphComponent.addComponentListener(this);
		graphComponent.setBorder(BorderFactory.createEmptyBorder());
		mxGraphOutline outln = new mxGraphOutline(graphComponent);
		// this.add(outln);
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, graphComponent, outln);
		splitPane.setOneTouchExpandable(false);
		splitPane.setDividerLocation(700);
		this.add(splitPane);
		// this.setBorder(BorderFactory.createEmptyBorder());
		// graphComponent.setMinimumSize(new Dimension(200, 50));
		// outln.setMinimumSize(new Dimension(100, 50));

	}

	public void draw(TraceData td) {
		Path path = td.getPath();
		numOfThreads = td.getNumberOfThreads();
		List<Pair<Integer, Integer>> group = td.getGroup();
		//List<String> detailList = td.getDetailList();
		//List<Integer> heightList = td.getHeightList();
		List<String> threadNames = td.getThreadNames();
		Map<Integer, TextLineList> lineTable = td.getLineTable();

		int cellWidth = (int) (Math.floor(
				(splitPane.getLeftComponent().getWidth() * 1.0 - PaneConstants.RANGE_SIZE - PaneConstants.SIGN_SIZE)
						/ numOfThreads));

		// content = new ContentPane(cellWidth, numOfThreads, path, group,
		// detailList, heightList);
		content = new NewContent(cellWidth, numOfThreads, path, group, lineTable);

		mxGraph graph = content.getGraph();
		graphComponent.setGraph(graph);
		KeyListener keyListener = new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
				// TODO Auto-generated method stub
			}

			@Override
			public void keyPressed(KeyEvent e) {
				// TODO Auto-generated method stub
				if ((e.getKeyCode() == KeyEvent.VK_C) && (((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)
						|| (e.getModifiers() & KeyEvent.META_MASK) != 0)) {
					System.out.println("copy???????");
					Object[] cells = graph.getSelectionCells();

					if (cells == null)
						return;

					StringBuilder myString = new StringBuilder();
					for (Object o : cells) {
						myString.append(((mxCell) o).getStyle());
						myString.append(((mxCell) o).getValue() + "\n");
					}
					StringSelection stringSelection = new StringSelection(myString.toString());
					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					clipboard.setContents(stringSelection, null);
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub
			}
		};
		graphComponent.addKeyListener(keyListener);
		content.resize(cellWidth);

		menu = new MenuPane(cellWidth, threadNames);
		mxGraph menuGraph = menu.getGraph();
		mxGraphComponent menuGraghComponent = new mxGraphComponent(menuGraph);
		menuGraghComponent.getGraphHandler().setRemoveCellsFromParent(false);
		menuGraghComponent.setBorder(BorderFactory.createEmptyBorder());
		graphComponent.setColumnHeaderView(menuGraghComponent);
	}

	public void expand(Set<Pair<Integer, Integer>> set, String color) {
		content.expand(set, color, false);
	}

	public void resetContent(Set<Pair<Integer, Integer>> set, String color) {
		content.expand(set, color, true);

	}

	public void foldAll(boolean b) {
		content.foldAll(b);
	}

	@Override
	public void componentShown(ComponentEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void componentHidden(ComponentEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void componentMoved(ComponentEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void componentResized(ComponentEvent arg0) {
		if (numOfThreads < 0)
			return;
		// - PaneConstants.OUTLINE_SIZE
		// TODO Auto-generated method stub

		int newWidth = (int) (Math.floor(
				(splitPane.getLeftComponent().getWidth() * 1.0 - PaneConstants.RANGE_SIZE - PaneConstants.SIGN_SIZE)
						/ numOfThreads));
		// System.out.println("resize" + newWidth);

		content.resize(newWidth);
		menu.resize(newWidth);

	}

	public JSplitPane getPane() {
		return splitPane;
	}
}
