#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.service.common;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

public interface FileStore extends Closeable {
    Path toPath(String path);
    void close();
    long size(String path) throws IOException;
    List<String> list(String path, String regex, boolean recursive) throws IOException;
    void createDirectories(String path) throws IOException;
    long write(String path, InputStream stream, Long contentLength, Instant lastModified) throws IOException;
    default long write(String path, InputStream stream) throws IOException {
        return write(path, stream, null, null);
    }
    default long write(String path, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return write(path, new ByteArrayInputStream(bytes), (long) bytes.length, Instant.now());
    }
    OutputStream newOutputStream(String path) throws IOException;
    InputStream newInputStream(String path) throws IOException;
    default byte[] readBytes(String path) throws IOException {
        InputStream stream = newInputStream(path);
        return stream != null ? IOUtils.toByteArray(stream) : null;
    }
    default String readString(String path) throws IOException {
        InputStream stream = newInputStream(path);
        return stream != null ? IOUtils.toString(stream, StandardCharsets.UTF_8) : null;
    }
    boolean exists(String path) throws IOException;
    void delete(String path) throws IOException;
    default void tryDelete(String path) {
        try {
            delete(path);
        } catch (IOException ignored) {
            // ignored
        }
    }
    default void copy(String sourcePath, String destinationPath) throws IOException {
        write(destinationPath, newInputStream(sourcePath));
    }
    default void move(String sourcePath, String destinationPath) throws IOException {
        copy(sourcePath, destinationPath);
        delete(sourcePath);
    }
    long getLastModifiedTime(String path) throws IOException;
    default String generateShareUrl(String path, int expireTimeInSecond) {
        return null;
    }
}
