package com.miles.milesagent.controller;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.util.List;

/**
 * SPA fallback: any GET request whose path doesn't resolve to an actual static
 * resource is served the SPA's index.html, so Vue Router history mode can take
 * over after a deep-link refresh. Static asset requests (which contain a dot)
 * still hit the normal resource pipeline and return 404 if the file is missing.
 */
@Configuration
public class SpaFallbackController implements WebMvcConfigurer {

    private static final List<String> EXCLUDED_PREFIXES = List.of(
            "actuator/",
            "error"
    );

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new SpaPathResolver());
    }

    private static class SpaPathResolver extends PathResourceResolver {
        private final ClassPathResource indexHtml = new ClassPathResource("/static/index.html");

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource requested = location.createRelative(resourcePath);
            if (requested.exists() && requested.isReadable()) {
                return requested;
            }
            // Don't shadow API endpoints, Actuator, or the error page.
            for (String prefix : EXCLUDED_PREFIXES) {
                if (resourcePath.equals(prefix) || resourcePath.startsWith(prefix)) {
                    return null;
                }
            }
            // Static asset miss (anything with an extension) — return 404 instead
            // of the SPA shell so missing-file bugs surface clearly.
            if (resourcePath.contains(".")) {
                return null;
            }
            return indexHtml.exists() ? indexHtml : null;
        }
    }
}
