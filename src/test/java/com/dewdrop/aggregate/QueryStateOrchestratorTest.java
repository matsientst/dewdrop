package com.dewdrop.aggregate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.dewdrop.api.result.Result;
import com.dewdrop.fixture.readmodel.accountdetails.details.DewdropAccountDetails;
import com.dewdrop.read.readmodel.DefaultAnnotationReadModelMapper;
import com.dewdrop.read.readmodel.QueryStateOrchestrator;
import com.dewdrop.read.readmodel.ReadModel;
import com.dewdrop.read.readmodel.ReadModelMapper;
import com.dewdrop.utils.QueryHandlerUtils;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class QueryStateOrchestratorTest {
    QueryStateOrchestrator queryStateOrchestrator;
    ReadModelMapper readModelMapper;

    @BeforeEach
    void setup() {
        readModelMapper = mock(DefaultAnnotationReadModelMapper.class);
        queryStateOrchestrator = new QueryStateOrchestrator(readModelMapper);

    }

    @Test
    @DisplayName("executeQuery() - Given a query, when we find the read model, then we execute the query by calling QueryHandlerUtils.callQueryHandler()")
    void executeQuery() {
        ReadModel readModel = mock(ReadModel.class);
        doNothing().when(readModel).updateState();
        doReturn(Optional.of(readModel)).when(readModelMapper).getReadModelByQuery(any());

        try (MockedStatic<QueryHandlerUtils> utilities = mockStatic(QueryHandlerUtils.class)) {
            utilities.when(() -> QueryHandlerUtils.callQueryHandler(any(ReadModel.class), any())).thenReturn(Result.of(new DewdropAccountDetails()));
            queryStateOrchestrator.executeQuery(new Object());
            verify(readModel, times(1)).updateState();
        }
    }

    @Test
    @DisplayName("executeQuery() - Given a query, when there is no read model, then return a Result with an exception")
    void executeQuer_noReadModel() {
        ReadModel readModel = mock(ReadModel.class);
        doNothing().when(readModel).updateState();
        doReturn(Optional.empty()).when(readModelMapper).getReadModelByQuery(any());

        Result<Object> result = queryStateOrchestrator.executeQuery(new Object());
        assertThat(result.isExceptionPresent(), is(true));
        assertThat(result.getException().getMessage(), Matchers.containsString("no read model found for query"));
        verify(readModel, times(0)).updateState();
    }


}
