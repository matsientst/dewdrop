package com.dewdrop.fixture;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DewdropGetAccountByIdQuery {
    private UUID accountId;
}
