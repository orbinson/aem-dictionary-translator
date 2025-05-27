package be.orbinson.aem.dictionarytranslator.servlets.action;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.post.HtmlResponse;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractDictionaryServlet extends SlingAllMethodsServlet {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AbstractDictionaryServlet.class);
    private static final long serialVersionUID = 1L;

    String getMandatoryParameter(SlingHttpServletRequest request, String parameterName, boolean allowEmptyValue) {
        return getMandatoryParameter(request, parameterName, allowEmptyValue, Function.identity());
    }

    <T> T getMandatoryParameter(SlingHttpServletRequest request, String parameterName, boolean allowEmptyValue, Function<String, T> parameterConverter) {
        String parameterValue = request.getParameter(parameterName);
        if (parameterValue == null) {
            throw new IllegalArgumentException("Missing mandatory parameter: " + parameterName);
        }
        if (!allowEmptyValue && parameterValue.isEmpty()) {
            throw new IllegalArgumentException("Empty mandatory parameter: " + parameterName);
        }
        return parameterConverter.apply(parameterValue);
    }

    Collection<String> getMandatoryParameters(SlingHttpServletRequest request, String parameterName, boolean allowEmptyValues) {
        return getMandatoryParameters(request, parameterName, allowEmptyValues, Function.identity());
    }

    <T> Collection<T> getMandatoryParameters(SlingHttpServletRequest request, String parameterName, boolean allowEmptyValues, Function<String, T> parameterConverter) {
        String[] parameterValues = request.getParameterValues(parameterName);
        if (parameterValues == null || parameterValues.length == 0) {
            throw new IllegalArgumentException("Missing mandatory parameter: " + parameterName);
        }
        return getParameters(parameterValues, parameterName, allowEmptyValues, parameterConverter);
    }

    Optional<String> getOptionalParameter(SlingHttpServletRequest request, String parameterName, boolean allowEmptyValue) {
        return getOptionalParameter(request, parameterName, allowEmptyValue, Function.identity());
    }

    <T> Optional<T> getOptionalParameter(SlingHttpServletRequest request, String parameterName, boolean allowEmptyValue, Function<String, T> parameterConverter) {
        String parameterValue = request.getParameter(parameterName);
        if (parameterValue == null) {
            return Optional.empty();
        }
        if (!allowEmptyValue && parameterValue.isEmpty()) {
            throw new IllegalArgumentException("Empty optional parameter: " + parameterName);
        }
        return Optional.of(parameterConverter.apply(parameterValue));
    }
    
    Collection<String> getOptionalParameters(SlingHttpServletRequest request, String parameterName, boolean allowEmptyValues) {
        return getOptionalParameters(request, parameterName, allowEmptyValues, Function.identity());
    }

    <T> Collection<T> getOptionalParameters(SlingHttpServletRequest request, String parameterName, boolean allowEmptyValues, Function<String, T> parameterConverter) {
        String[] parameterValues = request.getParameterValues(parameterName);
        if (parameterValues == null || parameterValues.length == 0) {
            return Collections.emptySet();
        }
        return getParameters(parameterValues, parameterName, allowEmptyValues, parameterConverter);
    }

    private <T> Collection<T> getParameters(String[] parameterValues, String parameterName, boolean allowEmptyValues, Function<String, T> parameterConverter) {
        return Arrays.stream(parameterValues)
                .map(value -> { 
                    if (!allowEmptyValues && value.isEmpty()) {
                        throw new IllegalArgumentException("Empty parameter value found for: " + parameterName);
                    }
                    return parameterConverter.apply(value);
                })
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    protected void service(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response)
            throws ServletException, IOException {
        try {
            super.service(request, response);
        } catch (IllegalArgumentException e) {
            LOG.error("Servlet parameter error: {}", e.getMessage());
            HtmlResponse htmlResponse = new HtmlResponse();
            htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            htmlResponse.send(response, true);
        }
    }
}
