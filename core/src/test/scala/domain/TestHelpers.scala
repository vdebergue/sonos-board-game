package domain

import eventsourcing.Entity.Id
import eventsourcing.StateStore
import zio.{IO, Layer, ZLayer}

object TestHelpers {
  class GameStoreForTest extends StateStore.Service[GameState] {
    var data: Map[Id, GameState] = Map.empty
    override def get(id: Id): IO[StateStore.StoreError, Option[GameState]] = IO(data.get(id)).mapError(_.toString)
    override def save(state: GameState): IO[StateStore.StoreError, Unit] =
      IO { data = data.updated(state.entityId, state) }.mapError(_.toString)
  }

  // This function helps the compiler to deduce the right type for the layer
  def makeLayer(store: GameStoreForTest): Layer[Nothing, StateStore.StateStore[GameState]] = ZLayer.succeed(store)
}
