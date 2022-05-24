package com.dewdrop.read.readmodel.cache;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;

import com.dewdrop.fixture.DewdropAccountCreated;
import com.dewdrop.fixture.DewdropAccountDetails;
import com.dewdrop.fixture.DewdropFundsAddedToAccount;
import com.dewdrop.fixture.DewdropUserCreated;
import com.dewdrop.utils.DewdropReflectionUtils;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class InMemoryCacheProcessorTest {
    private InMemoryCacheProcessor<DewdropAccountDetails> inMemoryCacheProcessor;
    private Cache<UUID, DewdropAccountDetails> cache;
    UUID accountId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    DewdropAccountCreated accountCreated = new DewdropAccountCreated(accountId, "test", userId);

    @BeforeEach
    void setup() {
        CacheManager cacheManager = new CacheManager(ConcurrentHashMapCache.class);
        inMemoryCacheProcessor = spy(new InMemoryCacheProcessor<>(DewdropAccountDetails.class, cacheManager));
        cache = new ConcurrentHashMapCache<>();
        cache.put(accountId, new DewdropAccountDetails());
    }

    @Test
    @DisplayName("getAll() - confirm we get back the cache we expect")
    void getAll() {
        inMemoryCacheProcessor.setCache(cache);

        assertThat(inMemoryCacheProcessor.getAll(), is(cache.getAll()));
    }

    @Test
    @DisplayName("primaryCache() - There's a primary cache and a cacheIndex - confirm that we get added to the cache")
    void primaryCache_isCacheRoot_and_accountId() {
        UUID id = accountCreated.getAccountId();
        inMemoryCacheProcessor.primaryCache(accountCreated, id);
        assertThat(inMemoryCacheProcessor.getAll().get(id).getName(), is("test"));
    }

    @Test
    @DisplayName("primaryCache() - There's a primary cache and a cacheIndex - confirm that if we already have it in the cache we then call the cache put")
    void primaryCache_isCacheRoot_and_accountId_alreadyInCache() {
        inMemoryCacheProcessor.primaryCache(accountCreated, accountId);
        BigDecimal funds = new BigDecimal(100);
        DewdropFundsAddedToAccount accountUpdated = new DewdropFundsAddedToAccount(accountId, funds);
        inMemoryCacheProcessor.primaryCache(accountUpdated, accountId);
        DewdropAccountDetails result = inMemoryCacheProcessor.getAll().get(accountId);
        assertThat("test", is(result.getName()));
        assertThat(funds, is(result.getBalance()));
    }

    @Test
    @DisplayName("primaryCache() - There's a primary cache and a cacheIndex - confirm that we do nothing when we can't construct the target object")
    void primaryCache_unableToConstruct() {
        UUID id = accountCreated.getAccountId();

        try (MockedStatic<DewdropReflectionUtils> utilities = mockStatic(DewdropReflectionUtils.class)) {
            utilities.when(() -> DewdropReflectionUtils.createInstance(any(Class.class))).thenReturn(Optional.empty());

            inMemoryCacheProcessor.primaryCache(accountCreated, id);
            assertThat(inMemoryCacheProcessor.getAll().containsKey(id), is(false));
        }
    }

    @Test
    @DisplayName("foundInCache() - Test that we already have the key in the cacheIndex")
    void foundInCacheIndex_containsKey() {
        Map<String, Map<UUID, UUID>> cacheIndex = new ConcurrentHashMap<>();
        Map<UUID, UUID> index = new ConcurrentHashMap<>();
        index.put(userId, accountId);
        cacheIndex.put("userId", index);
        inMemoryCacheProcessor.setCacheIndex(cacheIndex);
        DewdropAccountDetails target = new DewdropAccountDetails();
        inMemoryCacheProcessor.getAll().put(accountId, target);
        String username = "tester guy";
        DewdropUserCreated userCreated = new DewdropUserCreated(userId, username);
        inMemoryCacheProcessor.processAlternateKeyMessage(userCreated, "userId", userId);

        assertThat(target.getUsername(), is(username));
    }

    @Test
    @DisplayName("updateCacheIndex() - Test that we update the cacheIndex correctly")
    void updateCacheIndex() {
//        DewdropUserCreated userCreated = new DewdropUserCreated(userId, "tester guy");
//        Map<UUID, UUID> index = new HashMap<>();
//        DewdropAccountDetails details = new DewdropAccountDetails();
//        String id = "userId";
//        inMemoryCacheProcessor.updateCacheIndex(userCreated, id, userId, index, details, accountId);
//
//        assertThat(accountId, is(index.get(userId)));
//        assertThat(inMemoryCacheProcessor.getCacheIndex().get(id), is(index));
    }

    @Test
    @DisplayName("notFoundInCacheIndex() - Test that we update the cacheIndex correctly")
    void notFoundInCacheIndex() {
//        DewdropUserCreated userCreated = new DewdropUserCreated(userId, "tester guy");
//        Map<UUID, UUID> index = new HashMap<>();
//        DewdropAccountDetails details = new DewdropAccountDetails();
//        String id = "userId";
//        inMemoryCacheProcessor.updateCacheIndex(userCreated, id, userId, index, details, accountId);
//
//        assertThat(accountId, is(index.get(userId)));
//        assertThat(inMemoryCacheProcessor.getCacheIndex().get(id), is(index));
    }
}
