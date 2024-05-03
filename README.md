# Dewdrop
Dewdrop is a simple, fast, and powerful java based event sourcing framework. The idea of Dewdrop is to make it easy to build an event driven system easily and quickly by pushing all the complex reading, writing and marshalling down deep into the framework allowing your team to focus on building out the business logic in terms of AggregateRoot behavior, Query logic, and ReadModel composition. 

If you're new to event sourcing we highly suggest understanding the main concepts around the ideas of CQRS, DDD, Event Storming and Event Modeling.

Dewdrop is in its early stages of development and is not yet production ready. If you're interested in using it in a production environment, please reach out. We are looking to continue testing and devloping until it's ready for prime time. If you're only interested in using Dewdrop as a tool to understand event sourcing or for a simple project, then Dewdrop is perfect for you. We'd also love to hear your feedback!

Currently, Dewdrop only supports using [EventStore](https://www.eventstore.com/) as its backing store. We will be expanding it to use other data stores and would love your feedback on what is most interesting to you in terms of next steps.

Requirements:
* Java 11 or later
* EventStore


## Getting Started
First, you need to make sure you're running EventStore locally. To do this you can go download the [EventStore](https://www.eventstore.com/downloads) client and run it. Or, you can run a docker instance which is included in the repository.
To start the docker instance, run the following command in the dewdrop directory:

`docker-compose up -d`

We are also assuming that most people getting going with the project are running it inside a dependency injected framework like Spring Boot. If this is the case you need to create a class that wraps your DI application context. For the case of Spring Boot you'd create a class that implements `DependencyInjectionAdapter` and expose it as a bean.

```java
public class DewdropDependencyInjection implements DependencyInjectionAdapter {
        private ApplicationContext applicationContext;
        public DewdropDependencyInjection(ApplicationContext applicationContext) {
                this.applicationContext = applicationContext;
        }

        @Override
        public <T> T getBean(Class<?> clazz) {
            return (T) applicationContext.getBean(clazz);
        }
}
```
This lets Dewdrop know that it should use the application context to get the spring managed beans it needs.

The next step is to create a `DewdropConfiguration` class that will be used to configure the Dewdrop framework.

```java
import java.beans.BeanProperty;

public class DewdropConfiguration {
    @Autowired
    ApplicationContext applicationContext;
    
    @Bean 
    public DewdropDependencyInjection dependencyInjection() {
        return new DewdropDependencyInjection(applicationContext);
    }
    
    @Bean
    public DewdropProperties dewdropProperties() {
        return DewdropProperties.builder()
            .packageToScan("events.dewdrop")
            .packageToExclude("events.dewdrop.fixture.customized")
            .connectionString("esdb://localhost:2113?tls=false")
            .create();
    }
    
    @Bean 
    public Dewdrop dewdrop() {
        return DewdropSettings.builder()
            .properties(dewdropProperties())
            .dependencyInjectionAdapter(dependencyInjection())
            .create()
            .start();
    }
}
``` 
And that is it! You can now run the application and it will start up the Dewdrop framework.

## Core Concepts of Dewdrop
Dewdrop follows a few architectural principles religiously to make it easy to build a system that is event driven. The following are the core concepts of Dewdrop:
* DDD - Domain Driven Design
* CQRS - Command Query Responsibility Separation
* Event sourcing - Event driven architecture

If you are unfamiliar with these principles, we highly suggest diving deeper and understanding them before jumping into Dewdrop. It will make much more sense when you're familiar with these concepts.


## Using Dewdrop
The easiest way to use Dewdrop is to use the `Dewdrop` class which encapsulates the entire framework. The crazy simple API is:

`dewdrop.executeCommand(command)`

`dewdrop.executeSubsequentCommand(command, earlierCommand)`

`dewdrop.executeQuery(query)`

The framework will take care of the rest.

### Command side - dewdrop.executeCommand(command)
Eric Evans’ book Domain Driven Design describes an abstraction called “aggregate”:

> “An aggregate is a cluster of associated objects that we treat as a unit for the purpose of data changes. Each aggregate has a root and a boundary.”

Dewdrop follows this idea assuming that to mutate state we create an AggregateRoot that is used as a unit of persistent to group together related events. For example, if you have a `User` aggregate you would create a `UserAggregate` that then contains all the command handling and event handling for mutating state.

To create an AggregateRoot in Dewdrop, you need to create a class and then decorate it with the `@AggregateRoot` annotation. This tells the framework that this is the aggregateRoot object where the logic for state mutation exists. This is an example of a simple AggregateRoot: 

```java
@Aggregate
public class DewdropAccountAggregate {
    @AggregateId
    UUID accountId;
    String name;
    BigDecimal balance = BigDecimal.ZERO;

    public DewdropAccountAggregate() {}

    @CommandHandler
    public List<DewdropAccountCreated> handle(DewdropCreateAccountCommand command) {
        if (StringUtils.isEmpty(command.getName())) { throw new IllegalArgumentException("Name cannot be empty"); }
        if (command.getAccountId() == null) { throw new IllegalArgumentException("AccountId cannot be empty"); }
        if (command.getUserId() == null) { throw new IllegalArgumentException("UserId cannot be empty"); }

        return new DewdropAccountCreated(command.getAccountId(), command.getName(), command.getUserId());
    }

    @EventHandler
    public void on(DewdropAccountCreated event) {
        this.accountId = event.getAccountId();
        this.name = event.getName();
    }
}
```

To explain what is going first we need to talk about the `@Aggregate` annotation. This is a marker annotation that tells the framework that this is an aggregateRoot. The framework will use this to get the class from DI or create a class that is then wrapped by the `AggregateRoot` class. The `AggregateRoot` class is a base class that provides the basic functionality for an aggregateRoot.

The key here is to understand that there is a lifecycle to modifying an AggregateRoot. The first step is to create a command to modify the AggregateRoot. This is done by creating a class that extends the `Command` class.

### Command
```java
public class DewdropCreateAccountCommand extends DewdropAccountCommand {
    private String name;
    private UUID userId;

    public DewdropCreateAccountCommand(UUID accountId, String name, UUID userId) {
        super(accountId);
        this.name = name;
        this.userId = userId;
    }
}
```
Which as you can see extends `DewdropAccountCommand` which is a base class that wraps the `@AggregateRootId`.
```java
public abstract class DewdropAccountCommand extends Command {
    @AggregateId
    private UUID accountId;

    public DewdropAccountCommand(UUID accountId) {
        super();
        this.accountId = accountId;
    }
}
```
The `@AggregateRootId` is the identifier of the AggregateRoot. Currently the framework only supports UUID as an identifier. This id is used to read/write to the appropriate stream for the AggregateRoot. For example, if you have an AggregateRoot like the one listed above `DewdropAccountAggregate`, and a UUID of `e4cb9a9c-308f-4fb7-a3df-8ada95749c7e`, the framework would save this to a stream called `DewdropAccountAggregate-e4cb9a9c-308f-4fb7-a3df-8ada95749c7e`. And in turn, for each command that is run on this AggregateRoot after it's creation, you'll need to pass in the appropriate UUID as the `@AggregateRootId` for the framework to know which AggregateRoot to load.

In this situation we have created an abstract base class called `DewdropAccountCommand` to hold this value. And all commands that impact this AggregateRoot should extend this class. We suggest you create you're own version of this base command class for each of your AggregateRoots. 

The `Command` class is a base class that is used to create commands. All the command classes MUST extend this object for the framework to function correctly. 

This relates directly to the CQRS pattern. The `Command` class is used to create a command that is then sent to the aggregateRoot. The aggregateRoot then modifies the state of the aggregateRoot and then publishes an event that is then sent to the event store.

Here's an example test:
```java
    @Test
    private DewdropCreateAccountCommand createAccount(Dewdrop dewdrop, UUID userId) {
        DewdropCreateAccountCommand command = new DewdropCreateAccountCommand(UUID.randomUUID(), "test", userId);
        dewdrop.executeCommand(command);
        return command;
    }
```
This test creates a command to create an account and then sends it to the Dewdrop framework. The framework then sends the command to the aggregateRoot and is then handled by the `@CommandHandler` method on the AggregateRoot that has the `DewdropCreateAccountCommand` as it's first parameter.
```java
    @CommandHandler
    public List<DewdropAccountCreated> handle(DewdropCreateAccountCommand command) {
        if (StringUtils.isEmpty(command.getName())) { throw new IllegalArgumentException("Name cannot be empty"); }
        if (command.getAccountId() == null) { throw new IllegalArgumentException("AccountId cannot be empty"); }
        if (command.getUserId() == null) { throw new IllegalArgumentException("UserId cannot be empty"); }

        return new DewdropAccountCreated(command.getAccountId(), command.getName(), command.getUserId());
    }
```

### Event
The framework runs this method to generate the needed events. It then sends the events to the their applicable @EventHandler methods on the AggregateRoot. In this case:
```java
    @EventHandler
    public void on(DewdropAccountCreated event) {
        this.accountId = event.getAccountId();
        this.name = event.getName();
    }
```
Much like the `Command` class, the `Event` classes are DTO's used to represent the state of the AggregateRoot. The `Event` classes are used to both read/write to the event store and to publish events to the event store.

```java
public class DewdropAccountCreated extends DewdropAccountEvent {
    private String name;
    private UUID userId;

    public DewdropAccountCreated(UUID accountId, String name, UUID userId) {
        super(accountId);
        this.name = name;
        this.userId = userId;
    }
}
```
As you can see, this class also extends a base `Event` class and if we look at that:
```java
public abstract class `DewdropAccountEvent` extends Event {
    @AggregateId
    private UUID accountId;

}
```
Like the `DewdropAccountCommand` class, the `DewdropAccountCreated` class extends the abstract `DewdropAccountEvent` class which holds the `@AggregateRootId`. Again, this is the identifier of the AggregateRoot.
We highly suggest that you create base event classes for each of your AggregateRoots. This is a good way to keep your code clean and easy to maintain.

### Lifecycle of the AggregateRoot

To understand better what dewdrop is doing we want to outline exactly what the framework is doing under the covers. To help illustrate this, here is the lifecycle that occurs:
* User created code calls `dewdrop.executeCommand(command)`
* The framework looks for the AggregateRoot that implements `@CommandHandler` with the Command object as it's first parameter
* If the command object has an `@AggregateRootId` then the framework will load the AggregateRoot from the event store and replay all existing events to rebuild state
* It then calls the `@CommandHandler` method on the AggregateRoot with the Command object as it's first parameter, which returns either a single or list of events (extends Event)
* It then calls the `@EventHandler` method on the AggregateRoot with the Event object as it's first parameter to mutate the state of the AggregateRoot
* It then persists the events to the event store

## Query - dewdrop.executeQuery(query)
The query side relies on creation of ReadModels that are used to read from the event store. Think of a ReadModel as similar to a repository or a DAO in more traditional architectures. It is the object that defines how we read data from the event store. Now, your first intuition is to map it directly to your AggregateRoot, which can be correct at times, but more often it is not. 

Dewdrop will read the json events from the event store, find which ReadModels are listening to those events, convert them into the event objects and then replay those event on the ReadModel.

Usually, when we are reading from an event store we are looking to read from a complex interaction of events. If you start to think of what data you're looking to read as a collection of events it can help with architecting your ReadModel correctly. 

So, first off, let's look at an example ReadModel in Dewdrop
```java
@ReadModel
@Stream(name = "DewdropFundsAddedToAccount", streamType = StreamType.EVENT)
@Stream(name = "DewdropAccountCreated", streamType = StreamType.EVENT)
public class DewdropAccountSummaryReadModel {
    @DewdropCache
    DewdropAccountSummary dewdropAccountSummary;

    @QueryHandler
    public DewdropAccountSummary handle(DewdropAccountSummaryQuery query) {

        return dewdropAccountSummary;
    }
}
``` 
Here are the important bits:
* The `@ReadModel` annotation is used to mark the class as a ReadModel
* The `@Stream` annotation is used to identify what streams to read from the event store
* The `@DewdropCache` annotation is used to cache the data in the ReadModel
* The `@QueryHandler` annotation is used to identify the method that will handle the query

### @ReadModel
As you can see we have annotated this class with `@ReadModel`. Dewdrop on startup looks to identify ALL the classes that it can find that implement `@ReadModel` and then keeps track of them.
When it finds a ReadModel it will assume that we automatically want to create that ReadModel unless we tell it not to.

If we have a situation where we don't want a ReadModel to be created and live forever, we can mark the ReadModel as `ephemeral` which tells Dewdrop to only create this class when we see a query executed for this. Consider it a lazy ReadModel, or a just in time ReadModel. If we want to have an `ephemeral` ReadModel we can then choose what that means by adding the `destroyInMinutesUnused` field to our `@ReadModel` annotation on our class.
For example:
```java
@ReadModel(ephemeral = true, destroyInMinutesUnused = ReadModel.DESTROY_IMMEDIATELY)
@Stream(name = "DewdropAccountAggregate", subscribed = true)
@Stream(name = "DewdropUserAggregate", subscribed = false)
public class DewdropAccountDetailsReadModel {
    @DewdropCache
    Map<UUID, DewdropAccountDetails> cache;

    @QueryHandler
    public Result<DewdropAccountDetails> handle(DewdropGetAccountByIdQuery query) {
        DewdropAccountDetails dewdropAccountDetails = cache.get(query.getAccountId());
        if (dewdropAccountDetails != null) { return Result.of(dewdropAccountDetails); }
        return Result.empty();
    }
}
```
Here we've told Dewdrop that we want to create this ReadModel when we see a query executed for this ReadModel. We also told Dewdrop that we want to destroy this ReadModel immediately after it was used. This is a good way to keep your ReadModels from growing too large and eating up memory. However, this would be a bad strategy to use in a stream that has a lot of events associated with. This would become an expensive operation, and would take a long time to load if there were thousands of events.
The three states of `destroyInMinutesUnused` to note are:
* `DESTROY_IMMEDIATELY` - This will destroy the ReadModel immediately after it was used.
* `NEVER_DESTROY` - This will add the ReadModel to the cache, but it will never be destroyed until restart (just like a non-ephemeral ReadModel).
* `n` - Just add a value here for the count of minutes you want it to live on for before destruction.

Destroying a ReadModel after an hour of use is a good idea for situations where you have a UI that is being heavily used by a user and you want to keep the ReadModel alive for snappy performance. However, after that user is done we can then destroy this ReadModel and clear up memory.

### @Stream
The `@Stream` annotation is used to identify what streams to read from the event store. You can have multiple `@Stream` annotations onto a ReadModel. To identify what stream you want to read from the Stream has these fields:
* `name` - The name of the stream to read from
* `streamType` - The type of the stream you want to read from which you can use the enum `StreamType` (`EVENT`, `CATEGORY` or `AGGREGATE`) to identify. `CATEGORY` is default
* `subscribed` - Whether or not you want to subscribe to the stream. If you want to subscribe to the stream you can leave it blank since the default is `true`.
* `direction` - Which direction you want to read - you can use the enum `Direction` (`FORWARD` or `BACKWARD`)

The name annotation should NOT be the full name that you see in your event store. In this case we use a name generator to create the name that is needed. For example 
For the example above, you can see we passed in a name of 

`DewdropAccountAggregate` 

Since the Stream default type is `CATEGORY` we end up generating the name

`$ce-DewdropAccountAggregate` 

If the stream type is `EVENT` we end up generating the name

`$et-DewdropAccountAggregate` 

But, that wouldn't make sense since this is an AggregateRoot name, a better example would be:

`$et-DewdropAccountCreated`

### @DewdropCache
This annotation marks the field as a cache. This is used to cache the data in the ReadModel. 
```java
 @DewdropCache
 Map<UUID, DewdropAccountDetails> cache;
```
This is one of the huge benefits of Dewdrop. Dewdrop will automatically based on the ReadModel and streams decorating the class be able to read the events from the event store and populate the ReadModel. This is a powerful and elegant way to get up and running with event sourcing without having to build a HUGE amount of plumbing.
These events are replayed back to the ReadModel and the ReadModel is updated. You can choose NOT to have a cache for a ReadModel by adding not adding the `@DewdropCache` annotation field. In this scenario, you should create the `@EventHandler` on the ReadModel itself.


There are two types of caches:
* Map - This is a map that is used to cache the data in the ReadModel. The key is the id of the object and the value is the DTO that you want to map to (more on this later).
* Single item - This is a single item that is used to cache the data in the ReadModel. There is no key since this becomes an accumulator. So if, for example, you want to cache the total balance of all the accounts in the system you can use this.

This is a ridiculously useful feature and is very useful. However, be careful about what is stored in cache since it is not persisted and can get quite large.

#### How to use the cache
Both types of caches (map and single) assume that you have a DTO that you are writing state to. This should be a simple POJO, but it will need the `@EventHandler` methods decorating the methods to update state (much like the AggregateRoot).
The Cache DTO is made of a few annotations that dewdrop uses to know how to create and manage the cache:
* `@DewdropCache` - This is the annotation that marks the field as a cache. This is used to cache the data in the ReadModel.
* `@PrimaryKey` - This is the annotation that marks the field as the primary key. This is used to identify the primary key of the cache.
* `@SecondaryKey` - This is the annotation that marks the field as a secondary key. This is used to identify the secondary key of the cache.
* `@CreationEvent` - This is the annotation that marks the field as the event that marks the creation of the cache item. This is used to know to create the DTO and starting using it for that key.
* `@EventHandler` - This is the annotation that marks the method that updates the cache. This is used to update the DTO with the event.

For example:
```java
public class DewdropAccountDetails {
    @PrimaryCacheKey
    private UUID accountId;
    private String name;
    private BigDecimal balance = BigDecimal.ZERO;
    @ForeignCacheKey
    private UUID userId;
    private String username;

    @EventHandler
    public void on(DewdropAccountCreated event) {
        this.accountId = event.getAccountId();
        this.name = event.getName();
        this.userId = event.getUserId();
    }

    @EventHandler
    public void on(DewdropFundsAddedToAccount event) {
        this.balance = this.balance.add(event.getFunds());
    }

    @EventHandler
    public void on(DewdropUserCreated userSignedup) {
        this.username = userSignedup.getUsername();
    }
}
```
In this example, we are reading from two streams as outlined above in the ReadModel example:
```java
@Stream(name = "DewdropAccountAggregate", subscribed = true)
@Stream(name = "DewdropUserAggregate", subscribed = false)
```

This object becomes the intersection of those two streams and replays events from each. The framework will automatically call the `@EventHandler` methods to update the state of the object based on the event received. 
In this case, we have an event `DewdropAccountCreated` which we create a method to handel like:
```java
    @EventHandler
    public void on(DewdropAccountCreated event) {
        this.accountId = event.getAccountId();
        this.name = event.getName();
        this.userId = event.getUserId();
    }
```
#### @EventHandler
You can implement any `@EventHandler` methods to handle any events you wish to from a stream. You can handle them all, or ignore the ones that are not relevant.

The events that are handled here are the same events we created when modifying the `DewdropAccountAggregate`.

This event is a special event, so let's dive a little deeper into it. 
```java
@CreationEvent
public class DewdropAccountCreated extends DewdropAccountEvent {
    private String name;
    private UUID userId;

    public DewdropAccountCreated(UUID accountId, String name, UUID userId) {
        super(accountId);
        this.name = name;
        this.userId = userId;
    }
}
```
#### @CreationEvent
This event has the annotation of `@CreationEvent` which means that this event is the event that is used to create the cache item. Without it, Dewdrop has no idea which event is the starting point and when to create this object.

#### @PrimaryCacheKey
The `@PrimaryCacheKey` annotation is used to identify the field that is used as the key for the cache. This relates to the `@AggregateId` annotation. The framework cache first looks for the `@PrimaryCacheKey` annotation and if it finds it, it uses that field as the key and then looks for an `@AggregateId` annotation that matches it. 

#### @ForeignCacheKey
The `@ForeignCacheKey` annotation is used to identify the field that is the foreign key used as the key for the cache. For example, In this example, we have a `@ForeignCacheKey` annotation on the `userId` field. This means that when it finds the `userId` field in the events it will relate to that field.

### No Cache
If you decide to not use a cache, then you should skip adding the `@DewdropCache` field. When you do this, you should add the `@EventHandler` method to the ReadModel itself. If you want to persist the current state in a local datastore you can make your ReadModel a spring object and then inject your repository into the ReadModel. Then on each event you can update your repository with the new state.

You also need to add a `@StartFromPosition` decorated method that returns a long to retrieve the last version number from your cache to tell the framework where to start for that Stream. The `@StreamStartPosition` name and streamType must match the `@Stream` name and type on the same ReadModel. 

```java
    @StreamStartPosition(name = "DewdropAccountAggregate")
    public Long startPosition() {
        Optional<Long> position = accountDetailsRepository.lastAccountVersion();
        return position.orElse(0L);
    }
```


### Querying
To bring this all together, to query your ReadModel all you need to do is create a query object and then call the `query` method on the ReadModel.

```java
public class DewdropGetAccountByIdQuery {
    private UUID accountId;
}
```
The query objects are nothing special and can be whatever type of objects you wish. They do not extend any special classes. They are just plain old POJO's.

To query a ReadModel, you just pass your query object to the `executeQuery(query)` method on dewdrop.

```java
DewdropGetAccountByIdQuery query = new DewdropGetAccountByIdQuery(accountId);
dewdrop.executeQuery(query);
```
The framework will automatically find the ReadModel associated with the Query, construct it (if needed) and then call the `@QueryHandler` method on it.
Then, the `@QueryHandler` method will be called on the ReadModel and it will return the result of the query.

```java
    @QueryHandler
    public Result<DewdropAccountDetails> handle(DewdropGetAccountByIdQuery query) {
        DewdropAccountDetails dewdropAccountDetails = cache.get(query.getAccountId());
        if (dewdropAccountDetails != null) { return Result.of(dewdropAccountDetails); }
        return Result.empty();
    }
```
In this query, since there is a `@DewdropCache`, you just need to look into the cache to find the object and return it!

If you don't have cache, then you can query against a local datastore or however, you want to architect it.

### Acknowledgements
I want to thank a number of people for their help and support in creating this library.

First, I want to thank Chris Condron at Event Store, and Josh Kempner at PerkinElmer for their expertise and guidance in creating the Dewdrop client library. A lot of the basis of Dewdrop was inspired by the [reactive-domain](https://github.com/ReactiveDomain/reactive-domain) project built in C# by both Chris and Josh.
Second, I want to thank Tom Friedhof, Kurtis Moffett, and Ryan Hartman for their contributions and effort in creating Dewdrop.

Thanks Guys!