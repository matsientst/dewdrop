package com.dewdrop.fixture.readmodel.accountdetails.summary;

import com.dewdrop.read.StreamType;
import com.dewdrop.read.readmodel.annotation.DewdropCache;
import com.dewdrop.read.readmodel.annotation.ReadModel;
import com.dewdrop.read.readmodel.annotation.Stream;
import com.dewdrop.read.readmodel.query.QueryHandler;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ReadModel
@Stream(name = "DewdropFundsAddedToAccount", streamType = StreamType.EVENT)
@Stream(name = "DewdropAccountCreated", streamType = StreamType.EVENT)
public class DewdropAccountSummaryReadModel {
    @DewdropCache
    DewdropAccountSummary dewdropAccountSummary;

    @QueryHandler
    public DewdropAccountSummary handle(DewdropAccountSummaryQuery query) {
        return dewdropAccountSummary;
    }

}
