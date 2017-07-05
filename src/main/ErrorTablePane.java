import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.EventObject;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.text.JTextComponent;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.mxGraphOutline;
import com.mxgraph.swing.view.mxCellEditor;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxCellState;
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
		graphComponent.setCellEditor(new mxCellEditor(graphComponent) {
			public void startEditing(Object cell, EventObject evt) {
				if (editingCell != null) {
					stopEditing(true);
				}

				mxCellState state = graphComponent.getGraph().getView().getState(cell);

				if (state != null) {
					editingCell = cell;
					trigger = evt;

					double scale = Math.max(minimumEditorScale, graphComponent.getGraph().getView().getScale());
					scrollPane.setBounds(getEditorBounds(state, scale));
					scrollPane.setVisible(true);

					String value = getInitialValue(state, evt);
					JTextComponent currentEditor = null;

					// Configures the style of the in-place editor
					if (graphComponent.getGraph().isHtmlLabel(cell)) {
						if (isExtractHtmlBody()) {
							value = mxUtils.getBodyMarkup(value, isReplaceHtmlLinefeeds());
						}

						editorPane.setDocument(mxUtils.createHtmlDocumentObject(state.getStyle(), scale));
						editorPane.setText(value);

						// Workaround for wordwrapping in editor pane
						// FIXME: Cursor not visible at end of line
						JPanel wrapper = new JPanel(new BorderLayout());
						wrapper.setOpaque(false);
						wrapper.add(editorPane, BorderLayout.CENTER);
						scrollPane.setViewportView(wrapper);

						currentEditor = editorPane;
					} else {
						textArea.setFont(mxUtils.getFont(state.getStyle(), scale));
						Color fontColor = mxUtils.getColor(state.getStyle(), mxConstants.STYLE_FONTCOLOR, Color.black);
						textArea.setForeground(fontColor);
						textArea.setText(value);

						scrollPane.setViewportView(textArea);
						currentEditor = textArea;
					}

					graphComponent.getGraphControl().add(scrollPane, 0);

					if (isHideLabel(state)) {
						graphComponent.redraw(state);
					}

					currentEditor.revalidate();
					currentEditor.requestFocusInWindow();
					currentEditor.selectAll();
					// if(!editable){
					currentEditor.setEditable(false);
					// }

					configureActionMaps();
				}
			}
		});
		// this.add(graphComponent);
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
		List<String> detailList = td.getDetailList();
		List<Integer> heightList = td.getHeightList();
		List<String> threadNames = td.getThreadNames();

		int cellWidth = (int) (Math
				.floor((splitPane.getLeftComponent().getWidth() * 1.0 - PaneConstants.START_SIZE - 65) / numOfThreads));

		// content = new ContentPane(cellWidth, numOfThreads, path, group,
		// detailList, heightList);
		content = new NewContent(cellWidth, numOfThreads, path, group, detailList, heightList);

		mxGraph graph = content.getGraph();
		graphComponent.setGraph(graph);

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

	public void resetContent(Set<Pair<Integer, Integer>> set) {
		content.expand(set, null, true);

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

		int newWidth = (int) (Math
				.floor((splitPane.getLeftComponent().getWidth() * 1.0 - PaneConstants.START_SIZE - 65) / numOfThreads));
		// System.out.println("resize" + newWidth);

		// content.resize(newWidth);
		menu.resize(newWidth);

	}

	public JSplitPane getPane() {
		return splitPane;
	}
}
