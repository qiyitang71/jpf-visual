import gov.nasa.jpf.Config;
import gov.nasa.jpf.report.Publisher;
import gov.nasa.jpf.report.Reporter;
import gov.nasa.jpf.util.Pair;
import gov.nasa.jpf.vm.Path;
import gov.nasa.jpf.vm.Transition;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

public class ErrorTracePrinter extends Publisher {

	// output destinations

	//List<Pair<Integer, Integer>> group;
	//int numOfThreads;
	Path path;

	public ErrorTracePrinter(Config conf, Reporter reporter) {
		super(conf, reporter);
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "errorTracePrinter";
	}

	@Override
	protected void openChannel() {
		if (out == null) {
			out = new PrintWriter(System.out, true);
		}
	}

	@Override
	protected void closeChannel() {
		out.close();
	}

	@Override
	public void publishTopicStart(String topic) {
		out.println();
		out.print("====================================================== ");
		out.println(topic);
	}

	/**
	 * this is done as part of the property violation reporting, i.e. we have an
	 * error
	 */
	@Override
	protected void publishTrace() {
		path = reporter.getPath();

//		if (path.size() == 0) {
//			return; // nothing to publish
//		}
//
//		publishTopicStart("trace " + reporter.getCurrentErrorId());
//		int currTran = 0;
//		int prevThread = -1;
//		numOfThreads = -1;
//		int start = -1;
//		group = new LinkedList<>();
//		// first pass of the trace
//		for (Transition t : path) {
//			int currThread = t.getThreadIndex();
//			if (currTran == 0) {
//				start = 0;
//			}
//			if (currTran > 0 && currThread != prevThread) {
//				group.add(new Pair<Integer, Integer>(start, currTran - 1));
//				start = currTran;
//			}
//
//			if (currTran == path.size() - 1) {
//				group.add(new Pair<Integer, Integer>(start, currTran));
//			}
//
//			prevThread = currThread;
//			currTran++;
//			numOfThreads = Math.max(numOfThreads, currThread);
//		}

	}

//	public int getNumberOfThreads() {
//		return numOfThreads;
//	}

	public Path getPath() {
		return path.clone();
	}
//
//	public List<Pair<Integer, Integer>> getGroupOfTransitions() {
//		return new LinkedList<>(group);
//	}

}
