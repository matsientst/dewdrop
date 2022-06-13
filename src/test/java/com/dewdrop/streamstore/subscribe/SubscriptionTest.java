package com.dewdrop.streamstore.subscribe;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.dewdrop.fixture.events.DewdropUserCreated;
import com.dewdrop.fixture.events.DewdropUserEvent;
import com.dewdrop.read.NameAndPosition;
import com.dewdrop.read.StreamReader;
import com.dewdrop.read.StreamType;
import com.dewdrop.structure.read.Handler;
import com.dewdrop.structure.subscribe.EventProcessor;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SubscriptionTest {
    Subscription<DewdropUserEvent> subscription;
    StreamListener<DewdropUserEvent> streamListener;
    List<Class<?>> messageTypes;
    Handler<DewdropUserEvent> handler;
    NameAndPosition nameAndPosition;
    StreamReader streamReader;

    @BeforeEach
    void setup() {
        this.handler = mock(Handler.class);
        this.messageTypes = List.of(DewdropUserEvent.class);
        this.streamListener = mock(StreamListener.class);
        this.subscription = spy(new Subscription<>(handler, messageTypes, streamListener));
        this.nameAndPosition = new NameAndPosition(StreamType.CATEGORY, "ContentType", mock(Consumer.class));
        this.streamReader = mock(StreamReader.class);
    }

    @Test
    void registerToMessageType() {
        EventProcessor<DewdropUserEvent> eventProcessor = new EventProcessor<>(handler, List.of(DewdropUserEvent.class));
        subscription.registerToMessageType(eventProcessor, DewdropUserEvent.class);
        assertThat(subscription.getHandlesFor(DewdropUserEvent.class).size(), is(2));
    }

    @Test
    void registerToMessageType_isSame() {
        EventProcessor<DewdropUserEvent> eventProcessor = spy(new EventProcessor<>(handler, List.of(DewdropUserEvent.class)));
        subscription.getHandlers().put(DewdropUserEvent.class, List.of(eventProcessor));
        doReturn(true).when(eventProcessor).isSame(any(Class.class), any());

        subscription.registerToMessageType(eventProcessor, DewdropUserEvent.class);
        assertThat(subscription.getHandlesFor(DewdropUserEvent.class).size(), is(1));
    }

    @Test
    void getHandlesFor() {
        List<EventProcessor<DewdropUserEvent>> handlesFor = subscription.getHandlesFor(DewdropUserEvent.class);
        assertThat(handlesFor.size(), is(1));
    }

    @Test
    void getHandlesFor_none() {
        List<EventProcessor<DewdropUserEvent>> handlesFor = subscription.getHandlesFor(String.class);
        assertThat(handlesFor.size(), is(0));
    }

    @Test
    void publish() {
        DewdropUserCreated event = new DewdropUserCreated(UUID.randomUUID(), "userName");
        EventProcessor eventProcessor = mock(EventProcessor.class);
        doReturn(List.of(eventProcessor)).when(subscription).getHandlesFor(any(Class.class));
        doNothing().when(eventProcessor).process(any(DewdropUserEvent.class));
        subscription.publish(event);

        verify(eventProcessor, times(1)).process(event);
    }

    @Test
    void subscribeByNameAndPosition() {
        doReturn(true).when(streamListener).start(anyString(), anyLong(), any(Subscription.class));
        doReturn(true).when(streamReader).isStreamExists();
        doReturn(nameAndPosition).when(streamReader).nameAndPosition();

        nameAndPosition.completeTask("test", 0L);
        subscription.subscribeByNameAndPosition(streamReader);

        verify(streamListener, times(1)).start(anyString(), anyLong(), any(Subscription.class));
    }

    @Test
    void subscribeByNameAndPosition_subscribed() {
        doReturn(false).when(streamListener).start(anyString(), anyLong(), any(Subscription.class));
        doReturn(true).when(streamReader).isStreamExists();
        doReturn(nameAndPosition).when(streamReader).nameAndPosition();

        nameAndPosition.completeTask("test", 0L);
        subscription.subscribeByNameAndPosition(streamReader);

        verify(streamListener, times(1)).start(anyString(), anyLong(), any(Subscription.class));
    }

    @Test
    void subscribeByNameAndPosition_noStream() {
        doReturn(false).when(streamReader).isStreamExists();
        doReturn(nameAndPosition).when(streamReader).nameAndPosition();

        subscription.subscribeByNameAndPosition(streamReader);

        verify(streamListener, times(0)).start(anyString(), anyLong(), any(Subscription.class));
    }

    @Test
    void pollForCompletion() {
        doNothing().when(subscription).schedule(any(StreamReader.class), any(CompletableFuture.class), any(Runnable.class));
        doReturn(nameAndPosition).when(streamReader).nameAndPosition();
        doReturn(true).when(streamReader).isStreamExists();

        nameAndPosition.completeTask("test", 0L);
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<CompletableFuture> completableFutureCaptor = ArgumentCaptor.forClass(CompletableFuture.class);

        subscription.pollForCompletion(streamReader);

        verify(subscription, times(1)).schedule(any(StreamReader.class), completableFutureCaptor.capture(), captor.capture());

        Runnable result = captor.getValue();
        result.run();

        CompletableFuture future = completableFutureCaptor.getValue();
        assertThat(future.isDone(), is(true));
    }

    @Test
    void pollForCompletion_notComplete() {
        doNothing().when(subscription).schedule(any(StreamReader.class), any(CompletableFuture.class), any(Runnable.class));
        doReturn(nameAndPosition).when(streamReader).nameAndPosition();

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<CompletableFuture> completableFutureCaptor = ArgumentCaptor.forClass(CompletableFuture.class);

        subscription.pollForCompletion(streamReader);

        verify(subscription, times(1)).schedule(any(StreamReader.class), completableFutureCaptor.capture(), captor.capture());

        Runnable result = captor.getValue();
        result.run();

        CompletableFuture future = completableFutureCaptor.getValue();
        assertThat(future.isDone(), is(false));
    }

    @Test
    void pollForCompletion_streamDoesntExist() {
        doNothing().when(subscription).schedule(any(StreamReader.class), any(CompletableFuture.class), any(Runnable.class));
        doReturn(nameAndPosition).when(streamReader).nameAndPosition();
        doReturn(false).when(streamReader).isStreamExists();
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<CompletableFuture> completableFutureCaptor = ArgumentCaptor.forClass(CompletableFuture.class);

        subscription.pollForCompletion(streamReader);

        verify(subscription, times(1)).schedule(any(StreamReader.class), completableFutureCaptor.capture(), captor.capture());

        Runnable result = captor.getValue();
        result.run();

        CompletableFuture future = completableFutureCaptor.getValue();
        assertThat(future.isDone(), is(false));
    }

    @Test
    void schedule() {
        doReturn(true).when(subscription).subscribeByNameAndPosition(any(StreamReader.class));
        CompletableFuture<NameAndPosition> completableFuture = new CompletableFuture();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                completableFuture.complete(nameAndPosition);
            }
        };

        subscription.schedule(mock(StreamReader.class), completableFuture, runnable);
        await().until(() -> completableFuture.isDone());
        verify(subscription, times(1)).subscribeByNameAndPosition(any(StreamReader.class));
    }

}
