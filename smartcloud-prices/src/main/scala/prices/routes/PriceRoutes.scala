package prices.routes

import cats.effect._
import cats.implicits._
import org.http4s.{ HttpRoutes, Status }
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import prices.routes.protocol._
import prices.services.PriceService

final case class PriceRoutes[F[_]: Sync](priceService: PriceService[F]) extends Http4sDsl[F] {

  val prefix = "/prices"

  implicit val priceResponseEncoder = jsonEncoderOf[F, PriceResponse]

  private val get: HttpRoutes[F] = HttpRoutes.of {
    case GET -> Root :? InstanceKindQueryParam(kind) =>
      priceService.getPrice(kind).flatMap(price => Ok(PriceResponse(price))).handleErrorWith {
        case PriceService.Exception.DataNotAvailableFailure() => ServiceUnavailable("No price data currently available")
        case PriceService.Exception.KindNotFoundFailure(msg)  => NotFound(msg)
        case err: Throwable                                   => InternalServerError(s"Can't retrieve price: $err")
      }
  }

  def routes: HttpRoutes[F] =
    Router(
      prefix -> get
    )

}
