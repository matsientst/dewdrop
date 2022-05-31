package com.dewdrop.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dewdrop.fixture.events.DewdropFundsAddedToAccount;
import com.dewdrop.fixture.events.DewdropUserCreated;
import com.dewdrop.fixture.readmodel.DewdropAccountDetails;
import com.dewdrop.read.readmodel.annotation.PrimaryCacheKey;
import com.dewdrop.structure.api.Message;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
        List<Field> foreignCacheKeys = CacheUtils.getForeignCacheKeys(DewdropAccountDetails.class);
        assertThat(foreignCacheKeys.isEmpty(), is(false));
    }

    @Test
    @DisplayName("getPrimaryCacheKey() - Given a cacheObject with the primary key annotated with @PrimaryCacheKey, return the field that is annotated")
    void getPrimaryCacheKey() {
        Field primaryCacheKey = CacheUtils.getPrimaryCacheKey(DewdropAccountDetails.class);
        assertThat(primaryCacheKey, is(notNullValue()));
    }

    @Test
    @DisplayName("getPrimaryCacheKey() - Given a cacheObject with multiple fields annotated with @PrimaryCacheKey, return an IllegalArgumentException")
    void getPrimaryCacheKey_multiplePrimaryCacheKeys() {
        assertThrows(IllegalArgumentException.class, () -> CacheUtils.getPrimaryCacheKey(MultiplePrimaryCacheKeys.class));
    }

    @Test
    @DisplayName("getPrimaryCacheKey() - Given a cacheObject with no fields annotated with @PrimaryCacheKey, return an IllegalArgumentException")
    void getPrimaryCacheKey_noPrimaryCacheKeys() {
        assertThrows(IllegalArgumentException.class, () -> CacheUtils.getPrimaryCacheKey(NoPrimaryCacheKeys.class));
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

    @Data
    private class MultiplePrimaryCacheKeys {
        @PrimaryCacheKey
        UUID accountId;
        @PrimaryCacheKey
        UUID userId;
    }

    @Data
    private class NoPrimaryCacheKeys implements Message {
        UUID accountId;
        UUID userId;
        UUID messageId;
    }
}
