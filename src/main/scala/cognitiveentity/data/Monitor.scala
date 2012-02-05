package cognitiveentity.data

trait Monitor[T] {
  def apply[R](f: T => R): R
  def convert[R](f: T => R): Monitor[R]
  def depend[R](f: T => R): Monitor[R]
}

object Monitor {
  implicit def apply[S, T](s: S)(implicit p: S => T) = new MonitorImpl(p(s))

  private[Monitor] class MonitorImpl[T](private val v: T) extends Monitor[T] {
    def apply[R](f: T => R) = synchronized { f(v) }
    def convert[R](f: T => R) = new MonitorImpl(apply(f))
    def depend[R](f: T => R) = new DependentImpl(this, apply(f))

    private[Monitor] class DependentImpl[T](private val parent: Monitor[_], private val v: T) extends Monitor[T] {
      def apply[R](f: T => R) = parent.synchronized { f(v) }
      def convert[R](f: T => R) = new MonitorImpl(apply(f))
      def depend[R](f: T => R) = new DependentImpl(parent, apply(f))
    }
  }
}