package com.biglucas.agena.protocol.gemini;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.SSLSocket;

public class Gemini {
    public Gemini() {}

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
     * Public request method that validates URI and delegates to internal request with redirect tracking
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
        if (!uri.getScheme().equals("gemini")) {
            throw new FailedGeminiRequestException.GeminiInvalidUri("Invalid scheme: " + uri.getScheme());
        }
    }

    /**
     * Internal request method with redirect tracking
     */
    private List<String> requestInternal(Activity activity, Uri uri, int redirectCount) throws IOException, FailedGeminiRequestException, NoSuchAlgorithmException, KeyManagementException {
        System.out.printf("Requesting: '%s' (redirect count: %d)\n", uri.toString(), redirectCount);

        // Check redirect limit (max 5 as per spec)
        if (redirectCount > 5) {
            throw new FailedGeminiRequestException.GeminiTooManyRedirects();
        }

        if (!uri.getScheme().equals("gemini")) {
            new Invoker(activity, uri).invokeNewWindow();
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
            System.out.println("Server did not respond with a Gemini header");
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

        System.out.printf("response_code=%d, meta=%s\n", responseCode, meta);

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
     * Handles the response based on status code
     */
    private List<String> handleResponse(Activity activity, Uri uri, InputStream inputStream,
                                        BufferedOutputStream outputStream, int responseCode,
                                        String meta, String cleanedEntity, int redirectCount)
            throws IOException, FailedGeminiRequestException, NoSuchAlgorithmException, KeyManagementException {

        // Input required (10-19)
        if (responseCode >= 10 && responseCode < 20) {
            boolean sensitive = (responseCode == 11);
            throw new FailedGeminiRequestException.GeminiInputRequired(meta, sensitive);
        }

        // Success (20-29)
        if (responseCode >= 20 && responseCode < 30) {
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
                if (PermissionAsker.ensurePermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE, R.string.explain_permission_storage)) {
                    File cachedImage = download(inputStream, Uri.parse(cleanedEntity));
                    Uri fileUri = FileProvider.getUriForFile(activity, activity.getPackageName(), cachedImage);
                    activity.runOnUiThread(() -> Toast.makeText(activity, cachedImage.getAbsolutePath(), Toast.LENGTH_SHORT).show());
                    Intent intent = new Intent();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                    }
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setDataAndType(fileUri, meta);
                    activity.startActivity(intent);
                } else {
                    activity.runOnUiThread(() -> Toast.makeText(activity, activity.getResources().getString(R.string.please_repeat_action), Toast.LENGTH_SHORT).show());
                }
            }
            new DatabaseController(activity.openOrCreateDatabase("history", Context.MODE_PRIVATE, null))
                    .addHistoryEntry(uri);
            return lines;
        }

        // Redirect (30-39)
        if (responseCode >= 30 && responseCode < 40) {
            if (meta.isEmpty()) {
                throw new FailedGeminiRequestException.GeminiInvalidResponse();
            }
            Uri redirectUri = Uri.parse(meta.trim());
            validateUri(redirectUri);
            return requestInternal(activity, redirectUri, redirectCount + 1);
        }

        // Temporary failure (40-49)
        if (responseCode >= 40 && responseCode < 50) {
            switch (responseCode) {
                case 40:
                    throw new FailedGeminiRequestException.GeminiTemporaryFailure(meta);
                case 41:
                    throw new FailedGeminiRequestException.GeminiServerUnavailable(meta);
                case 42:
                    throw new FailedGeminiRequestException.GeminiCGIError(meta);
                case 43:
                    throw new FailedGeminiRequestException.GeminiProxyError(meta);
                case 44:
                    throw new FailedGeminiRequestException.GeminiSlowDown(meta);
                default:
                    throw new FailedGeminiRequestException.GeminiTemporaryFailure(meta);
            }
        }

        // Permanent failure (50-59)
        if (responseCode >= 50 && responseCode < 60) {
            switch (responseCode) {
                case 50:
                    throw new FailedGeminiRequestException.GeminiPermanentFailure(meta);
                case 51:
                    throw new FailedGeminiRequestException.GeminiNotFound();
                case 52:
                    throw new FailedGeminiRequestException.GeminiGone();
                case 53:
                    throw new FailedGeminiRequestException.GeminiProxyRequestRefused(meta);
                case 59:
                    throw new FailedGeminiRequestException.GeminiBadRequest(meta);
                default:
                    throw new FailedGeminiRequestException.GeminiPermanentFailure(meta);
            }
        }

        // Client certificate required (60-69)
        if (responseCode >= 60 && responseCode < 70) {
            switch (responseCode) {
                case 60:
                    throw new FailedGeminiRequestException.GeminiClientCertificateRequired(meta);
                case 61:
                    throw new FailedGeminiRequestException.GeminiCertificateNotAuthorized(meta);
                case 62:
                    throw new FailedGeminiRequestException.GeminiCertificateNotValid(meta);
                default:
                    throw new FailedGeminiRequestException.GeminiClientCertificateRequired(meta);
            }
        }

        // Unknown status code
        System.out.printf("Unknown response code: %d, meta: %s\n", responseCode, meta);
        throw new FailedGeminiRequestException.GeminiUnimplementedCase();
    }

    private File download(InputStream inputStream, Uri uriFile) throws IOException, NoSuchAlgorithmException {
        String uriString = uriFile.toString();
        if (uriString.endsWith("/")) {
            uriString = uriString.substring(0, uriString.length() - 1);
        }
        System.out.println(uriString);
        String[] sectors = uriString.split("\\.");
        String extension = sectors[sectors.length - 1];

        File agenaPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "AGENA");
        if (!agenaPath.mkdirs()) System.out.printf("Creating missing directory: '%s'\n", agenaPath.getAbsolutePath());
        File tempPath = File.createTempFile("agena", String.format(".%s", extension), agenaPath);
        if (!tempPath.createNewFile()) System.out.printf("Creating file: '%s'\n", tempPath.getAbsolutePath());
        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tempPath));
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[4096];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, len);
            digest.update(buffer, 0, len);
        }
        String hash = new BigInteger(1, digest.digest()).toString(16);
        outputStream.flush();
        outputStream.close();
        File outFile = new File(agenaPath, String.format("%s.%s", hash, extension));
        if (tempPath.renameTo(outFile)) {
            return outFile;
        } else {
            return tempPath;
        }
    }
}