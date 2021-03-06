package de.codewave.mytunesrss;

import org.apache.commons.lang3.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class OffHeapSessionStoreFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // nothing to initialize
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest && ((HttpServletRequest)request).getSession(false) != null) {
            String currentListId = request.getParameter(OffHeapSessionStore.CURRENT_LIST_ID);
            if (StringUtils.isBlank(currentListId)) {
                OffHeapSessionStore.get((HttpServletRequest) request).removeCurrentList();
            }

        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // nothing to destroy
    }
}
