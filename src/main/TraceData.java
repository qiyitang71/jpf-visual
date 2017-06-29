import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
//import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

//import gov.nasa.jpf.jvm.bytecode.EXECUTENATIVE;
import gov.nasa.jpf.jvm.bytecode.MONITORENTER;
import gov.nasa.jpf.jvm.bytecode.GETFIELD;
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
	private Set<String> fieldNames = new HashSet<>();
	private Set<Pair<Integer, Integer>> waitSet = new HashSet<>();
	private Map<String, Set<Pair<Integer, Integer>>> lockTable = new HashMap<>();

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

		for (int pi = 0; pi < group.size(); pi++) {
			Pair<Integer, Integer> p = group.get(pi);
			int from = p._1;
			int to = p._2;
			int height = 0;
			StringBuilder tempStr = new StringBuilder();

			String fieldName = "";

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

					Instruction insn = s.getInstruction();

					if (insn instanceof VirtualInvocation) {
						//System.out.println("insn = " + insn);
						String insnStr = insn.toString();
						if (insnStr.contains("java.lang.Object.wait()") || insnStr.contains("java.lang.Object.notify()")
								|| insnStr.contains("java.lang.Object.notifyAll()")) {
							waitSet.add(new Pair<>(pi, height - 1));
							//System.out.println("row = " + pi + " height =" + height);
							// break;
						}
					}

					if (insn instanceof GETFIELD) {
						fieldName = ((GETFIELD) insn).getClassName() + "." + ((GETFIELD) insn).getFieldName();
						fieldName = fieldName.replaceAll("^.*\\$", "");
					}

					if (insn instanceof MONITORENTER) {

						Pair<Integer, Integer> pair = new Pair<>(pi, height - 1);
						if (fieldNames.contains(fieldName)) {
							lockTable.get(fieldName).add(pair);
						} else {
							fieldNames.add(fieldName);
							Set<Pair<Integer, Integer>> newSet = new HashSet<>();
							newSet.add(pair);
							lockTable.put(fieldName, newSet);
						}

						//lockSet.add(new Pair<>(pi, height - 1));
						//System.out.println("row = " + pi + " height =" + height);
					}

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
		return new HashSet<>(waitSet);
	}


	public Set<String> getFieldNames() {
		return new HashSet<>(fieldNames);
	}

	public Set<Pair<Integer, Integer>> getLocks(String field) {
		//System.out.print(field + ": ");
//		for(Pair<Integer, Integer> p: lockTable.get(field)){
//			System.out.print("(" + p._1 + ", " + p._2 + ")" + ", ");
//		}
//		System.out.println();
		return new HashSet<>(lockTable.get(field));
	}
}
