package prices

import cats.effect._
import com.comcast.ip4s._
import fs2.Stream
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.http4s.server.middleware.Logger
import prices.config.Config
import prices.routes.{ InstanceKindRoutes, PriceRoutes }
import prices.services.{ SmartcloudInstanceKindService, SmartcloudPriceService, SmartcloudService }

object Server {

  def serve(config: Config): Stream[IO, ExitCode] = {

    val smartcloudService = SmartcloudService.make[IO](
      SmartcloudService.Config(
        config.smartcloud.baseUri,
        config.smartcloud.token,
        config.smartcloud.maxRetries
      )
    )

    val instanceKindService = SmartcloudInstanceKindService.make[IO](smartcloudService)
    val priceService        = SmartcloudPriceService.make[IO](smartcloudService)

    val httpApp = (
      Router(
        "" -> PriceRoutes[IO](priceService).routes,
        "" -> InstanceKindRoutes[IO](instanceKindService).routes
      )
    ).orNotFound

    Stream
      .eval(
        EmberServerBuilder
          .default[IO]
          .withHost(Host.fromString(config.app.host).get)
          .withPort(Port.fromInt(config.app.port).get)
          .withHttpApp(Logger.httpApp(true, true)(httpApp))
          .build
          .useForever
      )
  }

}
