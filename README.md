# dewdrop
Dewdrop is a simple, fast, and powerful java based event sourcing framework. The idea of Dewdrop is to make it easy to build a system that is event driven and easy to test by pushing all of the complex reading, writing and marshalling down deep into the framework allowing your team to focus on building out the business logic in terms of AggregateRoot behavior, Query logic, and ReadModel composition. 

If you're new to event sourcing we highly suggest understanding the main concepts around the ideas of CQRS, DDD, Event Storming and Event Modeling.

Dewdrop is in its early stages of development and is not yet production ready. If you're interested in using it in a production environment, please reach out. We are looking to continue testing and devloping until it's ready for prime time. If you're only interested in using Dewdrop as a tool to understand event sourcing or for a simple project this is perfect for you. We'd also love to hear your feedback!

Currently, Dewdrop only supports using [EventStore](https://www.eventstore.com/) as its backing store. We will be expanding it to use other data stores and would love your feedback on what is most interesting to you in terms of next steps.

Requirements:
* Java 11 or later
* EventStore


### Getting Started
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
            .packageToScan("com.dewdrop")
            .packageToExclude("com.dewdrop.fixture.customized")
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


### Using Dewdrop
The easiest way to use Dewdrop is to use the `Dewdrop` class which encapsulates the entire framework. The crazy simple API is:

`dewdrop.executeCommand(command)`

`dewdrop.executeSubsequentCommand(command, earlierCommand)`

`dewdrop.executeQuery(query)`

The framework will take care of the rest.

### AggregateRoot
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
The `@AggregateRootId` is used to know which aggregateRoot to modify. The `Command` class is a base class that is used to create commands. All the command classes MUST extend this object.

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
The framework runs this method to generate the needed events. It then sends the events to the their applicable @EventHandler methods on the AggregateRoot. In this case:
```java
    @EventHandler
    public void on(DewdropAccountCreated event) {
        this.accountId = event.getAccountId();
        this.name = event.getName();
    }
```
Which then updates the state of the aggregateRoot and then publishes the event to the event store.

