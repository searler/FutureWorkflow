package cognitiveentity.data

trait Immutable[T]

class Monitor[T](private val v: T) {
  def apply[R: Immutable](f: T => R): R = synchronized { f(v) }
  def convert[R](f: T => R) = new DependMonitor(synchronized { f(v) }, this)
}

class DependMonitor[T, L](private val v: T, private val lock: Monitor[L]) {
  def apply[R: Immutable](f: T => R): R = lock.synchronized { f(v) }
  def convert[R](f: T => R) = lock.synchronized { new DependMonitor(f(v), lock) }
}

object Monitor {
  implicit def create[S, T](s: S)(implicit p: S => T) = new Monitor(p(s))

  implicit object IntImmutable extends Immutable[Int]
  implicit object FloatImmutable extends Immutable[Float]
  implicit object StringImmutable extends Immutable[String]

}



