package sm.api.data;

import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Schema(
        name = "$Single$Resource",
        title = "$Single$ Resource"
)
public class DataResource {

    public String id;

    public String type;

    public Map<String, Object> attrs = new HashMap<>();
}
