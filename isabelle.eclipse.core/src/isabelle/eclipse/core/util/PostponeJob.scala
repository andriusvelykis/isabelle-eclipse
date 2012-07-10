package isabelle.eclipse.core.util

import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.core.runtime.IProgressMonitor

/** A convenience encapsulation of Eclipse Job that can be restarted periodically
  * (with possible postpone delay).
  *
  * The class can be used to keep calling #run(Long) method, which will postpone the job until
  * the specified delay passes since last call to #run(Long).
  * 
  * @author Andrius Velykis 
  */
class PostponeJob(name: String, private val f: IProgressMonitor => IStatus) {

  /** A convenience constructor, which allows simple setting of job contents */
  def this(name: String)(f: => Unit) = this(name, { _ => f; Status.OK_STATUS })
  
  /** The last scheduled job - will be cancelled in the subsequent #run(Long) invocation */
  private var lastJob:Option[Job] = None

  /** Executes the job in a separate thread with the indicated delay. Cancels the previous job. */
  def run(delay: Long) {
    
    // encapsulate the function into a job
    val job = new Job(name) {
      
      override def run(monitor: IProgressMonitor) = {
        if (monitor.isCanceled) {
          // check if the job is cancelled immediately
          Status.CANCEL_STATUS
        } else {
          // run the job
          f(monitor)
        }
      }
    }
    
    // allow configuration (e.g. set rule, priority)
    config(job)

    // cancel the previous job
    // will remove from queue if not yet run
    cancel()

    // mark the new job and schedule it
    lastJob = Some(job)
    job.schedule(delay)
  }
  
  def cancel() = lastJob.foreach(_.cancel)
  
  protected def config(job: Job) {}
  
}
