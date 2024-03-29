
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.report.Publisher;
import gov.nasa.jpf.shell.ShellManager;
import gov.nasa.jpf.shell.ShellPanel;
import gov.nasa.jpf.shell.commands.VerifyCommand;
import gov.nasa.jpf.shell.listeners.VerifyCommandListener;
import gov.nasa.jpf.shell.util.ProgressTrackerUI;
import gov.nasa.jpf.util.Pair;
import gov.nasa.jpf.vm.Path;

/**
 * Basic output panel that divides new trace printer's results into browseable
 * topics. This panel uses a
 * {@link gov.nasa.jpf.shell.listeners.VerifyCommandListener} to keep track of
 * when the VerifyCommand is executed.
 */

public class ErrorTracePanel extends ShellPanel implements VerifyCommandListener {
	private static final long serialVersionUID = 1L;
	private static final String PROGRESS = "PROGRESS";
	private static final String TOPICS = "TOPICS";

	// Panel
	private JLabel statusLabel = new JLabel();
	private ErrorTableAndMapPane errorTrace = new ErrorTableAndMapPane();

	private ProgressTrackerUI tracker = new ProgressTrackerUI();
	private CardLayout layout = new CardLayout();
	private Path path;
	private TraceData td = null;

	private JPanel userControlPanel = new JPanel();
	private ItemListener checkBoxListener = null;

	private JButton foldAllButton;
	private boolean isFoldSelected;

	private JButton expandAllButton;
	private boolean isExpandSelected;

	private JCheckBox waitBox;
	private JCheckBox threadStartBox;

	private Map<JCheckBox, Boolean> selectTable;
	private Map<String, String> colors = new HashMap<>();
	private int numOfColors = 0;
	private int colorID = 2;

	private ClassFieldExplorer classFieldExplorer;
	private ClassMethodExplorer classMethodExplorer;

	public ErrorTracePanel() {
		super("Error Trace", null, "View JPF's Output");
		ShellManager.getManager().addCommandListener(VerifyCommand.class, this);

		JPanel tablePanel = new JPanel();
		tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
		tablePanel.add(statusLabel);
		userControlPanel.setLayout(new BoxLayout(userControlPanel, BoxLayout.Y_AXIS));
		this.numOfColors = PaneConstants.COLOR_TABLE.length;

		checkBoxListener = new CheckBoxListener();
		ActionListener buttonListener = new ButtonListener();

		initFoldExpandButtons();
		foldAllButton.addActionListener(buttonListener);
		expandAllButton.addActionListener(buttonListener);

		initWaitThreadStartBoxes();

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, userControlPanel, errorTrace);
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerLocation(200);
		splitPane.setBorder(BorderFactory.createEmptyBorder());

		tablePanel.setBackground(Color.white);
		tablePanel.add(splitPane);
		setLayout(layout);

		add(tablePanel, TOPICS);
		add(tracker, PROGRESS);
		layout.show(this, PROGRESS);

	}

	private void initFoldExpandButtons() {
		foldAllButton = new JButton("Collapse all");
		foldAllButton.setMnemonic(KeyEvent.VK_C);
		foldAllButton.setActionCommand("foldAll");

		expandAllButton = new JButton("Expand all");
		expandAllButton.setMnemonic(KeyEvent.VK_E);
		expandAllButton.setActionCommand("expandAll");
	}

	private void initWaitThreadStartBoxes() {
		waitBox = new JCheckBox("wait/notify");
		waitBox.setBackground(Color.decode(PaneConstants.COLOR_TABLE[0]));
		waitBox.setOpaque(true);
		waitBox.setMnemonic(KeyEvent.VK_W);
		waitBox.addItemListener(checkBoxListener);

		threadStartBox = new JCheckBox("thread start/join");
		threadStartBox.setBackground(Color.decode(PaneConstants.COLOR_TABLE[1]));
		threadStartBox.setOpaque(true);
		threadStartBox.setMnemonic(KeyEvent.VK_S);
		threadStartBox.addItemListener(checkBoxListener);

	}

	String publishers = null;

	public void preCommand(final VerifyCommand command) {
		requestShellFocus();
		Config config = ShellManager.getManager().getConfig();
		publishers = config.getProperty("report.publisher", "");
		if (publishers.contains("errorTracePrinter")) {
			config.put("report.publisher", config.getProperty("report.publisher", "") + ",errorTrace");
			config.put("report.errorTrace.class", ErrorTracePrinter.class.getName());
		}
		tracker.resetFields();
	}

	/**
	 * Once JPF creates an instance of the TopicPublisher it is grabbed after
	 * initialization by the tracker.
	 * 
	 * @param command
	 */
	public void afterJPFInit(VerifyCommand command) {
		tracker.attachJPF(command.getJPF());
		layout.show(this, PROGRESS);
	}

	/**
	 * Just show the results of the JPF verification.
	 * 
	 * @param command
	 */
	public void postCommand(VerifyCommand command) {

		if (command.errorOccured()) {
			statusLabel.setText("An Error occured during the verify, check the Error Panel for more details");
			statusLabel.setForeground(Color.RED);
		} else {
			statusLabel.setText("The JPF run completed successfully");
			statusLabel.setForeground(Color.BLACK);
		}

		boolean found = false;
		for (Publisher publisher : command.getJPF().getReporter().getPublishers()) {
			if (publisher instanceof ErrorTracePrinter) {
				if (!found) {
					found = true;
				}
				path = ((ErrorTracePrinter) publisher).getPath();
			}
		}
		if (found && path != null) {
			// reset
			//process data
			td = new TraceData(path);
			
			//draw main/central panel and map/right panel
			errorTrace.draw(td);
			userControlPanel.removeAll();
			selectTable = new LinkedHashMap<>();
			colors.clear();
			colorID = 2;

			// Control panel install buttons, check boxes, dropdown list
			installFoldExpandButtons();
			errorTrace.setButton(foldAllButton, expandAllButton);
			installCheckBoxes();
			installDropDownList();
			layout.show(this, TOPICS);
			getShell().requestFocus(this);

		} else if (found && path == null) {
			ShellManager.getManager().getConfig().put("report.publisher", publishers);
			publishers = null;
			// layout.show(this, TOPICS);
			JOptionPane.showMessageDialog(this, "No error trace is generated!", "No Error Found",
					JOptionPane.NO_OPTION | JOptionPane.ERROR_MESSAGE);
		} else {
			ShellManager.getManager().getConfig().put("report.publisher", publishers);
			publishers = null;
			JOptionPane.showMessageDialog(this, "ErrorTracePrinter is not set as a publisher!", "Error config .jpf",
					JOptionPane.NO_OPTION | JOptionPane.ERROR_MESSAGE);
		}
	}

	private void installFoldExpandButtons() {
		// add fold all button
		userControlPanel.add(foldAllButton);
		// add expand all button
		userControlPanel.add(expandAllButton);
		foldAllButton.setEnabled(false);
		foldAllButton.setSelected(false);
		isFoldSelected = true;
		expandAllButton.setSelected(false);
		expandAllButton.setEnabled(true);
		isExpandSelected = false;
	}

	private void installCheckBoxes() {
		Set<String> fieldNames = td.getFieldNames();
		// add wait/notify check box
		waitBox.setSelected(false);
		selectTable.put(waitBox, false);
		userControlPanel.add(waitBox);

		// add thread start/join check box
		threadStartBox.setSelected(false);
		selectTable.put(threadStartBox, false);
		userControlPanel.add(threadStartBox);

		// add monitor enter/exit and synchronized method check boxes
		for (String s : fieldNames) {
			JCheckBox cb = new JCheckBox("(un)lock: " + s);
			cb.setSelected(false);
			cb.addItemListener(checkBoxListener);

			if (!colors.containsKey(s)) {
				colors.put(s, PaneConstants.COLOR_TABLE[colorID]);
				colorID = (colorID + 1) % numOfColors;
			}

			cb.setBackground(Color.decode(colors.get(s)));
			cb.setOpaque(true);
			selectTable.put(cb, false);
			userControlPanel.add(cb);
		}
	}

	private void installDropDownList() {
		/**
		 * add drop down list dynamically searching user input for field access/
		 * method call
		 */
		String[] dropDownStrs = { "Custom filter...", "Field Access ...", "Method call ..." };
		JComboBox<String> highlightList = new JComboBox<String>(dropDownStrs) {

			private static final long serialVersionUID = 1L;

			@Override
			public Dimension getMaximumSize() {
				Dimension max = super.getMaximumSize();
				max.height = getPreferredSize().height;
				return max;
			}

		};

		highlightList.setMaximumRowCount(3);
		highlightList.setAlignmentX(0);
		highlightList.setAlignmentY(0);
		highlightList.addActionListener(new dropDownListener());
		userControlPanel.add(highlightList);
	}

	public String showDialog(String str, Component comp) {
		String userInput;
		if (str.equals("field")) {
			userInput = (String) JOptionPane.showInputDialog(comp, "Input:\n" + "Class.field", "Text input",
					JOptionPane.PLAIN_MESSAGE, null, null, "Class.field");
		} else {
			userInput = (String) JOptionPane.showInputDialog(comp, "Input:\n" + "Class.method", "Text input",
					JOptionPane.PLAIN_MESSAGE, null, null, "Class.method");
		}
		// do things with user input;
		return userInput;

	}

	public void exceptionDuringVerify(Exception ex) {
	}

	private void popNotExistDialogue(String userInput) {
		JOptionPane.showMessageDialog(errorTrace,
				"Sorry, \"" + userInput + "\" " + "does not exist.\n" + "Please Try again", "Error message",
				JOptionPane.ERROR_MESSAGE);
	}

	private void popAlreadyExistDialogue(String userInput) {
		JOptionPane.showMessageDialog(errorTrace,
				"Sorry, \"" + userInput + "\" " + "already exist.\n" + "Please Try again", "Error message",
				JOptionPane.ERROR_MESSAGE);
	}

	private void fieldMethodSearch(String clsName, String fmName, String userInput, boolean isField) {
		Set<Pair<Integer, Integer>> targetList = null;
		if (isField) {
			targetList = td.getClassField(clsName, fmName);
		} else {
			targetList = td.getClassMethod(clsName, fmName);
		}

		if (targetList.isEmpty()) {
			popNotExistDialogue(userInput);
		} else {
			String s = clsName + "." + fmName;
			if (colors.containsKey(s)) {
				popAlreadyExistDialogue(userInput);
				return;
			}
			JCheckBox fmCheckBox = null;
			if (isField) {
				fmCheckBox = new JCheckBox("field: " + s);
			} else {
				fmCheckBox = new JCheckBox("method: " + s);
			}

			fmCheckBox.setSelected(true);
			fmCheckBox.addItemListener(checkBoxListener);

			if (!colors.containsKey(s)) {
				colors.put(s, PaneConstants.COLOR_TABLE[colorID]);
				colorID = (colorID + 1) % numOfColors;
			}
			fmCheckBox.setBackground(Color.decode(colors.get(s)));
			fmCheckBox.setOpaque(true);

			selectTable.put(fmCheckBox, true);
			userControlPanel.add(fmCheckBox);
			updateGraph();
		}
	}

	private void updateGraph() {
		errorTrace.foldAll(true);
		for (JCheckBox cb : selectTable.keySet()) {
			if (cb == waitBox) {
				Set<Pair<Integer, Integer>> set = td.getWaitNotify();
				if (selectTable.get(cb)) {
					errorTrace.expand(set, PaneConstants.COLOR_TABLE[0]);
				} else {
					errorTrace.resetContent(set, PaneConstants.COLOR_TABLE[0]);
				}
			} else if (cb == threadStartBox) {
				Set<Pair<Integer, Integer>> set = td.getThreadStart();
				if (selectTable.get(cb)) {
					errorTrace.expand(set, PaneConstants.COLOR_TABLE[1]);
				} else {
					errorTrace.resetContent(set, PaneConstants.COLOR_TABLE[1]);
				}
			} else if (selectTable.get(cb)) {
				String str = cb.getText();
				if (str.contains("(un)lock:")) {
					str = str.replace("(un)lock: ", "");
					Set<Pair<Integer, Integer>> set = td.getLocks(str);
					errorTrace.expand(set, colors.get(str));
				} else if (str.contains("field")) {
					// field access
					str = str.replaceAll(".*\\s", "");
					int dotPos = str.lastIndexOf(".");
					String cName = str.substring(0, dotPos);
					String fName = str.substring(dotPos + 1);
					Set<Pair<Integer, Integer>> set = td.getClassField(cName, fName);
					errorTrace.expand(set, colors.get(str));
				} else {
					// method access
					str = str.replaceAll(".*\\s", "");
					int dotPos = str.lastIndexOf(".");
					String cName = str.substring(0, dotPos);
					String mName = str.substring(dotPos + 1);
					Set<Pair<Integer, Integer>> set = td.getClassMethod(cName, mName);
					errorTrace.expand(set, colors.get(str));
				}

			} else {
				String str = cb.getText();
				if (str.contains("(un)lock:")) {
					str = str.replace("(un)lock: ", "");
					Set<Pair<Integer, Integer>> set = td.getLocks(str);
					errorTrace.resetContent(set, colors.get(str));
				} else if (str.contains("field")) {
					str = str.replaceAll(".*\\s", "");
					int dotPos = str.lastIndexOf(".");
					String cName = str.substring(0, dotPos);
					String fName = str.substring(dotPos + 1);
					Set<Pair<Integer, Integer>> set = td.getClassField(cName, fName);
					errorTrace.resetContent(set, colors.get(str));
				} else {
					str = str.replaceAll(".*\\s", "");
					int dotPos = str.lastIndexOf(".");
					String cName = str.substring(0, dotPos);
					String mName = str.substring(dotPos + 1);
					Set<Pair<Integer, Integer>> set = td.getClassMethod(cName, mName);
					errorTrace.resetContent(set, colors.get(str));
				}

			}
		}

		if (isFoldSelected) {
			errorTrace.foldAll(true);
		} else if (isExpandSelected) {
			errorTrace.foldAll(false);
		}

	}

	private class CheckBoxListener implements ItemListener {
		@Override
		public void itemStateChanged(ItemEvent e) {
			Object source = e.getItemSelectable();
			assert (source instanceof JCheckBox);
			JCheckBox cb = (JCheckBox) source;
			if (e.getStateChange() == ItemEvent.SELECTED) {
				selectTable.put(cb, true);
			} else {
				selectTable.put(cb, false);
			}
			updateGraph();
		}
	};

	class ButtonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			assert (e.getSource() instanceof JButton);

			JButton button = (JButton) e.getSource();
			if (button.equals(foldAllButton)) {
				expandAllButton.setSelected(false);
				expandAllButton.setEnabled(true);
				foldAllButton.setEnabled(false);
				foldAllButton.setSelected(false);
				isExpandSelected = false;
				isFoldSelected = true;
			}

			if (button.equals(expandAllButton)) {
				expandAllButton.setSelected(false);
				expandAllButton.setEnabled(false);
				foldAllButton.setEnabled(true);
				foldAllButton.setSelected(false);
				isExpandSelected = true;
				isFoldSelected = false;
			}
			updateGraph();
		}

	}

	class FieldTreeListener implements TreeSelectionListener {
		private JTree tree;
		private JDialog dialog;

		public FieldTreeListener(JTree tree, JDialog dialog) {
			this.tree = tree;
			this.dialog = dialog;
		}

		@Override
		public void valueChanged(TreeSelectionEvent e) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

			if (node == null)
				return;

			if (!(node.getUserObject() instanceof FieldNode)) {
				return;
			}

			FieldNode fn = (FieldNode) node.getUserObject();
			String clsName = fn.clsName;
			String fieldName = fn.fieldName;

			fieldMethodSearch(clsName, fieldName, clsName + "." + fieldName, true);
			dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
		}

	}

	class MethodTreeListener implements TreeSelectionListener {
		private JTree tree;
		private JDialog dialog;

		public MethodTreeListener(JTree tree, JDialog dialog) {
			this.tree = tree;
			this.dialog = dialog;
		}

		@Override
		public void valueChanged(TreeSelectionEvent e) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

			if (node == null)
				return;

			if (!(node.getUserObject() instanceof MethodNode)) {
				return;
			}

			MethodNode fn = (MethodNode) node.getUserObject();
			String clsName = fn.clsName;
			String methodName = fn.methodName;

			fieldMethodSearch(clsName, methodName, clsName + "." + methodName, false);
			dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
		}

	}

	/**
	 * the dropdown list listener
	 *
	 */
	class dropDownListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			if (e.getSource() instanceof JComboBox<?>) {
				JComboBox<?> cb = (JComboBox<?>) e.getSource();
				String newSelection = (String) cb.getSelectedItem();
				if (newSelection.contains("Field")) {
					cb.setSelectedIndex(0);

					if (classFieldExplorer == null) {
						classFieldExplorer = new ClassFieldExplorer(td);
					}
					JTree tree = classFieldExplorer.getTree();

					JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(userControlPanel);

					JDialog dialog = new JDialog(topFrame, "Field Access");
					dialog.add(classFieldExplorer);
					dialog.setVisible(true);
					dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					dialog.setMinimumSize(new Dimension(200, 100));
					dialog.setLocationRelativeTo(userControlPanel);

					FieldTreeListener treeListener = new FieldTreeListener(tree, dialog);
					classFieldExplorer.addTreeSelectionListener(treeListener);

				} else {

					cb.setSelectedIndex(0);
					if (classMethodExplorer == null) {
						classMethodExplorer = new ClassMethodExplorer(td);
					}
					JTree tree = classMethodExplorer.getTree();
					JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(userControlPanel);
					JDialog dialog = new JDialog(topFrame, "Method Access");
					dialog.add(classMethodExplorer);
					dialog.setVisible(true);
					dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					dialog.setMinimumSize(new Dimension(200, 100));
					dialog.setLocationRelativeTo(userControlPanel);

					MethodTreeListener treeListener = new MethodTreeListener(tree, dialog);
					classMethodExplorer.addTreeSelectionListener(treeListener);
				}

			}

		}
	}
}
