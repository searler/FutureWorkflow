package cognitiveentity.workflow
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import scala.util.Either

case object Getter {

  implicit def addGet[T](future: Future[T]) = Getter(future)
}

case class Getter[T](future: Future[T]) {
  import scala.reflect.ClassTag
  private val duration = Duration(100, TimeUnit.MILLISECONDS)
  def get: T = Await.result(future, duration)
  def exception: String = Await.result(future.failed, duration).getMessage
  def option(implicit ec: ExecutionContext,tag: ClassTag[T]): Option[T] = Await.result(future.mapTo[T].map { case v: T => Some(v) }.recover { case _ => None },
    duration)
  def asEither(implicit ec: ExecutionContext,tag: ClassTag[T]): Either[Throwable, T] = Await.result(future.mapTo[T].map { case v: T => Right(v) }.recover { case _@ e => Left(e) },
    duration)
}
