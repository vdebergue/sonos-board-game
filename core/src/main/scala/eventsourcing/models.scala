package eventsourcing

import java.util.UUID

import izumi.reflect.Tag
import zio.{Has, IO, ZIO}

trait Command { def entityId: Entity.Id }
trait Event { def entityId: Entity.Id }
trait State { def entityId: Entity.Id }

trait Entity[E <: Event, S <: State, C <: Command] {
  def zeroState: S
  def foldEvent(state: S, event: E): S
  def commandHandler(command: C, state: S): ZIO[Any, CommandEngine.CommandError, Seq[E]]
}

object Entity {
  type Id = UUID
}

object Journal {
  type JournalError = String
  type Journal[E <: Event] = Has[Service[E]]
  trait Service[E <: Event] {
    def saveEvents(events: Seq[E]): IO[JournalError, Unit]
    def listEvents(entityId: Entity.Id): IO[JournalError, Seq[E]]
  }

  def saveEvents[E <: Event: Tag](events: Seq[E]): ZIO[Journal[E], JournalError, Unit] =
    ZIO.accessM(_.get.saveEvents(events))
  def listEvents[E <: Event: Tag](entityId: Entity.Id): ZIO[Journal[E], JournalError, Seq[E]] =
    ZIO.accessM(_.get.listEvents(entityId))
}

object StateStore {
  type StoreError = String
  type StateStore[S <: State] = Has[Service[S]]
  trait Service[S <: State] {
    def get(id: Entity.Id): IO[StoreError, Option[S]]
    def save(state: S): IO[StoreError, Unit]
  }

  def get[S <: State: Tag](id: Entity.Id): ZIO[StateStore[S], StoreError, Option[S]] = ZIO.accessM(_.get.get(id))
  def save[S <: State: Tag](state: S): ZIO[StateStore[S], StoreError, Unit] = ZIO.accessM(_.get.save(state))
}
