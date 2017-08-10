import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.nasa.jpf.jvm.bytecode.JVMInvokeInstruction;
import gov.nasa.jpf.jvm.bytecode.JVMReturnInstruction;
import gov.nasa.jpf.jvm.bytecode.LockInstruction;
import gov.nasa.jpf.jvm.bytecode.VirtualInvocation;
import gov.nasa.jpf.util.Left;
import gov.nasa.jpf.util.Pair;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.Path;
import gov.nasa.jpf.vm.Step;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Transition;
import gov.nasa.jpf.vm.bytecode.FieldInstruction;
import gov.nasa.jpf.vm.choice.ThreadChoiceFromSet;

public class TraceData {

	private int numOfThreads = -1;
	private List<String> threadNames = null;

	private Path path;

	private List<Pair<Integer, Integer>> group = new ArrayList<>();

	private Set<String> fieldNames = new HashSet<>();
	private Set<Pair<Integer, Integer>> waitSet = new HashSet<>();
	private Map<String, Set<Pair<Integer, Integer>>> lockTable = new HashMap<>();
	private Set<Pair<Integer, Integer>> threadStartSet = new HashSet<>();
	private Map<Integer, TextLineList> lineTable = new HashMap<>();
	private Set<String> lockMethodName = new HashSet<>();
	private Map<String, Set<Pair<Integer, Integer>>> classFieldMap = new HashMap<>();
	private Map<String, Set<Pair<Integer, Integer>>> classMethodMap = new HashMap<>();
	private Map<Pair<Integer, Integer>, List<Pair<Integer, String>>> threadStateMap = new HashMap<>();

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
		int prevThreadIdx = 0;
		for (int pi = 0; pi < group.size(); pi++) {
			Pair<Integer, Integer> p = group.get(pi);
			int from = p._1;
			int to = p._2;
			int height = 0;
			StringBuilder tempStr = new StringBuilder();

			ArrayList<TextLine> lineList = new ArrayList<>();
			TextLineList txtLinelist = new TextLineList(lineList);
			lineTable.put(pi, txtLinelist);

			boolean isFirst = true;
			for (int i = from; i <= to; i++) {
				Transition transition = path.get(i);
				String lastLine = null;

				int nNoSrc = 0;
				ChoiceGenerator<?> cg = transition.getChoiceGenerator();

				if (cg instanceof ThreadChoiceFromSet) {
					// thread start/join highlight
					if (cg.getId() == "START" || cg.getId() == "JOIN") {
						if (lineTable.get(pi).getTextLine(height - 1).isSrc()) {
							threadStartSet.add(new Pair<>(pi, height - 1));
						}
					}
					ThreadInfo ti = transition.getThreadInfo();
					Pair<Integer, Integer> tmp = new Pair<>(pi, height);

					// thread state view
					// ROOT - main thread
					if (cg.getId() == "ROOT") {
						Pair<Integer, String> threadState = new Pair<>(ti.getId(), "ROOT");
						ArrayList<Pair<Integer, String>> list = new ArrayList<>();
						list.add(threadState);
						threadStateMap.put(tmp, list);
					}

					// START
					if (cg.getId() == "START") {
						int tid = ((ThreadChoiceFromSet) cg).getChoice(cg.getTotalNumberOfChoices() - 1).getId();
						Pair<Integer, String> threadState = new Pair<>(tid, "START");

						ArrayList<Pair<Integer, String>> list = new ArrayList<>();
						list.add(threadState);
						threadStateMap.put(tmp, list);
					}

					// LOCK
					if (cg.getId() == "LOCK") {
						Pair<Integer, String> threadState = new Pair<>(prevThreadIdx, "LOCK");

						ArrayList<Pair<Integer, String>> list = new ArrayList<>();
						list.add(threadState);
						threadStateMap.put(tmp, list);
					}

					// WAIT
					if (cg.getId() == "WAIT") {
						Pair<Integer, String> threadState = new Pair<>(prevThreadIdx, "WAIT");

						ArrayList<Pair<Integer, String>> list = new ArrayList<>();
						list.add(threadState);
						threadStateMap.put(tmp, list);
					}

					// RELEASE
					if (cg.getId() == "RELEASE") {
						Pair<Integer, String> threadState = new Pair<>(prevThreadIdx, "RELEASE");
						Pair<Integer, String> threadState2 = new Pair<>(ti.getId(), "RELEASE");

						ArrayList<Pair<Integer, String>> list = new ArrayList<>();
						list.add(threadState);
						list.add(threadState2);
						threadStateMap.put(tmp, list);
					}

				}

				tempStr.append(cg + "\n");
				TextLine txt = new TextLine(cg.toString(), true, false, transition, pi, height);
				lineList.add(txt);

				height++;
				TextLine txtSrc = null;
				for (int si = 0; si < transition.getStepCount(); si++) {
					Step s = transition.getStep(si);
					String line = s.getLineString();
					if (line != null) {
						String src = line.replaceAll("/\\*.*?\\*/", "").replaceAll("//.*$", "")
								.replaceAll("/\\*.*$", "").replaceAll("^.*?\\*/", "").replaceAll("\\*.*$", "").trim();

						if (!line.equals(lastLine) && src.length() > 0) {
							if (nNoSrc > 0) {
								String noSrc = " [" + nNoSrc + " insn w/o sources]";
								tempStr.append(noSrc + "\n");
								txtSrc = new TextLine(noSrc, false, false, transition, pi, height);
								lineList.add(txtSrc);
								height++;
							}
							tempStr.append(" ");
							tempStr.append(Left.format(s.getLocationString(), 20));
							tempStr.append(": ");
							tempStr.append(src + "\n");

							txtSrc = new TextLine(src, false, true, transition, pi, height);
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
						String mName = mi.getUniqueName();
						if (mci != null && mi.getUniqueName() != null && mName != null && !mName.contains("<clinit>")) {
							lockMethodName.add(mci.getName() + "." + mName);
						}
					}

					if (line != null && insn instanceof VirtualInvocation) {
						String insnStr = insn.toString();
						if (insnStr.contains("java.lang.Object.wait()") || insnStr.contains("java.lang.Object.notify()")
								|| insnStr.contains("java.lang.Object.notifyAll()")) {
							waitSet.add(new Pair<>(pi, height - 1));
						}
					}

					if (line != null && insn instanceof LockInstruction) {
						LockInstruction minsn = (LockInstruction) insn;
						ThreadInfo ti = transition.getThreadInfo();
						String fieldName = ti.getElementInfo(minsn.getLastLockRef()).toString().replace("$", ".")
								.replaceAll("@.*", "");
						Pair<Integer, Integer> pair = new Pair<>(pi, height - 1);

						if (fieldNames.contains(fieldName)) {
							lockTable.get(fieldName).add(pair);
						} else {
							fieldNames.add(fieldName);
							Set<Pair<Integer, Integer>> newSet = new HashSet<>();
							newSet.add(pair);
							lockTable.put(fieldName, newSet);
						}
					}

					if (line != null && insn instanceof JVMReturnInstruction) {
						String mName = mi.getFullName();
						String cName = mi.getClassName();
						if (lockMethodName.contains(mName)) {
							Pair<Integer, Integer> pair = new Pair<>(pi, height - 1);
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
				prevThreadIdx = transition.getThreadIndex();
			}

			tempStr.deleteCharAt(tempStr.length() - 1);

			/**
			 * set last line
			 */
			int li = lineList.size() - 1;
			for (; li >= 0; li--) {
				if (lineList.get(li).isSrc()) {
					lineList.get(li).setLast();
					break;
				}
			}

			if (li == -1) {
				txtLinelist.setNoSrc(true);
			}

		}

		/**
		 * synchronized methods
		 */
		if (!lockMethodName.isEmpty()) {
			for (TextLineList list : lineTable.values()) {
				for (TextLine tl : list.getList()) {
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
		if (classFieldMap.containsKey(target)) {
			return classFieldMap.get(target);
		}

		Set<String> srcSet = new HashSet<>();
		Set<Pair<Integer, Integer>> targetSet = new HashSet<>();
		for (TextLineList list : lineTable.values()) {
			for (TextLine tl : list.getList()) {
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
					}
				}
			}
		}
		classFieldMap.put(target, targetSet);
		return targetSet;

	}

	public Set<Pair<Integer, Integer>> getClassMethod(String clsName, String m) {
		String target = clsName + "." + m;
		if (classMethodMap.containsKey(target)) {
			return classMethodMap.get(target);
		}
		String methodName = m.replaceAll("\\(.*$", "");
		Set<Pair<Integer, Integer>> targetSet = new HashSet<>();
		for (TextLineList list : lineTable.values()) {
			Map<String, String> srcMap = new HashMap<>();
			for (TextLine tl : list.getList()) {
				if (tl.isSrc()) {
					for (int si = tl.getStartStep(); si <= tl.getEndStep(); si++) {
						Step step = tl.getTransition().getStep(si);
						Instruction insn = step.getInstruction();
						String cName = insn.getMethodInfo().getClassInfo().getName();
						if (cName.equals(srcMap.get(insn.getFileLocation()))) {
							targetSet.add(new Pair<Integer, Integer>(tl.getGroupNum(), tl.getLineNum()));
							break;
						} else if (insn instanceof JVMInvokeInstruction) {
							String mName = ((JVMInvokeInstruction) insn).getInvokedMethodName().replaceAll("\\(.*$",
									"");

							if (((JVMInvokeInstruction) insn).getInvokedMethodClassName().equals(clsName)
									&& methodName.equals(mName)) {
								targetSet.add(new Pair<Integer, Integer>(tl.getGroupNum(), tl.getLineNum()));
								srcMap.put(insn.getFileLocation(), insn.getMethodInfo().getClassName());
								System.out.println(clsName + "." + m + ": " + tl.getGroupNum() + tl.getLineNum());
								break;
							}
						}
					}
				}
			}
		}
		classMethodMap.put(target, targetSet);
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

	public Set<String> getFieldNames() {
		return new HashSet<>(fieldNames);
	}

	public Set<Pair<Integer, Integer>> getLocks(String field) {
		return new HashSet<>(lockTable.get(field));
	}

	public Map<Integer, TextLineList> getLineTable() {
		return new HashMap<>(lineTable);
	}

	public Map<Pair<Integer, Integer>, List<Pair<Integer, String>>> getThreadStateMap() {
		return new HashMap<>(threadStateMap);
	}

}
