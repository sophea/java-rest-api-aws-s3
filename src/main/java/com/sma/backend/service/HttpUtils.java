package com.sma.backend.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;


public final class HttpUtils {
    private static final String DEFAULT_ENCODING = "UTF-8";
    /**
     * Regex which matches any of the sequences that we need to fix up after URLEncoder.encode().
     */
    private static final Pattern ENCODED_CHARACTERS_PATTERN;

    static {
        String pattern = Pattern.quote("+") + "|" + Pattern.quote("*") + "|"
                + Pattern.quote("%7E") + "|" + Pattern.quote("%2F");

        ENCODED_CHARACTERS_PATTERN = Pattern.compile(pattern);
    }

    private HttpUtils() {
    }

    /**
     * Encode a string for use in the path of a URL; uses URLEncoder.encode, (which encodes a string for use in the
     * query portion of a URL), then applies some postfilters to fix things up per the RFC. Can optionally handle
     * strings which are meant to encode a path (ie include '/'es which should NOT be escaped).
     *
     * @param value the value to encode
     * @param path  true if the value is intended to represent a path
     * @return the encoded value
     */
    public static String urlEncode(String value, boolean path) {
        if (value == null) {
            return "";
        }

        try {
            String encoded = URLEncoder.encode(value, DEFAULT_ENCODING);

            Matcher matcher = ENCODED_CHARACTERS_PATTERN.matcher(encoded);
            StringBuffer buffer = new StringBuffer(encoded.length());

            while (matcher.find()) {
                String replacement = matcher.group(0);

                if ("+".equals(replacement)) {
                    replacement = "%20";
                } else if ("*".equals(replacement)) {
                    replacement = "%2A";
                } else if ("%7E".equals(replacement)) {
                    replacement = "~";
                } else if (path && "%2F".equals(replacement)) {
                    replacement = "/";
                }

                matcher.appendReplacement(buffer, replacement);
            }

            matcher.appendTail(buffer);
            return buffer.toString();

        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String getEffectiveMethod(HttpServletRequest request) {
        final String method = request.getMethod();
        final String method1 = request.getParameter("_method");

        /**it is used for jsonp with request as GET but it consider as POST*/
        if ("GET".equals(method) && method1 != null) {
            return method1.toUpperCase();
        }
        return method;
    }

}