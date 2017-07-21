import gov.nasa.jpf.Config;
import gov.nasa.jpf.report.Publisher;
import gov.nasa.jpf.shell.ShellManager;
import gov.nasa.jpf.shell.ShellPanel;
import gov.nasa.jpf.shell.commands.VerifyCommand;
import gov.nasa.jpf.shell.listeners.VerifyCommandListener;
import gov.nasa.jpf.shell.util.ProgressTrackerUI;
import gov.nasa.jpf.util.Pair;
import gov.nasa.jpf.vm.Path;

import java.awt.BorderLayout;
//import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
//import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
//import javax.swing.JList;
import javax.swing.JPanel;
//import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
//import javax.swing.ListSelectionModel;
//import javax.swing.event.ListSelectionEvent;
//import javax.swing.event.ListSelectionListener;
import javax.swing.ListCellRenderer;

import java.util.ArrayList;
//import java.util.ArrayList;
import java.util.HashMap;
//import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
//import java.util.Random;
import java.util.Set;

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
	private ErrorTablePane errorTrace = new ErrorTablePane();

	private ProgressTrackerUI tracker = new ProgressTrackerUI();
	private CardLayout layout = new CardLayout();
	private Path path;
	private TraceData td = null;
	private JPanel checkPanel = new JPanel();// new GridLayout(0, 1)
	private ItemListener checkBoxListener = null;
	// private ItemListener buttonListener = null;
	private Map<String, String> colors = new HashMap<>();
	private int numOfColors = 0;
	private int colorID = 2;
	// private JCheckBox foldAllButton;
	private JButton foldAllButton;
	private boolean isFoldSelected;
	// private JCheckBox expandAllButton;

	private JButton expandAllButton;
	private boolean isExpandSelected;

	private JCheckBox waitBox;
	private JCheckBox threadStartBox;
	// private JCheckBox threadTerminateButton;

	private Map<JCheckBox, Boolean> selectTable;// = new LinkedHashMap<>();

	public ErrorTracePanel() {
		super("Error Trace", null, "View JPF's Output");
		ShellManager.getManager().addCommandListener(VerifyCommand.class, this);

		JPanel tablePanel = new JPanel();
		tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
		tablePanel.add(statusLabel);
		checkPanel.setLayout(new BoxLayout(checkPanel, BoxLayout.Y_AXIS));
		this.numOfColors = PaneConstants.COLOR_TABLE.length;
		
		checkBoxListener = new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				// TODO Auto-generated method stub
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

		ActionListener buttonListener = new ButtonListener(); 

		foldAllButton = new JButton("Collapse all");
		foldAllButton.setMnemonic(KeyEvent.VK_C);
		foldAllButton.setActionCommand("foldAll");
		foldAllButton.addActionListener(buttonListener);

		expandAllButton = new JButton("Expand all");
		expandAllButton.setMnemonic(KeyEvent.VK_E);
		expandAllButton.setActionCommand("expandAll");
		expandAllButton.addActionListener(buttonListener);

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

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, checkPanel, errorTrace);
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

	String publishers = null;

	/**
	 * Requests focus from the Shell then adds the corresponding
	 * {@link gov.nasa.jpf.traceReporter.TopicPublisher TopicPublisher}s for
	 * every trace printer defined (see
	 * {@link gov.nasa.jpf.traceServer.printer.ConsoleTracePrinter
	 * ConsoleTracePrinter} and
	 * {@link gov.nasa.jpf.traceServer.printer.GenericConsoleTracePrinter
	 * GenericConsoleTracePrinter}) to the jpf config. If no trace printer is
	 * defined, {@link gov.nasa.jpf.traceReporter.ConsoleTopicPublisher
	 * ConsoleTopicPublisher} is added.
	 * 
	 * @param command
	 */
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
		if (found) {
			td = new TraceData(path);
			Set<String> fieldNames = td.getFieldNames();
			errorTrace.draw(td);
			checkPanel.removeAll();
			selectTable = new LinkedHashMap<>();
			// add fold all button
			checkPanel.add(foldAllButton);
			// add expand all button
			checkPanel.add(expandAllButton);
			foldAllButton.setEnabled(true);
			foldAllButton.setSelected(true);
			isFoldSelected = true;
			expandAllButton.setSelected(false);
			expandAllButton.setEnabled(false);
			isExpandSelected = false;

			// add wait/notify check box
			waitBox.setSelected(false);
			selectTable.put(waitBox, false);
			checkPanel.add(waitBox);

			// add thread start/join check box
			threadStartBox.setSelected(false);
			selectTable.put(threadStartBox, false);
			checkPanel.add(threadStartBox);

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
				checkPanel.add(cb);
			}

			/**
			 * add drop down list dynamically searching user input for field
			 * access/ method call
			 */
			String[] dropDownStrs = { "", "Field Access ...", "Method call ..." };
			JComboBox highlightList = new JComboBox(dropDownStrs) {
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
			checkPanel.add(highlightList);

			layout.show(this, TOPICS);
			getShell().requestFocus(this);

		} else {
			ShellManager.getManager().getConfig().put("report.publisher", publishers);
			publishers = null;
		}
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
		System.out.println(userInput);
		return userInput;

	}

	public void exceptionDuringVerify(Exception ex) {
	}

	private void popInvalidDialogue(String userInput) {
		JOptionPane.showMessageDialog(checkPanel,
				"Sorry, \"" + userInput + "\" " + "isn't a valid input.\n" + "Please Try again", "Error message",
				JOptionPane.ERROR_MESSAGE);
	}

	private void popNotExistDialogue(String userInput) {
		JOptionPane.showMessageDialog(checkPanel,
				"Sorry, \"" + userInput + "\" " + "does not exist.\n" + "Please Try again", "Error message",
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
			checkPanel.add(fmCheckBox);
			updateGraph();
		}
	}

	private void updateGraph() {
		errorTrace.foldAll(true);
		for (JCheckBox cb : selectTable.keySet()) {
			if (cb == waitBox) {
				Set<Pair<Integer, Integer>> set = td.getWaitNotify();
				if (selectTable.get(cb)) {
					System.out.println("wait start expand");

					errorTrace.expand(set, PaneConstants.COLOR_TABLE[0]);
					System.out.println("wait end expand");
				} else {
					System.out.println("wait start reset");
					errorTrace.resetContent(set, PaneConstants.COLOR_TABLE[0]);
					System.out.println("wait end reset");

				}
			} else if (cb == threadStartBox) {
				Set<Pair<Integer, Integer>> set = td.getThreadStart();
				if (selectTable.get(cb)) {
					errorTrace.expand(set, PaneConstants.COLOR_TABLE[1]);
					// System.out.println("wait expand");

				} else {
					// System.out.println("wait reset");
					errorTrace.resetContent(set, PaneConstants.COLOR_TABLE[1]);
				}
			} else if (selectTable.get(cb)) {
				String str = cb.getText();
				if (str.contains("(un)lock:")) {
					str = str.replace("(un)lock: ", "");
					Set<Pair<Integer, Integer>> set = td.getLocks(str);
					System.out.println("expand start " + "(un)lock " + str);

					errorTrace.expand(set, colors.get(str));
					System.out.println("expand end " + "(un)lock " + str);
				} else if (str.contains("field")) {
					System.out.println("expand start " + "(un)lock " + str);

					str = str.replaceAll(".*\\s", "");
					// String[] strs = str.split("\\.");
					int dotPos = str.lastIndexOf(".");
					String cName = str.substring(0, dotPos);
					String fName = str.substring(dotPos + 1);
					Set<Pair<Integer, Integer>> set = td.getClassField(cName, fName);
					errorTrace.expand(set, colors.get(str));
				} else {
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
					System.out.println("reset start " + "(un)lock " + str);

					errorTrace.resetContent(set, colors.get(str));
					System.out.println("reset end " + "(un)lock " + str);
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
		// System.out.println("isFoldSelected = " + isFoldSelected);
		// System.out.println("isExpandSelected = " + isExpandSelected);

		if (isFoldSelected) {
			errorTrace.foldAll(true);
		} else if (isExpandSelected) {
			errorTrace.foldAll(false);
		}

	}

	class ButtonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			if ("foldAll".equals(e.getActionCommand())) {
				if (isFoldSelected) {
					foldAllButton.setSelected(false);
					expandAllButton.setEnabled(true);
					isFoldSelected = false;
					isExpandSelected = false;
				} else {
					foldAllButton.setSelected(true);
					expandAllButton.setEnabled(false);
					errorTrace.foldAll(true);
					isFoldSelected = true;
					isExpandSelected = false;
				}
			} else {
				System.out.println(isExpandSelected + " expandAllButton");
				if (isExpandSelected) {
					expandAllButton.setSelected(false);
					foldAllButton.setEnabled(true);
					isExpandSelected = false;
					isFoldSelected = false;
				} else {
					expandAllButton.setSelected(true);
					foldAllButton.setEnabled(false);
					errorTrace.foldAll(false);
					isExpandSelected = true;
					isFoldSelected = false;
				}
			}
			updateGraph();
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
				String userInput;
				boolean isField;
				if (newSelection.contains("Field")) {
					userInput = showDialog("field", checkPanel);
					isField = true;
				} else {
					userInput = showDialog("method", checkPanel);
					isField = false;
				}
				cb.setSelectedIndex(0);

				if (userInput == null) {
					return;
				}
				if (!userInput.contains(".")) {
					popInvalidDialogue(userInput);

				} else {
					int dotPos = userInput.lastIndexOf(".");
					if (dotPos == 0 || dotPos == userInput.length() - 1) {
						popInvalidDialogue(userInput);
					} else {
						String clsName = userInput.substring(0, dotPos);
						String fmName = userInput.substring(dotPos + 1, userInput.length());
						// use TraceData to find clsname.fmName
						fieldMethodSearch(clsName, fmName, userInput, isField);
					}
				}
			}
		}
	}

}
