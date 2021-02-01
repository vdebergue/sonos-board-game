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