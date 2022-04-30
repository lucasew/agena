package com.biglucas.demos;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.icu.util.Output;
import android.net.Uri;
import android.os.Build;
import android.renderscript.ScriptGroup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

public class Gemini {
    public Gemini() {}

    public List<String> request(Activity activity, URI uri) throws IOException, FailedGeminiRequestException, NoSuchAlgorithmException, KeyManagementException {
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

        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
        PrintWriter outWriter = new PrintWriter(bufferedWriter);

        String cleanedEntity = uri.toString().replace("%2F", "/").trim();
        String requestEntity = cleanedEntity + "\r\n";

        outWriter.print(requestEntity);
        outWriter.flush();

        InputStream inputStream = socket.getInputStream();
        InputStreamReader headInputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(headInputStreamReader);
        String headerline = bufferedReader.readLine();
        if (headerline == null) {
            System.out.println("Servidor n respondeu com uma header gemini");
            bufferedReader.close();
            headInputStreamReader.close();
            inputStream.close();
            outWriter.close();
            bufferedReader.close();
            outputStreamWriter.close();
            throw new FailedGeminiRequestException.GeminiInvalidResponse();
        }
        int responseCode = Integer.parseInt(headerline.substring(0, headerline.indexOf(" ")));
        String meta = headerline.substring(headerline.indexOf(" ")).trim();
        System.out.printf("response_code=%d,meta=%s\n", responseCode, meta);
        if (responseCode >= 20 && responseCode < 30) {
            if (meta.startsWith("text/gemini")) {
                List<String> lines = new ArrayList<String>();
                while (true) {
                    String line = bufferedReader.readLine();
                    if (line == null) {
                        break;
                    }
                    for (String l : line.split("\n")) {
                        lines.add(line);
                    }
                }
                bufferedReader.close();
                headInputStreamReader.close();
                inputStream.close();
                outWriter.close();
                bufferedReader.close();
                outputStreamWriter.close();
                return lines;
            }
            if (meta.startsWith("image/")) {
                File cachedImage = saveFileToCache(socket, Uri.parse(cleanedEntity), activity.getCacheDir());
                bufferedReader.close();
                headInputStreamReader.close();
                inputStream.close();
                outWriter.close();
                bufferedReader.close();
                outputStreamWriter.close();
                new Invoker(activity, cachedImage.toURI().toString()).invokeNewWindow();
                return new ArrayList<String>();
            }
        }
        if (responseCode >= 30 && responseCode < 40) {
            return this.request(activity, URI.create(meta));
        }
        if (responseCode == 51) {
            throw new FailedGeminiRequestException.GeminiNotFound();
        }
        System.out.printf("server header: %s\n", headerline);
        System.out.printf("meta: %s\n", meta);
        String line;
        while ((line = bufferedReader.readLine()) != null) {
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

    private File saveFileToCache(SSLSocket socket, Uri uri, File cacheDir) throws IOException {
        return download(socket, uri, cacheDir);
    }

    private File download(SSLSocket socket, Uri uri, File cacheDir) throws IOException {
        String filename = uri.getLastPathSegment();
        File downloadFile = new File(cacheDir, filename);
        if (downloadFile.exists()) downloadFile.delete();
        downloadFile.createNewFile();
        FileOutputStream outputStream = new FileOutputStream(downloadFile);
        byte[] buffer = new byte[4096];
        int len = socket.getInputStream().read(buffer);
        while (len != -1) {
            outputStream.write(buffer, 0, len);
            len = socket.getInputStream().read(buffer);
        }
        socket.close();
        return downloadFile;
    }
}