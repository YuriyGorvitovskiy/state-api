package sm.api.service;

import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.callbacks.Callback;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.links.Link;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;
import org.apache.commons.lang3.tuple.Pair;
import sm.api.data.DataEndPoint;
import sm.api.data.DataResource;


import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class OpenApiReader extends Reader {
    static class TypeInfo {
        public final String param;
        public final String single;
        public final String plural;

        public TypeInfo(String param, String single, String plural) {
            this.param = param;
            this.single = single;
            this.plural = plural;
        }
    }

    static final List<TypeInfo> types = Arrays.asList(
            new TypeInfo("users", "User", "Users"),
            new TypeInfo("roles", "Role", "Roles"),
            new TypeInfo("tasks", "Task", "Tasks"));

    static final String PARAM_PATTERN = "{" + DataEndPoint.TYPE_PARAM + "}";
    static final String SINGLE_PATTERN = "$Single$";
    static final String PLURAL_PATTERN = "$Plural$";

    public OpenAPI read(Set<Class<?>> classes, Map<String, Object> resources) {
        OpenAPI api = super.read(classes, resources);
        api.getInfo().version("SM 1.0.0");

        List<Map.Entry<String, PathItem>> templatePathItems = new ArrayList<>();
        Iterator<Map.Entry<String, PathItem>> itPathItems = api.getPaths().entrySet().iterator();
        while(itPathItems.hasNext()) {
            Map.Entry<String, PathItem> entry = itPathItems.next();
            if (entry.getKey().contains(PARAM_PATTERN)) {
                templatePathItems.add(entry);
                itPathItems.remove();
            }
        }
        for (Map.Entry<String, PathItem> template : templatePathItems) {
            for(TypeInfo type: types) {
                api.getPaths().put(copy(template.getKey(), type), copy(template.getValue(), type));
            }
        }
        Map<String, Schema> schemas = api.getComponents().getSchemas();
        Schema templateSchema = schemas.remove(SINGLE_PATTERN+"Resource");
        if (null != templateSchema) {
            for(TypeInfo type: types) {
                Schema schema = copy(templateSchema, type, new BiFunction<String, Schema, Schema>() {
                    public Schema apply(String k, Schema s) {
                        if (!"attrs".equals(k)) {
                            return copy(s, type, this);
                        }
                        return new Schema().type("object")
                                .addProperties("name", new Schema().type("string"))
                                .addProperties("time", new Schema().type("string").format("Time in UTC ISO8601"))
                                .addProperties("is_virtual", new Schema().type("boolean"))
                                .addProperties("count", new Schema().type("number"));
                    }
                });
                schemas.put(type.single + "Resource", schema);
            }
        }

        return api;
    }

    PathItem copy(PathItem item, TypeInfo type) {
        return null == item ? null : new PathItem()
                .summary(copy(item.getSummary(), type))
                .description(copy(item.getDescription(), type))
                .get(copy(item.getGet(), type))
                .put(copy(item.getPut(), type))
                .post(copy(item.getPost(), type))
                .delete(copy(item.getDelete(), type))
                .head(copy(item.getHead(), type))
                .patch(copy(item.getPatch(), type))
                .trace(copy(item.getTrace(), type))
                .servers(copyServers(item.getServers(), type))
                .parameters(copyParameters(item.getParameters(), type))
                .$ref(copy(item.get$ref(), type))
                .extensions(copyExtensions(item.getExtensions(), type));
    }

    Operation copy(Operation item, TypeInfo type) {
        if (item == null) {
            return null;
        }

        Operation copy =  new Operation()
                .tags(copyStrings(item.getTags(), type))
                .summary(copy(item.getSummary(), type))
                .description(copy(item.getDescription(), type))
                .externalDocs(copy(item.getExternalDocs(), type))
                .operationId(copy(item.getOperationId(), type))
                .requestBody(copy(item.getRequestBody(), type))
                .responses(copy(item.getResponses(), type))
                .callbacks(copyCallbacks(item.getCallbacks(), type))
                .deprecated(item.getDeprecated())
                .security(copySecurity(item.getSecurity(), type))
                .servers(copyServers(item.getServers(), type))
                .extensions(copyExtensions(item.getExtensions(), type));

        List<Parameter> parameters = copyParameters(item.getParameters(), type);
        parameters.removeIf((p) -> "path".equals(p.getIn()));
        copy.parameters(parameters);

        return copy;
    }

    ExternalDocumentation copy(ExternalDocumentation item, TypeInfo type) {
        return null == item ? null : new ExternalDocumentation()
                .description(copy(item.getDescription(), type))
                .url(copy(item.getUrl(), type))
                .extensions(copyExtensions(item.getExtensions(), type));
    }

    RequestBody copy(RequestBody item, TypeInfo type) {
        return null == item ? null : new RequestBody()
                .description(copy(item.getDescription(), type))
                .content(copy(item.getContent(), type))
                .required(item.getRequired())
                .extensions(copyExtensions(item.getExtensions(), type))
                .$ref(copy(item.get$ref(), type));
    }

    Server copy(Server item, TypeInfo type) {
        return null == item ? null : new Server()
                .url(copy(item.getUrl(), type))
                .description(copy(item.getDescription(), type))
                .variables(copy(item.getVariables(), type))
                .extensions(copyExtensions(item.getExtensions(), type));
    }

    Parameter copy(Parameter item, TypeInfo type) {
        return null == item ? null : new Parameter()
                .name(copy(item.getDescription(), type))
                .in(copy(item.getIn(), type))
                .description(copy(item.getDescription(), type))
                .required(item.getRequired())
                .deprecated(item.getDeprecated())
                .allowEmptyValue(item.getAllowEmptyValue())
                .$ref(copy(item.get$ref(), type));
    }

    ApiResponse copy(ApiResponse item, TypeInfo type) {
        if (item == null) {
            return null;
        }

        ApiResponse copy =  new ApiResponse()
                .description(copy(item.getDescription(), type))
                .headers(copyHeaders(item.getHeaders(), type))
                .content(copy(item.getContent(), type))
                .extensions(copyExtensions(item.getExtensions(), type))
                .$ref(copy(item.get$ref(), type));

        copy.setLinks(copyLinks(item.getLinks(), type));

        return copy;
    }

    MediaType copy(MediaType item, TypeInfo type) {
        return null == item ? null : new MediaType()
                .schema(copy(item.getSchema(), type, null))
                .examples(copyExamples(item.getExamples(), type))
                .example(item.getExample())
                .encoding(copyEncodings(item.getEncoding(), type))
                .extensions(copyExtensions(item.getExtensions(), type));
    }

    ServerVariable copy(ServerVariable item, TypeInfo type) {
        return null == item ? null : new ServerVariable()
                ._enum(copyStrings(item.getEnum(), type))
                ._default(copy(item.getDefault(), type))
                .description(copy(item.getDescription(), type))
                .extensions(copyExtensions(item.getExtensions(), type));
    }

    Link copy(Link item, TypeInfo type) {
        if (item == null) {
            return null;
        }

        Link link = new Link()
                .operationRef(copy(item.getOperationRef(), type))
                .operationId(copy(item.getOperationId(), type))
                .requestBody(item.getRequestBody())
                .headers(copyHeaders(item.getHeaders(), type))
                .description(copy(item.getDescription(), type))
                .$ref(copy(item.get$ref(), type))
                .extensions(copyExtensions(item.getExtensions(), type))
                .server(copy(item.getServer(), type));

        link.setParameters(copyStringsMap(item.getParameters(), type));

        return link;
    }

    Header copy(Header item, TypeInfo type) {
        return null == item ? null : new Header()
                .description(copy(item.getDescription(), type))
                .$ref(copy(item.get$ref(), type))
                .required(item.getRequired())
                .deprecated(item.getDeprecated());
    }

    Schema copy(Schema item, TypeInfo type, BiFunction<String, Schema, Schema> process) {
        if (item == null) {
            return null;
        }

        Schema schema = new Schema()
                .name(copy(item.getName(), type))
                .title(copy(item.getTitle(), type))
                .multipleOf(item.getMultipleOf())
                .maximum(item.getMaximum())
                .exclusiveMaximum(item.getExclusiveMaximum())
                .minimum(item.getMinimum())
                .exclusiveMinimum(item.getExclusiveMinimum())
                .maxLength(item.getMaxLength())
                .minLength(item.getMinLength())
                .pattern(copy(item.getPattern(), type))
                .maxItems(item.getMaxItems())
                .minItems(item.getMinItems())
                .uniqueItems(item.getUniqueItems())
                .maxProperties(item.getMaxProperties())
                .minProperties(item.getMinProperties())
                .required(copyStrings(item.getRequired(), type))
                .type(copy(item.getType(), type))
                .not(copy(item.getNot(), type, null))
                .properties(copySchemas(item.getProperties(), type, process))
                .additionalProperties(item.getAdditionalProperties())
                .description(copy(item.getDescription(), type))
                .format(copy(item.getFormat(), type))
                .$ref(copy(item.get$ref(), type))
                .nullable(item.getNullable())
                .readOnly(item.getReadOnly())
                .writeOnly(item.getWriteOnly())
                .example(item.getExample())
                .externalDocs(copy(item.getExternalDocs(), type))
                .deprecated(item.getDeprecated())
                .xml(item.getXml())
                .extensions(copyExtensions(item.getExtensions(), type))
                .discriminator(copy(item.getDiscriminator(), type));

        schema.setEnum(item.getEnum());
        return schema;
    }

    Example copy(Example item, TypeInfo type) {
        return null == item ? null : new Example()
                .summary(copy(item.getSummary(), type))
                .description(copy(item.getDescription(), type))
                .value(item.getValue())
                .externalValue(copy(item.getExternalValue(), type))
                .$ref(copy(item.get$ref(), type))
                .extensions(copyExtensions(item.getExtensions(), type));
    }

    Encoding copy(Encoding item, TypeInfo type) {
        return null == item ? null : new Encoding()
                .contentType(copy(item.getContentType(), type))
                .headers(copyHeaders(item.getHeaders(), type))
                .style(item.getStyle())
                .explode(item.getExplode())
                .allowReserved(item.getAllowReserved())
                .extensions(copyExtensions(item.getExtensions(), type));
    }

    Discriminator copy(Discriminator item, TypeInfo type) {
        return null == item ? null : new Discriminator()
                .propertyName(copy(item.getPropertyName(), type))
                .mapping(copyStringsMap(item.getMapping(), type));
    }

    String copy(String  item, TypeInfo type) {
        return null == item ? null : item
            .replace(PARAM_PATTERN, type.param)
            .replace(SINGLE_PATTERN, type.single)
            .replace(PLURAL_PATTERN, type.plural);
    }


    Callback copy(Callback item, TypeInfo type) {
        if (item == null) {
            return null;
        }
        Callback copy = new Callback()
                .extensions(copyExtensions(item.getExtensions(), type));

        for(Map.Entry<String, PathItem> entry: item.entrySet()) {
            copy.addPathItem(copy(entry.getKey(), type), copy(entry.getValue(), type));
        }

        return copy;
    }

    ApiResponses copy(ApiResponses item, TypeInfo type) {
        if (item == null) {
            return null;
        }
        ApiResponses copy = new ApiResponses()
                .extensions(copyExtensions(item.getExtensions(), type));

        for(Map.Entry<String, ApiResponse> entry: item.entrySet()) {
            copy.addApiResponse(copy(entry.getKey(), type), copy(entry.getValue(), type));
        }

        return copy;
    }

    ServerVariables copy(ServerVariables item, TypeInfo type) {
        if (item == null) {
            return null;
        }
        ServerVariables copy = new ServerVariables()
                .extensions(copyExtensions(item.getExtensions(), type));

        for(Map.Entry<String, ServerVariable> entry: item.entrySet()) {
            copy.addServerVariable(copy(entry.getKey(), type), copy(entry.getValue(), type));
        }

        return copy;
    }

    SecurityRequirement copy(SecurityRequirement item, TypeInfo type) {
        if (item == null) {
            return null;
        }
        SecurityRequirement copy = new SecurityRequirement();

        for(Map.Entry<String, List<String>> entry: item.entrySet()) {
            copy.addList(copy(entry.getKey(), type), copyStrings(entry.getValue(), type));
        }

        return copy;
    }

    Content copy(Content item, TypeInfo type) {
        if (item == null) {
            return null;
        }
        Content copy = new Content();

        for(Map.Entry<String, MediaType> entry: item.entrySet()) {
            copy.addMediaType(copy(entry.getKey(), type), copy(entry.getValue(), type));
        }

        return copy;
    }

    List<Server> copyServers(List<Server> item, TypeInfo type) {
        return null == item ? null : item.stream().map((i)->copy(i, type)).collect(Collectors.toList());
    }

    List<Parameter> copyParameters(List<Parameter> item, TypeInfo type) {
        return null == item ? null : item.stream().map((i)->copy(i, type)).collect(Collectors.toList());
    }

    List<SecurityRequirement> copySecurity(List<SecurityRequirement> item, TypeInfo type) {
        return null == item ? null : item.stream().map((i)->copy(i, type)).collect(Collectors.toList());
    }

    List<String> copyStrings(List<String>  item, TypeInfo type) {
        return null == item ? null : item.stream().map((i)->copy(i, type)).collect(Collectors.toList());
    }

    Map<String, Callback> copyCallbacks(Map<String, Callback> item, TypeInfo type) {
        return null == item ? null : item.entrySet().stream()
                .map((e) -> Pair.of(copy(e.getKey(), type), copy(e.getValue(), type)))
                .collect(Collectors.toMap(p -> p.getKey(), p-> p.getValue()));
    }

    Map<String, Link> copyLinks(Map<String, Link> item, TypeInfo type) {
        return null == item ? null : item.entrySet().stream()
                .map((e) -> Pair.of(copy(e.getKey(), type), copy(e.getValue(), type)))
                .collect(Collectors.toMap(p -> p.getKey(), p-> p.getValue()));
    }

    Map<String, Header> copyHeaders(Map<String, Header> item, TypeInfo type) {
        return null == item ? null : item.entrySet().stream()
                .map((e) -> Pair.of(copy(e.getKey(), type), copy(e.getValue(), type)))
                .collect(Collectors.toMap(p -> p.getKey(), p-> p.getValue()));
    }

    Map<String, Example> copyExamples(Map<String, Example> item, TypeInfo type) {
        return null == item ? null : item.entrySet().stream()
                .map((e) -> Pair.of(copy(e.getKey(), type), copy(e.getValue(), type)))
                .collect(Collectors.toMap(p -> p.getKey(), p-> p.getValue()));
    }

    Map<String, Encoding> copyEncodings(Map<String, Encoding> item, TypeInfo type) {
        return null == item ? null : item.entrySet().stream()
                .map((e) -> Pair.of(copy(e.getKey(), type), copy(e.getValue(), type)))
                .collect(Collectors.toMap(p -> p.getKey(), p-> p.getValue()));
    }

    Map<String, Schema> copySchemas(Map<String, Schema> item, TypeInfo type, BiFunction<String, Schema, Schema> process) {
        BiFunction<String, Schema, Schema> perform = null != process ? process : (k, v) -> copy(v, type, null);
        return null == item ? null : item.entrySet().stream()
                .map((e) -> Pair.of(copy(e.getKey(), type), process.apply(e.getKey(), e.getValue())))
                .collect(Collectors.toMap(p -> p.getKey(), p-> p.getValue()));
    }

    Map<String, Object> copyExtensions(Map<String, Object> item, TypeInfo type) {
        return null == item ? null : item.entrySet().stream()
                .map((e) -> Pair.of(copy(e.getKey(), type), e.getValue()))
                .collect(Collectors.toMap(p -> p.getKey(), p-> p.getValue()));
    }

    Map<String, String> copyStringsMap(Map<String, String>  item, TypeInfo type) {
        return null == item ? null : item.entrySet().stream()
                .map((e) -> Pair.of(copy(e.getKey(), type), copy(e.getValue(), type)))
                .collect(Collectors.toMap(p -> p.getKey(), p-> p.getValue()));
    }
}
