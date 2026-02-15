package com.trako.filters;

import com.trako.services.JwtUserDetailsService;
import com.trako.services.UserService;
import com.trako.util.JwtTokenUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUserDetailsService jwtUserDetailsService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, @NonNull HttpServletResponse httpServletResponse, @NonNull FilterChain filterChain) throws ServletException, IOException {

        logger.info("JwtRequestFilter: Processing request " + httpServletRequest.getMethod() + " " + httpServletRequest.getRequestURI());

        try {
            final String requestTokenHeader = httpServletRequest.getHeader("Authorization");

            String username = null;
            String jwtToken = null;
            // JWT Token is in the form "Bearer token". Remove Bearer word and get
            // only the Token
            if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
                jwtToken = requestTokenHeader.substring(7);
                logger.info("JwtRequestFilter: Token found (prefix): " + (jwtToken.length() > 10 ? jwtToken.substring(0, 10) + "..." : jwtToken));

                if (jwtToken.isBlank()) {
                    logger.warn("JwtRequestFilter: Token is blank");
                    filterChain.doFilter(httpServletRequest, httpServletResponse);
                    return;
                }
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

            // Once we get the token, validate it.
            if (username != null && (SecurityContextHolder.getContext().getAuthentication() == null
                    || SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken)) {

                logger.info("JwtRequestFilter: Loading details for user: " + username);
                UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(username);

                // if token is valid configure Spring Security to manually set
                // authentication
                boolean isValid = jwtTokenUtil.validateToken(jwtToken, userDetails);
                if (isValid) {
                    logger.info("JwtRequestFilter: Token is valid. Setting authentication.");

                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    usernamePasswordAuthenticationToken
                            .setDetails(new WebAuthenticationDetailsSource().buildDetails(httpServletRequest));
                    // After setting the Authentication in the context, we specify
                    // that the current user is authenticated. So it passes the
                    // Spring Security Configurations successfully.
                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                } else {
                    logger.warn("JwtRequestFilter: Token validation failed for user: " + username);
                }
            } else {
                 if (username != null) {
                     logger.info("JwtRequestFilter: SecurityContext already has authentication: " + SecurityContextHolder.getContext().getAuthentication());
                 }
            }
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        } catch (Exception e) {
            logger.error("Unexpected error in JwtRequestFilter", e);
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        }
    }
}
