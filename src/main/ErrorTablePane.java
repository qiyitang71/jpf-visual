import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.mxGraphOutline;
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
	private ContentPane content;
	private MenuPane menu;

	public ErrorTablePane() {
		super();
		this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		graphComponent = new mxGraphComponent(new mxGraph());
		graphComponent.getGraphHandler().setRemoveCellsFromParent(false);
		graphComponent.addComponentListener(this);
		this.add(graphComponent);
		mxGraphOutline outln = new mxGraphOutline(graphComponent);
		Rectangle r = new Rectangle(100, 100);
		outln.setBounds(r);
		this.add(outln);
	}

	public void draw(Path path) {
		TraceData td = new TraceData(path);
		numOfThreads = td.getNumberOfThreads();
		List<Pair<Integer, Integer>> group = td.getGroup();
		List<String> detailList = td.getDetailList();
		List<Integer> heightList = td.getHeightList();
		List<String> threadNames = td.getThreadNames();

		content = new ContentPane(numOfThreads, path, group, detailList, heightList);
		mxGraph graph = content.getGraph();
		graphComponent.setGraph(graph);

		menu = new MenuPane(threadNames);
		mxGraph menuGraph = menu.getGraph();
		mxGraphComponent menuGraghComponent = new mxGraphComponent(menuGraph);
		menuGraghComponent.getGraphHandler().setRemoveCellsFromParent(false);
		graphComponent.setColumnHeaderView(menuGraghComponent);
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
		int newWidth = (int) (Math.floor(
				(this.getWidth() * 1.0 - PaneConstants.START_SIZE - PaneConstants.OUTLINE_SIZE - 10) / numOfThreads));
		content.resize(newWidth);
		menu.resize(newWidth);

	}
}
