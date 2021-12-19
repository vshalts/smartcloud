package prices.services

import cats.effect._
import cats.implicits._
import prices.data._

object SmartcloudInstanceKindService {

  def make[F[_]: Concurrent](smartcloudService: SmartcloudService[F]): InstanceKindService[F] = new SmartcloudInstanceKindService(smartcloudService)

  private final class SmartcloudInstanceKindService[F[_]: Concurrent](
      smartcloudService: SmartcloudService[F]
  ) extends InstanceKindService[F] {

    override def getAll(): F[List[InstanceKind]] =
      smartcloudService.getAllInstances().adaptError {
        case _: SmartcloudService.Exception => InstanceKindService.Exception.APICallFailure("Can't retrieve instance kinds")
      }
  }

}
