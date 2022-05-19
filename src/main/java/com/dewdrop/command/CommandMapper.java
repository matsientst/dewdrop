package com.dewdrop.command;

import com.dewdrop.streamstore.repository.StreamStoreRepository;
import com.dewdrop.structure.api.Command;
import java.lang.reflect.Method;
import java.util.Optional;

public interface CommandMapper {
    void init(StreamStoreRepository streamStoreRepository);

    Optional<Method> getCommandHandlersThatSupportCommand(Command command);
}
