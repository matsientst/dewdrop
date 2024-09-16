package events.dewdrop.fixture.readmodel.accountdetails.summary;

import events.dewdrop.read.readmodel.annotation.DewdropCache;
import events.dewdrop.read.readmodel.annotation.EventStream;
import events.dewdrop.read.readmodel.annotation.ReadModel;
import events.dewdrop.read.readmodel.query.QueryHandler;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ReadModel
@EventStream(name = "DewdropFundsAddedToAccount")
@EventStream(name = "DewdropAccountCreated")
public class DewdropAccountSummaryReadModel {
    @DewdropCache
    DewdropAccountSummary dewdropAccountSummary;

    @QueryHandler
    public DewdropAccountSummary handle(DewdropAccountSummaryQuery query) {

        return dewdropAccountSummary;
    }
}
