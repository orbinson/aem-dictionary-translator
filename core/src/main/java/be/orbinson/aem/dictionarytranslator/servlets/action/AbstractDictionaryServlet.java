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
    
    public static final String LANGUAGE_PARAM = "language";
    public static final String PARENT_PATH_PARAM = "parentPath";

    private final String exceptionMessagePrefix;

    /**
     * Constructor for the servlet, allowing to set a custom prefix for exception messages.
     * 
     * @param exceptionMessagePrefix the prefix to use for logging and printing exception messages. Separated by a colon and space (:) from the actual message.
     */
    protected AbstractDictionaryServlet(String exceptionMessagePrefix) {
        super();
        this.exceptionMessagePrefix = exceptionMessagePrefix;
    }

    protected String getMandatoryParameter(SlingHttpServletRequest request, String parameterName, boolean allowEmptyValue) {
        return getMandatoryParameter(request, parameterName, allowEmptyValue, Function.identity());
    }

    protected <T> T getMandatoryParameter(SlingHttpServletRequest request, String parameterName, boolean allowEmptyValue, Function<String, T> parameterConverter) {
        String parameterValue = request.getParameter(parameterName);
        if (parameterValue == null) {
            throw new IllegalArgumentException("Missing mandatory parameter: " + parameterName);
        }
        if (!allowEmptyValue && parameterValue.isEmpty()) {
            throw new IllegalArgumentException("Empty mandatory parameter: " + parameterName);
        }
        return parameterConverter.apply(parameterValue);
    }

    protected Collection<String> getMandatoryParameters(SlingHttpServletRequest request, String parameterName, boolean allowEmptyValues) {
        return getMandatoryParameters(request, parameterName, allowEmptyValues, Function.identity());
    }

    protected <T> Collection<T> getMandatoryParameters(SlingHttpServletRequest request, String parameterName, boolean allowEmptyValues, Function<String, T> parameterConverter) {
        String[] parameterValues = request.getParameterValues(parameterName);
        if (parameterValues == null || parameterValues.length == 0) {
            throw new IllegalArgumentException("Missing mandatory parameter: " + parameterName);
        }
        return getParameters(parameterValues, parameterName, allowEmptyValues, parameterConverter);
    }

    protected Optional<String> getOptionalParameter(SlingHttpServletRequest request, String parameterName, boolean allowEmptyValue) {
        return getOptionalParameter(request, parameterName, allowEmptyValue, Function.identity());
    }

    protected <T> Optional<T> getOptionalParameter(SlingHttpServletRequest request, String parameterName, boolean allowEmptyValue, Function<String, T> parameterConverter) {
        String parameterValue = request.getParameter(parameterName);
        if (parameterValue == null) {
            return Optional.empty();
        }
        if (!allowEmptyValue && parameterValue.isEmpty()) {
            throw new IllegalArgumentException("Empty optional parameter: " + parameterName);
        }
        return Optional.of(parameterConverter.apply(parameterValue));
    }
    
    protected Collection<String> getOptionalParameters(SlingHttpServletRequest request, String parameterName, boolean allowEmptyValues) {
        return getOptionalParameters(request, parameterName, allowEmptyValues, Function.identity());
    }

    protected <T> Collection<T> getOptionalParameters(SlingHttpServletRequest request, String parameterName, boolean allowEmptyValues, Function<String, T> parameterConverter) {
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
            LOG.error("{}: Servlet parameter error: {}", exceptionMessagePrefix, e.getMessage());
            HtmlResponse htmlResponse = new HtmlResponse();
            htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            htmlResponse.send(response, true);
        }
    }

    @Override
    protected void doPost(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response)
            throws ServletException, IOException {
        HtmlResponse htmlResponse = new HtmlResponse();
        try {
            internalDoPost(request, htmlResponse);
            htmlResponse.send(response, true);
        } catch (Throwable e) {
            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException)e; // rethrow IllegalArgumentException to be handled by the service method
            }
            LOG.error("{}: {}", exceptionMessagePrefix, e.getMessage(), e);
            htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, exceptionMessagePrefix + ": " + e.getMessage());
            htmlResponse.setTitle(exceptionMessagePrefix); // not exposed on client side for Ajax requests
            htmlResponse.send(response, true);
        }
    }

    /**
     * Internal method to handle the POST request logic. Only supposed to be used from POST servlets which don't deliver any HTML response except for the enriched status code.
     * 
     * @param request the Sling HTTP servlet request
     * @param htmlResponse the HTML response to be sent back
     * @throws Throwable if an error occurs during processing
     */
    protected void internalDoPost(@NotNull SlingHttpServletRequest request, @NotNull HtmlResponse htmlResponse) throws Throwable {
    }
}
