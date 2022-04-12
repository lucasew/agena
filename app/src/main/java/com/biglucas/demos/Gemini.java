package com.biglucas.demos;

import android.app.Activity;
import android.content.Intent;
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
    private URI lastUri = null;

    public Gemini(URI initialUri) {
        this.lastUri = initialUri;
    }

    public List<String> request(Activity activity, URI uri) throws IOException, FailedGeminiRequestException, NoSuchAlgorithmException, KeyManagementException {
        System.out.printf("Requesting: '%s'\n", uri.toString());
        if (this.lastUri != null) {
            uri = this.lastUri.resolve(uri);
        }
        if (!uri.getScheme().equals("gemini")) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri.toString()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
            activity.startActivity(intent);
            return new ArrayList<String>();
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
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    lines.add(line);
                }
                this.lastUri = uri;
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
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(cachedImage.toURI().toString()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                bufferedReader.close();
                headInputStreamReader.close();
                inputStream.close();
                outWriter.close();
                bufferedReader.close();
                outputStreamWriter.close();
                activity.startActivity(intent);
                return new ArrayList<String>();
            }
        }
        if (responseCode >= 30 && responseCode < 40) {
            this.lastUri = uri;
            return this.request(activity, URI.create(meta));
        }
        if (responseCode == 51) {
            this.lastUri = uri;
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
/*
private class Gemini2 {

    public interface GeminiListener {
        void showProgress();

        void message(String message);

        void gemtextReady(String address, ArrayList<String> lines);

        void imageReady(Drawable image);

        void cacheLastVisited(String address);
    }

    private final File cacheDir;
    private SSLSocketFactory socketFactory;
    private GeminiListener listener;
    private Uri prevUri = null;
    public final ArrayList<Uri> history = new ArrayList<>();

    public Gemini(GeminiListener listener, File cacheDir) {
        this.listener = listener;
        initialiseSSL();
        this.cacheDir = cacheDir;
    }

    public void requestThread(String link) {
        listener.showProgress();
        new Thread() {
            @Override
            public void run() {
                super.run();

                if (prevUri != null && !link.startsWith("gemini://")) {
                    if (prevUri.getPathSegments().size() > 0 && prevUri.getLastPathSegment().contains(".")) {
                        prevUri = Uri.parse(prevUri.toString().substring(0, prevUri.toString().lastIndexOf("/")));
                    }
                    prevUri = prevUri.buildUpon().appendPath(link).build();
                } else {
                    prevUri = Uri.parse(link);
                }

                request(prevUri);
            }
        }.start();
    }

    private void initialiseSSL() {

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            SecurityProvider.addConscryptIfAvailable();
        }

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            X509TrustManager[] trustManagers = {new DummyTrustManager()};
            sslContext.init(null, trustManagers, null);
            socketFactory = sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException e) {
            listener.message(e.toString());
            e.printStackTrace();
        } catch (KeyManagementException e) {
            listener.message(e.toString());
            e.printStackTrace();
        }
    }

    private void request(Uri uri) {
        l("******* GEMINI REQ: " + uri);
        try {
            SSLSocket socket = (SSLSocket) socketFactory.createSocket(uri.getHost(), 1965);
            socket.startHandshake();

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
            BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
            PrintWriter outWriter = new PrintWriter(bufferedWriter);

            //Not sure where this is coming from - but remove the encoded slash
            String cleanedEntity = uri.toString().replace("%2F", "/").trim();
            String requestEntity = cleanedEntity + "\r\n";

            l("Ariane socket requesting $requestEntity");
            outWriter.print(requestEntity);
            outWriter.flush();

            if (outWriter.checkError()) {
                listener.message("Print Writer Error");
                closeAll(null, null, null, outWriter, bufferedWriter, outputStreamWriter);
                return;
            }

            InputStream inputStream = socket.getInputStream();
            InputStreamReader headerInputReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(headerInputReader);
            String headerLine = bufferedReader.readLine();

            l("Ariane: response header: " + headerLine);

            if (headerLine == null) {
                listener.message("Server did not respond with a Gemini header");
                closeAll(bufferedReader, headerInputReader, inputStream, outWriter, bufferedWriter, outputStreamWriter);
                return;
            }

            int responseCode = Character.getNumericValue(headerLine.charAt(0));
            String meta = headerLine.substring(headerLine.indexOf(" ")).trim();
            switch (responseCode) {
                case 2: {
                    if (meta.startsWith("text/gemini")) {
                        addHistory(cleanedEntity);
                        ArrayList lines = new ArrayList<String>();
                        String line = bufferedReader.readLine();

                        boolean inCodeBlock = false;
                        StringBuffer sb = new StringBuffer();
                        while (line != null) {
                            if (line.startsWith("```")) {
                                if (inCodeBlock) {
                                    inCodeBlock = false;
                                    lines.add("```" + sb.toString());
                                    sb = new StringBuffer();
                                } else {
                                    inCodeBlock = true;
                                }
                            } else if (inCodeBlock) {
                                sb.append(line);
                                sb.append("\n");
                                l("Gemtext line: " + line);
                            } else {
                                lines.add(line);
                                l("Gemtext line: " + line);
                            }

                            line = bufferedReader.readLine();
                        }

                        prevUri = Uri.parse(cleanedEntity);
                        listener.gemtextReady(cleanedEntity, lines);

                    } else if (meta.startsWith("image/")) {
                        File cachedImage = saveFileToCache(socket, Uri.parse(cleanedEntity));
                        Drawable image = Drawable.createFromPath(cachedImage.getPath());
                        listener.imageReady(image);
                    } else if (meta.equals("application/vnd.android.package-archive")) {
                        listener.message(".apk download not available in Phaedra");
                        socket.close();
                    }
                    break;
                }
            }
            closeAll(bufferedReader, headerInputReader, inputStream, outWriter, bufferedWriter, outputStreamWriter);
        } catch (IOException e) {
            listener.message(e.toString());
            e.printStackTrace();
        }
    }

    private File saveFileToCache(SSLSocket socket, Uri uri) {
        return download(socket, uri, true);
    }

    private File download(SSLSocket socket, Uri uri, boolean isCache) {
        String filename = uri.getLastPathSegment();

        File downloadFile = new File(cacheDir, filename);

        if (downloadFile.exists()) downloadFile.delete();
        try {
            downloadFile.createNewFile();

            FileOutputStream outputStream = new FileOutputStream(downloadFile);
            byte[] buffer = new byte[1024];
            int len = socket.getInputStream().read(buffer);
            while (len != -1) {
                outputStream.write(buffer, 0, len);
                len = socket.getInputStream().read(buffer);
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return downloadFile;
    }

    private void closeAll(BufferedReader bufferedReader, InputStreamReader inputStreamReader, InputStream inputStream, PrintWriter outWriter, BufferedWriter bufferedWriter, OutputStreamWriter outputStreamWriter) {
        try {
            if (bufferedReader != null) bufferedReader.close();
            if (inputStreamReader != null) inputStreamReader.close();
            if (inputStream != null) inputStream.close();
            if (outWriter != null) outWriter.close();
            if (bufferedWriter != null) bufferedWriter.close();
            if (outputStreamWriter != null) outputStreamWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addHistory(String address) {
        if (!history.isEmpty() && history.get(history.size() - 1).toString().equals(address)) {
            l("Address already in history: " + address);
        } else {
            history.add(Uri.parse(address));
            listener.cacheLastVisited(address);
        }
    }

    private class DummyTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    private void l(String message) {
        System.out.println("Phaedra Gemini: " + message);
    }
}

*/