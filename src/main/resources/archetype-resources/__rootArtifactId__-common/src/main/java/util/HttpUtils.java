#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.util;

import org.apache.commons.collections4.MapUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class HttpUtils {
    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);

    private final static int CONNECT_TIMEOUT = 10 * 1000;
    private final static int READ_TIMEOUT = 30 * 1000;
    private final static int LIVE_TIMEOUT_M = 10;
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36";

    private static CloseableHttpClient httpClient;

    private static CloseableHttpClient getHttpClient() {
        if (Objects.isNull(httpClient)) {
            synchronized (HttpUtils.class) {
                if (Objects.isNull(httpClient)) {
                    httpClient = createHttpClient();
                }
            }
        }
        return httpClient;
    }

    private static CloseableHttpClient createHttpClient() {
        SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout(READ_TIMEOUT)
                .setTcpNoDelay(true)
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(READ_TIMEOUT)
                .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
                .build();


        return HttpClientBuilder.create()
                .setConnectionTimeToLive(LIVE_TIMEOUT_M, TimeUnit.MINUTES)
                .setDefaultSocketConfig(socketConfig)
                .setDefaultRequestConfig(requestConfig)
                .setUserAgent(DEFAULT_USER_AGENT)
                .build();
    }

    public static void shutdown() {
        if (Objects.nonNull(httpClient)) {
            try {
                httpClient.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static String getResponseAsString(HttpRequestBase request) throws IOException {
        try (CloseableHttpResponse response = getHttpClient().execute(request)) {
            HttpEntity entity = response.getEntity();
            String responseText = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(entity);
            return responseText;
        }
    }

    public static String post(String url, String data) {
        return post(url, data, null);
    }

    public static String post(String url, String data, Map<String, String> headers) {
        try {
            HttpPost request = new HttpPost(url);
            request.setHeader("Content-Type", "application/json;charset=UTF-8");
            if (MapUtils.isNotEmpty(headers)) {
                headers.forEach(request::setHeader);
            }
            request.setEntity(new StringEntity(data, ContentType.APPLICATION_JSON));
            return getResponseAsString(request);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid URL {}", url, e);
        } catch (ClientProtocolException e) {
            logger.warn("Invalid protocol {}", url, e);
        } catch (IOException | NullPointerException e) {
            logger.warn("Failed to send data to {}: {}", url, data, e);
        }
        return null;
    }

    public static String get(String url) {
        return get(url, null);
    }

    public static String get(String url, Map<String, String> headers) {
        try {
            HttpGet request = new HttpGet(url);
            request.setHeader("Content-Type", "application/json;charset=UTF-8");
            if (MapUtils.isNotEmpty(headers)) {
                headers.forEach(request::setHeader);
            }
            return getResponseAsString(request);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid URL {}", url, e);
        } catch (ClientProtocolException e) {
            logger.warn("Invalid protocol {}", url, e);
        } catch (IOException | NullPointerException e) {
            logger.warn("Failed to send data to {}", url, e);
        }
        return null;
    }

    public static String put(String url, String data) {
        return put(url, data, null);
    }

    public static String put(String url, String data, Map<String, String> headers) {
        try {
            HttpPut request = new HttpPut(url);
            request.setHeader("Content-Type", "application/json;charset=UTF-8");
            if (MapUtils.isNotEmpty(headers)) {
                headers.forEach(request::setHeader);
            }
            request.setEntity(new StringEntity(data, ContentType.APPLICATION_JSON));
            return getResponseAsString(request);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid URL {}", url, e);
        } catch (ClientProtocolException e) {
            logger.warn("Invalid protocol {}", url, e);
        } catch (IOException | NullPointerException e) {
            logger.warn("Failed to send data to {}", url, e);
        }
        return null;
    }

    public static String delete(String url) {
        return delete(url, null);
    }

    public static String delete(String url, Map<String, String> headers) {
        try {
            HttpDelete request = new HttpDelete(url);
            request.setHeader("Content-Type", "application/json;charset=UTF-8");
            if (MapUtils.isNotEmpty(headers)) {
                headers.forEach(request::setHeader);
            }
            return getResponseAsString(request);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid URL {}", url, e);
        } catch (ClientProtocolException e) {
            logger.warn("Invalid protocol {}", url, e);
        } catch (IOException | NullPointerException e) {
            logger.warn("Failed to send data to {}", url, e);
        }
        return null;
    }

    public static String upload(String url, File file, String filename) {
        return upload(url, file, filename, null);
    }

    public static String upload(String url, File file, String filename, Map<String, String> headers) {
        if (Objects.isNull(file) || !file.exists()) {
            return null;
        }

        if (Objects.isNull(filename)) {
            filename = file.getName();
        }

        try {
            HttpPost request = new HttpPost(url);
            if (MapUtils.isNotEmpty(headers)) {
                headers.forEach(request::setHeader);
            }
            FileBody fileBody = new FileBody(file);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.addPart(filename, fileBody);
            HttpEntity entity = builder.build();
            request.setEntity(entity);
            return getResponseAsString(request);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid URL {}", url, e);
        } catch (ClientProtocolException e) {
            logger.warn("Invalid protocol {}", url, e);
        } catch (IOException | NullPointerException e) {
            logger.warn("Failed to send data to {}", url, e);
        }
        return null;
    }
}
