
/*
 * Copyright (C) 2014, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * The Java Pathfinder core (jpf-core) platform is licensed under the
 * Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0. 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.Error;
import gov.nasa.jpf.report.Publisher;
import gov.nasa.jpf.report.Reporter;
import gov.nasa.jpf.report.Statistics;
import gov.nasa.jpf.util.Left;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ClassLoaderInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.Path;
import gov.nasa.jpf.vm.Step;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Transition;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.choice.ThreadChoiceFromSet;

public class TraceVisualPrinter extends Publisher {

	// output destinations
	String fileName;
	FileOutputStream fos;
	Path path;

	String port;

	// the various degrees of information for program traces
	protected boolean showCG;
	protected boolean showSteps;
	protected boolean showLocation;
	protected boolean showSource;
	protected boolean showMethod;
	protected boolean showCode;

	public TraceVisualPrinter(Config conf, Reporter reporter) {
		super(conf, reporter);

		// options controlling the output destination
		fileName = conf.getString("report.console.file");
		port = conf.getString("report.console.port");

		// options controlling what info should be included in a trace
		showCG = conf.getBoolean("report.console.show_cg", true);
		showSteps = conf.getBoolean("report.console.show_steps", true);
		showLocation = conf.getBoolean("report.console.show_location", true);
		showSource = conf.getBoolean("report.console.show_source", true);
		showMethod = conf.getBoolean("report.console.show_method", false);
		showCode = conf.getBoolean("report.console.show_code", false);
	}

	@Override
	protected void openChannel() {

		if (fileName != null) {
			try {
				fos = new FileOutputStream(fileName);
				out = new PrintWriter(fos);
			} catch (FileNotFoundException x) {
				// fall back to System.out
			}
		} else if (port != null) {
			// <2do>
		}

		if (out == null) {
			out = new PrintWriter(System.out, true);
		}
	}

	@Override
	protected void closeChannel() {
		if (fos != null) {
			out.close();
		}
	}

	@Override
	public void publishTopicStart(String topic) {
		out.println();
		out.print("====================================================== ");
		out.println(topic);
	}

	@Override
	public void publishTopicEnd(String topic) {
		// nothing here
	}

	@Override
	public void publishStart() {
		super.publishStart();

		if (startItems.length > 0) { // only report if we have output for this
										// phase
			publishTopicStart("search started: " + formatDTG(reporter.getStartDate()));
		}
	}

	@Override
	public void publishFinished() {
		super.publishFinished();

		if (finishedItems.length > 0) { // only report if we have output for
										// this phase
			publishTopicStart("search finished: " + formatDTG(reporter.getFinishedDate()));
		}
	}

	@Override
	protected void publishJPF() {
		out.println(reporter.getJPFBanner());
		out.println();
	}

	@Override
	protected void publishDTG() {
		out.println("started: " + reporter.getStartDate());
	}

	@Override
	protected void publishUser() {
		out.println("user: " + reporter.getUser());
	}

	@Override
	protected void publishJPFConfig() {
		publishTopicStart("JPF configuration");

		TreeMap<Object, Object> map = conf.asOrderedMap();
		Set<Map.Entry<Object, Object>> eSet = map.entrySet();

		for (Object src : conf.getSources()) {
			out.print("property source: ");
			out.println(conf.getSourceName(src));
		}

		out.println("properties:");
		for (Map.Entry<Object, Object> e : eSet) {
			out.println("  " + e.getKey() + "=" + e.getValue());
		}
	}

	@Override
	protected void publishPlatform() {
		publishTopicStart("platform");
		out.println("hostname: " + reporter.getHostName());
		out.println("arch: " + reporter.getArch());
		out.println("os: " + reporter.getOS());
		out.println("java: " + reporter.getJava());
	}

	@Override
	protected void publishSuT() {
		publishTopicStart("system under test");
		out.println(reporter.getSuT());
	}

	@Override
	protected void publishError() {
		Error e = reporter.getCurrentError();

		publishTopicStart("error " + e.getId());
		out.println(e.getDescription());

		String s = e.getDetails();
		if (s != null) {
			out.println(s);
		}

	}

	@Override
	protected void publishConstraint() {
		String constraint = reporter.getLastSearchConstraint();
		publishTopicStart("search constraint");
		out.println(constraint); // not much info here yet
	}

	@Override
	protected void publishResult() {
		List<Error> errors = reporter.getErrors();

		publishTopicStart("results");

		if (errors.isEmpty()) {
			out.println("no errors detected");
		} else {
			for (Error e : errors) {
				out.print("error #");
				out.print(e.getId());
				out.print(": ");
				out.print(e.getDescription());

				String s = e.getDetails();
				if (s != null) {
					s = s.replace('\n', ' ');
					s = s.replace('\t', ' ');
					s = s.replace('\r', ' ');
					out.print(" \"");
					if (s.length() > 50) {
						out.print(s.substring(0, 50));
						out.print("...");
					} else {
						out.print(s);
					}
					out.print('"');
				}

				out.println();
			}
		}
	}

	/**
	 * this is done as part of the property violation reporting, i.e. we have an
	 * error
	 */
	@Override
	protected void publishTrace() {
		this.path = reporter.getPath();
		int i = 0;

		if (path.size() == 0) {
			return; // nothing to publish
		}

		publishTopicStart("trace " + reporter.getCurrentErrorId());

		for (Transition t : path) {
			out.print("------------------------------------------------------ ");
			out.println("transition #" + i++ + " thread: " + t.getThreadIndex());

			if (showCG) {
				out.println(t.getChoiceGenerator());
				// out.println("stateId: " +
				// t.getChoiceGenerator().getStateId());
				out.println("id: " + t.getChoiceGenerator().getId());
				// if (t.getChoiceGenerator().getProcessedNumberOfChoices() <
				// t.getChoiceGenerator()
				// .getTotalNumberOfChoices()) {
				// out.println("next choice "
				// +
				// t.getChoiceGenerator().getChoice(t.getChoiceGenerator().getProcessedNumberOfChoices()));
				// }
				if (t.getChoiceGenerator() instanceof ThreadChoiceFromSet) {

					for (ThreadInfo ti : ((ThreadChoiceFromSet) t.getChoiceGenerator()).getChoices()) {
						out.println("	ti: " + ti);
					}

				}
			}

			if (showSteps) {
				String lastLine = null;
				MethodInfo lastMi = null;
				int nNoSrc = 0;

				for (Step s : t) {
					String line = s.getLineString();

					if (showSource) {
						if (line != null) {
							if (!line.equals(lastLine)) {
								if (nNoSrc > 0) {
									out.println("      [[[" + nNoSrc + " insn w/o sources]");
								}

								out.print("  ");
								if (showLocation) {
									out.print(Left.format(s.getLocationString(), 30));
									out.print(" : ");
								}
								out.println(line.trim());
								nNoSrc = 0;

							}

						} else { // no source
							nNoSrc++;
						}

						lastLine = line;
					}

					ThreadInfo ti = t.getThreadInfo();
					// out.println("threadinfo: " + ti);

					if (line != null) {

						/* more information of the trace */
						if (true) {
							Instruction insn = s.getInstruction();

							if (true) {
								MethodInfo mi = insn.getMethodInfo();
								out.println(" mi uniqueName: " + mi.getUniqueName());

								// if (mi != lastMi) {
								ClassInfo mci = mi.getClassInfo();

								if (mi.isSynchronized()) {
									out.print("mi synchronized : " + mi.isSynchronized());
								}
								// out.println("classinfo object: " +
								// mi.getClassInfo().getClassObject());
								out.print("mci:    ");
								if (mci != null) {
									out.println(" className: " + mci.getName());
									out.println(" simple className: " + mci.getSimpleName());

									//out.println("super class: " + mci.getSuperClass());
									out.println("outer class: " + mci.getEnclosingClassInfo());
									
									// out.print(" uniquename:");
									// FieldInfo[] fi = mci.getInstanceFields();
									// out.println("FieldInfos: ");
									// if (fi != null && fi.length > 0) {
									// for (int ii = 0; ii < fi.length; ii++) {
									// out.print(fi[ii].getName() + ",");
									// }
									// out.println();
									//
									// }

								}
								// out.println(" mi uniqueName: " +
								// mi.getUniqueName());
								lastMi = mi;
								// }
							}
							// out.print(" ");
							// out.println("insn: " + insn);

							// if (insn instanceof VirtualInvocation) {
							// VirtualInvocation vinsn = (VirtualInvocation)
							// insn;
							// String cName = vinsn.getInvokedMethodClassName();
							// String mName = vinsn.getInvokedMethodName();
							// out.println("InvokeClass: " + cName + ";
							// InvokeMethod: " + mName);
							// }
							// if (insn instanceof MONITORENTER) {
							// MONITORENTER minsn = (MONITORENTER) insn;
							// ThreadInfo ti = t.getThreadInfo();
							// out.println("monitor ei: " +
							// ti.getElementInfo(minsn.getLastLockRef()));
							//
							// }
							//
							// if (insn instanceof MONITOREXIT) {
							// MONITOREXIT minsn = (MONITOREXIT) insn;
							// ThreadInfo ti = t.getThreadInfo();
							// out.println("monitor ei: " +
							// ti.getElementInfo(minsn.getLastLockRef()));
							//
							// }
							//
							// if(insn instanceof JVMReturnInstruction){
							// JVMReturnInstruction jvminsn =
							// (JVMReturnInstruction) insn;
							// out.println("JVMReturnInstruction string: " +
							// jvminsn.toString());
							// out.println("JVMReturnInstruction mi fullname: "
							// + jvminsn.getMethodInfo().getFullName());
							//
							// }
							// if (insn instanceof VirtualInvocation) {
							// out.println("invoke: " + ((VirtualInvocation)
							// insn).getInvokedMethodClassName() + "."
							// +((VirtualInvocation)
							// insn).getInvokedMethodName());
							//
							// }

							// out.println("insn: " + insn + ", class =" +
							// insn.getClass());
						}
					}

					if (showSource && !showCode && (nNoSrc > 0)) {
						// out.println(" [" + nNoSrc + " insn w/o sources]");
					}
				}
			}
		}
	}

	@Override
	protected void publishOutput() {
		Path path = reporter.getPath();

		if (path.size() == 0) {
			return; // nothing to publish
		}

		publishTopicStart("output " + reporter.getCurrentErrorId());

		if (path.hasOutput()) {
			for (Transition t : path) {
				String s = t.getOutput();
				if (s != null) {
					out.print(s);
				}
			}
		} else {
			out.println("no output");
		}
	}

	@Override
	protected void publishSnapshot() {
		VM vm = reporter.getVM();

		// not so nice - we have to delegate this since it's using a lot of
		// internals, and is also
		// used in debugging
		publishTopicStart("snapshot " + reporter.getCurrentErrorId());

		if (vm.getPathLength() > 0) {
			vm.printLiveThreadStatus(out);
		} else {
			out.println("initial program state");
		}
	}

	public static final String STATISTICS_TOPIC = "statistics";

	// this is useful if somebody wants to monitor progress from a specialized
	// ConsolePublisher
	public synchronized void printStatistics(PrintWriter pw) {
		publishTopicStart(STATISTICS_TOPIC);
		printStatistics(pw, reporter);
	}

	// this can be used outside a publisher, to show the same info
	public static void printStatistics(PrintWriter pw, Reporter reporter) {
		Statistics stat = reporter.getStatistics();

		pw.println("elapsed time:       " + formatHMS(reporter.getElapsedTime()));
		pw.println("states:             new=" + stat.newStates + ",visited=" + stat.visitedStates + ",backtracked="
				+ stat.backtracked + ",end=" + stat.endStates);
		pw.println("search:             maxDepth=" + stat.maxDepth + ",constraints=" + stat.constraints);
		pw.println("choice generators:  thread=" + stat.threadCGs + " (signal=" + stat.signalCGs + ",lock="
				+ stat.monitorCGs + ",sharedRef=" + stat.sharedAccessCGs + ",threadApi=" + stat.threadApiCGs
				+ ",reschedule=" + stat.breakTransitionCGs + "), data=" + stat.dataCGs);
		pw.println("heap:               " + "new=" + stat.nNewObjects + ",released=" + stat.nReleasedObjects
				+ ",maxLive=" + stat.maxLiveObjects + ",gcCycles=" + stat.gcCycles);
		pw.println("instructions:       " + stat.insns);
		pw.println("max memory:         " + (stat.maxUsed >> 20) + "MB");

		pw.println("loaded code:        classes=" + ClassLoaderInfo.getNumberOfLoadedClasses() + ",methods="
				+ MethodInfo.getNumberOfLoadedMethods());
	}

	@Override
	public void publishStatistics() {
		printStatistics(out);
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "consoleTracePrinter";
	}

}
