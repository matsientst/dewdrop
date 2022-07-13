package events.dewdrop.fixture.readmodel.accountdetails.summary;

import events.dewdrop.read.readmodel.annotation.DewdropCache;
import events.dewdrop.read.readmodel.annotation.ReadModel;
import events.dewdrop.read.readmodel.annotation.Stream;
import events.dewdrop.read.readmodel.query.QueryHandler;
import events.dewdrop.read.readmodel.stream.StreamType;
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
