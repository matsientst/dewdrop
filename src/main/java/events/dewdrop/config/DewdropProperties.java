package events.dewdrop.config;

import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Singular;
import org.apache.commons.lang3.StringUtils;

@Getter
@Builder(buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DewdropProperties {
    @NonNull
    private String connectionString;
    private String streamPrefix;
    private String packageToScan;
    @Singular("packageToExclude")
    private List<String> packageToExclude;
}
