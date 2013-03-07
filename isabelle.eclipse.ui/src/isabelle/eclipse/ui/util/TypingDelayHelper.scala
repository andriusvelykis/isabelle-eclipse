package isabelle.eclipse.ui.util

import java.util.Date
import java.util.concurrent.locks.{Lock, ReentrantLock}

import org.eclipse.swt.widgets.Display


/**
 * Provides callbacks after no typing has occurred for a period.
 *
 * Adapted from Scala IDE: scala.tools.eclipse.semicolon.TypingDelayHelper
 *
 * @author Andrius Velykis
 */
class TypingDelayHelper(delay: Long = 500) {

  private val lock = new ReentrantLock

  private val condition = lock.newCondition

  private var active = true

  /**
   * A callback and a time to fire it.
   */
  private var nextScheduledEventOpt: Option[(Date, () => Any, Option[Display])] = None

  private object SchedulerThread extends Thread {

    setName(classOf[TypingDelayHelper].getSimpleName)

    override def run = loop()

  }

  SchedulerThread.start()

  /**
   * Schedule a callback on the UI thread (clearing any existing scheduled callback)
   */
  def scheduleCallback(display: Option[Display] = None)(f: => Any) = withLock(lock) {
    val timeToFireEvent = new Date(System.currentTimeMillis + delay)
    nextScheduledEventOpt = Some(timeToFireEvent, () => f, display)
    condition.signal()
    SchedulerThread.interrupt()
  }

  def stop() = withLock(lock) {
    nextScheduledEventOpt = None
    active = false
    condition.signal()
    SchedulerThread.interrupt()
  }

  private def loop() =
    while (active) {
      val timeToSleep = withLock(lock) {
        while (active && nextScheduledEventOpt == None)
          try
            condition.await()
          catch {
            case _: InterruptedException =>
          }
        if (active) {
          val (nextScheduledTime, callback, display) = nextScheduledEventOpt.get
          val now = new Date
          if (now.before(nextScheduledTime)) {
            nextScheduledTime.getTime - now.getTime
          } else {
            asyncExec(display)(callback())
            nextScheduledEventOpt = None
            0
          }
        } else
          0
      }
      try
        Thread.sleep(timeToSleep)
      catch {
        case _: InterruptedException =>
      }

    }

  private def withLock[T](lock: Lock)(f: => T): T = {
    lock.lock()
    try
      f
    finally
      lock.unlock()
  }

  /** Asynchronously run `f` on the UI thread.  */
  private def asyncExec(display: Option[Display] = None)(f: => Unit) {
    val execDisplay = display getOrElse Display.getDefault
    execDisplay asyncExec new Runnable {
      override def run() { f }
    }
  }
}
