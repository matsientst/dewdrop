package events.dewdrop;

import events.dewdrop.api.result.Result;
import events.dewdrop.api.validators.ValidationException;
import events.dewdrop.config.DewdropSettings;
import events.dewdrop.structure.api.Command;

/**
 * Dewdrop is a simple, fast, and powerful java based event sourcing framework. The idea of Dewdrop
 * is to make it easy to build an event driven system easily and quickly by pushing all the complex
 * reading, writing and marshalling down deep into the framework allowing your team to focus on
 * building out the business logic in terms of AggregateRoot behavior, Query logic, and ReadModel
 * composition.
 */
public class Dewdrop {
    private final DewdropSettings settings;

    public Dewdrop(DewdropSettings settings) {
        this.settings = settings;
    }

    /**
     * This is the main entry point for the Dewdrop framework. It is used to execute a command against
     * an AggregateRoot and return the Result. To have this work correctly, you'll need an AggregateRoot
     * class that is decorated with @AggregateRoot and a method on that class that is decorated
     * with @CommandHandler with your command class as the only parameter.
     *
     * The Command object needs to extend the Command class and can use JSR-303 validation annotations
     * to validate the command by calling DewdropValidator.validate(command).
     *
     * The event that is generated and returned can be of any type that extends the Event class.
     * Alternatively, it can return a List of Event objects.
     *
     * @CommandHandler public AccountCreated createAccount(CreateAccountCommand command) {
     *                 DewdropValidator.validate(command); return new
     *                 AccountCreated(command.getAccountId(), command.getUserId(),
     *                 command.getBalance()); }
     *
     * @param command The command to execute.
     * @return Result<Boolean>
     */
    public <T extends Command> Result<Boolean> executeCommand(T command) throws ValidationException {
        return settings.getAggregateStateOrchestrator().executeCommand(command);
    }

    /**
     * This method is exactly the same as `executeCommand()` except it will generate the corresponding
     * correlation and causation ids for the command. These are used to correlate events and ascribe a
     * tracking to understand where the event came from.
     *
     * @param command The command to execute.
     * @param previous The previous command that was executed.
     * @return Result<Boolean>
     */
    public <T extends Command> Result<Boolean> executeSubsequentCommand(T command, Command previous) throws ValidationException {
        return settings.getAggregateStateOrchestrator().executeSubsequentCommand(command, previous);
    }

    /**
     * This is the query entry point for the Dewdrop framework. It is used to execute a query against a
     * ReadModel and return the Result. To have this work correctly, you'll need a ReadModel class that
     * is decorated with @ReadModel and a method on that class that is decorated with @QueryHandler with
     * your query class as the only parameter.
     *
     * @QueryHandler public AccountDetails getById(GetAccountByIdQuery query) { return
     *               cache.get(query.getAccountId()); }
     *
     * @param query The query to execute.
     * @return Result<R>
     */
    public <T, R> Result<R> executeQuery(T query) {
        return settings.getQueryStateOrchestrator().executeQuery(query);
    }

    /**
     * `getSettings()` returns the settings object for the current Dewdrop instance
     *
     * @return The settings object.
     */
    public DewdropSettings getSettings() {
        return settings;
    }
}
