package com.dewdrop.read.readmodel.cache;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.dewdrop.config.DewdropSettings;
import com.dewdrop.fixture.events.DewdropAccountCreated;
import com.dewdrop.fixture.events.DewdropFundsAddedToAccount;
import com.dewdrop.fixture.events.DewdropUserCreated;
import com.dewdrop.fixture.readmodel.accountdetails.details.DewdropAccountDetails;
import com.dewdrop.structure.api.Message;
import com.dewdrop.utils.DewdropReflectionUtils;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class MapBackedInMemoryCacheProcessorTest {
    private MapBackedInMemoryCacheProcessor<DewdropAccountDetails> mapBackedInMemoryCacheProcessor;
    private Cache<UUID, DewdropAccountDetails, Map<UUID, DewdropAccountDetails>> cache;
    UUID accountId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    DewdropAccountCreated accountCreated = new DewdropAccountCreated(accountId, "test", userId);
    DewdropUserCreated userCreated = new DewdropUserCreated(userId, "tester guy");

    @BeforeEach
    void setup() {
        CacheManager cacheManager = new CacheManager();
        mapBackedInMemoryCacheProcessor = spy(new MapBackedInMemoryCacheProcessor<>(DewdropAccountDetails.class, cacheManager));
        cache = new ConcurrentHashMapCache<>();
        cache.put(accountId, new DewdropAccountDetails());
    }

    @Test
    @DisplayName("constructor() - confirm that we construct properly")
    void constructor() {
        assertThat(mapBackedInMemoryCacheProcessor.getCachedStateObjectType(), is(DewdropAccountDetails.class));
        assertThat(mapBackedInMemoryCacheProcessor.getPrimaryCacheKeyName(), is("accountId"));
        assertThat(mapBackedInMemoryCacheProcessor.getForeignCacheKeyNames(), is(List.of("userId")));
        assertThat(mapBackedInMemoryCacheProcessor.getCache().getAll().size(), is(0));
        assertThat(mapBackedInMemoryCacheProcessor.getCacheIndex().size(), is(1));
        assertThat(mapBackedInMemoryCacheProcessor.getCacheIndex().get("userId"), is(notNullValue()));
    }

    @Test
    @DisplayName("process() - confirm that we call primaryCache()")
    void process() {
        doNothing().when(mapBackedInMemoryCacheProcessor).primaryCache(any(Message.class), any(UUID.class));
        mapBackedInMemoryCacheProcessor.process(accountCreated);
        verify(mapBackedInMemoryCacheProcessor, times(1)).primaryCache(any(Message.class), any(UUID.class));
        verify(mapBackedInMemoryCacheProcessor, times(0)).foreignCache(any(Message.class), any(UUID.class));
    }

    @Test
    @DisplayName("process() - confirm that we call foreignCache()")
    void process_foreignCache() {
        doNothing().when(mapBackedInMemoryCacheProcessor).foreignCache(any(Message.class), any(UUID.class));
        mapBackedInMemoryCacheProcessor.process(userCreated);
        verify(mapBackedInMemoryCacheProcessor, times(0)).primaryCache(any(Message.class), any(UUID.class));
        verify(mapBackedInMemoryCacheProcessor, times(1)).foreignCache(any(Message.class), any(UUID.class));
    }

    @Test
    @DisplayName("getAll() - confirm we get back the cache we expect")
    void getAll() {
        mapBackedInMemoryCacheProcessor.setCache(cache);

        assertThat(mapBackedInMemoryCacheProcessor.getAll(), is(cache.getAll()));
    }

    @Test
    @DisplayName("primaryCache() - There's a primary cache and a cacheIndex - confirm that we get added to the cache")
    void primaryCache_isCacheRoot_and_accountId() {
        UUID id = accountCreated.getAccountId();
        mapBackedInMemoryCacheProcessor.primaryCache(accountCreated, id);
        assertThat(mapBackedInMemoryCacheProcessor.getAll().get(id).getName(), is("test"));
    }

    @Test
    @DisplayName("primaryCache() - There's a primary cache and a cacheIndex - confirm that if we already have it in the cache we then call the cache put")
    void primaryCache_isCacheRoot_and_accountId_alreadyInCache() {
        mapBackedInMemoryCacheProcessor.primaryCache(accountCreated, accountId);
        BigDecimal funds = new BigDecimal(100);
        DewdropFundsAddedToAccount accountUpdated = new DewdropFundsAddedToAccount(accountId, funds);
        mapBackedInMemoryCacheProcessor.primaryCache(accountUpdated, accountId);
        DewdropAccountDetails result = mapBackedInMemoryCacheProcessor.getAll().get(accountId);
        assertThat("test", is(result.getName()));
        assertThat(funds, is(result.getBalance()));
    }

    @Test
    @DisplayName("primaryCache() - Not in cache and no cacheRoot")
    void primaryCache_notACacheRoot() {
        doNothing().when(mapBackedInMemoryCacheProcessor).processCacheIndex(any(Message.class), any(UUID.class));
        mapBackedInMemoryCacheProcessor.primaryCache(new Message() {
            @Override
            public UUID getMessageId() {
                return null;
            }
        }, UUID.randomUUID());
        verify(mapBackedInMemoryCacheProcessor, times(0)).updatePrimaryCache(any(), any(Message.class), any(UUID.class));
        verify(mapBackedInMemoryCacheProcessor, times(0)).updatePrimaryCache(any(), any(Message.class), any(UUID.class));
        verify(mapBackedInMemoryCacheProcessor, times(1)).processCacheIndex(any(Message.class), any(UUID.class));
    }

    @Test
    @DisplayName("primaryCache() - There's a primary cache and a cacheIndex - confirm that we do nothing when we can't construct the target object")
    void primaryCache_unableToConstruct() {
        UUID id = accountCreated.getAccountId();

        try (MockedStatic<DewdropReflectionUtils> utilities = mockStatic(DewdropReflectionUtils.class)) {
            utilities.when(() -> DewdropReflectionUtils.createInstance(any(Class.class))).thenReturn(Optional.empty());

            mapBackedInMemoryCacheProcessor.primaryCache(accountCreated, id);
            assertThat(mapBackedInMemoryCacheProcessor.getAll().containsKey(id), is(false));
        }
    }

    @Test
    @DisplayName("foundInCache() - Test that we already have the key in the cacheIndex")
    void foundInCacheIndex_containsKey() {
        Map<String, Map<UUID, UUID>> cacheIndex = new ConcurrentHashMap<>();
        Map<UUID, UUID> index = new ConcurrentHashMap<>();
        index.put(userId, accountId);
        cacheIndex.put("userId", index);
        mapBackedInMemoryCacheProcessor.setCacheIndex(cacheIndex);
        DewdropAccountDetails target = new DewdropAccountDetails();
        mapBackedInMemoryCacheProcessor.getAll().put(accountId, target);

        mapBackedInMemoryCacheProcessor.processForeignKeyMessage(userCreated, "userId", userId);

        assertThat(target.getUsername(), is(userCreated.getUsername()));
    }

    @Test
    void foreignCache() {
        doNothing().when(mapBackedInMemoryCacheProcessor).processForeignCache(any(Message.class), any(UUID.class), anyString());
        mapBackedInMemoryCacheProcessor.foreignCache(userCreated, userId);
        verify(mapBackedInMemoryCacheProcessor, times(1)).processForeignCache(any(Message.class), any(UUID.class), anyString());
    }

    @Test
    @DisplayName("processForeignCache() - Test that we call processForeignKeyMessage()")
    void processForeignCache() {
        doNothing().when(mapBackedInMemoryCacheProcessor).processForeignKeyMessage(any(Message.class), any(String.class), any(UUID.class));
        mapBackedInMemoryCacheProcessor.process(accountCreated);
        mapBackedInMemoryCacheProcessor.processForeignCache(userCreated, userId, "userId");

        verify(mapBackedInMemoryCacheProcessor, times(0)).notFoundInCacheIndex(any(Message.class), any(UUID.class));
        verify(mapBackedInMemoryCacheProcessor, times(1)).processForeignKeyMessage(any(Message.class), any(String.class), any(UUID.class));
    }

    @Test
    @DisplayName("processForeignCache() - Test that do nothing when we can't find the key")
    void processForeignCache_noForeignKeyInMessage() {
        mapBackedInMemoryCacheProcessor.processForeignCache(new DewdropFundsAddedToAccount(), userId, "userId");

        verify(mapBackedInMemoryCacheProcessor, times(0)).notFoundInCacheIndex(any(Message.class), any(UUID.class));
        verify(mapBackedInMemoryCacheProcessor, times(0)).processForeignKeyMessage(any(Message.class), any(String.class), any(UUID.class));
    }

    @Test
    @DisplayName("processForeignCache() - Test that we call notFoundInCacheIndex()")
    void processForeignCache_notFoundInCacheIndex() {
        doNothing().when(mapBackedInMemoryCacheProcessor).notFoundInCacheIndex(any(Message.class), any(UUID.class));
        mapBackedInMemoryCacheProcessor.processForeignCache(userCreated, userId, "userId");

        verify(mapBackedInMemoryCacheProcessor, times(1)).notFoundInCacheIndex(any(Message.class), any(UUID.class));
        verify(mapBackedInMemoryCacheProcessor, times(0)).processForeignKeyMessage(any(Message.class), any(String.class), any(UUID.class));
    }

    @Test
    @DisplayName("notFoundInCacheIndex() - Test that we add to unprocessed correctly")
    void notFoundInCacheIndex() {
        mapBackedInMemoryCacheProcessor.notFoundInCacheIndex(userCreated, userId);
        assertThat(mapBackedInMemoryCacheProcessor.getUnprocessedMessages().containsKey(userId), is(true));
    }

    @Test
    @DisplayName("isInCacheIndex() - Test that we can retrieve data from cacheIndex")
    void isInCacheIndex() {
        assertThat(mapBackedInMemoryCacheProcessor.isInCacheIndex("userId", UUID.randomUUID()), is(false));
        mapBackedInMemoryCacheProcessor.process(accountCreated);
        assertThat(mapBackedInMemoryCacheProcessor.isInCacheIndex("userId", userId), is(true));
    }

    @Test
    @DisplayName("processCacheIndex() - Test that we added userId to the cacheIndex")
    void processCacheIndex() {
        mapBackedInMemoryCacheProcessor.processCacheIndex(accountCreated, accountId);

        assertThat(true, is(mapBackedInMemoryCacheProcessor.getCacheIndex().get("userId").containsKey(accountCreated.getUserId())));
    }

    @Test
    @DisplayName("processCacheIndex() - Test that we processed any unprocessedMessages")
    void processCacheIndex_unprocessedMessages() {
        mapBackedInMemoryCacheProcessor.process(userCreated);
        doNothing().when(mapBackedInMemoryCacheProcessor).processForeignKeyMessage(any(Message.class), anyString(), any(UUID.class));
        mapBackedInMemoryCacheProcessor.processCacheIndex(accountCreated, accountId);

        assertThat(true, is(mapBackedInMemoryCacheProcessor.getCacheIndex().get("userId").containsKey(accountCreated.getUserId())));

        verify(mapBackedInMemoryCacheProcessor, times(1)).processForeignKeyMessage(any(Message.class), anyString(), any(UUID.class));
    }

    @Test
    @DisplayName("initializePrimaryCache() - Test that we call updatePrimaryCache()")
    void initializePrimaryCache() {
        doNothing().when(mapBackedInMemoryCacheProcessor).updatePrimaryCache(any(), any(Message.class), any(UUID.class));
        mapBackedInMemoryCacheProcessor.initializePrimaryCache(accountCreated, accountId);

        verify(mapBackedInMemoryCacheProcessor, times(1)).updatePrimaryCache(any(), any(Message.class), any(UUID.class));
    }

    @Test
    @DisplayName("initializePrimaryCache() - Test that we don't call updatePrimaryCache() when we are unable to construct a cachedStateObjectType")
    void initializePrimaryCache_cantCreate_cachedStateObjectType() {
        // Add a class that has no default constructor
        mapBackedInMemoryCacheProcessor.setCachedStateObjectType(DewdropSettings.class);
        mapBackedInMemoryCacheProcessor.initializePrimaryCache(accountCreated, accountId);

        verify(mapBackedInMemoryCacheProcessor, times(0)).updatePrimaryCache(any(), any(Message.class), any(UUID.class));
    }

    @Test
    @DisplayName("updatePrimaryCache() - Test that are able to process and add to cache")
    public void updatePrimaryCache() {
        DewdropAccountDetails dto = new DewdropAccountDetails();
        mapBackedInMemoryCacheProcessor.updatePrimaryCache(dto, accountCreated, accountId);
        assertThat(true, is(mapBackedInMemoryCacheProcessor.getCache().containsKey(accountId)));
        assertThat(accountId, is(dto.getAccountId()));
    }

    @Test
    @DisplayName("processForeignKeyMessage() - Test that are able to process and add to cache")
    void processForeignKeyMessage() {
        mapBackedInMemoryCacheProcessor.process(accountCreated);
        mapBackedInMemoryCacheProcessor.processForeignKeyMessage(userCreated, "userId", userId);
        assertThat(mapBackedInMemoryCacheProcessor.getCache().get(accountId).getUsername(), is(userCreated.getUsername()));
    }

    @Test
    @DisplayName("put() - Test that are able to add to cache")
    void put() {
        mapBackedInMemoryCacheProcessor.put(accountId, new DewdropAccountDetails());
        assertThat(mapBackedInMemoryCacheProcessor.getCache().containsKey(accountId), is(true));
    }
}
