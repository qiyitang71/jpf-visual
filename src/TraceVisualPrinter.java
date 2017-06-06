
//
// Copyright (C) 2012 Igor Andjelkovic (igor.andjelkovic@gmail.com).
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.LinkedList;
import java.util.HashSet;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.Error;
import gov.nasa.jpf.report.Publisher;
import gov.nasa.jpf.report.Reporter;
import gov.nasa.jpf.report.Statistics;
import gov.nasa.jpf.traceServer.printer.TracePrinter;
import gov.nasa.jpf.traceServer.traceQuery.TraceQuery;
import gov.nasa.jpf.traceServer.traceStorer.event.*;
import gov.nasa.jpf.traceServer.traceStorer.event.ThreadAction.*;
import gov.nasa.jpf.util.RepositoryEntry;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.Path;
import gov.nasa.jpf.vm.Transition;
import gov.nasa.jpf.vm.VM;
import scala.collection.Iterator;

/**
 * Console printer parameterized with
 * [[gov.nasa.jpf.traceServer.printer.TracePrinter]].
 * 
 * @author Igor Andjelkovic
 * 
 */
public class TraceVisualPrinter extends Publisher {

	/**
	 * Printer used to print the <code>trace</code> when {@link #publishTrace()}
	 * is called.
	 */
	protected TracePrinter tracePrinter;
	protected FileOutputStream fos;
	protected String fileName;
	protected String port;

	/**
	 * Configures publisher and {@link #tracePrinter}, by using
	 * <code>Config</code> instance.
	 */
	public TraceVisualPrinter(Config conf, Reporter reporter) {
		super(conf, reporter);

		// options controlling the output destination
		fileName = conf.getString("report." + getName() + ".file");
		port = conf.getString("report." + getName() + ".port");
	}

	@Override
	public String getName() {
		return "consoleTracePrinter";
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

		String mainCls = conf.getTarget();
		if (mainCls != null) {
			String mainPath = reporter.getSuT();
			if (mainPath != null) {
				out.println("application: " + mainPath);

				RepositoryEntry rep = RepositoryEntry.getRepositoryEntry(mainPath);
				if (rep != null) {
					out.println("repository: " + rep.getRepository());
					out.println("revision: " + rep.getRevision());
				}
			} else {
				out.println("application: " + mainCls + ".class");
			}
		} else {
			out.println("application: ?");
		}

		String[] args = conf.getTargetArgs();
		if (args.length > 0) {
			out.print("arguments:   ");
			for (String s : args) {
				out.print(s);
				out.print(' ');
			}
			out.println();
		}
	}

	@Override
	protected void publishError() {
		Error e = reporter.getCurrentError();

		publishTopicStart("error " + reporter.getCurrentErrorId());
		out.println("@@@@@@@@@@@@@@@@@@@@@");
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
	 * This is done as part of the property violation reporting, i.e. we have an
	 * error.
	 */
	@Override
	protected void publishTrace() {
		publishTopicStart("trace " + reporter.getCurrentErrorId());
		if (tracePrinter == null) {
			tracePrinter = new TracePrinter(conf, out);
		}
		TraceQuery tq = TraceQuery.getTraceQuery();
		tq.startTraceQuery();
		scala.collection.immutable.List<TraceEvent> lastPath = tq.getLastPath(true);
		//scala.collection.immutable.List<TraceEvent> path =
		// Filter.getEssentialEvents(lastPath, out);
		Filter.printEssential(lastPath, out);

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
			out.println("@@@@@@@@@@@@@@@@@@@@@");
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

		if (!vm.getPath().isEmpty()) {
			vm.printLiveThreadStatus(out);
		} else {
			out.println("initial program state");
		}
	}

	@Override
	protected void publishStatistics() {
		Statistics stat = reporter.getStatistics();
		publishTopicStart("statistics");
		out.println("elapsed time:       " + formatHMS(reporter.getElapsedTime()));
		out.println("states:             new=" + stat.newStates + ", visited=" + stat.visitedStates + ", backtracked="
				+ stat.backtracked + ", end=" + stat.endStates);
		out.println("search:             maxDepth=" + stat.maxDepth + ", constraints hit=" + stat.constraints);
		out.println("choice generators:  thread=" + stat.threadCGs + " (signal=" + stat.signalCGs + ", lock="
				+ stat.monitorCGs + ", shared ref=" + stat.sharedAccessCGs + "), data=" + stat.dataCGs);
		out.println("heap:               " + "new=" + stat.nNewObjects + ", released=" + stat.nReleasedObjects
				+ ", max live=" + stat.maxLiveObjects + ", gc-cycles=" + stat.gcCycles);
		out.println("instructions:       " + stat.insns);
		out.println("max memory:         " + (stat.maxUsed >> 20) + "MB");

		out.println("loaded code:        classes=" + ClassInfo.getNumberOfLoadedClasses() + ", methods="
				+ MethodInfo.getNumberOfLoadedMethods());
	}

}
