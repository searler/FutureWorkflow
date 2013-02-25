package cognitiveentity.data

 class Monitor[T](private val v: T) {
  def apply[R](f: T => R): R = synchronized { f(v) }
  def convert[R](f: T => R): Monitor[R] = new Monitor(synchronized { f(v) })
  def depend[R](f: T => R): DependMonitor[R, T] = new DependMonitor(synchronized { f(v) }, this)
}

class DependMonitor[T, L](private val v: T, private val lock: Monitor[L]) {
  def apply[R](f: T => R): R = lock.synchronized{ f(v) }
  def convert[R](f: T => R): Monitor[R] = lock.synchronized{new Monitor(synchronized { f(v) })}
  def depend[R](f: T => R): DependMonitor[R, L] = lock.synchronized{new DependMonitor(f(v), lock) }
}

object Monitor {
  implicit def create[S, T](s: S)(implicit p: S => T) = new Monitor(p(s))
 
}



