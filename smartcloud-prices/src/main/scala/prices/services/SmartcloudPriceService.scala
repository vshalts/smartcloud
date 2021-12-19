package prices.services

import cats.effect._
import cats.implicits._
import prices.data._

object SmartcloudPriceService {

  def make[F[_]: Concurrent](smartcloudService: SmartcloudService[F]): PriceService[F] = new SmartcloudPriceService(smartcloudService)

  private final class SmartcloudPriceService[F[_]: Concurrent](
      smartcloudService: SmartcloudService[F]
  ) extends PriceService[F] {

    def getPrice(kind: InstanceKind): F[Price] =
      // Some rate limiting can be done here
      smartcloudService.getPrice(kind).adaptError {
        case _: SmartcloudService.Exception.TooManyRequestsFailure => PriceService.Exception.DataNotAvailableFailure()
        case _: SmartcloudService.Exception.NotFoundFailure        => PriceService.Exception.KindNotFoundFailure(s"Incorrect parameter kind=${kind.getString}")
        case _: SmartcloudService.Exception                        => PriceService.Exception.APICallFailure("Can't retrieve instance kinds")
      }

  }

}
