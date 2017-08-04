
//import java.awt.BorderLayout;
//import java.awt.Color;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.mxGraphOutline;
import com.mxgraph.view.mxGraph;

import gov.nasa.jpf.util.Pair;
import gov.nasa.jpf.vm.Path;

public class ErrorTablePane extends JPanel implements ComponentListener {
	private static final long serialVersionUID = 1L;
	private mxGraphComponent graphComponent;
	mxGraph graph;
	private mxGraphComponent menuGraphComponent;
	private mxGraphOutline outln = null;
	private JButton foldButton = null;
	private JButton expandButton = null;

	private int numOfThreads = -1;
	// private ContentPane content;
	private NewContent content;

	private MenuPane menu;
	private JSplitPane splitPane;
	double cellWidth = -1;

	public ErrorTablePane() {
		this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		graphComponent = new mxGraphComponent(new mxGraph());
		graphComponent.getGraphHandler().setRemoveCellsFromParent(false);
		graphComponent.addComponentListener(this);
		graphComponent.setBorder(BorderFactory.createEmptyBorder());
		menuGraphComponent = new mxGraphComponent(new mxGraph());
		outln = new mxGraphOutline(graphComponent);
		outln.setDrawLabels(true);
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, graphComponent, outln);
		splitPane.setOneTouchExpandable(false);
		splitPane.setDividerLocation(700);
		this.add(splitPane);
	}

	public void setButton(JButton foldButton, JButton expandButton) {
		this.foldButton = foldButton;
		this.expandButton = expandButton;
	}

	public void draw(TraceData td) {
		Path path = td.getPath();
		numOfThreads = td.getNumberOfThreads();
		List<Pair<Integer, Integer>> group = td.getGroup();
		List<String> threadNames = td.getThreadNames();
		Map<Integer, TextLineList> lineTable = td.getLineTable();

		// the main table
		cellWidth = (splitPane.getLeftComponent().getBounds().getWidth() - PaneConstants.RANGE_SIZE
				- PaneConstants.SIGN_SIZE - PaneConstants.BAR_SIZE) / numOfThreads;
		content = new NewContent(cellWidth, numOfThreads, path, group, lineTable);
		content.resize(cellWidth);

		content.foldAll(true);
		graph = content.getGraph();
		graphComponent.setGraph(graph);

		KeyListener keyListener = new CopyListener();
		graphComponent.addKeyListener(keyListener);
		graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				System.out.println("mouse released");
				if (content.areAllExpanded()) {
					foldButton.setSelected(false);
					foldButton.setEnabled(true);
					expandButton.setSelected(false);
					expandButton.setEnabled(false);
				} else if (content.areAllFolded()) {
					foldButton.setSelected(false);
					foldButton.setEnabled(false);
					expandButton.setSelected(false);
					expandButton.setEnabled(true);
				} else {
					foldButton.setSelected(false);
					foldButton.setEnabled(true);
					expandButton.setSelected(false);
					expandButton.setEnabled(true);
				}
			}

		});

		// set menu
		menu = new MenuPane(cellWidth, threadNames);
		mxGraph menuGraph = menu.getGraph();
		menuGraphComponent.setGraph(menuGraph);
		menuGraphComponent.getGraphHandler().setRemoveCellsFromParent(false);
		menuGraphComponent.setBorder(BorderFactory.createEmptyBorder());

		graphComponent.setColumnHeaderView(menuGraphComponent);

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

	public boolean areAllFolded() {
		return content.areAllFolded();
	}

	public boolean areAllExpanded() {
		return content.areAllExpanded();
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
		// TODO Auto-generated method stub
		double newWidth = (splitPane.getLeftComponent().getBounds().getWidth() * 1.0 - PaneConstants.RANGE_SIZE
				- PaneConstants.SIGN_SIZE - PaneConstants.BAR_SIZE) / numOfThreads;
		cellWidth = newWidth;
		content.resize(newWidth);
		menu.resize(newWidth);

		outln.setGraphComponent(graphComponent);

	}

	public JSplitPane getPane() {
		return splitPane;
	}

	private class CopyListener implements KeyListener {
		@Override
		public void keyTyped(KeyEvent e) {
			// TODO Auto-generated method stub
		}

		@Override
		public void keyPressed(KeyEvent e) {
			// TODO Auto-generated method stub
			if ((e.getKeyCode() == KeyEvent.VK_C) && (((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)
					|| (e.getModifiers() & KeyEvent.META_MASK) != 0)) {
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
}
