package com.dewdropper;

import com.dewdropper.structure.api.Message;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TestMessage implements Message {
    UUID id;
    String test;
}
