package persistence

import domain.{GameAvailable, GameState}
import eventsourcing.StateStore.StoreError
import zio.{Has, IO, ZIO}

object GameStateQuery {

  type GameStateQuery = Has[Service]
  trait Service {
    def listAllGames(): IO[StoreError, Seq[GameState]]
    def listAvailableGames(): IO[StoreError, Seq[GameAvailable]]
  }

  def listAllGames(): ZIO[GameStateQuery, StoreError, Seq[GameState]] = ZIO.accessM(_.get.listAllGames())
  def listAvailableGames(): ZIO[GameStateQuery, StoreError, Seq[GameAvailable]] =
    ZIO.accessM(_.get.listAvailableGames())
}
