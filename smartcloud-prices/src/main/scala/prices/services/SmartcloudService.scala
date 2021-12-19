package prices.services

import cats._
import cats.implicits._
import cats.effect._
import io.circe._
import io.circe.generic.semiauto._
import org.http4s._
import org.http4s.circe.jsonOf
import org.http4s.client._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers._
import org.http4s.MediaType
import org.http4s.Method._
import retry._
import retry.RetryPolicies._
import retry.syntax.all._

import scala.concurrent.duration._
import prices.data._

import scala.util.control.NoStackTrace

trait SmartcloudService[F[_]] {
  def getAllInstances(): F[List[InstanceKind]]
  def getPrice(kind: InstanceKind): F[Price]
}

object SmartcloudService {

  sealed trait Exception extends NoStackTrace
  object Exception {
    case class NotFoundFailure(message: String) extends Exception
    case class TooManyRequestsFailure() extends Exception
    case class APICallFailure(message: String) extends Exception
  }

  final case class Config(
      baseUri: String,
      token: String,
      maxRetries: Int
  )

  def make[F[_]: Async: Sleep](config: Config): SmartcloudService[F] = new SmartcloudPriceServiceImpl(config)

  private final class SmartcloudPriceServiceImpl[F[_]: Async: Sleep: Monad](
      config: Config
  ) extends SmartcloudService[F]
      with Http4sClientDsl[F] {

    implicit val instanceKindDecoder: Decoder[InstanceKind]                 = Decoder[String].map(InstanceKind(_))
    implicit val instanceKindsEntityDecoder: EntityDecoder[F, List[String]] = jsonOf[F, List[String]]

    implicit val priceDecoder: Decoder[Price]                = deriveDecoder
    implicit val priceEntityDecoder: EntityDecoder[F, Price] = jsonOf

    private val baseInstanceUri       = Uri.unsafeFromString(s"${config.baseUri}/instances")
    private val httpClient: Client[F] = JavaNetClientBuilder[F].create

    private def getRequestFor(uri: Uri): Request[F] = GET(
      uri,
      Authorization(Credentials.Token(AuthScheme.Bearer, config.token)),
      Accept(MediaType.application.json)
    )

    private def getPriceRequest(kind: InstanceKind): Request[F] = getRequestFor(baseInstanceUri.addPath(kind.getString))
    private def getAllInstancesRequest: Request[F]              = getRequestFor(baseInstanceUri)

    private val retryPolicy = limitRetries[F](config.maxRetries) join exponentialBackoff[F](10.milliseconds)

    private def isWorthRetrying(error: Throwable) =
      error match {
        case Exception.NotFoundFailure(_)       => Applicative[F].pure(false)
        case Exception.TooManyRequestsFailure() => Applicative[F].pure(false)
        case _                                  => Applicative[F].pure(true)
      }

    implicit final class RetryingOps[A, E](action: => F[A]) {
      def withRetry = action.retryingOnSomeErrors(
        isWorthRetrying = isWorthRetrying,
        policy = retryPolicy,
        onError = retry.noop[F, Throwable]
      )
    }

    override def getAllInstances(): F[List[InstanceKind]] =
      httpClient
        .expect[List[String]](getAllInstancesRequest)
        .map(k => k.map(InstanceKind(_)))
        .adaptErr {
          case UnexpectedStatus(Status.TooManyRequests, _, _) => Exception.TooManyRequestsFailure()
          case _: Throwable                                   => Exception.APICallFailure(s"Can't get all instances") // TODO: log real error
        }
        .withRetry

    override def getPrice(kind: InstanceKind): F[Price] =
      httpClient
        .expect[Price](getPriceRequest(kind))
        .adaptErr {
          case UnexpectedStatus(Status.NotFound, _, _)        => Exception.NotFoundFailure(s"Can't find price for kind ${kind}")
          case UnexpectedStatus(Status.TooManyRequests, _, _) => Exception.TooManyRequestsFailure()
          case _: Throwable                                   => Exception.APICallFailure(s"Can't get price for ${kind}")
        }
        .withRetry
  }

}
