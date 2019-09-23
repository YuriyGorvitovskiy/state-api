package sm.api.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.jaxrs2.integration.resources.AcceptHeaderOpenApiResource;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import sm.api.data.CheckEndPoint;
import sm.api.data.DataEndPoint;


import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationPath("/api")
@OpenAPIDefinition(
    info = @Info(
        title = "Stata Machine REST API"
    )
)
public class RESTApplication  extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> s = new HashSet<>();

        // Jackson JSON provider with C8 configuration
        s.add(JacksonJsonProvider.class);
        s.add(ObjectMapperContextResolver.class);

        // Endpoints
        s.add(DataEndPoint.class);
        s.add(CheckEndPoint.class);

        // OpenAPI 3.0
        s.add(OpenApiResource.class);

        return s;
    }

    public static ObjectMapper createObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.CLOSE_CLOSEABLE)
                .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
    }

}
