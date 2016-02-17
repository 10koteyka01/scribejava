package com.github.scribejava.apis.google;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.github.scribejava.core.extractors.OAuth2AccessTokenJsonExtractor;

/**
 * additionally parses OpenID id_token
 */
public class GoogleJsonTokenExtractor extends OAuth2AccessTokenJsonExtractor {

    private static final Pattern ID_TOKEN_PATTERN = Pattern.compile("\"id_token\"\\s*:\\s*\"(\\S*?)\"");

    protected GoogleJsonTokenExtractor() {
    }

    private static class InstanceHolder {

        private static final GoogleJsonTokenExtractor INSTANCE = new GoogleJsonTokenExtractor();
    }

    public static GoogleJsonTokenExtractor instance() {
        return InstanceHolder.INSTANCE;
    }

    @Override
    public GoogleToken extract(String response) {
        return new GoogleToken(extractAccessToken(response), response, extractOpenIdToken(response));
    }

    private String extractOpenIdToken(String response) {
        final Matcher matcher = ID_TOKEN_PATTERN.matcher(response);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
