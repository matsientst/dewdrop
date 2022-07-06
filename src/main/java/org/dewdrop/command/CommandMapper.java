package org.dewdrop.command;

import org.dewdrop.structure.api.Command;
import java.lang.reflect.Method;
import java.util.Optional;

public interface CommandMapper {

    Optional<Method> getCommandHandlersThatSupportCommand(Command command);
}
