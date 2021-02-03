For this coding challenge, I chose to focus on creating a small eventsourcing engine. This is more a toy example and not 
a production ready library. For production I would rather use Akka persistence for instance.

The project is divided in two modules:
- `core`: provides the eventsourcing model and the game domain
- `api`: provides a graphql api based on the library caliban

Some tests are written for the core module.

# The eventsourcing part

For the eventsourcing, I first chose to represent the different states and events of the entity (`Game` here).
Then I implemented the commands first using functions (one function per command) but I had to change my approach 
due to Caliban interface and I represented the commands by case classes and define a command handler (like in Akka persistence).

In the end there are 3 traits to define an entity: 
- `State`: represents the different states the entity
- `Command`: commands used to interact with the current state of the entity. Commands can be rejected. When accepted, they produce some events.
- `Event`: events that happened on the state and make it change. Events are the source of truth in eventsourcing. 
By replaying the events that happened, it should be possible to reconstruct the current state of the entity. Thus applying events should not fail. 

Then an entity need to define one command handler and one event handler:
```
def commandHandler(command: Command, currentState: State): ZIO[Env, Error, Seq[Event]]
def eventHandler(event: Event, currentState: State): State
```

With those handlers, we can accept commands and make the State evolve


**Note**: It's possible to define a simpler command engine that takes a command and the current state in input and produces the updated state.
But in this case we lose the benefits of the events. 

<br>

There are then several choices on what and how to persist our objects, each solution comes with advantages and drawbacks.
On my part I chose to have a way to store and get the current state (`StateStore`) and a simple way to persist the events: `Journal`.
This means that it's easy to get the current state as you don't need to replay all the former events every time.

# Limitations of the current solution

- In a real world application the commands should be processed sequentially for the same entity (by an actor for instance) 
to avoid conflicting commands being accepted at the same time
- 

# Possible improvements

- When producing events, they could be published into a message queue (like Kafka). Then consumers could subscribe to those events to get the latest changes.
- On the graphql side, we could add a subscription endpoint to receive events from an entity (and receive game updates)

