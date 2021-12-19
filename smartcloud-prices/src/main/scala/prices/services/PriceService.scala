package prices.services

import prices.data._

import scala.util.control.NoStackTrace

trait PriceService[F[_]] {
  def getPrice(kind: InstanceKind): F[Price]
}

object PriceService {

  sealed trait Exception extends NoStackTrace
  object Exception {
    case class DataNotAvailableFailure() extends Exception
    case class KindNotFoundFailure(message: String) extends Exception
    case class APICallFailure(message: String) extends Exception
  }

}
