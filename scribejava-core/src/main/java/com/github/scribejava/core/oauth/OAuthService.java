package com.github.scribejava.core.oauth;

import com.github.scribejava.core.exceptions.OAuthException;
import com.github.scribejava.core.httpclient.HttpClientProvider;
import com.github.scribejava.core.model.AbstractRequest;
import com.github.scribejava.core.model.ForceTypeOfHttpRequest;
import com.github.scribejava.core.httpclient.HttpClient;
import com.github.scribejava.core.httpclient.HttpClientConfig;
import com.github.scribejava.core.model.OAuthAsyncRequestCallback;
import com.github.scribejava.core.model.OAuthConfig;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.OAuthRequestAsync;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.ScribeJavaConfig;
import com.github.scribejava.core.model.Token;
import java.io.File;

import java.io.IOException;
import java.util.ServiceLoader;
import java.util.concurrent.Future;

/**
 * The main ScribeJava object.
 *
 * A facade responsible for the retrieval of request and access tokens and for the signing of HTTP requests.
 * @param <T> type of token used to sign the request
 */
public abstract class OAuthService<T extends Token> {

    private final OAuthConfig config;
    private final HttpClient httpClient;

    public OAuthService(OAuthConfig config) {
        this.config = config;
        final ForceTypeOfHttpRequest forceTypeOfHttpRequest = ScribeJavaConfig.getForceTypeOfHttpRequests();
        final HttpClientConfig httpClientConfig = config.getHttpClientConfig();
        final HttpClient externalHttpClient = config.getHttpClient();

        if (httpClientConfig == null && externalHttpClient == null) {
            if (ForceTypeOfHttpRequest.FORCE_ASYNC_ONLY_HTTP_REQUESTS == forceTypeOfHttpRequest) {
                throw new OAuthException("Cannot use sync operations, only async");
            }
            if (ForceTypeOfHttpRequest.PREFER_ASYNC_ONLY_HTTP_REQUESTS == forceTypeOfHttpRequest) {
                config.log("Cannot use sync operations, only async");
            }
            httpClient = null;
        } else {
            if (ForceTypeOfHttpRequest.FORCE_SYNC_ONLY_HTTP_REQUESTS == forceTypeOfHttpRequest) {
                throw new OAuthException("Cannot use async operations, only sync");
            }
            if (ForceTypeOfHttpRequest.PREFER_SYNC_ONLY_HTTP_REQUESTS == forceTypeOfHttpRequest) {
                config.log("Cannot use async operations, only sync");
            }

            httpClient = externalHttpClient == null ? getClient(httpClientConfig) : externalHttpClient;
        }
    }

    private static HttpClient getClient(HttpClientConfig config) {
        for (HttpClientProvider provider : ServiceLoader.load(HttpClientProvider.class)) {
            final HttpClient client = provider.createClient(config);
            if (client != null) {
                return client;
            }
        }
        return null;
    }

    public void closeAsyncClient() throws IOException {
        httpClient.close();
    }

    public OAuthConfig getConfig() {
        return config;
    }

    /**
     * Returns the OAuth version of the service.
     *
     * @return OAuth version as string
     */
    public abstract String getVersion();

    public abstract void signRequest(T token, AbstractRequest request);

    public <T> Future<T> execute(OAuthRequestAsync request, OAuthAsyncRequestCallback<T> callback,
            OAuthRequestAsync.ResponseConverter<T> converter) {

        final ForceTypeOfHttpRequest forceTypeOfHttpRequest = ScribeJavaConfig.getForceTypeOfHttpRequests();
        if (ForceTypeOfHttpRequest.FORCE_SYNC_ONLY_HTTP_REQUESTS == forceTypeOfHttpRequest) {
            throw new OAuthException("Cannot use async operations, only sync");
        }
        if (ForceTypeOfHttpRequest.PREFER_SYNC_ONLY_HTTP_REQUESTS == forceTypeOfHttpRequest) {
            config.log("Cannot use async operations, only sync");
        }

        final File filePayload = request.getFilePayload();
        if (filePayload != null) {
            return httpClient.executeAsync(config.getUserAgent(), request.getHeaders(), request.getVerb(),
                    request.getCompleteUrl(), filePayload, callback, converter);
        } else if (request.getStringPayload() != null) {
            return httpClient.executeAsync(config.getUserAgent(), request.getHeaders(), request.getVerb(),
                    request.getCompleteUrl(), request.getStringPayload(), callback, converter);
        } else {
            return httpClient.executeAsync(config.getUserAgent(), request.getHeaders(), request.getVerb(),
                    request.getCompleteUrl(), request.getByteArrayPayload(), callback, converter);
        }
    }

    public Future<Response> execute(OAuthRequestAsync request, OAuthAsyncRequestCallback<Response> callback) {
        return execute(request, callback, null);
    }

    /**
     * the same as {@link OAuthRequest#send()}
     *
     * @param request request
     * @return Response
     */
    public Response execute(OAuthRequest request) {
        return request.send();
    }
}
