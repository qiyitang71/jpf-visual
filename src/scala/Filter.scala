//import gov.nasa.jpf.traceServer.traceStorer.event.TraceEvent
import scala.collection.mutable.LinkedHashSet
import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.ListBuffer
import gov.nasa.jpf.traceServer.traceStorer.event._
import gov.nasa.jpf.traceEmitter.DeadlockThreadData
import gov.nasa.jpf.traceEmitter.DeadlockObjectData
import ThreadAction._
import ObjectAction._

import java.io.PrintWriter;

object Filter {
  def essentialFilter(events: ListBuffer[TraceEvent]): (List[TraceEvent], LinkedHashSet[Int]) = {
    val threads = LinkedHashSet[Int]()
    val ops = ListBuffer[TraceEvent]()
    val waits = LinkedHashMap[Int, Int]()
    val blocks = LinkedHashMap[Int, Int]()
    val runnables = LinkedHashSet[Int]()

    def filter(event: TraceEvent): Boolean = {
      var toReturn = false
      val threadId = event.traceData.threadId

      def f1(map: LinkedHashMap[Int, Int], ref: Int): Boolean = {
        if (!runnables.contains(event.traceData.threadId) && !threads.contains(threadId)) {
          threads.add(threadId)
          map.put(ref, threadId)
          ops += event
          true
        } else false
      }

      def f2(map: LinkedHashMap[Int, Int], ref: Int): Boolean = {
        var toReturn = false

        val ti = map.get(ref)

        if (ti.isDefined && ti.get != threadId) {
          if (!threads.contains(threadId)) {
            threads.add(threadId);
          }
          map -= ti.get
          ops += event
          toReturn = true
        }
        runnables.add(threadId)
        toReturn
      }

      event match {
        case e @ ThreadEvent(Waiting, _, _, d: DeadlockThreadData) =>
          f1(waits, d.objectReference)
        case e @ ThreadEvent(Blocked, _, _, d: DeadlockThreadData) =>
          f1(blocks, d.objectReference)
        case e @ ObjectEvent(Wait, _, d: ObjectData, _) =>
          f1(waits, d.reference)

        case e @ ThreadEvent(Notified, _, _, d: DeadlockThreadData) =>
          f2(waits, d.objectReference)
        case e @ ObjectEvent(Notify, _, d: ObjectData, _) =>
          f2(waits, d.reference)
        case e @ ObjectEvent(NotifyAll, _, d: ObjectData, _) =>
          f2(waits, d.reference)
        case e @ ObjectEvent(Locked, _, d: ObjectData, _) =>
          f2(blocks, d.reference)

        case e @ ObjectEvent(Unlocked, _, _, _) =>
          runnables.add(event.traceData.threadId)
          false

        case ThreadEvent(Terminated, _, _, _) | ThreadEvent(Started, _, _, _) =>
          ops += event
          true
        case _ => false
      }
    }

    //Remove all starts/terminates of irrelevant threads.
    def postProcess(it: ListBuffer[TraceEvent]): ListBuffer[TraceEvent] = {
      for (tOp <- it)
        tOp match {
          case ThreadEvent(Terminated, _, _, _) | ThreadEvent(Started, _, _, _) =>
            if (!threads.contains(tOp.traceData.threadId)) it -= tOp
          case _ =>
        }
      it
    }

    (postProcess(events.filter(filter(_))).toList, threads)
  }

  def getEssentialEvents(events: List[TraceEvent], out: PrintWriter): List[TraceEvent] = {
    val (es, ts) = essentialFilter(ListBuffer() ++ events)
    es
  }

  def printEssential(events: List[TraceEvent], out: PrintWriter) = {
    val (es, ts) = essentialFilter(ListBuffer() ++ events);
    printHeader(ts.toList, out)
    printOps(es, ts.toList, out)
  }

  // Returns an acronym of the event.
  private def eventTypeToChar(eType: TraceEvent): Char = {
    eType match {
      case ThreadEvent(Waiting, _, _, _)    => 'W'
      case ObjectEvent(Wait, _, _, _)       => 'W'
      case ObjectEvent(Notify, _, _, _)     => 'N'
      case ThreadEvent(Notified, _, _, _)   => 'N'
      case ThreadEvent(Blocked, _, _, _)    => 'B'
      case ObjectEvent(NotifyAll, _, _, _)  => 'A'
      case ObjectEvent(Locked, _, _, _)     => 'L'
      case ObjectEvent(Unlocked, _, _, _)   => 'U'
      case ThreadEvent(Started, _, _, _)    => 'S'
      case ThreadEvent(Terminated, _, _, _) => 'T'
      case _                                => ' '
    }
  }

  def printOps(events: List[TraceEvent], threadList: List[Int], out: PrintWriter) {
    for (event <- events) {
      for (id <- threadList) {
        if (event.traceData.threadId == id) {
          event match {
            case e @ ThreadEvent(Started, _, _, _) =>
              out.print("   %1$s    ".format(eventTypeToChar(e))) // or just 'S'
            case e @ ThreadEvent(Terminated, _, _, _) =>
              out.print("   %1$s    ".format(eventTypeToChar(e))) // or just 'T'
            case e @ ThreadEvent(_, _, _, ext: DeadlockThreadData) =>
              out.print("%1$s:%2$-5d ".format(eventTypeToChar(e), ext.objectReference))
            case e @ ObjectEvent(_, _, data: ObjectData, _) =>
              out.print("%1$s:%2$-5d ".format(eventTypeToChar(e), data.reference))
          }
        } else {
          out.print("   |    ")
        }
      }
      out.print("%6d".format(event.traceData.stateId))

      event match {
        case ThreadEvent(_, _, _, e: DeadlockThreadData) =>
          out.print(" %1$18.18s %2$s".format(e.insnOpcode, e.insnFileLocation))
        case ObjectEvent(_, _, _, e: DeadlockObjectData) =>
          out.print(" %1$18.18s %2$s".format(e.insnOpcode, e.insnFileLocation))
      }
      out.println()
    }
  }
  def printHeader(tlist: List[Int], out: PrintWriter) {
    for (ti <- tlist) {
      out.print("  %1$2d    ".format(ti))
    }
    out.print(" trans      insn          loc")
    out.println

    for (i <- 0 until tlist.size) {
      out.print("------- ");
    }
    out.print("---------------------------------------------------");
    out.println();
  }
}

import Filter._