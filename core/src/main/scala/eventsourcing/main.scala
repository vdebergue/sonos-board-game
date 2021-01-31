package eventsourcing

import java.util.UUID

import izumi.reflect.Tag
import zio.{Has, IO, ZIO}

trait Entity[E <: Event, S <: State] {
  def zeroState: S
  def foldEvent(state: S, event: E): S
}

object Entity {
  type Id = UUID
}
trait Event {
  def entityId: Entity.Id
}
trait State {
  def entityId: Entity.Id
}

trait JournalError

object Journal {
  type Journal[E <: Event] = Has[Service[E]]
  trait Service[E <: Event] {
    def saveEvent(event: E): IO[JournalError, Unit]
    def listEvents(entityId: Entity.Id): IO[JournalError, Seq[E]]
  }

  def saveEvent[E <: Event: Tag](event: E): ZIO[Journal[E], JournalError, Unit] = ZIO.accessM(_.get.saveEvent(event))
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

class StateEngine[E <: Event, S <: State: Tag](entity: Entity[E, S]) {
  def processEvent(event: E): ZIO[StateStore.StateStore[S], StateStore.StoreError, S] = for {
    storeState <- StateStore.get(event.entityId)
    currentState = storeState.getOrElse(entity.zeroState)
    updatedState = entity.foldEvent(currentState, event)
    _ <- StateStore.save(updatedState)
  } yield updatedState
}
