import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;

import gov.nasa.jpf.vm.ClassInfo;

public class ClassExplorer extends JPanel implements TreeSelectionListener {

	private static final long serialVersionUID = 1L;
	private JTree tree;
	private TraceData td;

	public ClassExplorer(TraceData td) {

		super(new GridLayout(1, 0));
		this.td = td;

		// Create the nodes.
		DefaultMutableTreeNode top = new DefaultMutableTreeNode("Classes");
		createNodes(top);

		// Create a tree that allows one selection at a time.
		tree = new JTree(top);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		// Listen for when the selection changes.
		// tree.addTreeSelectionListener(this);

		// Create the scroll pane and add the tree to it.
		JScrollPane treeView = new JScrollPane(tree);

		Dimension minimumSize = new Dimension(200, 100);
		treeView.setMinimumSize(minimumSize);

		// Add the split pane to this panel.
		add(treeView);
	}

	public JTree getTree() {
		return this.tree;
	}

	public void addTreeSelectionListener(TreeSelectionListener listener) {
		this.tree.addTreeSelectionListener(listener);
	}

	/** Required by TreeSelectionListener interface. */
	public void valueChanged(TreeSelectionEvent e) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

		if (node == null)
			return;

		if (node.toString().equals("classes")) {
			return;
		}

	}

	private void depthFirstSearch(ClassInfo ci, DefaultMutableTreeNode branch, DefaultMutableTreeNode parent) {
		branch = new DefaultMutableTreeNode(new ClassNode(ci));
		parent.add(branch);
		Map<ClassInfo, Set<ClassInfo>> classStruture = td.getClassStruture();
		if (!classStruture.containsKey(ci)) {
			return;
		}
		for (ClassInfo child : classStruture.get(ci)) {
			depthFirstSearch(child, new DefaultMutableTreeNode(), branch);
		}
	}

	private void createNodes(DefaultMutableTreeNode top) {
		DefaultMutableTreeNode parent = null;
		DefaultMutableTreeNode branch = null;
		System.out.println(top);

		Map<ClassInfo, Set<ClassInfo>> classStruture = td.getClassStruture();
		Set<ClassInfo> classRoots = td.getClassRoots();
		System.out.println("---------------------");
		for (ClassInfo ci : classStruture.keySet()) {
			System.out.println(ci);
		}
		System.out.println("---------------------");

		for (ClassInfo ci : classRoots) {
			System.out.println(ci);

			parent = new DefaultMutableTreeNode(new ClassNode(ci));
			top.add(parent);

			for (ClassInfo child : classStruture.get(ci)) {
				System.out.println(child);
				depthFirstSearch(child, branch, parent);
			}
		}

	}

}

class ClassNode {
	public ClassInfo ci;

	public ClassNode(ClassInfo ci) {
		this.ci = ci;
	}

	public String toString() {
		return this.ci.getSimpleName();
	}

	public ClassInfo getClassInfo() {
		return ci;
	}
}
