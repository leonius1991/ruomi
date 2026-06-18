package fi.newdoska.doska.config;

import fi.newdoska.doska.service.VisitStatsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(20)
@RequiredArgsConstructor
public class VisitTrackingFilter extends OncePerRequestFilter {

    private final VisitStatsService visitStatsService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return true;
        }
        return uri.startsWith("/css/")
                || uri.startsWith("/js/")
                || uri.startsWith("/images/")
                || uri.startsWith("/data/")
                || uri.startsWith("/files/")
                || uri.startsWith("/theme/")
                || uri.startsWith("/api/")
                || uri.startsWith("/actuator/")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            HttpSession session = request.getSession(true);
            if (session.getAttribute("ruomiVisitRecorded") == null) {
                visitStatsService.recordSessionVisit();
                session.setAttribute("ruomiVisitRecorded", Boolean.TRUE);
            }
        } catch (Exception ignored) {
            // stats must not break page rendering
        }
        filterChain.doFilter(request, response);
    }
}
