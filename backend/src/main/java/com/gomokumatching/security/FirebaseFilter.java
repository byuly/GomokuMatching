package com.gomokumatching.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * A servlet filter that intercepts all incoming HTTP requests to verify the Firebase ID token.
 */
@Component
public class FirebaseFilter extends OncePerRequestFilter {

    /**
     * This method is executed for every incoming request.
     *
     * How it works:
     * 1. It checks for an "Authorization" header with the "Bearer " prefix.
     * 2. If found, it extracts the Firebase ID token.
     * 3. It uses the Firebase Admin SDK's `verifyIdToken` method to validate the token.
     *    This check ensures the token is correctly signed, not expired, and issued by your Firebase project.
     * 4. If the token is valid, it creates a Spring Security `Authentication` object
     *    and sets it in the `SecurityContextHolder`. This makes the user's authentication
     *    principal (including their UID) available to the rest of the application (e.g., in controllers).
     * 5. If the token is invalid or not present, the security context remains empty, and access will be
     *    denied by Spring Security for protected endpoints.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String idToken = header.substring(7);

        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            // The principal is the Firebase UID, and credentials are the full token.
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    decodedToken.getUid(), decodedToken, new ArrayList<>()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            // Token is invalid. Let the request continue without authentication.
            // Access will be denied later by Spring Security if the endpoint is protected.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
