package com.biglucas.agena.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.biglucas.agena.R;
import com.biglucas.agena.protocol.gemini.FailedGeminiRequestException;
import com.biglucas.agena.protocol.gemini.GeminiDownloader;
import com.biglucas.agena.protocol.gemini.GeminiPageContentFragment;
import com.biglucas.agena.protocol.gemini.GeminiResponse;
import com.biglucas.agena.protocol.gemini.GeminiSingleton;
import com.biglucas.agena.utils.DatabaseController;
import com.biglucas.agena.utils.Invoker;
import com.biglucas.agena.utils.StacktraceDialogHandler;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PageActivity extends AppCompatActivity {
    private static final String TAG = "PageActivity";

    private Uri url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.page_activity);
        String uriStr = Objects.requireNonNull(this.getIntent().getData()).toString();

        this.url = Uri.parse(uriStr.trim());
        TextView urlText = findViewById(R.id.browser_url);
        urlText.setText(this.url.toString());
        handlePageReload(null);
    }

    public void handlePageGo(View view) { // this method is called from the XML
        TextView urlText = findViewById(R.id.browser_url);
        String urlToGoTo = urlText.getText().toString().trim();

        Uri destURL;
        // Check if input looks like an absolute domain (not a relative path)
        if (urlToGoTo.contains("://")) {
            // Already has a scheme, use as-is
            destURL = Uri.parse(urlToGoTo);
        } else if (isAbsoluteDomain(urlToGoTo)) {
            // Looks like a domain (e.g., "foo.bar"), add gemini:// prefix
            destURL = Uri.parse("gemini://" + urlToGoTo);
        } else {
            // Treat as relative path
            destURL = Uri.parse(URI.create(this.url.toString()).resolve(urlToGoTo).toString());
        }

        Log.d(TAG, "scheme: " + destURL.getScheme());
        Invoker.invoke(this, destURL);
    }

    /**
     * Checks if input looks like an absolute domain rather than a relative path.
     * A string is considered an absolute domain if it:
     * - Contains at least one dot (.)
     * - Does not start with a forward slash (/)
     * - Does not contain a forward slash before the first dot
     */
    private boolean isAbsoluteDomain(String input) {
        if (input.startsWith("/")) {
            return false; // Absolute path, not domain
        }

        int dotIndex = input.indexOf('.');
        int slashIndex = input.indexOf('/');

        // Must have a dot to be a domain
        if (dotIndex == -1) {
            return false;
        }

        // If there's a slash, it must come after the dot (e.g., "foo.bar/path")
        // If slash comes before dot, it's a relative path (e.g., "path/file.gmi")
        return slashIndex == -1 || slashIndex >= dotIndex;
    }
    public void handlePageReload(View view) {
        handlePageLoad();
    }
    public void handleLoad(List<String> content) {
        if (this.getSupportFragmentManager().isDestroyed()) return;
        this.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.browser_content, new GeminiPageContentFragment(new ArrayList<>(content), this.url))
                .commit();
    }
    private void handleLoad(Exception e) {
        // Handle input prompts (status codes 10-19) with dialogs
        if (e instanceof FailedGeminiRequestException.GeminiInputRequired) {
            FailedGeminiRequestException.GeminiInputRequired inputEx = (FailedGeminiRequestException.GeminiInputRequired) e;
            showInputDialog(inputEx.getPrompt(), inputEx.isSensitive());
            return;
        }

        String errText;
        Context appctx = this.getApplicationContext();

        // Network errors
        if (e instanceof UnknownHostException) {
            errText = appctx.getString(R.string.error_unable_to_resolve_host);
        } else if (e instanceof SocketTimeoutException) {
            errText = appctx.getString(R.string.error_connection_timeout);

        // Temporary failures (40-49)
        } else if (e instanceof FailedGeminiRequestException.GeminiSlowDown) {
            errText = appctx.getString(R.string.error_slow_down) + ": " + e.getMessage();
        } else if (e instanceof FailedGeminiRequestException.GeminiServerUnavailable) {
            errText = appctx.getString(R.string.error_server_unavailable);
        } else if (e instanceof FailedGeminiRequestException.GeminiCGIError) {
            errText = Objects.requireNonNull(e.getMessage()).replaceFirst("^CGI error: CGI [Ee]rror: ", "CGI error: ");
        } else if (e instanceof FailedGeminiRequestException.GeminiProxyError) {
            errText = appctx.getString(R.string.error_proxy_error);
        } else if (e instanceof FailedGeminiRequestException.GeminiTemporaryFailure) {
            errText = appctx.getString(R.string.error_temporary_failure);

        // Permanent failures (50-59)
        } else if (e instanceof FailedGeminiRequestException.GeminiNotFound) {
            errText = appctx.getString(R.string.error_gemini_not_found);
        } else if (e instanceof FailedGeminiRequestException.GeminiGone) {
            errText = appctx.getString(R.string.error_gone);
        } else if (e instanceof FailedGeminiRequestException.GeminiProxyRequestRefused) {
            errText = appctx.getString(R.string.error_proxy_request_refused);
        } else if (e instanceof FailedGeminiRequestException.GeminiBadRequest) {
            errText = appctx.getString(R.string.error_bad_request);
        } else if (e instanceof FailedGeminiRequestException.GeminiPermanentFailure) {
            errText = appctx.getString(R.string.error_permanent_failure);

        // Client certificate errors (60-69)
        } else if (e instanceof FailedGeminiRequestException.GeminiClientCertificateRequired) {
            errText = appctx.getString(R.string.error_client_certificate_required);
        } else if (e instanceof FailedGeminiRequestException.GeminiCertificateNotAuthorized) {
            errText = appctx.getString(R.string.error_certificate_not_authorized);
        } else if (e instanceof FailedGeminiRequestException.GeminiCertificateNotValid) {
            errText = appctx.getString(R.string.error_certificate_not_valid);

        // Redirect errors
        } else if (e instanceof FailedGeminiRequestException.GeminiTooManyRedirects) {
            errText = appctx.getString(R.string.error_too_many_redirects);

        // URI validation errors
        } else if (e instanceof FailedGeminiRequestException.GeminiInvalidUri) {
            errText = appctx.getString(R.string.error_invalid_uri) + ": " + e.getMessage();

        // Other Gemini errors
        } else if (e instanceof FailedGeminiRequestException.GeminiInvalidResponse) {
            errText = appctx.getString(R.string.error_gemini_invalid_response);
        } else if (e instanceof FailedGeminiRequestException.GeminiUnimplementedCase) {
            errText = appctx.getString(R.string.error_gemini_unimplemented);

        // Generic error
        } else {
            errText = appctx.getString(R.string.error_generic);
            StacktraceDialogHandler.show(this, e);
        }

        if (this.getSupportFragmentManager().isDestroyed()) return;
        this.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.browser_content, new PageErrorFragment(errText, e))
                .commit();
    }

    /**
     * Shows an input dialog for Gemini status codes 10-19
     */
    private void showInputDialog(String prompt, boolean sensitive) {
        // Create Material TextInputLayout programmatically
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_input, null);
        TextInputEditText input = dialogView.findViewById(R.id.dialog_input_text);

        if (sensitive) {
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            input.setInputType(InputType.TYPE_CLASS_TEXT);
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.input_prompt_title);
        builder.setMessage(prompt);
        builder.setView(dialogView);

        builder.setPositiveButton(R.string.input_prompt_ok, (dialog, which) -> {
            String userInput = Objects.requireNonNull(input.getText()).toString();
            // Append input as query parameter to current URL
            Uri.Builder uriBuilder = url.buildUpon();
            uriBuilder.query(userInput);
            this.url = uriBuilder.build();
            handlePageLoad();
        });

        builder.setNegativeButton(R.string.input_prompt_cancel, (dialog, which) -> {
            dialog.cancel();
            // Show error fragment on cancel
            if (!getSupportFragmentManager().isDestroyed()) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.browser_content, new PageErrorFragment(
                                getApplicationContext().getString(R.string.error_generic),
                                new Exception("Input cancelled by user")))
                        .commit();
            }
        });

        builder.show();
    }

    private static class LoadResult {
        List<String> textLines;
        GeminiDownloader.Result downloadResult;
        boolean isDownload;
        Exception exception;
        Uri uri;
        String meta;
    }

    public void handlePageLoad() {
        handlePageLoad(this.url.toString());
    }
    public void handlePageLoad(String url) {
        Log.d(TAG, "page load");
        if (this.getSupportFragmentManager().isDestroyed()) return;
        this.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.browser_content, new PageLoadingFragment())
                .commit();
        Uri uri = Uri.parse(url);
        Log.i(TAG, uri.toString());
        ((TextView)this.findViewById(R.id.browser_url)).setText(uri.toString());

        // Check scheme, delegate to Invoker if not gemini
        if (!"gemini".equals(uri.getScheme())) {
            Invoker.invoke(this, uri);
            handleLoad(new ArrayList<>());
            return;
        }

        PageActivity that = this;
        AsyncTask<String, Integer, LoadResult> task = new AsyncTask<String, Integer, LoadResult>() {
            @Override
            protected LoadResult doInBackground(String ..._ignore) {
                LoadResult result = new LoadResult();
                try {
                    GeminiResponse response = GeminiSingleton.getGemini().request(that.url);
                    result.uri = response.getUri();
                    result.meta = response.getMeta();

                    if (response.isText()) {
                        result.isDownload = false;
                        result.textLines = response.getTextBody();
                        response.close();
                    } else {
                        result.isDownload = true;
                        GeminiDownloader downloader = new GeminiDownloader();
                        result.downloadResult = downloader.download(that, response.getInputStream(), response.getUri(), response.getMeta());
                        response.close();
                    }

                    // Save history
                    try {
                        new DatabaseController(DatabaseController.openDatabase(that))
                                .addHistoryEntry(result.uri);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to save history for URI: " + result.uri, e);
                    }

                } catch (Exception e) {
                    result.exception = e;
                }
                return result;
            }

            @Override
            protected void onPostExecute(LoadResult result) {
                Log.d(TAG, "* post execute *");
                if (result.exception != null) {
                    that.handleLoad(result.exception);
                    return;
                }

                if (result.isDownload) {
                    if (result.downloadResult != null) {
                        Toast.makeText(that, result.downloadResult.displayPath, Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent();
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setDataAndType(result.downloadResult.uri, result.meta);
                        that.startActivity(intent);
                    } else {
                        Toast.makeText(that, that.getResources().getString(R.string.please_repeat_action), Toast.LENGTH_SHORT).show();
                    }
                    that.handleLoad(new ArrayList<>()); // Show empty page
                } else {
                    that.handleLoad(result.textLines);
                }
            }
        };
        task.execute("");
    }
}
