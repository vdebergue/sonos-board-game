# Sonos Board Game

## How to run

## Query examples

Go to [graphiql](http://localhost:8080) page

- List available games
```graphql
{
  availableGames {
  	entityId,
    host { name }
  } 
}
```


- Host a new game
```
mutation HostGame($id: ID!, $kind: GameKind!, $player: PlayerInput!) {
  hostGame(entityId: $id, kind: $kind, player: $player) {
    __typename,
    ... on GameAvailable {
      entityId,
      players { name },
      kind,
      host { name}
    }
  }
}



{"id": "b9f8c3e0-1e80-46ee-9c22-da2e013b077b", "kind": "Chess", "player": {"name": "foo"} }
```

- Join a new game
```
mutation JoinGame($id: ID!, $player: PlayerInput!) {
  joinGame(entityId: $id, player: $player) {
    ... on GameAvailable {
      players {name}
    }
  }
}

{"id": "b9f8c3e0-1e80-46ee-9c22-da2e013b077b", "player": {"name": "bar"} }
```

- Start the game
```
mutation StartGame($id: ID!, $player: PlayerInput!) {
  startGame(entityId: $id, player: $player) {
    __typename,
    ... on GameInProgress {
      players { name},
      board
    }
  }
}

{"id": "b9f8c3e0-1e80-46ee-9c22-da2e013b077b", "player": {"name": "foo"} }
```

- Play some moves
```
mutation SendMove($id: ID!, $player: PlayerInput!, $move: MoveInput!) {
  sendMove(entityId: $id, player: $player, move: $move) {
    __typename,
    ... on GameInProgress {
      board,
    }
    ... on GameFinished {
      status {
        __typename
        ... on Won {
          by { name}
        }
      }
    }
  }
}

{"id": "b9f8c3e0-1e80-46ee-9c22-da2e013b077b", "player": {"name": "foo"}, "move": {"value": "e2e4"} }

{"id": "b9f8c3e0-1e80-46ee-9c22-da2e013b077b", "player": {"name": "bar"}, "move": {"value": "e7e5"} }
```