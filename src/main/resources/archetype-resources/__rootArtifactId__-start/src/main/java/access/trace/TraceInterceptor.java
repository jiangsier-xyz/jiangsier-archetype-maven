#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.access.trace;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.servlet.HandlerInterceptor;
import ${package}.service.exception.BadRequestException;
import ${package}.util.TraceUtils;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class TraceInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(TraceInterceptor.class);

    private final List<Class<? extends Exception>> badRequestExceptions = Arrays.asList(
            BadRequestException.class,
            ServletRequestBindingException.class,
            RequestRejectedException.class,
            AuthenticationException.class,
            ConstraintViolationException.class,
            ClientAbortException.class
    );

    private boolean isBadRequest(int status, Exception ex) {
        if (ex != null && badRequestExceptions.contains(ex.getClass())) {
            return true;
        } else {
            return status >= 400 && status < 500;
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        TraceUtils.startTrace();
        TraceUtils.putTraceAttribute("traceId", TraceUtils.getTraceId());
        Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .map(Principal::getName)
                .ifPresent(username ->  TraceUtils.putTraceAttribute("username", username));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!TraceUtils.isTracing()) {
            return;
        }

        String username = Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .map(Principal::getName)
                .orElse(TraceUtils.getTraceAttribute("username"));

        String service = request.getServletPath();
        String method = request.getMethod();
        String[] args = new String[]{ request.getQueryString() };
        int responseCode;
        if (ex instanceof AuthenticationException) {
            responseCode = HttpStatus.UNAUTHORIZED.value();
        } else if (ex instanceof AccessDeniedException) {
            responseCode = HttpStatus.FORBIDDEN.value();
        } else {
            responseCode = response.getStatus();
        }
        TraceUtils.TraceStatus status = TraceUtils.TraceStatus.SUCCESSFUL;
        if (ex != null || responseCode >= 400) {
            status = isBadRequest(responseCode, ex) ?
                    TraceUtils.TraceStatus.BAD_REQUEST :
                    TraceUtils.TraceStatus.FAILED;
        }
        long elapseTime = TraceUtils.stopTrace();

        TraceUtils.TraceInfoBuilder builder = new TraceUtils.TraceInfoBuilder();
        builder.traceId(TraceUtils.getTraceId())
                .username(username)
                .method(service + "::" + method)
                .status(status)
                .args(args)
                .response(responseCode)
                .throwable(ex)
                .elapseTime(elapseTime);
        logger.trace(builder.build());
        TraceUtils.endTrace();
    }
}
