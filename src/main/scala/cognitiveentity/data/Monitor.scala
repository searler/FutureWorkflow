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

trait TaggedMonitor[P, T] {
  def apply[R](f: T => R): R
  def convert[R](f: T => R): TaggedMonitor[P, R]
  def depend[R](f: T => R): TaggedMonitor[P, R]
}

object TaggedMonitor {
  implicit def apply[P, S, T](s: S)(implicit p: S => T): TaggedMonitor[P, T] = new TaggedMonitorImpl(p(s))

  private[TaggedMonitor] class TaggedMonitorImpl[P, T](private val v: T) extends TaggedMonitor[P, T] {
    def apply[R](f: T => R) = synchronized { f(v) }
    def convert[R](f: T => R) = new TaggedMonitorImpl(apply(f))
    def depend[R](f: T => R) = new DependentImpl(this, apply(f))

    private[TaggedMonitor] class DependentImpl[T](private val parent: TaggedMonitor[P, _], private val v: T) extends TaggedMonitor[P, T] {
      def apply[R](f: T => R) = parent.synchronized { f(v) }
      def convert[R](f: T => R) = new TaggedMonitorImpl(apply(f))
      def depend[R](f: T => R) = new DependentImpl(parent, apply(f))
    }
  }
}

abstract class BaseMonitor[T, S](implicit p: S => T) {
  def s: S
  private val v = p(s)
  def apply[R](f: T => R) = synchronized { f(v) }
}
