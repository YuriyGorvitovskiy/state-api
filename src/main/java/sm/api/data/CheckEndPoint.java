package sm.api.data;


import io.swagger.v3.oas.annotations.Operation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.Instant;

@Path("check")
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON})
public class CheckEndPoint {

    @GET
    @Operation(
            tags={"Check"},
            summary = "Check Access"
    )
    public boolean getData() {
        return true;
    }

}
