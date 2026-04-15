package org.example.server.filter_chain;

import org.example.dto.Request;
import org.example.server.filter.BaseRequestFilter;

import java.util.ArrayList;
import java.util.List;

public class FilterChain {
    private final List<BaseRequestFilter> filters = new ArrayList<>();

    public FilterChain addFilter(BaseRequestFilter filter) {
        filters.add(filter);
        return this;
    }

    public void execute(Request request) {
        for (BaseRequestFilter filter : filters) {
            filter.doFilter(request);  // если ошибка — исключение
        }
    }
}
