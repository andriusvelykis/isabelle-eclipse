package isabelle.eclipse.core.util

import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.Lock
import org.eclipse.core.runtime.jobs.ISchedulingRule
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.NullProgressMonitor
import org.eclipse.core.runtime.jobs.Job

/** Utility classes and methods related to concurrency uses 
  * 
  * @author Andrius Velykis 
  */
object ConcurrentUtil {

  /** Converts Java ReadWriteLock to one that supports Scala functional style */
  implicit def funLock(rwLock: ReadWriteLock): FunReadWriteLock = new FunReadWriteLock(rwLock)
  
  /** A wrapper for ReadWriteLock to allow functional style (more concise) usage */
  class FunReadWriteLock(val rwLock: ReadWriteLock) {
    
    def read[R] = withLock[R](rwLock.readLock) _
    def write[R] = withLock[R](rwLock.writeLock) _

    def withLock[R](lock: Lock)(f: => R): R = {
      lock.lock
      val res = f
      lock.unlock
      
      res
    }
  }

}
