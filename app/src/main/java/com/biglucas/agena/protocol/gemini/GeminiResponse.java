package com.biglucas.agena.protocol.gemini;

import android.net.Uri;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.List;

/**
 * Represents the result of a successful Gemini request (2x status).
 * <p>
 * This class encapsulates the response metadata and body.
 * It implements {@link Closeable} to ensure network resources (socket, streams)
 * are properly released when the response is consumed.
 */
public class GeminiResponse implements Closeable {
    private final Uri uri;
    private final String meta;
    private final List<String> textBody;
    private final InputStream inputStream;
    private final Socket socket;

    /**
     * Constructs a GeminiResponse.
     *
     * @param uri The final URI of the response (after redirects).
     * @param meta The meta string (MIME type).
     * @param textBody The text content if the response is text/gemini (otherwise null).
     * @param inputStream The input stream for binary content (otherwise null).
     * @param socket The underlying socket connection (must be closed by the caller).
     */
    public GeminiResponse(Uri uri, String meta, List<String> textBody, InputStream inputStream, Socket socket) {
        this.uri = uri;
        this.meta = meta;
        this.textBody = textBody;
        this.inputStream = inputStream;
        this.socket = socket;
    }

    public Uri getUri() {
        return uri;
    }

    public String getMeta() {
        return meta;
    }

    /**
     * @return The lines of text content if the response is text/gemini, null otherwise.
     */
    public List<String> getTextBody() {
        return textBody;
    }

    /**
     * @return The input stream for binary content if the response is not text/gemini, null otherwise.
     * The caller is responsible for closing this stream via {@link #close()}.
     */
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * Checks if the response contains text body (text/gemini).
     */
    public boolean isText() {
        return textBody != null;
    }

    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
