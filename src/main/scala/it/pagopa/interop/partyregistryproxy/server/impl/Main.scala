package it.pagopa.interop.partyregistryproxy.server.impl

import akka.actor.typed.{ActorSystem, DispatcherSelector}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.management.scaladsl.AkkaManagement
import buildinfo.BuildInfo
import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.jwt.JWTConfiguration
import it.pagopa.interop.commons.logging.renderBuildInfo
import it.pagopa.interop.commons.utils.OpenapiUtils
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.partyregistryproxy.api.impl._
import it.pagopa.interop.partyregistryproxy.common.system.{ApplicationConfiguration, CorsSupport}
import it.pagopa.interop.partyregistryproxy.server.Controller
import kamon.Kamon

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object Main extends App with CorsSupport with Dependencies {

  private implicit val logger = Logger(this.getClass)

  val system = ActorSystem[Nothing](
    Behaviors.setup[Nothing] { context =>
      implicit val actorSystem: ActorSystem[Nothing] = context.system
      implicit val ec: ExecutionContextExecutor      = actorSystem.executionContext

      val selector: DispatcherSelector         = DispatcherSelector.fromConfig("futures-dispatcher")
      val blockingEc: ExecutionContextExecutor = actorSystem.dispatchers.lookup(selector)

      Kamon.init()

      AkkaManagement.get(actorSystem.classicSystem).start()

      logger.info(renderBuildInfo(BuildInfo))

      val serverBinding: Future[ServerBinding] = for {
        _         <- loadOpenData(
          openDataService(),
          mockOpenDataServiceImpl(),
          institutionsWriterService,
          categoriesWriterService,
          blockingEc
        )
        jwtReader <- JWTConfiguration.jwtReader.loadKeyset().map(createJwtReader).toFuture
        controller = new Controller(
          category = categoryApi()(jwtReader),
          health = healthApi,
          institution = institutionApi()(jwtReader),
          validationExceptionToRoute = Some(report => {
            val error =
              problemOf(StatusCodes.BadRequest, OpenapiUtils.errorFromRequestValidationReport(report))
            complete(error.status, error)(entityMarshallerProblem)
          })
        )(actorSystem.classicSystem)
        binding <- http()
          .newServerAt("0.0.0.0", ApplicationConfiguration.serverPort)
          .bind(corsHandler(controller.routes))
      } yield binding

      serverBinding.onComplete {
        case Success(b) =>
          logger.info(s"Started server at ${b.localAddress.getHostString()}:${b.localAddress.getPort()}")
        case Failure(e) =>
          actorSystem.terminate()
          logger.error("Startup error: ", e)
      }

      Behaviors.same
    },
    BuildInfo.name
  )

  system.whenTerminated.onComplete { case _ => Kamon.stop() }(scala.concurrent.ExecutionContext.global)

}
