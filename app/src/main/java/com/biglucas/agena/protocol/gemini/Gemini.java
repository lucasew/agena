package com.biglucas.agena.protocol.gemini;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.biglucas.agena.R;
import com.biglucas.agena.utils.DatabaseController;
import com.biglucas.agena.utils.Invoker;
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
 *     <li>File downloads respecting Android's storage APIs.</li>
 * </ul>
 */
public class Gemini {
    private static final String TAG = "Gemini";

    private String readLineFromStream(InputStream input) throws IOException, FailedGeminiRequestException {
        ArrayList<Byte> bytes = new ArrayList<>();
        int b = input.read();
        if (b == -1) {
            return null;
        }
        while (b != -1 && b != 0xA) {
            bytes.add((byte) b);
            if (bytes.size() > GeminiSpec.MAX_LINE_LENGTH_BYTES) {
                throw new FailedGeminiRequestException.GeminiResponseTooLarge("Line length exceeded limit");
            }
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
     * @param activity The context used for launching intents or showing Toasts.
     * @param uri The Gemini URI to request.
     * @return A list of strings representing the response body (if text/gemini), or empty if handled otherwise.
     * @throws IOException If a network error occurs.
     * @throws FailedGeminiRequestException If the protocol returns an error status.
     * @throws NoSuchAlgorithmException If hashing algorithms are missing.
     * @throws KeyManagementException If SSL setup fails.
     */
    public List<String> request(Activity activity, Uri uri) throws IOException, FailedGeminiRequestException, NoSuchAlgorithmException, KeyManagementException {
        // Validate URI according to Gemini spec
        validateUri(uri);
        // Start request with redirect counter at 0
        return requestInternal(activity, uri, 0);
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
    private List<String> requestInternal(Activity activity, Uri uri, int redirectCount) throws IOException, FailedGeminiRequestException, NoSuchAlgorithmException, KeyManagementException {
        Log.i(TAG, "Requesting: '" + uri.toString() + "' (redirect count: " + redirectCount + ")");

        // Check redirect limit (max 5 as per spec)
        if (redirectCount > GeminiSpec.MAX_REDIRECTS) {
            throw new FailedGeminiRequestException.GeminiTooManyRedirects();
        }

        if (uri.getScheme() == null || !uri.getScheme().equals("gemini")) {
            Invoker.invokeNewWindow(activity, uri);
            return new ArrayList<>();
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

        BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());

        String cleanedEntity = uri.toString().replace("%2F", "/").trim();
        String requestEntity = cleanedEntity + "\r\n";

        outputStream.write(requestEntity.getBytes());
        outputStream.flush();

        InputStream inputStream = new BufferedInputStream(socket.getInputStream());
        String headerLine = readLineFromStream(inputStream);
        if (headerLine == null) {
            Log.i(TAG, "Server did not respond with a Gemini header");
            inputStream.close();
            outputStream.close();
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
            inputStream.close();
            outputStream.close();
            throw new FailedGeminiRequestException.GeminiInvalidResponse();
        }

        Log.i(TAG, "response_code=" + responseCode + ", meta=" + meta);

        // Handle response based on status code ranges
        try {
            return handleResponse(activity, uri, inputStream, outputStream, responseCode, meta, cleanedEntity, redirectCount);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                // Ignore close errors
            }
            try {
                outputStream.close();
            } catch (IOException e) {
                // Ignore close errors
            }
        }
    }

    /**
     * Dispatches logic based on the Gemini status code family.
     * <p>
     * - 1x (Input): Throws exception to prompt user.
     * - 2x (Success): Reads body. If text/gemini, returns lines. If binary, downloads it.
     * - 3x (Redirect): Recurses into `requestInternal` with new URI.
     * - 4x/5x (Failure): Throws specific exceptions.
     * - 6x (Client Cert): Throws auth exception.
     */
    private List<String> handleResponse(Activity activity, Uri uri, InputStream inputStream,
                                        BufferedOutputStream outputStream, int responseCode,
                                        String meta, String cleanedEntity, int redirectCount)
            throws IOException, FailedGeminiRequestException, NoSuchAlgorithmException, KeyManagementException {

        // Input required (10-19)
        if (GeminiSpec.isInput(responseCode)) {
            boolean sensitive = (responseCode == GeminiSpec.STATUS_SENSITIVE_INPUT);
            throw new FailedGeminiRequestException.GeminiInputRequired(meta, sensitive);
        }

        // Success (20-29)
        if (GeminiSpec.isSuccess(responseCode)) {
            List<String> lines = new ArrayList<>();
            if (meta.startsWith("text/gemini")) {
                long totalBytes = 0;
                while (true) {
                    String line = readLineFromStream(inputStream);
                    if (line == null) {
                        break;
                    }
                    // Estimate size (UTF-8 bytes)
                    totalBytes += line.length();
                    if (totalBytes > GeminiSpec.MAX_RESPONSE_BODY_SIZE_BYTES) {
                        throw new FailedGeminiRequestException.GeminiResponseTooLarge("Response body exceeded limit");
                    }
                    Collections.addAll(lines, line.split("\n"));
                }
            } else {
                // Download to public Downloads folder
                GeminiDownloader downloader = new GeminiDownloader();
                GeminiDownloader.Result result = downloader.download(activity, inputStream, Uri.parse(cleanedEntity), meta);
                if (result != null) {
                    activity.runOnUiThread(() -> Toast.makeText(activity, result.displayPath, Toast.LENGTH_SHORT).show());
                    Intent intent = new Intent();
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setDataAndType(result.uri, meta);
                    activity.startActivity(intent);
                } else {
                    // Permission was denied, show retry message
                    activity.runOnUiThread(() -> Toast.makeText(activity, activity.getResources().getString(R.string.please_repeat_action), Toast.LENGTH_SHORT).show());
                }
            }
            try {
                new DatabaseController(DatabaseController.openDatabase(activity))
                        .addHistoryEntry(uri);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save history for URI: " + uri, e);
                activity.runOnUiThread(() -> Toast.makeText(activity, R.string.error_database_write, Toast.LENGTH_SHORT).show());
            }
            return lines;
        }

        // Redirect (30-39)
        if (GeminiSpec.isRedirect(responseCode)) {
            if (meta.isEmpty()) {
                throw new FailedGeminiRequestException.GeminiInvalidResponse();
            }
            // Resolve relative URIs against the current request URI (RFC 3986)
            URI currentUri = URI.create(uri.toString());
            URI resolvedUri = currentUri.resolve(meta.trim());
            Uri redirectUri = Uri.parse(resolvedUri.toString());
            validateUri(redirectUri);
            return requestInternal(activity, redirectUri, redirectCount + 1);
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
