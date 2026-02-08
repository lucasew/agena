package com.biglucas.agena.protocol.gemini;

import android.net.Uri;
import android.util.Log;

import com.biglucas.agena.utils.SSLSocketFactorySingleton;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.SSLSocket;

/**
 * Core client implementation for the Gemini protocol (gemini://).
 * <p>
 * This class handles the low-level details of the Gemini protocol, including:
 * <ul>
 *     <li>TLS connection establishment (with mandatory SNI).</li>
 *     <li>Request formatting (`gemini://<host>/<path>\r\n`).</li>
 *     <li>Response header parsing (Status + Meta).</li>
 *     <li>Status code logic dispatch (Success, Redirect, Input, Error).</li>
 * </ul>
 */
public class Gemini {
    private static final String TAG = "Gemini";

    private String readLineFromStream(InputStream input) throws IOException {
        ArrayList<Byte> bytes = new ArrayList<>();
        int b = input.read();
        if (b == -1) {
            return null;
        }
        while (b != -1 && b != 0xA) {
            bytes.add((byte) b);
            b = input.read();
        }
        byte[] buf = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            buf[i] = bytes.get(i);
        }
        return Charset.defaultCharset().decode(ByteBuffer.wrap(buf)).toString();
    }

    /**
     * Public entry point for initiating a Gemini request.
     * <p>
     * Validates the URI against the Gemini spec and delegates to {@link #requestInternal}
     * to handle the request lifecycle, including redirect following.
     *
     * @param uri The Gemini URI to request.
     * @return A {@link GeminiResponse} containing the response body (text lines or input stream).
     * @throws IOException If a network error occurs.
     * @throws FailedGeminiRequestException If the protocol returns an error status.
     * @throws NoSuchAlgorithmException If hashing algorithms are missing.
     * @throws KeyManagementException If SSL setup fails.
     */
    public GeminiResponse request(Uri uri) throws IOException, FailedGeminiRequestException, NoSuchAlgorithmException, KeyManagementException {
        // Validate URI according to Gemini spec
        validateUri(uri);
        // Start request with redirect counter at 0
        return requestInternal(uri, 0);
    }

    /**
     * Validates URI according to Gemini protocol specification
     */
    private void validateUri(Uri uri) throws FailedGeminiRequestException {
        String uriString = uri.toString();

        // Check maximum URI length (1024 bytes as per spec)
        if (uriString.getBytes().length > GeminiSpec.MAX_URI_LENGTH_BYTES) {
            throw new FailedGeminiRequestException.GeminiInvalidUri("URI exceeds maximum length of " + GeminiSpec.MAX_URI_LENGTH_BYTES + " bytes");
        }

        // Check for userinfo (not allowed in Gemini URIs)
        if (uri.getUserInfo() != null && !uri.getUserInfo().isEmpty()) {
            throw new FailedGeminiRequestException.GeminiInvalidUri("Userinfo not allowed in Gemini URIs");
        }

        // Validate scheme
        if (uri.getScheme() == null || !uri.getScheme().equals("gemini")) {
            throw new FailedGeminiRequestException.GeminiInvalidUri("Invalid scheme: " + uri.getScheme());
        }
    }

    /**
     * Executes the raw network request.
     * <p>
     * 1. Establishes a TLS connection (SNI is explicitly enabled as required by Gemini).
     * 2. Sends the request line (`<URL>\r\n`).
     * 3. Parses the response header (`<STATUS> <META>`).
     * 4. Delegates body handling to {@link #handleResponse} based on the status code.
     *
     * @param redirectCount Current depth of recursion for redirects (max 5).
     */
    private GeminiResponse requestInternal(Uri uri, int redirectCount) throws IOException, FailedGeminiRequestException, NoSuchAlgorithmException, KeyManagementException {
        Log.i(TAG, "Requesting: '" + uri.toString() + "' (redirect count: " + redirectCount + ")");

        // Check redirect limit (max 5 as per spec)
        if (redirectCount > GeminiSpec.MAX_REDIRECTS) {
            throw new FailedGeminiRequestException.GeminiTooManyRedirects();
        }

        int port = uri.getPort();
        if (port == -1) {
            port = GeminiSpec.DEFAULT_PORT;
        }

        SSLSocket socket = (SSLSocket) SSLSocketFactorySingleton
                .getSSLSocketFactory()
                .createSocket();

        // Enable SNI (Server Name Indication) as required by Gemini spec
        // This is enabled by default on Android, but we set it explicitly to be certain
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            javax.net.ssl.SNIHostName serverName = new javax.net.ssl.SNIHostName(uri.getHost());
            javax.net.ssl.SSLParameters params = socket.getSSLParameters();
            params.setServerNames(java.util.Collections.singletonList(serverName));
            socket.setSSLParameters(params);
        }

        // TODO: configurable timeout
        socket.connect(new InetSocketAddress(uri.getHost(), port), GeminiSpec.DEFAULT_TIMEOUT_MS);
        socket.setSoTimeout(GeminiSpec.DEFAULT_TIMEOUT_MS);
        socket.startHandshake();

        BufferedOutputStream outputStream = null;
        InputStream inputStream = null;

        try {
            outputStream = new BufferedOutputStream(socket.getOutputStream());

            String cleanedEntity = uri.toString().replace("%2F", "/").trim();
            String requestEntity = cleanedEntity + "\r\n";

            outputStream.write(requestEntity.getBytes());
            outputStream.flush();

            inputStream = new BufferedInputStream(socket.getInputStream());
            String headerLine = readLineFromStream(inputStream);
            if (headerLine == null) {
                Log.i(TAG, "Server did not respond with a Gemini header");
                throw new FailedGeminiRequestException.GeminiInvalidResponse();
            }

            // Parse response code and meta
            int responseCode;
            String meta;
            try {
                int spaceIndex = headerLine.indexOf(" ");
                if (spaceIndex == -1) {
                    // No meta field, just status code
                    responseCode = Integer.parseInt(headerLine.trim());
                    meta = "";
                } else {
                    responseCode = Integer.parseInt(headerLine.substring(0, spaceIndex));
                    meta = headerLine.substring(spaceIndex).trim();
                }
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                throw new FailedGeminiRequestException.GeminiInvalidResponse();
            }

            Log.i(TAG, "response_code=" + responseCode + ", meta=" + meta);

            // Handle response based on status code ranges
            return handleResponse(uri, inputStream, outputStream, socket, responseCode, meta, redirectCount);

        } catch (Exception e) {
            // Close resources on error
            if (inputStream != null) {
                try { inputStream.close(); } catch (IOException ignored) {}
            }
            if (outputStream != null) {
                try { outputStream.close(); } catch (IOException ignored) {}
            }
            if (!socket.isClosed()) {
                try { socket.close(); } catch (IOException ignored) {}
            }
            throw e;
        }
    }

    /**
     * Dispatches logic based on the Gemini status code family.
     * <p>
     * - 1x (Input): Throws exception to prompt user.
     * - 2x (Success): Reads body. If text/gemini, returns lines. If binary, returns stream.
     * - 3x (Redirect): Recurses into `requestInternal` with new URI.
     * - 4x/5x (Failure): Throws specific exceptions.
     * - 6x (Client Cert): Throws auth exception.
     */
    private GeminiResponse handleResponse(Uri uri, InputStream inputStream,
                                        BufferedOutputStream outputStream, SSLSocket socket, int responseCode,
                                        String meta, int redirectCount)
            throws IOException, FailedGeminiRequestException, NoSuchAlgorithmException, KeyManagementException {

        // Input required (10-19)
        if (GeminiSpec.isInput(responseCode)) {
            boolean sensitive = (responseCode == GeminiSpec.STATUS_SENSITIVE_INPUT);
            throw new FailedGeminiRequestException.GeminiInputRequired(meta, sensitive);
        }

        // Success (20-29)
        if (GeminiSpec.isSuccess(responseCode)) {
            if (meta.startsWith("text/gemini")) {
                List<String> lines = new ArrayList<>();
                while (true) {
                    String line = readLineFromStream(inputStream);
                    if (line == null) {
                        break;
                    }
                    Collections.addAll(lines, line.split("\n"));
                }
                // Close resources for text response as we have consumed the stream
                inputStream.close();
                outputStream.close();
                socket.close();
                return new GeminiResponse(uri, meta, lines, null, null);
            } else {
                // Return open stream for binary content
                return new GeminiResponse(uri, meta, null, inputStream, socket);
            }
        }

        // Redirect (30-39)
        if (GeminiSpec.isRedirect(responseCode)) {
            if (meta.isEmpty()) {
                throw new FailedGeminiRequestException.GeminiInvalidResponse();
            }
            // Close resources before following redirect
            inputStream.close();
            outputStream.close();
            socket.close();

            // Resolve relative URIs against the current request URI (RFC 3986)
            URI currentUri = URI.create(uri.toString());
            URI resolvedUri = currentUri.resolve(meta.trim());
            Uri redirectUri = Uri.parse(resolvedUri.toString());
            validateUri(redirectUri);
            return requestInternal(redirectUri, redirectCount + 1);
        }

        // Temporary failure (40-49)
        if (GeminiSpec.isTemporaryFailure(responseCode)) {
            switch (responseCode) {
                case GeminiSpec.STATUS_SERVER_UNAVAILABLE:
                    throw new FailedGeminiRequestException.GeminiServerUnavailable(meta);
                case GeminiSpec.STATUS_CGI_ERROR:
                    throw new FailedGeminiRequestException.GeminiCGIError(meta);
                case GeminiSpec.STATUS_PROXY_ERROR:
                    throw new FailedGeminiRequestException.GeminiProxyError(meta);
                case GeminiSpec.STATUS_SLOW_DOWN:
                    throw new FailedGeminiRequestException.GeminiSlowDown(meta);
                default:
                    throw new FailedGeminiRequestException.GeminiTemporaryFailure(meta);
            }
        }

        // Permanent failure (50-59)
        if (GeminiSpec.isPermanentFailure(responseCode)) {
            switch (responseCode) {
                case GeminiSpec.STATUS_NOT_FOUND:
                    throw new FailedGeminiRequestException.GeminiNotFound();
                case GeminiSpec.STATUS_GONE:
                    throw new FailedGeminiRequestException.GeminiGone();
                case GeminiSpec.STATUS_PROXY_REQUEST_REFUSED:
                    throw new FailedGeminiRequestException.GeminiProxyRequestRefused(meta);
                case GeminiSpec.STATUS_BAD_REQUEST:
                    throw new FailedGeminiRequestException.GeminiBadRequest(meta);
                default:
                    throw new FailedGeminiRequestException.GeminiPermanentFailure(meta);
            }
        }

        // Client certificate required (60-69)
        if (GeminiSpec.isClientCertificateRequired(responseCode)) {
            switch (responseCode) {
                case GeminiSpec.STATUS_CERT_NOT_AUTHORIZED:
                    throw new FailedGeminiRequestException.GeminiCertificateNotAuthorized(meta);
                case GeminiSpec.STATUS_CERT_NOT_VALID:
                    throw new FailedGeminiRequestException.GeminiCertificateNotValid(meta);
                default:
                    throw new FailedGeminiRequestException.GeminiClientCertificateRequired(meta);
            }
        }

        // Unknown status code
        Log.i(TAG, "Unknown response code: " + responseCode + ", meta: " + meta);
        throw new FailedGeminiRequestException.GeminiUnimplementedCase();
    }
}
