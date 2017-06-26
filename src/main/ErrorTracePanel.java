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

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.util.List;
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

	public ErrorTracePanel() {
		super("Error Trace", null, "View JPF's Output");
		ShellManager.getManager().addCommandListener(VerifyCommand.class, this);

		JPanel tablePanel = new JPanel();
		tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
		tablePanel.add(statusLabel);
		//tablePanel.setBorder(BorderFactory.createEmptyBorder());

		String[] selectionList = new String[] { "table", "wait/notify", "lock/unlock" };
		JList list = new JList(selectionList);
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
					
					if(topic.equals("lock/unlock")){
						if (td == null)
							return;
						Set<Pair<Integer, Integer>> set = td.getLocks();
						errorTrace.expand(set,"red");
						System.out.println("lock/unlock");
					}
					
					if (topic.equals("table")) {
						errorTrace.foldAll();
					}


				}

			}
		});

		JScrollPane listScrollPane = new JScrollPane(list);
		listScrollPane.setMinimumSize(new Dimension(100, 50));

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScrollPane, errorTrace);
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerLocation(100);
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
			errorTrace.draw(td);
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
