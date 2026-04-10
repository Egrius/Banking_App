package org.example.server.filter;

public interface BaseRequestFilter {
    void doFilter();
    void nextFilter ();
}
