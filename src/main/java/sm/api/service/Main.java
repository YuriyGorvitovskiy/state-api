package sm.api.service;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiContextLocator;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.RedirectHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.DefaultResourceSupplier;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.util.Headers;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

import javax.ws.rs.core.Application;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;

public class Main {

    public static void main(String[] args) throws Exception {

        // RESTeasy servlet setup
        final Class<? extends Application> appClass = RESTApplication.class;

        ServletInfo restEasyServletInfo = Servlets.servlet(HttpServletDispatcher.class)
                .setLoadOnStartup(1)
                .addInitParam("javax.ws.rs.Application", appClass.getName())
                .addMapping("/*");

        DeploymentInfo servletBuilder = Servlets.deployment()
                .setClassLoader(appClass.getClassLoader())
                .setContextPath("/api")
                .setDeploymentName("REST API")
                .addServlets(restEasyServletInfo);

        DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();

        // Swagger Configuration
        new JaxrsOpenApiContextBuilder()
                        .openApiConfiguration(new SwaggerConfiguration()
                                .resourcePackages(new HashSet<>(Arrays.asList("sm.api")))
                                .readerClass(OpenApiReader.class.getName())
                                .cacheTTL(0L)
                                .prettyPrint(true))
                        .ctxId(OpenApiContext.OPENAPI_CONTEXT_ID_PREFIX + "servlet." + restEasyServletInfo.getName())
                        .buildContext(true);


        // Serve swagger UI from WebJar
        ResourceHandler resourceHandler = new ResourceHandler(
                new ClassPathResourceManager(Main.class.getClassLoader(), "META-INF/resources/webjars/swagger-ui/3.23.8"))
                    .setWelcomeFiles("index.html?url=/api/openapi.json");

        Undertow server = Undertow.builder()
                .addHttpListener(3780, "localhost")
                .setHandler(Handlers.path()
                        .addExactPath("/", new RedirectHandler("/ui/index.html?url=/api/openapi.json"))
                        .addPrefixPath("/ui", resourceHandler)
                        .addPrefixPath(servletBuilder.getContextPath(), manager.start()))
                .build();

        server.start();
    }


}
