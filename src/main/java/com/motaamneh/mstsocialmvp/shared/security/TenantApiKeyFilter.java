package com.motaamneh.mstsocialmvp.shared.security;

import com.motaamneh.mstsocialmvp.tenant.application.TenantApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.filter.OncePerRequestFilter;

public class TenantApiKeyFilter extends OncePerRequestFilter {
    private final TenantApiKeyService keys;

    public TenantApiKeyFilter(TenantApiKeyService keys) {
        this.keys = keys;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String key = authorization.substring("Bearer ".length());
            keys.authenticate(key).ifPresent(tenantId -> {
                var authentication = new UsernamePasswordAuthenticationToken(tenantId, null,
                        List.of(new SimpleGrantedAuthority("ROLE_TENANT")));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }
        chain.doFilter(request, response);
    }
}
