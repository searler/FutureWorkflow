package cognitiveentity.data

trait Immutable[T]

private case class Lock

class Monitor[T] private (private val v: T, private val lock: Lock) {
  def apply[R: Immutable](f: T => R): R = lock.synchronized { f(v) }
  def map[R](f: T => R) = new Monitor(lock.synchronized { f(v) }, lock)
}

object Monitor {
  implicit def create[S, T](s: S)(implicit p: S => T) = new Monitor(p(s), Lock())

  implicit object IntImmutable extends Immutable[Int]
  implicit object FloatImmutable extends Immutable[Float]
  implicit object StringImmutable extends Immutable[String]

}



