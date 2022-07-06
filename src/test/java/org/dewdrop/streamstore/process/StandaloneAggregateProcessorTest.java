package org.dewdrop.streamstore.process;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.dewdrop.aggregate.AggregateRoot;
import org.dewdrop.fixture.automated.DewdropUserAggregate;
import org.dewdrop.fixture.command.DewdropCreateUserCommand;
import org.dewdrop.read.readmodel.stream.StreamFactory;
import org.dewdrop.read.readmodel.stream.Stream;
import org.dewdrop.streamstore.repository.StreamStoreGetByIDRequest;
import org.dewdrop.utils.AggregateIdUtils;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class StandaloneAggregateProcessorTest {

    StandaloneAggregateProcessor standaloneAggregateProcessor;
    StreamFactory streamFactory;
    AggregateRoot aggregateRoot;
    Stream stream;
    UUID id = UUID.randomUUID();

    @BeforeEach
    void setup() {
        streamFactory = mock(StreamFactory.class);
        standaloneAggregateProcessor = Mockito.spy(new StandaloneAggregateProcessor(streamFactory));
        DewdropUserAggregate target = new DewdropUserAggregate();
        aggregateRoot = spy(new AggregateRoot(target));
        stream = mock(Stream.class);
    }

    @Test
    @DisplayName("save() - Given a valid aggregate root, save it to the stream")
    void save() {
        doReturn(stream).when(streamFactory).constructStreamFromAggregateRoot(any(AggregateRoot.class), any(UUID.class));
        try (MockedStatic<AggregateIdUtils> utilities = mockStatic(AggregateIdUtils.class)) {
            utilities.when(() -> AggregateIdUtils.getAggregateId(any(DewdropUserAggregate.class))).thenReturn(Optional.of(UUID.randomUUID()));
            AggregateRoot result = standaloneAggregateProcessor.save(aggregateRoot);
            assertThat(result, is(notNullValue()));

            verify(stream, times(1)).save(aggregateRoot);
        }
    }

    @Test
    @DisplayName("save() - Given a valid aggregate root, save it to the stream")
    void save_noAggregateId() {
        try (MockedStatic<AggregateIdUtils> utilities = mockStatic(AggregateIdUtils.class)) {
            utilities.when(() -> AggregateIdUtils.getAggregateId(any(DewdropUserAggregate.class))).thenReturn(Optional.empty());
            assertThrows(IllegalArgumentException.class, () -> standaloneAggregateProcessor.save(aggregateRoot));
        }
    }

    @Test
    @DisplayName("getById() - Given a valid aggregate root and a UUID and a command, construct a stream and call stream.getById()")
    void getById() {
        doReturn(aggregateRoot).when(standaloneAggregateProcessor).getAggregateRoot(any(DewdropUserAggregate.class));
        doReturn(stream).when(streamFactory).constructStreamFromAggregateRoot(any(AggregateRoot.class), any(UUID.class));
        doReturn(aggregateRoot).when(stream).getById(any(StreamStoreGetByIDRequest.class));

        DewdropCreateUserCommand command = mock(DewdropCreateUserCommand.class);
        standaloneAggregateProcessor.getById(aggregateRoot, id, command);

        ArgumentCaptor<StreamStoreGetByIDRequest> requestCaptor = ArgumentCaptor.forClass(StreamStoreGetByIDRequest.class);
        verify(stream, times(1)).getById(requestCaptor.capture());

        StreamStoreGetByIDRequest request = requestCaptor.getValue();
        assertThat(request.getAggregateRoot(), is(aggregateRoot));
        assertThat(request.getId(), is(id));
        assertThat(request.getVersion(), is(Integer.MAX_VALUE));
        assertThat(request.getCommand(), is(command));
    }

    @Test
    @DisplayName("getById() - Given a valid aggregate root and a UUID, call the getById() method with a null command")
    void getById_noCommand() {
        doReturn(aggregateRoot).when(standaloneAggregateProcessor).getById(any(AggregateRoot.class), any(UUID.class), isNull());

        standaloneAggregateProcessor.getById(aggregateRoot, id);

        verify(standaloneAggregateProcessor, times(1)).getById(any(AggregateRoot.class), any(UUID.class), isNull());
    }


    @Test
    @DisplayName("getAggregateRoot() - Given a valid aggregate root, do we retrieve an AggregateRoot")
    void getAggregateRoot() {
        AggregateRoot result = standaloneAggregateProcessor.getAggregateRoot(aggregateRoot);
        assertThat(result, is(aggregateRoot));
    }

    @Test
    @DisplayName("getAggregateRoot() - Given an object annotated with @AggregateRoot, do we return an AggregateRoot")
    void getAggregateRoot_AggregateRootObject() {
        AggregateRoot result = standaloneAggregateProcessor.getAggregateRoot(new DewdropUserAggregate());
        assertThat(result, is(instanceOf(AggregateRoot.class)));
    }
}
