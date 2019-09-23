package sm.api.data;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import org.jboss.resteasy.annotations.Body;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.Instant;

@Path("data")
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON})
public class DataEndPoint {

    public static final String TYPE_PARAM = "Type";
    public static final String TYPE_ACCESS = "$Single$";

    @GET
    @Path("/{" + TYPE_PARAM + "}")
    @Operation(
            tags={TYPE_ACCESS},
            summary = "Retrieve $Plural$"
    )
    public DataResource getData(@PathParam(TYPE_PARAM) String type) {
        DataResource data = new  DataResource();
        data.id = "1";
        data.type = type;
        data.attrs.put("name","Hello");
        data.attrs.put("time", Instant.now());
        data.attrs.put("is_virtual", true);
        data.attrs.put("count", 1);
        return data;
    }

    @POST
    @Path("/{" + TYPE_PARAM + "}")
    @Operation(
            tags={TYPE_ACCESS},
            summary = "Create $Single$"
    )
    public DataResource getData(@PathParam(TYPE_PARAM) String type,
                                DataResource resource) {
        DataResource data = new  DataResource();
        data.id = "1";
        data.type = type;
        data.attrs = resource.attrs;
        return data;
    }

}
