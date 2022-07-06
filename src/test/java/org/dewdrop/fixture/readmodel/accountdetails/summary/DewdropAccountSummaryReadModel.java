package org.dewdrop.fixture.readmodel.accountdetails.summary;

import org.dewdrop.read.readmodel.stream.StreamType;
import org.dewdrop.read.readmodel.annotation.DewdropCache;
import org.dewdrop.read.readmodel.annotation.ReadModel;
import org.dewdrop.read.readmodel.annotation.Stream;
import org.dewdrop.read.readmodel.query.QueryHandler;
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
