package util

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}
import scalaz.concurrent.Task

final class FutureExtensionOps[A](x: => Future[A]) {

  import scalaz.Scalaz._

  def asTask: Task[A] = {
    Task.async {
      register =>
        x.onComplete {
          case Success(v) => register(v.right)
          case Failure(ex) => register(ex.left)
        }(Implicits.global)
    }
  }
}

final class TaskExtensionOps[A](x: => Task[A]) {

  import scalaz.{\/-, -\/}

  val p: Promise[A] = Promise()

  def runFuture(): Future[A] = {
    x.unsafePerformAsync {
      case -\/(ex) =>
        p.failure(ex); ()
      case \/-(r) => p.success(r); ()
    }
    p.future
  }
}
