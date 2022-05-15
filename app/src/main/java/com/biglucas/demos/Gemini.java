package com.biglucas.demos;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.FileUtils;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.DigestException;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

    public List<String> request(Activity activity, Uri uri) throws IOException, FailedGeminiRequestException, NoSuchAlgorithmException, KeyManagementException, DigestException {
        System.out.printf("Requesting: '%s'\n", uri.toString());
        System.out.printf("scheme: '%s'", uri.getScheme());
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
        // TODO: configurable timeout
        socket.connect(new InetSocketAddress(uri.getHost(), port), 5 * 1000);
        socket.setSoTimeout(5*1000);
        socket.startHandshake();

        BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());

        String cleanedEntity = uri.toString().replace("%2F", "/").trim();
        String requestEntity = cleanedEntity + "\r\n";

        outputStream.write(requestEntity.getBytes());
        outputStream.flush();

        InputStream inputStream = new BufferedInputStream(socket.getInputStream());
        String headerline = readLineFromStream(inputStream);
        if (headerline == null) {
            System.out.println("Servidor n respondeu com uma header gemini");
            inputStream.close();
            outputStream.close();
            throw new FailedGeminiRequestException.GeminiInvalidResponse();
        }
        int responseCode = Integer.parseInt(headerline.substring(0, headerline.indexOf(" ")));
        String meta = headerline.substring(headerline.indexOf(" ")).trim();
        System.out.printf("response_code=%d,meta=%s\n", responseCode, meta);
        if (responseCode >= 20 && responseCode < 30) {
            List<String> lines = new ArrayList<>();
            if (meta.startsWith("text/gemini")) {
                while (true) {
                    String line = readLineFromStream(inputStream);
                    if (line == null) {
                        break;
                    }
                    for (String l : line.split("\n")) {
                        lines.add(l);
                    }
                }

            } else {
                if (PermissionAsker.ensurePermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE, R.string.explain_permission_storage)) {
                    File cachedImage = download(inputStream, Uri.parse(cleanedEntity));
                    Uri fileUri = FileProvider.getUriForFile(activity, activity.getPackageName(), cachedImage);
                    activity.runOnUiThread(() -> {
                        Toast.makeText(activity, cachedImage.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                    });
                    Intent intent = new Intent();
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setDataAndType(fileUri, meta);
                    activity.startActivity(intent);
                } else {
                    activity.runOnUiThread(() -> {
                        Toast.makeText(activity, activity.getResources().getString(R.string.please_repeat_action), Toast.LENGTH_SHORT).show();
                    });
                }
            }
            outputStream.close();
            return lines;
        }
        if (responseCode >= 30 && responseCode < 40) {
            return this.request(activity, Uri.parse(meta.trim()));
        }
        if (responseCode == 51) {
            throw new FailedGeminiRequestException.GeminiNotFound();
        }
        System.out.printf("server header: %s\n", headerline);
        System.out.printf("meta: %s\n", meta);
        String line;
        while ((line = readLineFromStream(inputStream)) != null) {
            System.out.printf("server return: %s\n", line);
        }
        System.out.flush();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        throw new FailedGeminiRequestException.GeminiUnimplementedCase();
    }

    private File download(InputStream inputStream, Uri uriFile) throws IOException, DigestException, NoSuchAlgorithmException {
        String uriString = uriFile.toString();
        String extension = "dat";
        if (uriString.endsWith("/")) {
            uriString.substring(0, uriString.length() - 1);
        }
        System.out.println(uriString);
        String[] sectors = uriString.split("\\.");
        System.out.println(sectors.toString());
        System.out.println(sectors.length);
        extension = sectors[sectors.length - 1];

        File agenaPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "AGENA");
        agenaPath.mkdirs();
        File tempPath = File.createTempFile("agena", String.format(".%s", extension), agenaPath);
        tempPath.createNewFile();
        tempPath.setWritable(true);
        tempPath.setReadable(true);
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