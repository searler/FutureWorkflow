package cognitiveentity.data

class Monitor[T] private (private val v: T) {
  def apply[R](f: T => R) = synchronized { f(v) }
  def convert[R](f: T => R) = new Monitor(apply(f))
  def depend[R](f: T => R) = new Dependent(this, apply(f))

  class Dependent[T](private val parent: Monitor[_], private val v: T) {
    def apply[R](f: T => R) = parent.synchronized { f(v) }
    def convert[R](f: T => R) = new Monitor(apply(f))
    def depend[R](f: T => R) = new Dependent(parent, apply(f))
  }
}

object Monitor {
  implicit def apply[S, T](s: S)(implicit p: S => T) = new Monitor(p(s))
}