import java.util.ArrayList;
import java.util.HashSet;
//import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import gov.nasa.jpf.jvm.bytecode.EXECUTENATIVE;
import gov.nasa.jpf.jvm.bytecode.MONITORENTER;
import gov.nasa.jpf.jvm.bytecode.VirtualInvocation;
import gov.nasa.jpf.util.Left;
import gov.nasa.jpf.util.Pair;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MethodInfo;
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

		// group the transition range
		group = new ArrayList<>();
		threadNames = new ArrayList<>();
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

	public Set<Pair<Integer, Integer>> getWaitNotify() {
		Set<Integer> hasInfo = new HashSet<>();
		// Set<Integer> lineInfo = new HashSet<>();
		// Pair<Set<Integer>, Set<Integer>> hasInfos = new Pair<Set<Integer>,
		// Set<Integer>>(hasInfo, lineInfo);
		Set<Pair<Integer, Integer>> hasInfos = new HashSet<>();
		for (int pi = 0; pi < group.size(); pi++) {
			Pair<Integer, Integer> p = group.get(pi);
			MethodInfo lastMi = null;
			int start = p._1;
			int end = p._2;
			int height = 0;

			for (int i = start; i <= end; i++) {
				Transition t = path.get(i);
				String lastLine = null;
				int nNoSrc = 0;
				// tempStr.append(t.getChoiceGenerator() + "\n");
				height++;
				for (int ithStep = 0; ithStep < t.getStepCount(); ithStep++) {
					Step s = t.getStep(ithStep);
					String line = s.getLineString();
					if (line != null) {
						String src = line.replaceAll("/\\*.*?\\*/", "").replaceAll("//.*$", "")
								.replaceAll("/\\*.*$", "").replaceAll("^.*?\\*/", "").replaceAll("\\*.*$", "").trim();

						if (!line.equals(lastLine) && src.length() > 1) {
							if (nNoSrc > 0) {
								// tempStr.append(" [" + nNoSrc + " insn w/o
								// sources]" + "\n");
								height++;

							}

							// tempStr.append(" ");
							// tempStr.append(Left.format(s.getLocationString(),
							// 30));
							// tempStr.append(": ");
							// tempStr.append(src + "\n");
							height++;
							nNoSrc = 0;
						}
					} else { // no source
						nNoSrc++;
					}
					lastLine = line;

					Instruction insn = s.getInstruction();

					if (insn instanceof VirtualInvocation) {
						System.out.println("insn = " + insn);
						String insnStr = insn.toString();
						if (insnStr.contains("java.lang.Object.wait()") || insnStr.contains("java.lang.Object.notify()")
								|| insnStr.contains("java.lang.Object.notifyAll()")) {
							hasInfos.add(new Pair<>(pi, height - 1));
							hasInfo.add(pi);
							System.out.println("row = " + pi + " height =" + height);
							// break;
						}
					}
					// if (nNoSrc == 0) {

					// if (i == end && ithStep == t.getStepCount() - 1) {
					// height--;
					// }

					// }
				}
				// if (hasInfo.size() > 0 && hasInfo.contains(pi)) {
				// break;
				// }
			}
			System.out.println("height = " + height);
		}
		return new HashSet<>(hasInfos);
	}

	// pair of <group num, line num>
	public Set<Pair<Integer, Integer>> getLocks() {
		Set<Integer> hasInfo = new HashSet<>();
		Set<Pair<Integer, Integer>> hasInfos = new HashSet<>();
		for (int pi = 0; pi < group.size(); pi++) {
			Pair<Integer, Integer> p = group.get(pi);
			MethodInfo lastMi = null;
			int start = p._1;
			int end = p._2;
			int height = 0;

			for (int i = start; i <= end; i++) {
				Transition t = path.get(i);
				String lastLine = null;
				int nNoSrc = 0;
				// tempStr.append(t.getChoiceGenerator() + "\n");
				height++;
				for (int ithStep = 0; ithStep < t.getStepCount(); ithStep++) {
					Step s = t.getStep(ithStep);
					String line = s.getLineString();
					if (line != null) {
						String src = line.replaceAll("/\\*.*?\\*/", "").replaceAll("//.*$", "")
								.replaceAll("/\\*.*$", "").replaceAll("^.*?\\*/", "").replaceAll("\\*.*$", "").trim();

						if (!line.equals(lastLine) && src.length() > 1) {
							if (nNoSrc > 0) {
								height++;

							}
							height++;
							nNoSrc = 0;
						}
					} else { // no source
						nNoSrc++;
					}
					lastLine = line;

					Instruction insn = s.getInstruction();

					if (insn instanceof MONITORENTER) {
						hasInfos.add(new Pair<>(pi, height - 1));
						hasInfo.add(pi);
						System.out.println("row = " + pi + " height =" + height);
					}
				}
			}
			System.out.println("height = " + height);
		}
		return new HashSet<>(hasInfos);
	}
}
