#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.util;

import okhttp3.*;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class HttpUtils {
    private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);

    private final static int CALL_TIMEOUT_M = 3;
    private final static int CONNECT_TIMEOUT_S = 15;
    private final static int READ_TIMEOUT_S = 30;
    private final static int WRITE_TIMEOUT_S = 15;
    private final static int MAX_IDLE_CONNECTIONS = 4;
    private final static long MAX_LIVE_TIMEOUT_M = 10;
    private static final MediaType DEFAULT_MEDIA_TYPE =
            MediaType.parse("application/json; charset=utf-8");
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36";

    private static OkHttpClient client;

    private static OkHttpClient getClient() {
        if (client == null) {
            synchronized (HttpUtils.class) {
                if (client == null) {
                    client = createClient();
                }
            }
        }
        return client;
    }

    private static OkHttpClient createClient() {
        ConnectionPool connectionPool = new ConnectionPool(
                MAX_IDLE_CONNECTIONS, MAX_LIVE_TIMEOUT_M, TimeUnit.MINUTES);

        return new OkHttpClient.Builder()
                .callTimeout(CALL_TIMEOUT_M, TimeUnit.MINUTES)
                .connectTimeout(CONNECT_TIMEOUT_S, TimeUnit.SECONDS)
                .connectionPool(connectionPool)
                .cookieJar(CookieJar.NO_COOKIES)
                .followRedirects(true)
                .followSslRedirects(true)
                .readTimeout(READ_TIMEOUT_S, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .writeTimeout(WRITE_TIMEOUT_S, TimeUnit.SECONDS)
                .build();
    }

    private static Request.Builder builderFor(String url, Map<String, String> headers) {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .header("User-Agent", DEFAULT_USER_AGENT);
        if (MapUtils.isNotEmpty(headers)) {
            headers.forEach(builder::header);
        }
        return builder;
    }

    private static String getResponseAsString(Request request) {
        try (Response response = getClient().newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            }
        } catch (IOException e) {
            log.warn("Failed to send data to {}", request.url(), e);
        }
        return null;
    }

    public static String get(String url) {
        return get(url, null);
    }

    public static String get(String url, Map<String, String> headers) {
        return getResponseAsString(builderFor(url, headers).get().build());
    }

    public static String post(String url, String data) {
        return post(url, data, null);
    }

    public static String post(String url, String data, Map<String, String> headers) {
        Request.Builder builder = builderFor(url, headers);
        if (StringUtils.isNotBlank(data)) {
            builder.post(RequestBody.create(data, DEFAULT_MEDIA_TYPE));
        } else {
            builder.post(RequestBody.create(new byte[0], null));
        }
        return getResponseAsString(builder.build());
    }

    public static String put(String url, String data) {
        return put(url, data, null);
    }

    public static String put(String url, String data, Map<String, String> headers) {
        Request.Builder builder = builderFor(url, headers);
        if (StringUtils.isNotBlank(data)) {
            builder.put(RequestBody.create(data, DEFAULT_MEDIA_TYPE));
        } else {
            builder.put(RequestBody.create(new byte[0], null));
        }
        return getResponseAsString(builder.build());
    }

    public static String delete(String url) {
        return delete(url, null);
    }

    public static String delete(String url, Map<String, String> headers) {
        return getResponseAsString(builderFor(url, headers).delete().build());
    }

    public static String upload(String url, File file, String filename) {
        return upload(url, file, filename, null);
    }

    public static String upload(String url, File file, String filename, Map<String, String> headers) {
        if (file == null || !file.exists()) {
            return null;
        }

        if (filename == null) {
            filename = file.getName();
        }

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file",
                        filename,
                        RequestBody.create(file, MediaType.parse("application/octet-stream")))
                .build();

        Request.Builder builder = builderFor(url, headers);
        builder.post(requestBody);
        return getResponseAsString(builder.build());
    }

    // See: https://stackoverflow.com/questions/161738/what-is-the-best-regular-expression-to-check-if-a-string-is-a-valid-url
    private static final String URL_REGEX = "^[a-z](?:[-a-z0-9${symbol_escape}${symbol_escape}+${symbol_escape}${symbol_escape}.])*:(?:${symbol_escape}${symbol_escape}/${symbol_escape}${symbol_escape}/(?:(?:%[0-9a-f][0-9a-f]|[-a-z0-9${symbol_escape}${symbol_escape}._~${symbol_escape}${symbol_escape}x{A0}-${symbol_escape}${symbol_escape}x{D7FF}${symbol_escape}${symbol_escape}x{F900}-${symbol_escape}${symbol_escape}x{FDCF}${symbol_escape}${symbol_escape}x{FDF0}-${symbol_escape}${symbol_escape}x{FFEF}${symbol_escape}${symbol_escape}x{10000}-${symbol_escape}${symbol_escape}x{1FFFD}${symbol_escape}${symbol_escape}x{20000}-${symbol_escape}${symbol_escape}x{2FFFD}${symbol_escape}${symbol_escape}x{30000}-${symbol_escape}${symbol_escape}x{3FFFD}${symbol_escape}${symbol_escape}x{40000}-${symbol_escape}${symbol_escape}x{4FFFD}${symbol_escape}${symbol_escape}x{50000}-${symbol_escape}${symbol_escape}x{5FFFD}${symbol_escape}${symbol_escape}x{60000}-${symbol_escape}${symbol_escape}x{6FFFD}${symbol_escape}${symbol_escape}x{70000}-${symbol_escape}${symbol_escape}x{7FFFD}${symbol_escape}${symbol_escape}x{80000}-${symbol_escape}${symbol_escape}x{8FFFD}${symbol_escape}${symbol_escape}x{90000}-${symbol_escape}${symbol_escape}x{9FFFD}${symbol_escape}${symbol_escape}x{A0000}-${symbol_escape}${symbol_escape}x{AFFFD}${symbol_escape}${symbol_escape}x{B0000}-${symbol_escape}${symbol_escape}x{BFFFD}${symbol_escape}${symbol_escape}x{C0000}-${symbol_escape}${symbol_escape}x{CFFFD}${symbol_escape}${symbol_escape}x{D0000}-${symbol_escape}${symbol_escape}x{DFFFD}${symbol_escape}${symbol_escape}x{E1000}-${symbol_escape}${symbol_escape}x{EFFFD}!${symbol_escape}${symbol_escape}${symbol_dollar}&'${symbol_escape}${symbol_escape}(${symbol_escape}${symbol_escape})${symbol_escape}${symbol_escape}*${symbol_escape}${symbol_escape}+,;=:])*@)?(?:${symbol_escape}${symbol_escape}[(?:(?:(?:[0-9a-f]{1,4}:){6}(?:[0-9a-f]{1,4}:[0-9a-f]{1,4}|(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(?:${symbol_escape}${symbol_escape}.(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])){3})|::(?:[0-9a-f]{1,4}:){5}(?:[0-9a-f]{1,4}:[0-9a-f]{1,4}|(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(?:${symbol_escape}${symbol_escape}.(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])){3})|(?:[0-9a-f]{1,4})?::(?:[0-9a-f]{1,4}:){4}(?:[0-9a-f]{1,4}:[0-9a-f]{1,4}|(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(?:${symbol_escape}${symbol_escape}.(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])){3})|(?:(?:[0-9a-f]{1,4}:){0,1}[0-9a-f]{1,4})?::(?:[0-9a-f]{1,4}:){3}(?:[0-9a-f]{1,4}:[0-9a-f]{1,4}|(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(?:${symbol_escape}${symbol_escape}.(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])){3})|(?:(?:[0-9a-f]{1,4}:){0,2}[0-9a-f]{1,4})?::(?:[0-9a-f]{1,4}:){2}(?:[0-9a-f]{1,4}:[0-9a-f]{1,4}|(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(?:${symbol_escape}${symbol_escape}.(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])){3})|(?:(?:[0-9a-f]{1,4}:){0,3}[0-9a-f]{1,4})?::[0-9a-f]{1,4}:(?:[0-9a-f]{1,4}:[0-9a-f]{1,4}|(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(?:${symbol_escape}${symbol_escape}.(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])){3})|(?:(?:[0-9a-f]{1,4}:){0,4}[0-9a-f]{1,4})?::(?:[0-9a-f]{1,4}:[0-9a-f]{1,4}|(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(?:${symbol_escape}${symbol_escape}.(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])){3})|(?:(?:[0-9a-f]{1,4}:){0,5}[0-9a-f]{1,4})?::[0-9a-f]{1,4}|(?:(?:[0-9a-f]{1,4}:){0,6}[0-9a-f]{1,4})?::)|v[0-9a-f]+${symbol_escape}${symbol_escape}.[-a-z0-9${symbol_escape}${symbol_escape}._~!${symbol_escape}${symbol_escape}${symbol_dollar}&'${symbol_escape}${symbol_escape}(${symbol_escape}${symbol_escape})${symbol_escape}${symbol_escape}*${symbol_escape}${symbol_escape}+,;=:]+)${symbol_escape}${symbol_escape}]|(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(?:${symbol_escape}${symbol_escape}.(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])){3}|(?:%[0-9a-f][0-9a-f]|[-a-z0-9${symbol_escape}${symbol_escape}._~${symbol_escape}${symbol_escape}x{A0}-${symbol_escape}${symbol_escape}x{D7FF}${symbol_escape}${symbol_escape}x{F900}-${symbol_escape}${symbol_escape}x{FDCF}${symbol_escape}${symbol_escape}x{FDF0}-${symbol_escape}${symbol_escape}x{FFEF}${symbol_escape}${symbol_escape}x{10000}-${symbol_escape}${symbol_escape}x{1FFFD}${symbol_escape}${symbol_escape}x{20000}-${symbol_escape}${symbol_escape}x{2FFFD}${symbol_escape}${symbol_escape}x{30000}-${symbol_escape}${symbol_escape}x{3FFFD}${symbol_escape}${symbol_escape}x{40000}-${symbol_escape}${symbol_escape}x{4FFFD}${symbol_escape}${symbol_escape}x{50000}-${symbol_escape}${symbol_escape}x{5FFFD}${symbol_escape}${symbol_escape}x{60000}-${symbol_escape}${symbol_escape}x{6FFFD}${symbol_escape}${symbol_escape}x{70000}-${symbol_escape}${symbol_escape}x{7FFFD}${symbol_escape}${symbol_escape}x{80000}-${symbol_escape}${symbol_escape}x{8FFFD}${symbol_escape}${symbol_escape}x{90000}-${symbol_escape}${symbol_escape}x{9FFFD}${symbol_escape}${symbol_escape}x{A0000}-${symbol_escape}${symbol_escape}x{AFFFD}${symbol_escape}${symbol_escape}x{B0000}-${symbol_escape}${symbol_escape}x{BFFFD}${symbol_escape}${symbol_escape}x{C0000}-${symbol_escape}${symbol_escape}x{CFFFD}${symbol_escape}${symbol_escape}x{D0000}-${symbol_escape}${symbol_escape}x{DFFFD}${symbol_escape}${symbol_escape}x{E1000}-${symbol_escape}${symbol_escape}x{EFFFD}!${symbol_escape}${symbol_escape}${symbol_dollar}&'${symbol_escape}${symbol_escape}(${symbol_escape}${symbol_escape})${symbol_escape}${symbol_escape}*${symbol_escape}${symbol_escape}+,;=])*)(?::[0-9]*)?(?:${symbol_escape}${symbol_escape}/(?:(?:%[0-9a-f][0-9a-f]|[-a-z0-9${symbol_escape}${symbol_escape}._~${symbol_escape}${symbol_escape}x{A0}-${symbol_escape}${symbol_escape}x{D7FF}${symbol_escape}${symbol_escape}x{F900}-${symbol_escape}${symbol_escape}x{FDCF}${symbol_escape}${symbol_escape}x{FDF0}-${symbol_escape}${symbol_escape}x{FFEF}${symbol_escape}${symbol_escape}x{10000}-${symbol_escape}${symbol_escape}x{1FFFD}${symbol_escape}${symbol_escape}x{20000}-${symbol_escape}${symbol_escape}x{2FFFD}${symbol_escape}${symbol_escape}x{30000}-${symbol_escape}${symbol_escape}x{3FFFD}${symbol_escape}${symbol_escape}x{40000}-${symbol_escape}${symbol_escape}x{4FFFD}${symbol_escape}${symbol_escape}x{50000}-${symbol_escape}${symbol_escape}x{5FFFD}${symbol_escape}${symbol_escape}x{60000}-${symbol_escape}${symbol_escape}x{6FFFD}${symbol_escape}${symbol_escape}x{70000}-${symbol_escape}${symbol_escape}x{7FFFD}${symbol_escape}${symbol_escape}x{80000}-${symbol_escape}${symbol_escape}x{8FFFD}${symbol_escape}${symbol_escape}x{90000}-${symbol_escape}${symbol_escape}x{9FFFD}${symbol_escape}${symbol_escape}x{A0000}-${symbol_escape}${symbol_escape}x{AFFFD}${symbol_escape}${symbol_escape}x{B0000}-${symbol_escape}${symbol_escape}x{BFFFD}${symbol_escape}${symbol_escape}x{C0000}-${symbol_escape}${symbol_escape}x{CFFFD}${symbol_escape}${symbol_escape}x{D0000}-${symbol_escape}${symbol_escape}x{DFFFD}${symbol_escape}${symbol_escape}x{E1000}-${symbol_escape}${symbol_escape}x{EFFFD}!${symbol_escape}${symbol_escape}${symbol_dollar}&'${symbol_escape}${symbol_escape}(${symbol_escape}${symbol_escape})${symbol_escape}${symbol_escape}*${symbol_escape}${symbol_escape}+,;=:@]))*)*|${symbol_escape}${symbol_escape}/(?:(?:(?:(?:%[0-9a-f][0-9a-f]|[-a-z0-9${symbol_escape}${symbol_escape}._~${symbol_escape}${symbol_escape}x{A0}-${symbol_escape}${symbol_escape}x{D7FF}${symbol_escape}${symbol_escape}x{F900}-${symbol_escape}${symbol_escape}x{FDCF}${symbol_escape}${symbol_escape}x{FDF0}-${symbol_escape}${symbol_escape}x{FFEF}${symbol_escape}${symbol_escape}x{10000}-${symbol_escape}${symbol_escape}x{1FFFD}${symbol_escape}${symbol_escape}x{20000}-${symbol_escape}${symbol_escape}x{2FFFD}${symbol_escape}${symbol_escape}x{30000}-${symbol_escape}${symbol_escape}x{3FFFD}${symbol_escape}${symbol_escape}x{40000}-${symbol_escape}${symbol_escape}x{4FFFD}${symbol_escape}${symbol_escape}x{50000}-${symbol_escape}${symbol_escape}x{5FFFD}${symbol_escape}${symbol_escape}x{60000}-${symbol_escape}${symbol_escape}x{6FFFD}${symbol_escape}${symbol_escape}x{70000}-${symbol_escape}${symbol_escape}x{7FFFD}${symbol_escape}${symbol_escape}x{80000}-${symbol_escape}${symbol_escape}x{8FFFD}${symbol_escape}${symbol_escape}x{90000}-${symbol_escape}${symbol_escape}x{9FFFD}${symbol_escape}${symbol_escape}x{A0000}-${symbol_escape}${symbol_escape}x{AFFFD}${symbol_escape}${symbol_escape}x{B0000}-${symbol_escape}${symbol_escape}x{BFFFD}${symbol_escape}${symbol_escape}x{C0000}-${symbol_escape}${symbol_escape}x{CFFFD}${symbol_escape}${symbol_escape}x{D0000}-${symbol_escape}${symbol_escape}x{DFFFD}${symbol_escape}${symbol_escape}x{E1000}-${symbol_escape}${symbol_escape}x{EFFFD}!${symbol_escape}${symbol_escape}${symbol_dollar}&'${symbol_escape}${symbol_escape}(${symbol_escape}${symbol_escape})${symbol_escape}${symbol_escape}*${symbol_escape}${symbol_escape}+,;=:@]))+)(?:${symbol_escape}${symbol_escape}/(?:(?:%[0-9a-f][0-9a-f]|[-a-z0-9${symbol_escape}${symbol_escape}._~${symbol_escape}${symbol_escape}x{A0}-${symbol_escape}${symbol_escape}x{D7FF}${symbol_escape}${symbol_escape}x{F900}-${symbol_escape}${symbol_escape}x{FDCF}${symbol_escape}${symbol_escape}x{FDF0}-${symbol_escape}${symbol_escape}x{FFEF}${symbol_escape}${symbol_escape}x{10000}-${symbol_escape}${symbol_escape}x{1FFFD}${symbol_escape}${symbol_escape}x{20000}-${symbol_escape}${symbol_escape}x{2FFFD}${symbol_escape}${symbol_escape}x{30000}-${symbol_escape}${symbol_escape}x{3FFFD}${symbol_escape}${symbol_escape}x{40000}-${symbol_escape}${symbol_escape}x{4FFFD}${symbol_escape}${symbol_escape}x{50000}-${symbol_escape}${symbol_escape}x{5FFFD}${symbol_escape}${symbol_escape}x{60000}-${symbol_escape}${symbol_escape}x{6FFFD}${symbol_escape}${symbol_escape}x{70000}-${symbol_escape}${symbol_escape}x{7FFFD}${symbol_escape}${symbol_escape}x{80000}-${symbol_escape}${symbol_escape}x{8FFFD}${symbol_escape}${symbol_escape}x{90000}-${symbol_escape}${symbol_escape}x{9FFFD}${symbol_escape}${symbol_escape}x{A0000}-${symbol_escape}${symbol_escape}x{AFFFD}${symbol_escape}${symbol_escape}x{B0000}-${symbol_escape}${symbol_escape}x{BFFFD}${symbol_escape}${symbol_escape}x{C0000}-${symbol_escape}${symbol_escape}x{CFFFD}${symbol_escape}${symbol_escape}x{D0000}-${symbol_escape}${symbol_escape}x{DFFFD}${symbol_escape}${symbol_escape}x{E1000}-${symbol_escape}${symbol_escape}x{EFFFD}!${symbol_escape}${symbol_escape}${symbol_dollar}&'${symbol_escape}${symbol_escape}(${symbol_escape}${symbol_escape})${symbol_escape}${symbol_escape}*${symbol_escape}${symbol_escape}+,;=:@]))*)*)?|(?:(?:(?:%[0-9a-f][0-9a-f]|[-a-z0-9${symbol_escape}${symbol_escape}._~${symbol_escape}${symbol_escape}x{A0}-${symbol_escape}${symbol_escape}x{D7FF}${symbol_escape}${symbol_escape}x{F900}-${symbol_escape}${symbol_escape}x{FDCF}${symbol_escape}${symbol_escape}x{FDF0}-${symbol_escape}${symbol_escape}x{FFEF}${symbol_escape}${symbol_escape}x{10000}-${symbol_escape}${symbol_escape}x{1FFFD}${symbol_escape}${symbol_escape}x{20000}-${symbol_escape}${symbol_escape}x{2FFFD}${symbol_escape}${symbol_escape}x{30000}-${symbol_escape}${symbol_escape}x{3FFFD}${symbol_escape}${symbol_escape}x{40000}-${symbol_escape}${symbol_escape}x{4FFFD}${symbol_escape}${symbol_escape}x{50000}-${symbol_escape}${symbol_escape}x{5FFFD}${symbol_escape}${symbol_escape}x{60000}-${symbol_escape}${symbol_escape}x{6FFFD}${symbol_escape}${symbol_escape}x{70000}-${symbol_escape}${symbol_escape}x{7FFFD}${symbol_escape}${symbol_escape}x{80000}-${symbol_escape}${symbol_escape}x{8FFFD}${symbol_escape}${symbol_escape}x{90000}-${symbol_escape}${symbol_escape}x{9FFFD}${symbol_escape}${symbol_escape}x{A0000}-${symbol_escape}${symbol_escape}x{AFFFD}${symbol_escape}${symbol_escape}x{B0000}-${symbol_escape}${symbol_escape}x{BFFFD}${symbol_escape}${symbol_escape}x{C0000}-${symbol_escape}${symbol_escape}x{CFFFD}${symbol_escape}${symbol_escape}x{D0000}-${symbol_escape}${symbol_escape}x{DFFFD}${symbol_escape}${symbol_escape}x{E1000}-${symbol_escape}${symbol_escape}x{EFFFD}!${symbol_escape}${symbol_escape}${symbol_dollar}&'${symbol_escape}${symbol_escape}(${symbol_escape}${symbol_escape})${symbol_escape}${symbol_escape}*${symbol_escape}${symbol_escape}+,;=:@]))+)(?:${symbol_escape}${symbol_escape}/(?:(?:%[0-9a-f][0-9a-f]|[-a-z0-9${symbol_escape}${symbol_escape}._~${symbol_escape}${symbol_escape}x{A0}-${symbol_escape}${symbol_escape}x{D7FF}${symbol_escape}${symbol_escape}x{F900}-${symbol_escape}${symbol_escape}x{FDCF}${symbol_escape}${symbol_escape}x{FDF0}-${symbol_escape}${symbol_escape}x{FFEF}${symbol_escape}${symbol_escape}x{10000}-${symbol_escape}${symbol_escape}x{1FFFD}${symbol_escape}${symbol_escape}x{20000}-${symbol_escape}${symbol_escape}x{2FFFD}${symbol_escape}${symbol_escape}x{30000}-${symbol_escape}${symbol_escape}x{3FFFD}${symbol_escape}${symbol_escape}x{40000}-${symbol_escape}${symbol_escape}x{4FFFD}${symbol_escape}${symbol_escape}x{50000}-${symbol_escape}${symbol_escape}x{5FFFD}${symbol_escape}${symbol_escape}x{60000}-${symbol_escape}${symbol_escape}x{6FFFD}${symbol_escape}${symbol_escape}x{70000}-${symbol_escape}${symbol_escape}x{7FFFD}${symbol_escape}${symbol_escape}x{80000}-${symbol_escape}${symbol_escape}x{8FFFD}${symbol_escape}${symbol_escape}x{90000}-${symbol_escape}${symbol_escape}x{9FFFD}${symbol_escape}${symbol_escape}x{A0000}-${symbol_escape}${symbol_escape}x{AFFFD}${symbol_escape}${symbol_escape}x{B0000}-${symbol_escape}${symbol_escape}x{BFFFD}${symbol_escape}${symbol_escape}x{C0000}-${symbol_escape}${symbol_escape}x{CFFFD}${symbol_escape}${symbol_escape}x{D0000}-${symbol_escape}${symbol_escape}x{DFFFD}${symbol_escape}${symbol_escape}x{E1000}-${symbol_escape}${symbol_escape}x{EFFFD}!${symbol_escape}${symbol_escape}${symbol_dollar}&'${symbol_escape}${symbol_escape}(${symbol_escape}${symbol_escape})${symbol_escape}${symbol_escape}*${symbol_escape}${symbol_escape}+,;=:@]))*)*|(?!(?:%[0-9a-f][0-9a-f]|[-a-z0-9${symbol_escape}${symbol_escape}._~${symbol_escape}${symbol_escape}x{A0}-${symbol_escape}${symbol_escape}x{D7FF}${symbol_escape}${symbol_escape}x{F900}-${symbol_escape}${symbol_escape}x{FDCF}${symbol_escape}${symbol_escape}x{FDF0}-${symbol_escape}${symbol_escape}x{FFEF}${symbol_escape}${symbol_escape}x{10000}-${symbol_escape}${symbol_escape}x{1FFFD}${symbol_escape}${symbol_escape}x{20000}-${symbol_escape}${symbol_escape}x{2FFFD}${symbol_escape}${symbol_escape}x{30000}-${symbol_escape}${symbol_escape}x{3FFFD}${symbol_escape}${symbol_escape}x{40000}-${symbol_escape}${symbol_escape}x{4FFFD}${symbol_escape}${symbol_escape}x{50000}-${symbol_escape}${symbol_escape}x{5FFFD}${symbol_escape}${symbol_escape}x{60000}-${symbol_escape}${symbol_escape}x{6FFFD}${symbol_escape}${symbol_escape}x{70000}-${symbol_escape}${symbol_escape}x{7FFFD}${symbol_escape}${symbol_escape}x{80000}-${symbol_escape}${symbol_escape}x{8FFFD}${symbol_escape}${symbol_escape}x{90000}-${symbol_escape}${symbol_escape}x{9FFFD}${symbol_escape}${symbol_escape}x{A0000}-${symbol_escape}${symbol_escape}x{AFFFD}${symbol_escape}${symbol_escape}x{B0000}-${symbol_escape}${symbol_escape}x{BFFFD}${symbol_escape}${symbol_escape}x{C0000}-${symbol_escape}${symbol_escape}x{CFFFD}${symbol_escape}${symbol_escape}x{D0000}-${symbol_escape}${symbol_escape}x{DFFFD}${symbol_escape}${symbol_escape}x{E1000}-${symbol_escape}${symbol_escape}x{EFFFD}!${symbol_escape}${symbol_escape}${symbol_dollar}&'${symbol_escape}${symbol_escape}(${symbol_escape}${symbol_escape})${symbol_escape}${symbol_escape}*${symbol_escape}${symbol_escape}+,;=:@])))(?:${symbol_escape}${symbol_escape}?(?:(?:%[0-9a-f][0-9a-f]|[-a-z0-9${symbol_escape}${symbol_escape}._~${symbol_escape}${symbol_escape}x{A0}-${symbol_escape}${symbol_escape}x{D7FF}${symbol_escape}${symbol_escape}x{F900}-${symbol_escape}${symbol_escape}x{FDCF}${symbol_escape}${symbol_escape}x{FDF0}-${symbol_escape}${symbol_escape}x{FFEF}${symbol_escape}${symbol_escape}x{10000}-${symbol_escape}${symbol_escape}x{1FFFD}${symbol_escape}${symbol_escape}x{20000}-${symbol_escape}${symbol_escape}x{2FFFD}${symbol_escape}${symbol_escape}x{30000}-${symbol_escape}${symbol_escape}x{3FFFD}${symbol_escape}${symbol_escape}x{40000}-${symbol_escape}${symbol_escape}x{4FFFD}${symbol_escape}${symbol_escape}x{50000}-${symbol_escape}${symbol_escape}x{5FFFD}${symbol_escape}${symbol_escape}x{60000}-${symbol_escape}${symbol_escape}x{6FFFD}${symbol_escape}${symbol_escape}x{70000}-${symbol_escape}${symbol_escape}x{7FFFD}${symbol_escape}${symbol_escape}x{80000}-${symbol_escape}${symbol_escape}x{8FFFD}${symbol_escape}${symbol_escape}x{90000}-${symbol_escape}${symbol_escape}x{9FFFD}${symbol_escape}${symbol_escape}x{A0000}-${symbol_escape}${symbol_escape}x{AFFFD}${symbol_escape}${symbol_escape}x{B0000}-${symbol_escape}${symbol_escape}x{BFFFD}${symbol_escape}${symbol_escape}x{C0000}-${symbol_escape}${symbol_escape}x{CFFFD}${symbol_escape}${symbol_escape}x{D0000}-${symbol_escape}${symbol_escape}x{DFFFD}${symbol_escape}${symbol_escape}x{E1000}-${symbol_escape}${symbol_escape}x{EFFFD}!${symbol_escape}${symbol_escape}${symbol_dollar}&'${symbol_escape}${symbol_escape}(${symbol_escape}${symbol_escape})${symbol_escape}${symbol_escape}*${symbol_escape}${symbol_escape}+,;=:@])|[${symbol_escape}${symbol_escape}x{E000}-${symbol_escape}${symbol_escape}x{F8FF}${symbol_escape}${symbol_escape}x{F0000}-${symbol_escape}${symbol_escape}x{FFFFD}${symbol_escape}${symbol_escape}x{100000}-${symbol_escape}${symbol_escape}x{10FFFD}${symbol_escape}${symbol_escape}/${symbol_escape}${symbol_escape}?])*)?(?:${symbol_escape}${symbol_escape}${symbol_pound}(?:(?:%[0-9a-f][0-9a-f]|[-a-z0-9${symbol_escape}${symbol_escape}._~${symbol_escape}${symbol_escape}x{A0}-${symbol_escape}${symbol_escape}x{D7FF}${symbol_escape}${symbol_escape}x{F900}-${symbol_escape}${symbol_escape}x{FDCF}${symbol_escape}${symbol_escape}x{FDF0}-${symbol_escape}${symbol_escape}x{FFEF}${symbol_escape}${symbol_escape}x{10000}-${symbol_escape}${symbol_escape}x{1FFFD}${symbol_escape}${symbol_escape}x{20000}-${symbol_escape}${symbol_escape}x{2FFFD}${symbol_escape}${symbol_escape}x{30000}-${symbol_escape}${symbol_escape}x{3FFFD}${symbol_escape}${symbol_escape}x{40000}-${symbol_escape}${symbol_escape}x{4FFFD}${symbol_escape}${symbol_escape}x{50000}-${symbol_escape}${symbol_escape}x{5FFFD}${symbol_escape}${symbol_escape}x{60000}-${symbol_escape}${symbol_escape}x{6FFFD}${symbol_escape}${symbol_escape}x{70000}-${symbol_escape}${symbol_escape}x{7FFFD}${symbol_escape}${symbol_escape}x{80000}-${symbol_escape}${symbol_escape}x{8FFFD}${symbol_escape}${symbol_escape}x{90000}-${symbol_escape}${symbol_escape}x{9FFFD}${symbol_escape}${symbol_escape}x{A0000}-${symbol_escape}${symbol_escape}x{AFFFD}${symbol_escape}${symbol_escape}x{B0000}-${symbol_escape}${symbol_escape}x{BFFFD}${symbol_escape}${symbol_escape}x{C0000}-${symbol_escape}${symbol_escape}x{CFFFD}${symbol_escape}${symbol_escape}x{D0000}-${symbol_escape}${symbol_escape}x{DFFFD}${symbol_escape}${symbol_escape}x{E1000}-${symbol_escape}${symbol_escape}x{EFFFD}!${symbol_escape}${symbol_escape}${symbol_dollar}&'${symbol_escape}${symbol_escape}(${symbol_escape}${symbol_escape})${symbol_escape}${symbol_escape}*${symbol_escape}${symbol_escape}+,;=:@])|[${symbol_escape}${symbol_escape}/${symbol_escape}${symbol_escape}?])*)?${symbol_dollar}";
    private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX);
    public static boolean isValidUrl(String url) {
        return StringUtils.isNotBlank(url) &&
                URL_PATTERN.matcher(url.toLowerCase()).matches();
    }
}
