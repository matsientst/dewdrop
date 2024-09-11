package events.dewdrop.utils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import events.dewdrop.fixture.events.DewdropAccountCreated;
import events.dewdrop.fixture.events.DewdropAccountEvent;
import events.dewdrop.fixture.events.DewdropFundsAddedToAccount;
import events.dewdrop.fixture.events.DewdropUserCreated;
import events.dewdrop.fixture.readmodel.accountdetails.details.DewdropAccountDetails;
import events.dewdrop.read.readmodel.annotation.PrimaryCacheKey;
import events.dewdrop.structure.api.Event;
import events.dewdrop.structure.api.Message;
import lombok.Data;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CacheUtilsTest {
    private CacheUtilsTest() {}

    @Test
    @DisplayName("isCacheRoot() - Given an event that is annotated with @CacheRoot, we should return true")
    void isCacheRoot() {
        DewdropUserCreated createUser = new DewdropUserCreated();
        assertThat(CacheUtils.isCacheRoot(createUser), is(true));
    }

    @Test
    @DisplayName("isCacheRoot() - Given an event that is NOT annotated with @CacheRoot, we should return false")
    void isCacheRoot_notCacheRoot() {
        DewdropFundsAddedToAccount addFunds = new DewdropFundsAddedToAccount();
        assertThat(CacheUtils.isCacheRoot(addFunds), is(false));
    }

    @Test
    @DisplayName("getForeignCacheKeys() - Given a cacheObject with the foreign key annotated with @ForeignCacheKey, return the field that is annotated")
    void getForeignCacheKeys() {
        List<String> foreignCacheKeys = CacheUtils.getForeignCacheKeys(DewdropAccountDetails.class);
        assertThat(foreignCacheKeys.isEmpty(), is(false));
    }

    @Test
    @DisplayName("getPrimaryCacheKeys() - Given a cacheObject with the primary key annotated with @PrimaryCacheKey, return the name of the field that is annotated")
    void getPrimaryCacheKeys() {
        List<String> primaryCacheKey = CacheUtils.getPrimaryCacheKeys(DewdropAccountDetails.class);
        assertThat(primaryCacheKey.size(), is(1));
        assertThat(primaryCacheKey.get(0), is("accountId"));
    }

    @Test
    @DisplayName("getPrimaryCacheKeys() - Given a cacheObject with the primary key annotated with @PrimaryCacheKey and alternateCacheKeys, return the name of the fields that are annotated")
    void getPrimaryCacheKeys_alternateCacheKeys() {
        List<String> primaryCacheKey = CacheUtils.getPrimaryCacheKeys(AlternatePrimaryCacheKeys.class);
        assertThat(primaryCacheKey.size(), is(2));
        assertThat(primaryCacheKey.get(0), is("accountId"));
        assertThat(primaryCacheKey.get(1), is("oldAccountId"));
    }

    @Test
    @DisplayName("getPrimaryCacheKeys() - Given a cacheObject with multiple fields annotated with @PrimaryCacheKey, return an IllegalArgumentException")
    void getPrimaryCacheKey_multiplePrimaryCacheKeys() {
        assertThrows(IllegalArgumentException.class, () -> CacheUtils.getPrimaryCacheKeys(MultiplePrimaryCacheKeys.class));
    }

    @Test
    @DisplayName("getPrimaryCacheKeys() - Given a cacheObject with no fields annotated with @PrimaryCacheKey, return an IllegalArgumentException")
    void getPrimaryCacheKey_noPrimaryCacheKeys() {
        assertThrows(IllegalArgumentException.class, () -> CacheUtils.getPrimaryCacheKeys(NoPrimaryCacheKeys.class));
    }

    @Test
    @DisplayName("getCacheRootKey() - Given a message with @AggregateId, return the value of the aggregateId (cache Key)")
    void getCacheRootKey() {
        DewdropUserCreated userCreated = new DewdropUserCreated(UUID.randomUUID(), "Test");
        Optional<UUID> cacheRootKey = CacheUtils.getCacheRootKey(userCreated);
        assertThat(cacheRootKey.get(), is(userCreated.getUserId()));
    }

    @Test
    @DisplayName("getCacheRootKey() - Given a message without @AggregateId, return Optional.empty()")
    void getCacheRootKey_noAggregateId() {
        Optional<UUID> cacheRootKey = CacheUtils.getCacheRootKey(new NoPrimaryCacheKeys());
        assertThat(cacheRootKey.isEmpty(), is(true));
    }

    @Test
    @DisplayName("getClassWithAggregateId - Given a message with @AggregateId, return the class with the @AggregateId annotation")
    void getClassWithAggregateId() {
        Class<? extends Event> aggregateIdClass = CacheUtils.getClassWithAggregateId(DewdropAccountCreated.class);
        assertThat(aggregateIdClass, is(DewdropAccountEvent.class));

    }

    @Data
    private class MultiplePrimaryCacheKeys {
        @PrimaryCacheKey(creationEvent = MultiplePrimaryCacheKeys.class)
        UUID accountId;
        @PrimaryCacheKey(creationEvent = MultiplePrimaryCacheKeys.class)
        UUID userId;
    }

    @Data
    private class AlternatePrimaryCacheKeys {
        @PrimaryCacheKey(creationEvent = AlternatePrimaryCacheKeys.class, alternateCacheKeys = "oldAccountId")
        UUID accountId;
        UUID userId;
    }

    @Data
    private class NoPrimaryCacheKeys implements Message {
        UUID accountId;
        UUID userId;
        UUID messageId;
    }

}
