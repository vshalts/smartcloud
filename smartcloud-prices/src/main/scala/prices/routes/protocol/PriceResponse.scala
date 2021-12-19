package prices.routes.protocol

import io.circe._
import io.circe.syntax._

import prices.data._

final case class PriceResponse(price: Price)

object PriceResponse {

  implicit val encoder: Encoder[PriceResponse] =
    Encoder.instance[PriceResponse] {
      case PriceResponse(p) =>
        Json.obj(
          "kind" -> p.kind.getString.asJson,
          "amount" -> p.price.asJson
        )
    }

}
