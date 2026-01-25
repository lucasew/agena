package com.biglucas.agena.protocol.gemini;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.biglucas.agena.R;
import com.biglucas.agena.utils.DatabaseController;
import com.biglucas.agena.utils.Invoker;
import com.biglucas.agena.utils.PermissionAsker;
import com.biglucas.agena.utils.SSLSocketFactorySingleton;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.KeyManagementException;
import java.security.MessageDigest;
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

    // Gemini Protocol Status Codes
    // Input
    private static final int STATUS_INPUT = 10;
    private static final int STATUS_SENSITIVE_INPUT = 11;
    // Success
    private static final int STATUS_SUCCESS = 20;
    // Redirect
    private static final int STATUS_REDIRECT = 30;
    // Temporary Failure
    private static final int STATUS_TEMP_FAILURE = 40;
    private static final int STATUS_SERVER_UNAVAILABLE = 41;
    private static final int STATUS_CGI_ERROR = 42;
    private static final int STATUS_PROXY_ERROR = 43;
    private static final int STATUS_SLOW_DOWN = 44;
    // Permanent Failure
    private static final int STATUS_PERM_FAILURE = 50;
    private static final int STATUS_NOT_FOUND = 51;
    private static final int STATUS_GONE = 52;
    private static final int STATUS_PROXY_REQUEST_REFUSED = 53;
    private static final int STATUS_BAD_REQUEST = 59;
    // Client Certificate Required
    private static final int STATUS_CLIENT_CERT_REQUIRED = 60;
    private static final int STATUS_CERT_NOT_AUTHORIZED = 61;
    private static final int STATUS_CERT_NOT_VALID = 62;


    /**
     * Result of a download operation containing URI and display path
     */
    private static class DownloadResult {
        final Uri uri;
        final String displayPath;

        DownloadResult(Uri uri, String displayPath) {
            this.uri = uri;
            this.displayPath = displayPath;
        }
    }

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
        if (uriString.getBytes().length > 1024) {
            throw new FailedGeminiRequestException.GeminiInvalidUri("URI exceeds maximum length of 1024 bytes");
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
        if (redirectCount > 5) {
            throw new FailedGeminiRequestException.GeminiTooManyRedirects();
        }

        if (uri.getScheme() == null || !uri.getScheme().equals("gemini")) {
            Invoker.invokeNewWindow(activity, uri);
            return new ArrayList<>();
        }

        int port = uri.getPort();
        if (port == -1) {
            port = 1965;
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
        socket.connect(new InetSocketAddress(uri.getHost(), port), 5 * 1000);
        socket.setSoTimeout(5 * 1000);
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
        if (responseCode >= STATUS_INPUT && responseCode < STATUS_SUCCESS) {
            boolean sensitive = (responseCode == STATUS_SENSITIVE_INPUT);
            throw new FailedGeminiRequestException.GeminiInputRequired(meta, sensitive);
        }

        // Success (20-29)
        if (responseCode >= STATUS_SUCCESS && responseCode < STATUS_REDIRECT) {
            List<String> lines = new ArrayList<>();
            if (meta.startsWith("text/gemini")) {
                while (true) {
                    String line = readLineFromStream(inputStream);
                    if (line == null) {
                        break;
                    }
                    Collections.addAll(lines, line.split("\n"));
                }
            } else {
                // Download to public Downloads folder
                DownloadResult result = download(activity, inputStream, Uri.parse(cleanedEntity), meta);
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
        if (responseCode >= STATUS_REDIRECT && responseCode < STATUS_TEMP_FAILURE) {
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
        if (responseCode >= STATUS_TEMP_FAILURE && responseCode < STATUS_PERM_FAILURE) {
            switch (responseCode) {
                case STATUS_SERVER_UNAVAILABLE:
                    throw new FailedGeminiRequestException.GeminiServerUnavailable(meta);
                case STATUS_CGI_ERROR:
                    throw new FailedGeminiRequestException.GeminiCGIError(meta);
                case STATUS_PROXY_ERROR:
                    throw new FailedGeminiRequestException.GeminiProxyError(meta);
                case STATUS_SLOW_DOWN:
                    throw new FailedGeminiRequestException.GeminiSlowDown(meta);
                default:
                    throw new FailedGeminiRequestException.GeminiTemporaryFailure(meta);
            }
        }

        // Permanent failure (50-59)
        if (responseCode >= STATUS_PERM_FAILURE && responseCode < STATUS_CLIENT_CERT_REQUIRED) {
            switch (responseCode) {
                case STATUS_NOT_FOUND:
                    throw new FailedGeminiRequestException.GeminiNotFound();
                case STATUS_GONE:
                    throw new FailedGeminiRequestException.GeminiGone();
                case STATUS_PROXY_REQUEST_REFUSED:
                    throw new FailedGeminiRequestException.GeminiProxyRequestRefused(meta);
                case STATUS_BAD_REQUEST:
                    throw new FailedGeminiRequestException.GeminiBadRequest(meta);
                default:
                    throw new FailedGeminiRequestException.GeminiPermanentFailure(meta);
            }
        }

        // Client certificate required (60-69)
        if (responseCode >= STATUS_CLIENT_CERT_REQUIRED && responseCode < 70) {
            switch (responseCode) {
                case STATUS_CERT_NOT_AUTHORIZED:
                    throw new FailedGeminiRequestException.GeminiCertificateNotAuthorized(meta);
                case STATUS_CERT_NOT_VALID:
                    throw new FailedGeminiRequestException.GeminiCertificateNotValid(meta);
                default:
                    throw new FailedGeminiRequestException.GeminiClientCertificateRequired(meta);
            }
        }

        // Unknown status code
        Log.i(TAG, "Unknown response code: " + responseCode + ", meta: " + meta);
        throw new FailedGeminiRequestException.GeminiUnimplementedCase();
    }

    /**
     * Handles file downloads, choosing the appropriate storage strategy based on the Android version.
     * <p>
     * - Android 10+ (Q): Uses the MediaStore API. No permissions required.
     * - Android 9 and below: Uses legacy external storage access. Requires WRITE_EXTERNAL_STORAGE permission.
     */
    private DownloadResult download(Activity activity, InputStream inputStream, Uri uriFile, String mimeType) throws IOException, NoSuchAlgorithmException {
        String uriString = uriFile.toString();
        if (uriString.endsWith("/")) {
            uriString = uriString.substring(0, uriString.length() - 1);
        }
        Log.i(TAG, "Downloading: " + uriString);

        String[] sectors = uriString.split("\\.");
        String extension = sectors.length > 0 ? sectors[sectors.length - 1] : "bin";

        // Calculate hash while downloading
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[4096];

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ (API 29+): Use MediaStore API (no permissions needed)
            return downloadViaMediaStore(activity, inputStream, extension, mimeType, digest, buffer);
        } else {
            // Android 9 and below: Use legacy method (requires permission)
            return downloadLegacy(activity, inputStream, extension, digest, buffer);
        }
    }

    /**
     * Download using MediaStore API for Android 10+ (no permissions needed)
     */
    private DownloadResult downloadViaMediaStore(Activity activity, InputStream inputStream,
                                                 String extension, String mimeType,
                                                 MessageDigest digest, byte[] buffer) throws IOException {
        ContentResolver resolver = activity.getContentResolver();

        // Generate filename with timestamp
        String filename = "agena_" + System.currentTimeMillis() + "." + extension;
        String subdir = "AGENA";
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + File.separator + subdir);
        } else {
            File dir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), subdir);
            boolean ignored = dir.mkdirs();
            File file = new File(dir, filename);
            values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());
        }


        Uri downloadUri = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            downloadUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        }
        if (downloadUri == null) {
            throw new IOException("Failed to create MediaStore entry");
        }

        try (OutputStream outputStream = resolver.openOutputStream(downloadUri)) {
            if (outputStream == null) {
                throw new IOException("Failed to open output stream");
            }

            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
                digest.update(buffer, 0, len);
            }
            outputStream.flush();
        }

        String hash = new BigInteger(1, digest.digest()).toString(16);
        String displayPath = Environment.DIRECTORY_DOWNLOADS + "/AGENA/" + filename;
        Log.i(TAG, "Downloaded to: " + displayPath + " (hash: " + hash + ")");

        return new DownloadResult(downloadUri, displayPath);
    }

    /**
     * Legacy download method for Android 9 and below (requires WRITE_EXTERNAL_STORAGE permission)
     */
    private DownloadResult downloadLegacy(Activity activity, InputStream inputStream,
                                         String extension, MessageDigest digest, byte[] buffer) throws IOException {
        // Check permission for Android 9 and below
        if (!PermissionAsker.ensurePermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE, R.string.explain_permission_storage)) {
            return null; // Permission denied
        }

        File agenaPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "AGENA");
        if (!agenaPath.exists() && !agenaPath.mkdirs()) {
            throw new IOException("Failed to create directory: " + agenaPath.getAbsolutePath());
        }

        File tempPath = File.createTempFile("agena", "." + extension, agenaPath);

        try (BufferedOutputStream outputStream = new BufferedOutputStream(new java.io.FileOutputStream(tempPath))) {
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
                digest.update(buffer, 0, len);
            }
            outputStream.flush();
        }

        String hash = new BigInteger(1, digest.digest()).toString(16);
        File outFile = new File(agenaPath, hash + "." + extension);

        File finalFile = tempPath.renameTo(outFile) ? outFile : tempPath;
        Uri fileUri = FileProvider.getUriForFile(activity, activity.getPackageName(), finalFile);

        Log.i(TAG, "Downloaded to: " + finalFile.getAbsolutePath());
        return new DownloadResult(fileUri, finalFile.getAbsolutePath());
    }
}
