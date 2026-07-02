package com.trako.filters;

import com.trako.services.JwtUserDetailsService;
import com.trako.services.UserService;
import com.trako.util.JwtTokenUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter that processes incoming HTTP requests to validate JWT tokens.
 * <p>
 * This filter intercepts every request (except OPTIONS pre-flight requests) to:
 * 1. Extract JWT token from Authorization header (Bearer format)
 * 2. Validate the token and extract username
 * 3. Load user details and authenticate the user in Spring Security context
 * 4. Reject unauthenticated requests to protected endpoints with HTTP 401
 * <p>
 * Whitelisted public endpoints (login, health, Swagger UI, H2 console) are allowed
 * without authentication. All other endpoints require a valid JWT token.
 *
 * @author Tracko Team
 * @since 1.0
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final String[] WHITE_LIST_API = {
            "/api/oauth/token",
            "/api/login",
            "/api/health",
            "/h2-console/**",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**"
    };

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * JWT utility for token validation and username extraction
     */
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    /**
     * User service for user-related operations (currently unused but kept for future functionality)
     */
    @Autowired
    private UserService userService;

    /**
     * Custom user details service for loading user-specific data
     */
    @Autowired
    private JwtUserDetailsService jwtUserDetailsService;

    /**
     * Determines whether the filter should be skipped for certain requests.
     * <p>
     * This method skips JWT validation for OPTIONS requests to support CORS pre-flight
     * requests, which don't contain authentication headers and should be allowed
     * to proceed without authentication.
     *
     * @param request The current HTTP request
     * @return true if the filter should be skipped (OPTIONS requests), false otherwise
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    private boolean isWhitelisted(String requestUri) {
        for (String pattern : WHITE_LIST_API) {
            if (pathMatcher.match(pattern, requestUri)) {
                return true;
            }
        }
        return false;
    }

    private void proceedOrReject(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if ((auth == null || auth instanceof AnonymousAuthenticationToken)
                && !isWhitelisted(request.getRequestURI())) {
            logger.warn("JwtRequestFilter: No valid authentication for protected endpoint " + request.getMethod() + " " + request.getRequestURI());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }
        chain.doFilter(request, response);
    }

    /**
     * Core filter method that processes each incoming HTTP request for JWT authentication.
     * <p>
     * This method implements the JWT authentication flow:
     * 1. Extracts JWT token from Authorization header (Bearer format)
     * 2. Validates the token and extracts username
     * 3. Loads user details from the database
     * 4. Validates token against user details
     * 5. Sets authentication in Spring Security context if valid
     * 6. Rejects unauthenticated requests to protected endpoints with HTTP 401
     * <p>
     * Whitelisted endpoints (login, health, Swagger, H2 console) are allowed
     * without authentication. All other endpoints require a valid JWT token.
     *
     * @param httpServletRequest  The incoming HTTP request
     * @param httpServletResponse The HTTP response
     * @param filterChain         The filter chain to continue processing
     * @throws ServletException If servlet processing fails
     * @throws IOException      If I/O operations fail
     */
    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, @NonNull HttpServletResponse httpServletResponse, @NonNull FilterChain filterChain) throws ServletException, IOException {

        logger.info("JwtRequestFilter: Processing request " + httpServletRequest.getMethod() + " " + httpServletRequest.getRequestURI());

        try {
            // Extract Authorization header containing the JWT token
            final String requestTokenHeader = httpServletRequest.getHeader("Authorization");

            String username = null;
            String jwtToken = null;

            // JWT Token is expected in the format "Bearer <token>"
            // Remove "Bearer " prefix to get the actual token
            if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
                jwtToken = requestTokenHeader.substring(7);
                // Validate that the token is not empty after extracting
                if (jwtToken.isBlank()) {
                    logger.warn("JwtRequestFilter: Token is blank");
                    filterChain.doFilter(httpServletRequest, httpServletResponse);
                    return;
                }

                // Attempt to extract username from the JWT token
                try {
                    username = jwtTokenUtil.getUserNameFromToken(jwtToken);
                    logger.info("JwtRequestFilter: Username from token: " + username);
                } catch (IllegalArgumentException e) {
                    logger.error("JwtRequestFilter: Unable to get JWT Token", e);
                } catch (ExpiredJwtException e) {
                    logger.warn("JwtRequestFilter: JWT Token has expired", e);
                } catch (JwtException e) {
                    logger.error("JwtRequestFilter: Invalid JWT Token", e);
                }
            } else {
                logger.info("JwtRequestFilter: No Bearer token found in Authorization header");
            }

            // Validate token and set authentication if user is not already authenticated
            // Only proceed if we have a username and either:
            // - No authentication exists in security context, OR
            // - Current authentication is anonymous (not logged in)
            if (username != null && (SecurityContextHolder.getContext().getAuthentication() == null
                    || SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken)) {

                logger.info("JwtRequestFilter: Loading details for user: " + username);
                // Load user details from a database using the username from token
                UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(username);

                // Validate the JWT token against the loaded user details
                // This checks token expiration, signature, and user validity
                boolean isValid = jwtTokenUtil.validateToken(jwtToken, userDetails);
                if (isValid) {
                    logger.info("JwtRequestFilter: Token is valid. Setting authentication.");

                    // Create authentication token with user details and authorities
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

                    // Set authentication details including request information
                    usernamePasswordAuthenticationToken
                            .setDetails(new WebAuthenticationDetailsSource().buildDetails(httpServletRequest));

                    // Set the authentication in Spring Security context
                    // This marks the user as authenticated for the current request
                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                } else {
                    logger.warn("JwtRequestFilter: Token validation failed for user: " + username);
                }
            } else {
                // Log if user is already authenticated (useful for debugging)
                if (username != null) {
                    logger.info("JwtRequestFilter: SecurityContext already has authentication: " + SecurityContextHolder.getContext().getAuthentication());
                }
            }

            // Continue with the filter chain via proceedOrReject which blocks
            // unauthenticated requests to protected endpoints
            proceedOrReject(httpServletRequest, httpServletResponse, filterChain);
        } catch (Exception e) {
            // Catch any unexpected errors to prevent the filter from breaking the request processing
            logger.error("Unexpected error in JwtRequestFilter", e);
            proceedOrReject(httpServletRequest, httpServletResponse, filterChain);
        }
    }
}
