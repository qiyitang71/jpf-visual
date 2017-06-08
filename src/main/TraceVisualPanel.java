//
// Copyright (C) 2010 Igor Andjelkovic (igor.andjelkovic@gmail.com).
// All Rights Reserved.
//
// This software is distributed under the NASA Open Source Agreement
// (NOSA), version 1.3.  The NOSA has been approved by the Open Source
// Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
// directory tree for the complete NOSA document.
//
// THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
// KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
// LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
// SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
// A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
// THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
// DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//

import gov.nasa.jpf.Config;
import gov.nasa.jpf.report.Publisher;
import gov.nasa.jpf.shell.ShellManager;
import gov.nasa.jpf.shell.ShellPanel;
import gov.nasa.jpf.shell.commands.VerifyCommand;
import gov.nasa.jpf.shell.listeners.VerifyCommandListener;
import gov.nasa.jpf.shell.util.HyperlinkEditorPane;
import gov.nasa.jpf.shell.util.ProgressTrackerUI;
import gov.nasa.jpf.shell.util.hyperlinks.BasicHyperLinkDecorator;
import gov.nasa.jpf.shell.util.hyperlinks.JavaSourceFileHyperlinkPattern;
import gov.nasa.jpf.shell.util.hyperlinks.StacktraceHyperlinkPattern;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.AbstractListModel;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;

/**
 * Basic output panel that divides new trace printer's results into browseable
 * topics. This panel uses a
 * {@link gov.nasa.jpf.shell.listeners.VerifyCommandListener} to keep track of
 * when the VerifyCommand is executed.
 */

public class TraceVisualPanel extends ShellPanel implements VerifyCommandListener {
	private static final long serialVersionUID = 1L;
	private static final String PROGRESS = "PROGRESS";
	private static final String TOPICS = "TOPICS";

	// Topics Panel
	private JLabel statusLabel = new JLabel();
	private JSplitPane splitPane;
	private HyperlinkEditorPane outputArea;
	private TopicListModel topicListModel = new TopicListModel();
	private JList topicList = new JList(topicListModel);

	private ProgressTrackerUI tracker = new ProgressTrackerUI();
	private boolean isSaveable = false;

	private CardLayout layout = new CardLayout();

	public TraceVisualPanel() {
		super("Trace Visual", null, "View JPF's Output");
		ShellManager.getManager().addCommandListener(VerifyCommand.class, this);

		// Prepare the list of topics
		topicList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		topicList.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mousePressed(java.awt.event.MouseEvent evt) {
				popupMenu(evt);
			}

			@Override
			public void mouseReleased(java.awt.event.MouseEvent evt) {
				popupMenu(evt);
			}
		});
		topicList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
			public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
				// Make sure this doesn't get called multiple times for one
				// event
				if (evt.getValueIsAdjusting() == false) {
					updateTextArea();
				}
			}
		});

		// Prepare the text area
		outputArea = new HyperlinkEditorPane();

		JavaSourceFileHyperlinkPattern jsf = new JavaSourceFileHyperlinkPattern();
		StacktraceHyperlinkPattern sh = new StacktraceHyperlinkPattern();
		outputArea.addHyperlinkPattern(jsf);
		outputArea.addHyperlinkPattern(sh);

		// Decorate!
		BasicHyperLinkDecorator decorator = new BasicHyperLinkDecorator();
		outputArea.setHyperlinkDecorator(jsf, decorator);
		outputArea.setHyperlinkDecorator(sh, decorator);

		outputArea.setEditable(false);

		JScrollPane textScroll = new JScrollPane(outputArea);
		textScroll.getViewport().setBackground(Color.white);
		textScroll.setMinimumSize(new Dimension(100, 50));

		JScrollPane listScroll = new JScrollPane(topicList);
		listScroll.setMinimumSize(new Dimension(100, 50));

		JPanel topicsPanel = new JPanel();
		topicsPanel.setLayout(new BoxLayout(topicsPanel, BoxLayout.Y_AXIS));
		topicsPanel.add(statusLabel);

		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, textScroll);
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerLocation(150);
		topicsPanel.add(splitPane);

		setLayout(layout);

		add(topicsPanel, TOPICS);
		add(tracker, PROGRESS);
		layout.show(this, PROGRESS);
	}

	private void updateTextArea() {
		String topic = (String) topicList.getSelectedValue();
		if (topic != null) {
			String text = topicListModel.getTopics().get(topic).toString();
			outputArea.setText(text); // Keep the white space
			outputArea.setCaretPosition(0);
		}
	}

	private void popupMenu(java.awt.event.MouseEvent evt) {
		if (isSaveable && evt.isPopupTrigger()) {
			JPopupMenu popup = new JPopupMenu();
			JMenuItem item = new JMenuItem("Save Result...");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JFileChooser chooser = new JFileChooser();
					int result = chooser.showSaveDialog(null);

					if (result != JFileChooser.APPROVE_OPTION)
						return;

					final File file = chooser.getSelectedFile();
					if (file.exists() && !approveOverwrite(file.getName()))
						return;

					// Output the file in a different thread
					Runnable saveFile = new Runnable() {
						public void run() {
							try {
								file.createNewFile();
								PrintWriter out = new PrintWriter(file);
								String time = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss z").format(new Date());
								out.print("Created: ");
								out.println(time);
								out.print("Created By: ");
								out.println(System.getProperty("user.name"));

								for (Entry<String, Topic> e : topicListModel.getTopics().entrySet())
									out.println(e.getValue().getContent());

								if (out.checkError()) {
									getShell().error("There was an error saving the results.");
								}
								out.close();
							} catch (IOException ex) {
								getShell().error("File: " + file.getName() + " could not created.");
								return;
							}
						}
					};
					new Thread(saveFile).start();
				}
			});
			popup.add(item);
			popup.show(evt.getComponent(), evt.getX(), evt.getY());
		}
	}

	private boolean approveOverwrite(String fileName) {
		String message = "File: " + fileName + " already exists.\n"
				+ "Are you sure that you want to overwrite its contents?";
		return JOptionPane.showConfirmDialog(null, message, "Overwrite " + fileName + " ?",
				JOptionPane.YES_NO_CANCEL_OPTION) == JOptionPane.YES_OPTION;
	}

	private void showResults() {
		isSaveable = true;
		topicList.repaint();
		topicList.setSelectedIndex(0);
	}

	private void addResults(Map<String, Topic> topics) {
		topicListModel.addTopics(topics);
	}

	private void reset() {
		isSaveable = false;
		topicListModel.clear();
		outputArea.setText("");
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

		if (publishers.contains("consoleTracePrinter") || !publishers.contains("genericConsoleTracePrinter")) {
			config.put("report.publisher", config.getProperty("report.publisher", "") + ",consoleTopic");
			config.put("report.consoleTopic.class", ConsoleTopicPublisher.class.getName());
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
			if (publisher instanceof TopicPublisher) {
				if (!found) {
					reset();
					found = true;
				}
				addResults(((TopicPublisher) publisher).getResults());
			}
		}
		if (found) {
			showResults();
			layout.show(this, TOPICS);
			getShell().requestFocus(this);
		} else {
			ShellManager.getManager().getConfig().put("report.publisher", publishers);
			publishers = null;
		}
	}

	public void exceptionDuringVerify(Exception ex) {
	}

	@SuppressWarnings("serial")
	private class TopicListModel extends AbstractListModel {

		private Map<String, Topic> topics = new HashMap<String, Topic>();

		public void addTopics(Map<String, Topic> topics) {
			this.topics.putAll(topics);
			fireIntervalAdded(this, 0, topics.size());
		}

		public void clear() {
			int size = getSize();
			topics.clear();
			fireIntervalRemoved(this, 0, size < 0 ? 0 : size);
		}

		public Map<String, Topic> getTopics() {
			return topics;
		}

		public int getSize() {
			return topics.size();
		}

		public Object getElementAt(int index) {
			return topics.keySet().toArray()[index];
		}
	}
}
