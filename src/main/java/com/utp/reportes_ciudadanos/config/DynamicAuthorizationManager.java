package com.utp.reportes_ciudadanos.config;
import com.utp.reportes_ciudadanos.model.Permiso;
import com.utp.reportes_ciudadanos.service.RolService;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;


@Component
public class DynamicAuthorizationManager
        implements AuthorizationManager<RequestAuthorizationContext> {

    private final RolService rolService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final RequestMappingHandlerMapping handlerMapping;

    public DynamicAuthorizationManager(RolService rolService,
            RequestMappingHandlerMapping handlerMapping) {
        this.rolService = rolService;
        this.handlerMapping = handlerMapping;
    }

    @Override
    public AuthorizationResult authorize(
            Supplier<? extends Authentication> authentication,
            RequestAuthorizationContext context) {

        return evaluarPermiso(authentication, context);
    }

    public AuthorizationDecision check(
            Supplier<Authentication> authentication,
            RequestAuthorizationContext context) {

        return evaluarPermiso(authentication, context);
    }

    private AuthorizationDecision evaluarPermiso(
            Supplier<? extends Authentication> authentication,
            RequestAuthorizationContext context) {

        String url = context.getRequest().getServletPath();
        String method = context.getRequest().getMethod();

        // Si la ruta NO existe en ningún @Controller, dejamos pasar
        // para que Spring MVC muestre 404.
        if (!existeRutaMvc(url)) {
            return new AuthorizationDecision(true);
        }

        Authentication auth = authentication.get();

        if (auth == null || !auth.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }

        String rol = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");

        List<Permiso> permisos = rolService.findPermisosByRolNombre(rol);

        boolean permitido = permisos.stream().anyMatch(p -> {
            String metodoPermiso = p.getHttpMethod() == null
                    ? "*"
                    : p.getHttpMethod();

            return pathMatcher.match(p.getUrlPattern(), url)
                    && (metodoPermiso.equals("*")
                    || metodoPermiso.equalsIgnoreCase(method));
        });

        return new AuthorizationDecision(permitido);
    }

    private boolean existeRutaMvc(String url) {
        return handlerMapping.getHandlerMethods().keySet().stream()
                .anyMatch(info -> {

                    if (info.getPathPatternsCondition() != null) {
                        return info.getPathPatternsCondition()
                                .getPatternValues()
                                .stream()
                                .anyMatch(pattern -> pathMatcher.match(pattern, url));
                    }

                    if (info.getPatternsCondition() != null) {
                        return info.getPatternsCondition()
                                .getPatterns()
                                .stream()
                                .anyMatch(pattern -> pathMatcher.match(pattern, url));
                    }

                    return false;
                });
    }
}
