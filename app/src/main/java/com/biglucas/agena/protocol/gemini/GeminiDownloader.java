package com.biglucas.agena.protocol.gemini;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;

import com.biglucas.agena.R;
import com.biglucas.agena.utils.PermissionAsker;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Handles file downloads for the Gemini protocol.
 * <p>
 * This class abstracts the differences between Android versions regarding storage access:
 * <ul>
 *     <li>Android 10+ (Q): Uses MediaStore API (scoped storage).</li>
 *     <li>Android 9-: Uses legacy external storage access.</li>
 * </ul>
 */
public class GeminiDownloader {
    private static final String TAG = "GeminiDownloader";

    /**
     * Result of a download operation containing URI and display path
     */
    public static class Result {
        public final Uri uri;
        public final String displayPath;

        public Result(Uri uri, String displayPath) {
            this.uri = uri;
            this.displayPath = displayPath;
        }
    }

    /**
     * Handles file downloads, choosing the appropriate storage strategy based on the Android version.
     * <p>
     * - Android 10+ (Q): Uses the MediaStore API. No permissions required.
     * - Android 9 and below: Uses legacy external storage access. Requires WRITE_EXTERNAL_STORAGE permission.
     */
    public Result download(Activity activity, InputStream inputStream, Uri uriFile, String mimeType) throws IOException, NoSuchAlgorithmException {
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
     * Performs a download using the Android 10+ MediaStore API (Scoped Storage).
     * <p>
     * This method does not require storage permissions as it writes to the app's own
     * contribution to the 'Downloads' collection.
     * <p>
     * It performs the following actions:
     * <ul>
     *     <li>Creates a new entry in {@link MediaStore.Downloads} with the filename and MIME type.</li>
     *     <li>Writes the input stream to the output stream provided by the ContentResolver.</li>
     *     <li>Simultaneously computes the SHA-256 hash of the downloaded content.</li>
     * </ul>
     *
     * @param activity    The context used to access ContentResolver.
     * @param inputStream The source input stream.
     * @param extension   The file extension (e.g., "gmi", "pdf").
     * @param mimeType    The MIME type of the content.
     * @param digest      The MessageDigest instance to update with downloaded bytes.
     * @param buffer      A shared buffer for reading/writing to avoid allocation churn.
     * @return A {@link Result} containing the content URI and display path.
     * @throws IOException If the MediaStore entry cannot be created or written to.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private Result downloadViaMediaStore(Activity activity, InputStream inputStream,
                                                 String extension, String mimeType,
                                                 MessageDigest digest, byte[] buffer) throws IOException {
        ContentResolver resolver = activity.getContentResolver();

        // Generate filename with timestamp
        String filename = "agena_" + System.currentTimeMillis() + "." + extension;
        String subdir = "AGENA";
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + File.separator + subdir);


        Uri downloadUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
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

        return new Result(downloadUri, displayPath);
    }

    /**
     * Performs a download using the legacy File API for Android 9 and below.
     * <p>
     * This method requires {@link Manifest.permission#WRITE_EXTERNAL_STORAGE} permission.
     * It writes directly to the public Downloads directory.
     * <p>
     * Flow:
     * <ol>
     *     <li>Checks for the required permission. Returns null if not granted.</li>
     *     <li>Ensures the 'AGENA' subdirectory exists in Downloads.</li>
     *     <li>Downloads to a temporary file first to ensure atomicity.</li>
     *     <li>Computes the SHA-256 hash during download.</li>
     *     <li>Renames the temporary file to its hash (content-addressable naming strategy).</li>
     * </ol>
     *
     * @param activity    The context used for permission checks and file access.
     * @param inputStream The source input stream.
     * @param extension   The file extension.
     * @param digest      The MessageDigest instance.
     * @param buffer      A shared buffer.
     * @return A {@link Result} with the file URI and path, or null if permission is denied.
     * @throws IOException If file creation or writing fails.
     */
    private Result downloadLegacy(Activity activity, InputStream inputStream,
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
        return new Result(fileUri, finalFile.getAbsolutePath());
    }
}
