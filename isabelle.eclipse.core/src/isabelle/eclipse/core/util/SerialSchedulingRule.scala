package isabelle.eclipse.core.util

import org.eclipse.core.runtime.jobs.ISchedulingRule


/**
 * Eclipse Job scheduling rule that ensures serial execution if the same rule object is used
 * for the jobs.
 *
 * @author Andrius Velykis
 */
class SerialSchedulingRule extends ISchedulingRule {

  // conflict with itself, thus preventing concurrent execution
  override def isConflicting(rule: ISchedulingRule): Boolean = rule == this

  // allow containment, e.g. can start another job with the rule from within a job
  override def contains(rule: ISchedulingRule): Boolean = rule == this

}
