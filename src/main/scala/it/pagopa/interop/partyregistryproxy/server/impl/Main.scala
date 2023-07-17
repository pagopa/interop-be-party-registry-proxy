package it.pagopa.interop.partyregistryproxy.server.impl

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, DispatcherSelector}
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.management.scaladsl.AkkaManagement
import buildinfo.BuildInfo
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.jwt.JWTConfiguration
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog, renderBuildInfo}
import it.pagopa.interop.commons.utils.{CORRELATION_ID_HEADER, OpenapiUtils}
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.utils.errors.{Problem => CommonProblem}
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import it.pagopa.interop.partyregistryproxy.api.impl.serviceCode
import it.pagopa.interop.partyregistryproxy.common.system.{ApplicationConfiguration, CorsSupport}
import it.pagopa.interop.partyregistryproxy.common.util.DataSourceOps.loadOpenData
import it.pagopa.interop.partyregistryproxy.server.Controller

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object Main extends App with CorsSupport with Dependencies {

  private implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  private val bootContext: Seq[(String, String)] = Seq(
    CORRELATION_ID_HEADER -> s"application-boot-${OffsetDateTimeSupplier.get()}"
  )

  val system = ActorSystem[Nothing](
    Behaviors.setup[Nothing] { context =>
      implicit val actorSystem: ActorSystem[Nothing] = context.system
      implicit val ec: ExecutionContextExecutor      = actorSystem.executionContext

      val selector: DispatcherSelector = DispatcherSelector.fromConfig("futures-dispatcher")
      val blockingEc: ExecutionContext = actorSystem.dispatchers.lookup(selector)

      val openDataService = getOpenDataService()

      AkkaManagement.get(actorSystem.classicSystem).start()

      logger.info(renderBuildInfo(BuildInfo))(bootContext)

      val serverBinding: Future[ServerBinding] = for {
        _         <- loadOpenData(
          openDataService,
          institutionsWriterService,
          aooWriterService,
          uoWriterService,
          categoriesWriterService,
          blockingEc
        )(logger, bootContext)
        jwtReader <- JWTConfiguration.jwtReader.loadKeyset().map(createJwtReader).toFuture
        controller = new Controller(
          category = categoryApi()(jwtReader),
          health = healthApi,
          institution = institutionApi()(jwtReader),
          aoo = aooApi()(jwtReader),
          uo = uoApi()(jwtReader),
          datasource = datasourceApi(
            openDataService,
            institutionsWriterService,
            aooWriterService,
            uoWriterService,
            categoriesWriterService
          )(blockingEc),
          validationExceptionToRoute = Some(report => {
            val error =
              CommonProblem(
                StatusCodes.BadRequest,
                OpenapiUtils.errorFromRequestValidationReport(report),
                serviceCode,
                None
              )
            complete(error.status, error)
          })
        )(actorSystem.classicSystem)
        binding <- http()
          .newServerAt("0.0.0.0", ApplicationConfiguration.serverPort)
          .bind(corsHandler(controller.routes))
      } yield binding

      serverBinding.onComplete {
        case Success(b) =>
          logger.info(s"Started server at ${b.localAddress.getHostString()}:${b.localAddress.getPort()}")(bootContext)
        case Failure(e) =>
          actorSystem.terminate()
          logger.error("Startup error: ", e)(bootContext)
      }

      Behaviors.same
    },
    BuildInfo.name
  )

}
