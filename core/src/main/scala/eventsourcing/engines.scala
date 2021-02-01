package eventsourcing

import eventsourcing.StateEngine.StateEngineError
import izumi.reflect.Tag
import zio.{Task, ZIO}

object StateEngine {
  type StateEngineError = String
}
class StateEngine[E <: Event, S <: State: Tag](entity: Entity[E, S, _]) {
  def processEvent(event: E): ZIO[StateStore.StateStore[S], StateStore.StoreError, S] = for {
    storeState <- StateStore.get(event.entityId)
    currentState = storeState.getOrElse(entity.zeroState)
    updatedState <- Task {
      entity.foldEvent(currentState, event)
    }.mapError(error => s"Error when applying event: $error")
    _ <- StateStore.save(updatedState)
  } yield updatedState

  def processEvents(events: Seq[E]): ZIO[StateStore.StateStore[S], StateEngineError, S] = {
    if (events.isEmpty) ZIO.fail("Cannot run with empty events list")
    else if (!events.forall(_.entityId == events.head.entityId)) ZIO.fail("Events should be for the same entity")
    else {
      for {
        storeState <- StateStore.get(events.head.entityId)
        currentState = storeState.getOrElse(entity.zeroState)
        updatedState <- Task {
          events.foldLeft(currentState)((state, event) => entity.foldEvent(state, event))
        }.mapError(error => s"Error when applying events: $error")
        _ <- StateStore.save(updatedState)
      } yield updatedState
    }
  }
}

object CommandEngine {
  type CommandError = String
}
class CommandEngine[C <: Command, E <: Event: Tag, S <: State: Tag](
    entity: Entity[E, S, C]
) {
  import CommandEngine.CommandError
  def handleCommand(command: C): ZIO[StateStore.StateStore[S] with Journal.Journal[E], CommandError, S] = for {
    storeState <- StateStore.get(command.entityId)
    currentState = storeState.getOrElse(entity.zeroState)
    events <- entity.commandHandler(command, currentState)
    _ <- Journal.saveEvents(events)
    newState = events.foldLeft(currentState)((state, event) => entity.foldEvent(state, event))
  } yield newState
}
