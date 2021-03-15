package it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.persistence

import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.Institution

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object InstitutionsPersistentBehavior {

  final case class State(institutions: Map[String, Institution], index: TrieMap[String, Institution])
      extends CborSerializable {

    def add(institution: Institution): State =
      copy(
        institutions = institutions + (institution.id -> institution),
        index = index.addOne(institution.description.toLowerCase -> institution)
      )

    def delete(institution: Institution): State =
      copy(institutions = institutions - institution.id, index = index.subtractOne(institution.description.toLowerCase))

  }

  object State {
    val empty: State = State(institutions = Map.empty, index = TrieMap.empty)
  }

  /* Command */
  sealed trait Command extends CborSerializable

  final case class AddInstitution(entity: Institution)                                             extends Command
  final case class DeleteInstitution(entity: Institution)                                          extends Command
  final case class GetInstitution(id: String, replyTo: ActorRef[StatusReply[Option[Institution]]]) extends Command
  final case class Search(text: String, replyTo: ActorRef[StatusReply[List[Institution]]])         extends Command

  /* Event */
  sealed trait Event extends CborSerializable

  final case class InstitutionAdded(institution: Institution)   extends Event
  final case class InstitutionDeleted(institution: Institution) extends Event

  val commandHandler: (State, Command) => Effect[Event, State] = { (state, command) =>
    command match {
      case AddInstitution(institution) =>
        Effect
          .persist(InstitutionAdded(institution))
      case DeleteInstitution(institution) =>
        Effect
          .persist(InstitutionDeleted(institution))
      case GetInstitution(id, replyTo) =>
        println(s"State status : ${state.institutions.size}")
        val institution: Option[Institution] = state.institutions.get(id)

        replyTo ! StatusReply.Success(institution)

        Effect.none
      case Search(text, replyTo) if text.length >= 3 =>

        println(s"State status : ${state.institutions.size}")

        val searches: List[String] = text.split("\\s").toList

        val result: TrieMap[String, Institution] =
          searches.foldLeft(state.index)((index, s) => index.filter(_._1.contains(s.toLowerCase)))

        val institutions: List[Institution] = result.take(20).values.toList

        replyTo ! StatusReply.Success(institutions)

        Effect.none

      case Search(_, replyTo) =>
        replyTo ! StatusReply.Success(List.empty)
        Effect.none

    }
  }

  val eventHandler: (State, Event) => State = (state, event) =>
    event match {
      case InstitutionAdded(institution)   => state.add(institution)
      case InstitutionDeleted(institution) => state.delete(institution)
    }

  def apply(): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      persistenceId = PersistenceId.ofUniqueId("pdnd-interop-uservice-party-registry-proxy"),
      emptyState = State.empty,
      commandHandler = commandHandler,
      eventHandler = eventHandler
    ).withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 1000, keepNSnapshots = 1))
      .onPersistFailure(SupervisorStrategy.restartWithBackoff(200 millis, 5 seconds, 0.1))
}
