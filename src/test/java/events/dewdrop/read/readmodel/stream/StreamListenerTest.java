package events.dewdrop.read.readmodel.stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import events.dewdrop.fixture.events.DewdropUserCreated;
import events.dewdrop.read.readmodel.stream.subscription.Subscription;
import events.dewdrop.streamstore.eventstore.EventStore;
import events.dewdrop.streamstore.serialize.JsonSerializer;
import events.dewdrop.structure.datastore.StreamStore;
import events.dewdrop.structure.events.ReadEventData;
import events.dewdrop.structure.serialize.EventSerializer;
import events.dewdrop.structure.subscribe.SubscribeRequest;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StreamListenerTest {
    StreamStore streamStore;
    EventSerializer eventSerializer;
    StreamListener<DewdropUserCreated> streamListener;
    Subscription<DewdropUserCreated> subscription;
    ReadEventData readEventData;

    @BeforeEach
    void setup() {
        subscription = mock(Subscription.class);
        readEventData = mock(ReadEventData.class);
        streamStore = mock(EventStore.class);
        eventSerializer = mock(JsonSerializer.class);
        streamListener = spy(StreamListener.getInstance(streamStore, eventSerializer));
    }

    @Test
    void start() {
        doReturn(true).when(streamListener).subscribe(anyLong(), any(Consumer.class));

        assertThat(streamListener.start("streamName", 0L, subscription), is(true));
        assertThat(streamListener.getStreamName(), is("streamName"));
    }

    @Test
    @DisplayName("onEvent() - Given a valid ReadEventData, our consumer should publish the event to the subscription and update the stream position")
    void onEvent() {
        DewdropUserCreated event = new DewdropUserCreated();
        when(eventSerializer.deserialize(any(ReadEventData.class))).thenReturn(Optional.of(event));
        doNothing().when(subscription).publish(any(DewdropUserCreated.class));
        Consumer<ReadEventData> readEventDataConsumer = streamListener.onEvent(subscription);
        doReturn(50L).when(readEventData).getEventNumber();
        readEventDataConsumer.accept(readEventData);

        assertThat(streamListener.getStreamPosition().get(), is(50L));
        verify(subscription, times(1)).publish(event);
    }

    @Test
    @DisplayName("onEvent() - Given a ReadEventData that we cannot deserialize, log and do nothing")
    void onEvent_unableToDeserialize() {
        DewdropUserCreated event = new DewdropUserCreated();
        doReturn(Optional.empty()).when(eventSerializer).deserialize(any(ReadEventData.class));

        Consumer<ReadEventData> readEventDataConsumer = streamListener.onEvent(subscription);

        readEventDataConsumer.accept(readEventData);

        assertThat(streamListener.getStreamPosition().get(), is(0L));
        verify(subscription, times(0)).publish(event);
    }

    @Test
    @DisplayName("subscribe() - Given a valid checkpoint and a consumer, subscribe to the stream and return true")
    void subscribe() {
        doReturn(true).when(streamStore).subscribeToStream(any(SubscribeRequest.class));
        Consumer<ReadEventData> consumer = mock(Consumer.class);
        assertThat(streamListener.subscribe(0L, consumer), is(true));
    }
}
