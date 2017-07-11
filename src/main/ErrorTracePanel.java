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
	private ItemListener listener = null;
	// private ItemListener buttonListener = null;
	private Map<String, String> colors = new HashMap<>();
	private int colorID = 2;
	// private JCheckBox foldAllButton;
	private JButton foldAllButton;
	private boolean isFoldSelected;
	// private JCheckBox expandAllButton;

	private JButton expandAllButton;
	private boolean isExpandSelected;

	private JCheckBox waitButton;
	private JCheckBox threadStartButton;
	// private JCheckBox threadTerminateButton;

	private Map<JCheckBox, Boolean> selectTable;// = new LinkedHashMap<>();

	public ErrorTracePanel() {
		super("Error Trace", null, "View JPF's Output");
		ShellManager.getManager().addCommandListener(VerifyCommand.class, this);

		JPanel tablePanel = new JPanel();
		tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
		tablePanel.add(statusLabel);
		// tablePanel.setBorder(BorderFactory.createEmptyBorder());
		checkPanel.setLayout(new BoxLayout(checkPanel, BoxLayout.Y_AXIS));

		listener = new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				// TODO Auto-generated method stub
				Object source = e.getItemSelectable();
				assert (source instanceof JCheckBox);
				// if (source instanceof JCheckBox) {

				JCheckBox cb = (JCheckBox) source;
				// System.out.println("################" + cb.getText());
				//
				// if (cb.getText() == "summary") {
				// //
				// // if (e.getStateChange() == ItemEvent.SELECTED) {
				// // selectTable.put(cb, true);
				// // expandAllButton.setSelected(false);
				// // selectTable.put(expandAllButton, false);
				// // }else{
				// // selectTable.put(cb, false);
				// // }
				// // } else if (cb.getText() == "Expand All") {
				// // if (e.getStateChange() == ItemEvent.SELECTED) {
				// // foldAllButton.setSelected(false);
				// // selectTable.put(cb, true);
				// // selectTable.put(foldAllButton, false);
				// // }else{
				// // selectTable.put(cb, false);
				// // }
				// } else {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					selectTable.put(cb, true);
				} else {
					selectTable.put(cb, false);
				}
				// }
				updateGraph();
				// }
			}
		};

		// buttonListener = new ItemListener() {
		// @Override
		// public void itemStateChanged(ItemEvent e) {
		// // TODO Auto-generated method stub
		// System.out.println("buttonListener");
		//
		// Object source = e.getItemSelectable();
		// assert (source instanceof JButton);
		// // if (source instanceof JCheckBox) {
		//
		// JButton button = (JButton) source;
		// if (button == foldAllButton) {
		// System.out.println("foldAllButton");
		//
		// if (e.getStateChange() == ItemEvent.SELECTED) {
		// expandAllButton.setEnabled(false);
		// errorTrace.foldAll(true);
		// System.out.println("selected");
		// } else {
		// System.out.println("diselect");
		// expandAllButton.setEnabled(true);
		// }
		// } else {
		// if (e.getStateChange() == ItemEvent.SELECTED) {
		// foldAllButton.setEnabled(false);
		// errorTrace.foldAll(false);
		// } else {
		// foldAllButton.setEnabled(true);
		// }
		// }
		// }
		// };
		ActionListener buttonListener = new ActionListener() {

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
						// updateGraph();
					}
					// foldAllButton.setEnabled(false);
					// expandAllButton.setEnabled(true);
				}
				updateGraph();
				// if (e.getSource() == foldAllButton) {
				// }
			}

		};

		foldAllButton = new JButton("Collapse all");
		// b1.setVerticalTextPosition(AbstractButton.CENTER);
		// b1.setHorizontalTextPosition(AbstractButton.LEADING); //aka LEFT, for
		// left-to-right locales
		foldAllButton.setMnemonic(KeyEvent.VK_C);
		foldAllButton.setActionCommand("foldAll");
		foldAllButton.addActionListener(buttonListener);
		// foldAllButton.add
		// foldAllButton.addItemListener(buttonListener);

		// foldAllButton = new JCheckBox("summary");
		// foldAllButton.setMnemonic(KeyEvent.VK_S);
		// foldAllButton.setSelected(true);
		// foldAllButton.addItemListener(listener);

		expandAllButton = new JButton("Expand all");
		// b1.setVerticalTextPosition(AbstractButton.CENTER);
		// b1.setHorizontalTextPosition(AbstractButton.LEADING); //aka LEFT, for
		// left-to-right locales
		expandAllButton.setMnemonic(KeyEvent.VK_E);
		// `expandAllButton.addItemListener(buttonListener);
		expandAllButton.setActionCommand("expandAll");
		expandAllButton.addActionListener(buttonListener);
		// expandAllButton.addActionListener(buttonListener);
		// expandAllButton = new JCheckBox("Expand All");
		// expandAllButton.setMnemonic(KeyEvent.VK_E);
		// expandAllButton.setSelected(false);
		// expandAllButton.addItemListener(listener);

		// JCheckBox foldButton = new JCheckBox("Fold All");
		// foldButton.setMnemonic(KeyEvent.VK_F);
		// foldButton.setSelected(true);
		// foldButton.addItemListener(listener);
		waitButton = new JCheckBox("wait/notify");
		waitButton.setBackground(Color.decode(PaneConstants.COLOR_TABLE[0]));
		waitButton.setOpaque(true);
		waitButton.setMnemonic(KeyEvent.VK_W);
		waitButton.addItemListener(listener);

		threadStartButton = new JCheckBox("thread start/join");
		threadStartButton.setBackground(Color.decode(PaneConstants.COLOR_TABLE[1]));
		threadStartButton.setOpaque(true);
		threadStartButton.setMnemonic(KeyEvent.VK_S);
		threadStartButton.addItemListener(listener);

		// threadTerminateButton = new JCheckBox("thread terminate");
		// threadTerminateButton.setBackground(Color.decode(PaneConstants.COLOR_TABLE[2]));
		// threadTerminateButton.setOpaque(true);
		// threadTerminateButton.setMnemonic(KeyEvent.VK_T);
		// threadTerminateButton.addItemListener(listener);

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

	public void updateGraph() {
		errorTrace.foldAll(true);
		for (JCheckBox cb : selectTable.keySet()) {
			if (cb == waitButton) {
				Set<Pair<Integer, Integer>> set = td.getWaitNotify();
				if (selectTable.get(cb)) {
					System.out.println("wait start expand");

					errorTrace.expand(set, PaneConstants.COLOR_TABLE[0]);
					System.out.println("wait end expand");
				} else {
					System.out.println("wait start reset");
					errorTrace.resetContent(set);
					System.out.println("wait end reset");

				}
			} else if (cb == threadStartButton) {
				Set<Pair<Integer, Integer>> set = td.getThreadStart();
				if (selectTable.get(cb)) {
					errorTrace.expand(set, PaneConstants.COLOR_TABLE[1]);
					// System.out.println("wait expand");

				} else {
					// System.out.println("wait reset");
					errorTrace.resetContent(set);
				}
			} else if (selectTable.get(cb)) {
				String str = cb.getText().replace("(un)lock: ", "");
				Set<Pair<Integer, Integer>> set = td.getLocks(str);
				System.out.println("expand start " + "(un)lock " + str);

				errorTrace.expand(set, colors.get(str));
				System.out.println("expand end " + "(un)lock " + str);

			} else {
				String str = cb.getText().replace("(un)lock: ", "");
				Set<Pair<Integer, Integer>> set = td.getLocks(str);
				System.out.println("reset start " + "(un)lock " + str);

				errorTrace.resetContent(set);
				System.out.println("reset end " + "(un)lock " + str);

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
			// foldAllButton.setEnabled(true);
			// foldAllButton.setSelected(true);
			// expandAllButton.setEnabled(false);
			checkPanel.add(foldAllButton);
			checkPanel.add(expandAllButton);
			foldAllButton.setEnabled(true);
			foldAllButton.setSelected(true);
			isFoldSelected = true;
			expandAllButton.setSelected(false);
			expandAllButton.setEnabled(false);
			isExpandSelected = false;
			// selectTable.put(foldAllButton, true);
			// selectTable.put(expandAllButton, false);
			waitButton.setSelected(false);
			selectTable.put(waitButton, false);
			threadStartButton.setSelected(false);
			selectTable.put(threadStartButton, false);
			// threadTerminateButton.setSelected(false);
			// selectTable.put(threadTerminateButton, false);
			// checkButtons.add(foldButton);
			checkPanel.add(waitButton);
			checkPanel.add(threadStartButton);
			// checkPanel.add(threadTerminateButton);

			for (String s : fieldNames) {
				JCheckBox cb = new JCheckBox("(un)lock: " + s);
				cb.setSelected(false);
				cb.addItemListener(listener);

				if (!colors.containsKey(s)) {
					// int nextInt = new Random().nextInt(256 * 256 * 256);
					// while (nextInt < 100000) {
					// nextInt = new Random().nextInt(256 * 256 * 256);
					// }
					// // format it as hexadecimal string (with hashtag and
					// leading
					// // zeros)
					// String colorCode = String.format("#%06x", nextInt);

					if (colorID < 14)
						colors.put(s, PaneConstants.COLOR_TABLE[colorID++]);
					else
						colors.put(s, PaneConstants.COLOR_TABLE[14]);

				}
				cb.setBackground(Color.decode(colors.get(s)));
				cb.setOpaque(true);
				selectTable.put(cb, false);
				checkPanel.add(cb);
			}

			int remainColors = 15 - colorID;

			/**
			 * add drop down list
			 */

			JLabel dropDownLabel = new JLabel("Highlight other");
			//checkPanel.add(dropDownLabel);
			String[] dropDownStrs = { "Class.field", "Class.method", };
			ImageIcon removeIcon = null;
			File f = new File("remove.png");
			if (f.exists() && !f.isDirectory()) {
				removeIcon = new ImageIcon("remove.png");
			} else {
				System.err.println("Couldn't find file: " + "remove.png");
			}

			// double scale = 0.001;
			// if (removeIcon != null) {
			// Image src = removeIcon.getImage();
			// int w = 10;
			// int h = 10;
			// int type = BufferedImage.TYPE_INT_RGB;
			// BufferedImage dst = new BufferedImage(w, h, type);
			// Graphics2D g2 = dst.createGraphics();
			// g2.drawImage(src, 0, 0, w, h, this);
			// g2.dispose();
			// removeIcon = new ImageIcon(dst);
			// }

			// final ImageIcon icon = removeIcon;

			Integer[] intArray = new Integer[2];
			for (int i = 0; i < 2; i++) {
				intArray[i] = i;
			}
			JComboBox highlightList = new JComboBox(intArray) {
				@Override
				public Dimension getMaximumSize() {
					Dimension max = super.getMaximumSize();
					max.height = getPreferredSize().height;
					return max;
				}

			};

			highlightList.getRenderer();
			ComboBoxRenderer renderer = new ComboBoxRenderer(removeIcon, dropDownStrs);
			renderer.setPreferredSize(new Dimension(50, 30));
			highlightList.setRenderer(renderer);
			highlightList.setMaximumRowCount(remainColors);
			highlightList.setAlignmentX(0);
			highlightList.setAlignmentY(0);
			;

			checkPanel.add(highlightList);

			layout.show(this, TOPICS);
			getShell().requestFocus(this);

		} else {
			ShellManager.getManager().getConfig().put("report.publisher", publishers);
			publishers = null;
		}
	}

	@SuppressWarnings("serial")
	class ComboBoxRenderer extends JLabel implements ListCellRenderer {
		private Font uhOhFont;
		ImageIcon removeIcon;
		String[] dropDownStrs;

		public ComboBoxRenderer(ImageIcon removeIcon, String[] dropDownStrs) {
			this.removeIcon = removeIcon;
			this.dropDownStrs = dropDownStrs;
			setOpaque(true);
			setHorizontalAlignment(LEFT);
			setVerticalAlignment(TOP);
		}

		/*
		 * This method finds the image and text corresponding to the selected
		 * value and returns the label, set up to display the text and image.
		 */
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			// Get the selected index. (The index param isn't
			// always valid, so just use the value.)
			// System.out.println(value);

			int selectedIndex = ((Integer) value).intValue();
			System.out.println(selectedIndex);
			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			} else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}

			// Set the icon and text. If icon was null, say so.
			String item = dropDownStrs[selectedIndex];
			setIcon(removeIcon);
			if (removeIcon != null) {
				setText(item);
				setFont(list.getFont());
			} else {
				setUhOhText(item + " (no image available)", list.getFont());
			}

			return this;
		}

		// Set the font and text when no image was found.
		protected void setUhOhText(String uhOhText, Font normalFont) {
			if (uhOhFont == null) { // lazily create this font
				uhOhFont = normalFont.deriveFont(Font.ITALIC);
			}
			setFont(uhOhFont);
			setText(uhOhText);
		}

	}

	public void exceptionDuringVerify(Exception ex) {
	}
}
