import java.util.ArrayList;
//import java.util.LinkedList;
import java.util.List;

import gov.nasa.jpf.util.Left;
import gov.nasa.jpf.util.Pair;
import gov.nasa.jpf.vm.Path;
import gov.nasa.jpf.vm.Step;
import gov.nasa.jpf.vm.Transition;

public class TraceData {

	private int numOfThreads = -1;
	private List<String> threadNames = null;

	private Path path;

	private List<Pair<Integer, Integer>> group = new ArrayList<>();

	private List<String> detailList = new ArrayList<>();
	private List<Integer> heightList = new ArrayList<>();

	public TraceData(Path path) {
		this.path = path;
		if (path.size() == 0) {
			return; // nothing to publish
		}

		int currTran = 0;
		int prevThread = -1;
		int start = -1;

		group = new ArrayList<>(); // group the
									// transition
		threadNames = new ArrayList<>(); // range
		numOfThreads = -1;
		// first pass of the trace
		for (Transition t : path) {
			int currThread = t.getThreadIndex();
			if (threadNames.size() == currThread) {
				threadNames.add(t.getThreadInfo().getName());
			}
			if (currTran == 0) {
				start = 0;
			}
			if (currTran > 0 && currThread != prevThread) {
				group.add(new Pair<Integer, Integer>(start, currTran - 1));
				start = currTran;
			}

			if (currTran == path.size() - 1) {
				group.add(new Pair<Integer, Integer>(start, currTran));
			}

			prevThread = currThread;
			currTran++;
			numOfThreads = Math.max(numOfThreads, currThread);
		}
		numOfThreads++;

		// second pass of the path
		detailList = new ArrayList<>();
		heightList = new ArrayList<>();

		for (Pair<Integer, Integer> p : group) {
			int from = p._1;
			int to = p._2;
			int height = 0;
			StringBuilder tempStr = new StringBuilder();

			for (int i = from; i <= to; i++) {
				Transition t = path.get(i);
				String lastLine = null;
				int nNoSrc = 0;
				tempStr.append(t.getChoiceGenerator() + "\n");
				height++;
				for (Step s : t) {
					String line = s.getLineString();
					if (line != null) {
						String src = line.replaceAll("/\\*.*?\\*/", "").replaceAll("//.*$", "")
								.replaceAll("/\\*.*$", "").replaceAll("^.*?\\*/", "").replaceAll("\\*.*$", "").trim();

						if (!line.equals(lastLine) && src.length() > 1) {
							if (nNoSrc > 0) {
								tempStr.append(" [" + nNoSrc + " insn w/o sources]" + "\n");
								height++;
							}
							tempStr.append(" ");
							tempStr.append(Left.format(s.getLocationString(), 30));
							tempStr.append(": ");
							tempStr.append(src + "\n");
							height++;
							nNoSrc = 0;
						}
					} else { // no source
						nNoSrc++;
					}
					lastLine = line;
				}

			}
			tempStr.deleteCharAt(tempStr.length() - 1);
			detailList.add(tempStr.toString());
			heightList.add(height);

		}

	}

	// getters
	public int getNumberOfThreads() {
		return this.numOfThreads;
	}

	public List<String> getThreadNames() {
		return new ArrayList<>(this.threadNames);
	}

	public int getRows() {
		return path.size();
	}

	public List<String> getDetailList() {
		return new ArrayList<>(this.detailList);
	}

	public List<Integer> getHeightList() {
		return new ArrayList<>(this.heightList);
	}

	public Path getPath() {
		return this.path;
	}

	public List<Pair<Integer, Integer>> getGroup() {
		return group;
	}
}
