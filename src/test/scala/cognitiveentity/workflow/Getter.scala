package cognitiveentity.workflow
import akka.dispatch.Future
import akka.dispatch.Await
import akka.util.Duration
import java.util.concurrent.TimeUnit

case object Getter {

  implicit def addGet[T](future: Future[T]) = Getter(future)
}

case class Getter[T](future: Future[T]) {
  private val duration = Duration(100, TimeUnit.MILLISECONDS)
  def get: T = Await.result(future, duration)
  def exception: String = Await.result(future.failed, duration).getMessage
  def option: Option[T] = Await.result(future.map { case v: T => Some(v) }.recover { case _ => None },
    duration)
  def either: Either[Throwable, T] = Await.result(future.map { case v: T => Right(v) }.recover { case _@ e => Left(e) },
    duration)
}
