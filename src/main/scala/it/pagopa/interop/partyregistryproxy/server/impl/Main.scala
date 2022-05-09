package it.pagopa.interop.partyregistryproxy.server.impl

import it.pagopa.interop.commons.logging.renderBuildInfo
import akka.http.scaladsl.Http.ServerBinding
import akka.management.scaladsl.AkkaManagement
import it.pagopa.interop.partyregistryproxy.common.system.{ApplicationConfiguration, CorsSupport}
import it.pagopa.interop.partyregistryproxy.server.Controller
import kamon.Kamon
import it.pagopa.interop.commons.jwt.JWTConfiguration

import it.pagopa.interop.commons.utils.TypeConversions._

import com.typesafe.scalalogging.Logger
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem
import buildinfo.BuildInfo
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future
import scala.util.{Success, Failure}

object Main extends App with CorsSupport with Dependencies {

  private implicit val logger = Logger(this.getClass)

  val system = ActorSystem[Nothing](
    Behaviors.setup[Nothing] { context =>
      implicit val actorSystem: ActorSystem[Nothing] = context.system
      implicit val ec: ExecutionContextExecutor      = actorSystem.executionContext

      Kamon.init()

      loadOpenData(openDataService(), mockOpenDataServiceImpl(), institutionsWriterService, categoriesWriterService)

      AkkaManagement.get(actorSystem.classicSystem).start()

      logger.info(renderBuildInfo(BuildInfo))

      val serverBinding: Future[ServerBinding] = for {
        jwtReader <- JWTConfiguration.jwtReader.loadKeyset().toFuture.map(createJwtReader)
        controller = new Controller(
          category = categoryApi()(jwtReader),
          health = healthApi,
          institution = institutionApi()(jwtReader)
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
