import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.mxGraphOutline;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;

import gov.nasa.jpf.util.Pair;
import gov.nasa.jpf.vm.Path;

public class ErrorTablePane extends JPanel implements ComponentListener {
	private static final long serialVersionUID = 1L;
	private mxGraphComponent graphComponent;
	mxGraph graph;
	LocationInGraph location;
	private mxGraphComponent menuGraphComponent;
	private mxGraphOutline outln = null;
	private ThreadStateView threadStateView = null;
	private mxGraphComponent threadStateComponent;

	private JButton foldButton = null;
	private JButton expandButton = null;

	private JButton outlnButton = null;

	private int numOfThreads = -1;
	private NewContent content;

	private MenuPane menu;
	private JSplitPane splitPane;
	private JSplitPane mapPane;
	private JPanel buttonPanel;

	double cellWidth = -1;

	private Path path;
	private List<Pair<Integer, Integer>> group;
	private List<String> threadNames;
	private Map<Integer, TextLineList> lineTable;
	private Map<Pair<Integer, Integer>, List<Pair<Integer, String>>> threadStateMap;
	private List<Double> threadHeightList;

	public ErrorTablePane() {
		this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		graphComponent = new mxGraphComponent(new mxGraph());
		graphComponent.getGraphHandler().setRemoveCellsFromParent(false);
		graphComponent.addComponentListener(this);
		graphComponent.setBorder(BorderFactory.createEmptyBorder());
		menuGraphComponent = new mxGraphComponent(new mxGraph());
		outln = new mxGraphOutline(graphComponent);
		outln.setDrawLabels(true);

		buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

		mapPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, buttonPanel, outln);
		mapPane.setOneTouchExpandable(false);
		mapPane.setDividerLocation(PaneConstants.CELL_HEIGHT);
		mapPane.setBorder(BorderFactory.createEmptyBorder());

		addButtons();

		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, graphComponent, mapPane);
		splitPane.setOneTouchExpandable(false);
		splitPane.setDividerLocation(600);
		splitPane.setBorder(BorderFactory.createEmptyBorder());

		this.add(splitPane);

	}

	public void setButton(JButton foldButton, JButton expandButton) {
		this.foldButton = foldButton;
		this.expandButton = expandButton;
	}

	public void draw(TraceData td) {
		path = td.getPath();
		numOfThreads = td.getNumberOfThreads();
		group = td.getGroup();
		threadNames = td.getThreadNames();
		lineTable = td.getLineTable();
		threadStateMap = td.getThreadStateMap();

		// the main table
		cellWidth = (splitPane.getLeftComponent().getBounds().getWidth() - PaneConstants.RANGE_SIZE
				- PaneConstants.SIGN_SIZE - PaneConstants.BAR_SIZE) / numOfThreads;
		content = new NewContent(cellWidth, numOfThreads, path, group, lineTable);
		content.resize(cellWidth);

		double rightCellWidth = (splitPane.getWidth() - splitPane.getLeftComponent().getBounds().getWidth()
				- PaneConstants.RANGE_SIZE - PaneConstants.SIGN_SIZE - PaneConstants.BAR_SIZE) / numOfThreads;
		threadStateView = new ThreadStateView(rightCellWidth, numOfThreads, path, group, lineTable, threadStateMap);
		threadStateComponent = threadStateView.getComponent();
		threadHeightList = threadStateView.getHeightList();

		threadStateComponent.addComponentListener(new MapListener());
		threadStateComponent.getGraphControl().addMouseListener(new ClickListener());
		// threadStateComponent.addMouseWheelListener(new ScrollListener());
		threadStateComponent.getViewport().addChangeListener(new ViewportListener());

		content.foldAll(true);
		graph = content.getGraph();
		graphComponent.setGraph(graph);
		location = content.getLocation();

		graphComponent.addKeyListener(new CopyListener());
		graphComponent.getGraphControl().addMouseListener(new FoldListener());

		// set menu
		menu = new MenuPane(cellWidth, threadNames);
		mxGraph menuGraph = menu.getGraph();
		menuGraphComponent.setGraph(menuGraph);
		menuGraphComponent.getGraphHandler().setRemoveCellsFromParent(false);
		menuGraphComponent.setBorder(BorderFactory.createEmptyBorder());

		graphComponent.setColumnHeaderView(menuGraphComponent);
		outln.setGraphComponent(graphComponent);

	}

	private void addButtons() {
		ButtonListener buttonListener = new ButtonListener();
		outlnButton = new JButton("Thread State");
		outlnButton.addActionListener(buttonListener);

		buttonPanel.add(outlnButton);
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
		// do nothing
	}

	@Override
	public void componentHidden(ComponentEvent arg0) {
		// do nothing

	}

	@Override
	public void componentMoved(ComponentEvent arg0) {
		// do nothing
	}

	@Override
	public void componentResized(ComponentEvent arg0) {
		if (numOfThreads < 0)
			return;
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

	private class FoldListener extends MouseAdapter {
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

	}

	private class ClickListener extends MouseAdapter {
		public void mouseClicked(MouseEvent e) {
			System.out.println("mouse clicked");
			System.out.println(e.getPoint());
		}
	}

	private class CopyListener extends KeyAdapter {

		public void keyPressed(KeyEvent e) {
			// TODO Auto-generated method stub
			if ((e.getKeyCode() == KeyEvent.VK_C) && (((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)
					|| (e.getModifiers() & KeyEvent.META_MASK) != 0)) {
				Object[] cells = graph.getSelectionCells();

				if (cells == null)
					return;

				StringBuilder myString = new StringBuilder();
				for (Object o : cells) {
					// myString.append(((mxCell) o).getStyle());
					myString.append(((mxCell) o).getValue() + "\n");
				}
				StringSelection stringSelection = new StringSelection(myString.toString());
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(stringSelection, null);
			}
		}

	};

	private class ButtonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			assert (e.getSource() instanceof JButton);

			JButton button = (JButton) e.getSource();
			if (button.getText().equals("Outline")) {
				outlnButton.setText("Thread State");
				mapPane.setBottomComponent(outln);
			} else {
				outlnButton.setText("Outline");
				mapPane.setBottomComponent(threadStateComponent);
			}

		}

	}

	private class MapListener implements ComponentListener {

		@Override
		public void componentResized(ComponentEvent e) {
			System.out.println("resize thread state");
			double rightCellWidth = ((splitPane.getWidth() - splitPane.getLeftComponent().getBounds().getWidth()
					- PaneConstants.RANGE_SIZE - PaneConstants.SIGN_SIZE - PaneConstants.BAR_SIZE)) / numOfThreads;
			threadStateView.setCellWidth(rightCellWidth);
			threadStateView.resize();
			// System.out.println(threadStateComponent.getViewport());

		}

		@Override
		public void componentMoved(ComponentEvent e) {
			// do nothing

		}

		@Override
		public void componentShown(ComponentEvent e) {
			// do nothing

		}

		@Override
		public void componentHidden(ComponentEvent e) {
			// do nothing

		}

	}

	private class ScrollListener implements MouseWheelListener {

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			System.out.println("mouse wheel");
			System.out.println(e.getYOnScreen());
		}
	}

	private class ViewportListener implements ChangeListener {

		@Override
		public void stateChanged(ChangeEvent e) {
			System.out.println("view port change");

			// System.out.println(e.getSource());
			JViewport myViewport = (JViewport) e.getSource();
			Field field;
			try {
				field = myViewport.getClass().getDeclaredField("lastPaintPosition");
				field.setAccessible(true);
				Object value = field.get(myViewport);
				Point point = (Point) value;
				if (point == null)
					return;
				int y = (int) Math.abs(point.getY());
				System.out.println(findGroup(y));
				int row = findGroup(y);
				// graphComponent.getVerticalScrollBar().setValue(y);
				//graphComponent.cell
				//graphComponent.scrollCellToVisible(location.getRowCell(3), true);
				mxCellState state = graph.getView().getState(location.getRowCell(row));
				mxRectangle bounds = state;
				System.out.println(state);

				graphComponent.getVerticalScrollBar().setValue((int) bounds.getY());


			} catch (NoSuchFieldException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (SecurityException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IllegalArgumentException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IllegalAccessException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}

	}

	private int findGroup(double y) {
		int len = threadHeightList.size();
		int left = 0;
		int right = len - 1;
		while (left <= right) {
			int mid = left + (right - left) / 2;
			if (y < threadHeightList.get(mid)) {
				right = mid - 1;
			} else if (y > threadHeightList.get(mid)) {
				left = mid + 1;
			} else {
				return mid;
			}
		}
		return left;

	}
}
