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
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ClassInfo;
//import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.Path;
import gov.nasa.jpf.vm.Step;
import gov.nasa.jpf.vm.Transition;
import gov.nasa.jpf.vm.bytecode.FieldInstruction;
import gov.nasa.jpf.vm.choice.ThreadChoiceFromSet;

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
	private Set<Pair<Integer, Integer>> threadStartSet = new HashSet<>();
	private Map<Integer, List<TextLine>> lineTable = new HashMap<>();
	private Set<String> lockMethodName = new HashSet<>();

	// private Set<Pair<Integer, Integer>> threadTerminateSet = new HashSet<>();

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

			ArrayList<TextLine> lineList = new ArrayList<>();
			lineTable.put(pi, lineList);

			boolean isFirst = true;
			for (int i = from; i <= to; i++) {
				Transition t = path.get(i);
				String lastLine = null;
				// MethodInfo lastMi = null;

				int nNoSrc = 0;
				ChoiceGenerator<?> cg = t.getChoiceGenerator();

				if (cg instanceof ThreadChoiceFromSet) {
					if (cg.getId() == "START" || cg.getId() == "JOIN") {
						threadStartSet.add(new Pair<>(pi, height - 1));
					}
				}

				tempStr.append(cg + "\n");
				TextLine txt = new TextLine(cg.toString(), true, false, t, pi, height);
				lineList.add(txt);

				height++;
				TextLine txtSrc = null;
				int lastSi = 0;
				for (int si = 0; si < t.getStepCount(); si++) {
					Step s = t.getStep(si);
					String line = s.getLineString();
					if (line != null) {
						String src = line.replaceAll("/\\*.*?\\*/", "").replaceAll("//.*$", "")
								.replaceAll("/\\*.*$", "").replaceAll("^.*?\\*/", "").replaceAll("\\*.*$", "").trim();

						if (!line.equals(lastLine) && src.length() > 1) {
							if (nNoSrc > 0) {
								String noSrc = " [" + nNoSrc + " insn w/o sources]";
								tempStr.append(noSrc + "\n");
								txtSrc = new TextLine(noSrc, false, false, t, pi, height);
								lineList.add(txtSrc);
								height++;
							}
							tempStr.append(" ");
							tempStr.append(Left.format(s.getLocationString(), 30));
							tempStr.append(": ");
							tempStr.append(src + "\n");

							txtSrc = new TextLine(src, false, true, t, pi, height);
							txtSrc.setStartStep(si);
							if (isFirst) {
								isFirst = false;
								txtSrc.setFirst();
							}
							lineList.add(txtSrc);

							height++;
							nNoSrc = 0;
						}
					} else { // no source
						nNoSrc++;
					}
					lastLine = line;

					if (line != null && txtSrc != null) {
						txtSrc.setEndStep(si);
					}

					Instruction insn = s.getInstruction();
					MethodInfo mi = insn.getMethodInfo();

					if (line != null && mi.isSynchronized()) {
						ClassInfo mci = mi.getClassInfo();
						if (mci != null && mi.getUniqueName() != null) {
							lockMethodName.add(mci.getName() + "." + mi.getUniqueName());
						}
					}

					if (insn instanceof VirtualInvocation) {
						// System.out.println("insn = " + insn);
						String insnStr = insn.toString();
						if (insnStr.contains("java.lang.Object.wait()") || insnStr.contains("java.lang.Object.notify()")
								|| insnStr.contains("java.lang.Object.notifyAll()")) {
							waitSet.add(new Pair<>(pi, height - 1));
							// System.out.println("row = " + pi + " height =" +
							// height);
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

						// lockSet.add(new Pair<>(pi, height - 1));
						// System.out.println("row = " + pi + " height =" +
						// height);
					}

				}

			}
			tempStr.deleteCharAt(tempStr.length() - 1);
			detailList.add(tempStr.toString());
			heightList.add(height);

			/**
			 * set last line
			 */
			for (int li = lineList.size() - 1; li >= 0; li--) {
				if (lineList.get(li).isSrc()) {
					lineList.get(li).setLast();
					break;
				}
			}

		}

		/**
		 * synchronized methods
		 */
		if (!lockMethodName.isEmpty()) {
			for (List<TextLine> list : lineTable.values()) {
				for (TextLine tl : list) {
					if (tl.isSrc()) {
						for (int si = tl.getStartStep(); si <= tl.getEndStep(); si++) {
							Step s = tl.getTransition().getStep(si);
							Instruction insn = s.getInstruction();
							if (insn instanceof VirtualInvocation) {
								VirtualInvocation vinsn = (VirtualInvocation) insn;
								String cName = vinsn.getInvokedMethodClassName();
								String tmp = cName + "." + vinsn.getInvokedMethodName();
								Pair<Integer, Integer> pair = new Pair<>(tl.getGroupNum(), tl.getLineNum());

								if (lockMethodName.contains(tmp)) {
									if (fieldNames.contains(cName)) {
										lockTable.get(cName).add(pair);
									} else {
										fieldNames.add(cName);
										Set<Pair<Integer, Integer>> newSet = new HashSet<>();
										newSet.add(pair);
										lockTable.put(cName, newSet);
									}
								}
							}
						}
					}
				}
			}
		}

	}

	public Set<Pair<Integer, Integer>> getClassField(String clsName, String fieldName) {
		String target = clsName + "." + fieldName;
		Set<String> srcSet = new HashSet<>();
		Set<Pair<Integer, Integer>> targetSet = new HashSet<>();
		for (List<TextLine> list : lineTable.values()) {
			for (TextLine tl : list) {
				if (tl.isSrc()) {
					for (int si = tl.getStartStep(); si <= tl.getEndStep(); si++) {
						Step step = tl.getTransition().getStep(si);
						Instruction insn = step.getInstruction();
						String cName = insn.getMethodInfo().getClassInfo().getName();
						if (clsName.equals(cName) && srcSet.contains(insn.getFileLocation())) {
							targetSet.add(new Pair<Integer, Integer>(tl.getGroupNum(), tl.getLineNum()));
							break;
						} else if (insn instanceof FieldInstruction) {
							if (((FieldInstruction) insn).getVariableId().equals(target)) {
								targetSet.add(new Pair<Integer, Integer>(tl.getGroupNum(), tl.getLineNum()));
								srcSet.add(insn.getFileLocation());
								break;
							}
						}
						// if(insn instanceof )

					}
				}
			}
		}

		return targetSet;

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

	public Set<Pair<Integer, Integer>> getThreadStart() {
		return new HashSet<>(threadStartSet);
	}

	// public Set<Pair<Integer, Integer>> getThreadTerminate() {
	// return new HashSet<>(threadTerminateSet);
	// }

	public Set<String> getFieldNames() {
		return new HashSet<>(fieldNames);
	}

	public Set<Pair<Integer, Integer>> getLocks(String field) {
		// System.out.print(field + ": ");
		// for(Pair<Integer, Integer> p: lockTable.get(field)){
		// System.out.print("(" + p._1 + ", " + p._2 + ")" + ", ");
		// }
		// System.out.println();
		// if (!lockMethodName.isEmpty()) {
		// for (String str : lockMethodName) {
		// System.out.println(str);
		// }
		// }
		return new HashSet<>(lockTable.get(field));
	}

	public Map<Integer, List<TextLine>> getLineTable() {
		return new HashMap<>(lineTable);
	}

}
