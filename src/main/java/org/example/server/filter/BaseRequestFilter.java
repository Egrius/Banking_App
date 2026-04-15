package org.example.server.filter;

import org.example.dto.Request;
import org.example.server.filter_chain.FilterChain;

public abstract class BaseRequestFilter {

    public abstract void doFilter(Request request, FilterChain filterChain);
}