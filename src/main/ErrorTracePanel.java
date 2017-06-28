import gov.nasa.jpf.Config;
import gov.nasa.jpf.report.Publisher;
import gov.nasa.jpf.shell.ShellManager;
import gov.nasa.jpf.shell.ShellPanel;
import gov.nasa.jpf.shell.commands.VerifyCommand;
import gov.nasa.jpf.shell.listeners.VerifyCommandListener;
import gov.nasa.jpf.shell.util.ProgressTrackerUI;
import gov.nasa.jpf.util.Pair;
import gov.nasa.jpf.vm.Path;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
	private JPanel checkPanel = new JPanel(new GridLayout(0, 1));
	private Set<JCheckBox> checkButtons = new HashSet<>();
	private Set<String> fieldNames = null;
	private ItemListener listener = null;
	private Map<String, String> colors = new HashMap<>();

	public ErrorTracePanel() {
		super("Error Trace", null, "View JPF's Output");
		ShellManager.getManager().addCommandListener(VerifyCommand.class, this);

		JPanel tablePanel = new JPanel();
		tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
		tablePanel.add(statusLabel);
		// tablePanel.setBorder(BorderFactory.createEmptyBorder());

		listener = new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				// TODO Auto-generated method stub
				Object source = e.getItemSelectable();
				boolean isWait = false;
				String str = null;
				if (source instanceof JCheckBox) {

					JCheckBox cb = (JCheckBox) source;
					System.out.println("################" + cb.getText());

					if (cb.getText() == "wait/notify") {
						if (td == null)
							return;
						Set<Pair<Integer, Integer>> set = td.getWaitNotify();
						if (e.getStateChange() == ItemEvent.SELECTED) {
							errorTrace.expand(set, "yellow");
						} else {
							errorTrace.expand(set, "white");
						}
					} else {
						if (td == null)
							return;
						str = cb.getText().replace("(un)lock: ", "");
						Set<Pair<Integer, Integer>> set = td.getLocks(str);
						if (e.getStateChange() == ItemEvent.SELECTED) {
							errorTrace.expand(set, colors.get(str));
						} else {
							errorTrace.expand(set, "white");
						}
					}
				}
				// if (source == ) {
				//
				// } else if (source == ) {
				//
				// } else {
				//
				// }
				// Now that we know which button was pushed, find out
				// whether it was selected or deselected.

			}

		};

		// JCheckBox foldButton = new JCheckBox("Fold All");
		// foldButton.setMnemonic(KeyEvent.VK_F);
		// foldButton.setSelected(true);
		// foldButton.addItemListener(listener);

		JCheckBox waitButton = new JCheckBox("wait/notify");
		waitButton.setMnemonic(KeyEvent.VK_W);
		waitButton.setSelected(false);
		waitButton.addItemListener(listener);

		// checkButtons.add(foldButton);
		checkButtons.add(waitButton);
		checkPanel.add(waitButton);

		String[] selectionList = new String[] { "table", "wait/notify", "lock/unlock" };
		JList<String> list = new JList<>(selectionList);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setSelectedIndex(0);
		list.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent evt) {
				// Make sure this doesn't get called multiple times for one
				// event
				// System.out.println("yyy");

				if (evt.getValueIsAdjusting() == false) {

					String topic = (String) list.getSelectedValue();
					if (topic.equals("wait/notify")) {
						if (td == null)
							return;
						Set<Pair<Integer, Integer>> set = td.getWaitNotify();
						errorTrace.expand(set, "yellow");
						System.out.println("wait/notify");

						// for (int i : set) {
						// System.out.println(i);
						// }
					}

					if (topic.equals("lock/unlock")) {
						if (td == null)
							return;
						Set<Pair<Integer, Integer>> set = td.getLocks();
						errorTrace.expand(set, "red");
						System.out.println("lock/unlock");
						Set<String> fieldNames = td.getFieldNames();
						System.out.println(fieldNames);

					}

					if (topic.equals("table")) {
						errorTrace.foldAll();
					}

				}

			}
		});

		JScrollPane listScrollPane = new JScrollPane(list);
		listScrollPane.setMinimumSize(new Dimension(100, 50));

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
			fieldNames = td.getFieldNames();
			errorTrace.draw(td);
			for (String s : fieldNames) {
				JCheckBox cb = new JCheckBox("(un)lock: " + s);
				cb.setSelected(false);
				cb.addItemListener(listener);
				int nextInt = new Random().nextInt(256 * 256 * 256);
				// format it as hexadecimal string (with hashtag and leading
				// zeros)
				String colorCode = String.format("#%06x", nextInt);
				colors.put(s, colorCode);

				checkButtons.add(cb);
				checkPanel.add(cb);
			}

			layout.show(this, TOPICS);
			getShell().requestFocus(this);

		} else {
			ShellManager.getManager().getConfig().put("report.publisher", publishers);
			publishers = null;
		}
	}

	public void exceptionDuringVerify(Exception ex) {
	}
}
