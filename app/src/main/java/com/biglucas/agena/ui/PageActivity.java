package com.biglucas.agena.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.biglucas.agena.R;
import com.biglucas.agena.protocol.gemini.FailedGeminiRequestException;
import com.biglucas.agena.protocol.gemini.GeminiPageContentFragment;
import com.biglucas.agena.protocol.gemini.GeminiSingleton;
import com.biglucas.agena.utils.Invoker;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class PageActivity extends AppCompatActivity {
    private Uri url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.page_activity);
        String uriStr = this.getIntent().getData().toString();

        this.url = Uri.parse(uriStr.trim());
        TextView urlText = findViewById(R.id.browser_url);
        urlText.setText(this.url.toString());
        handlePageReload(null);
    }

    public void handlePageGo(View view) { // this method is called from the XML
        TextView urlText = findViewById(R.id.browser_url);
        String urlToGoTo = urlText.getText().toString();
        Uri destURL = Uri.parse(URI.create(this.url.toString()).resolve(urlToGoTo.trim()).toString());
        System.out.printf("scheme: '%s'", destURL.getScheme());
        new Invoker(this, destURL).invoke();
    }
    public void handlePageReload(View view) {
        handlePageLoad();
    }
    public void handleLoad(ArrayList<String> content) {
        if (this.getSupportFragmentManager().isDestroyed()) return;
        this.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.browser_content, new GeminiPageContentFragment(content, this.url))
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
            errText = appctx.getString(R.string.error_cgi_error);
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
            e.printStackTrace();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.input_prompt_title);
        builder.setMessage(prompt);

        final EditText input = new EditText(this);
        if (sensitive) {
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            input.setInputType(InputType.TYPE_CLASS_TEXT);
        }
        builder.setView(input);

        builder.setPositiveButton(R.string.input_prompt_ok, (dialog, which) -> {
            String userInput = input.getText().toString();
            // Append input as query parameter to current URL
            Uri.Builder uriBuilder = url.buildUpon();
            uriBuilder.query(userInput);
            Uri newUrl = uriBuilder.build();
            this.url = newUrl;
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

    public void handlePageLoad() {
        handlePageLoad(this.url.toString());
    }
    public void handlePageLoad(String url) {
        System.out.println("page load");
        if (this.getSupportFragmentManager().isDestroyed()) return;
        this.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.browser_content, new PageLoadingFragment())
                .commit();
        Uri uri = Uri.parse(url);
        System.out.println(uri.toString());
        ((TextView)this.findViewById(R.id.browser_url)).setText(uri.toString());
        PageActivity that = this;
        AsyncTask<String, Integer, ArrayList<String>> task = new AsyncTask<String, Integer, ArrayList<String>>() {
            private Exception exception;
            private ArrayList<String> list;

            @Override
            protected ArrayList<String> doInBackground(String ..._ignore) {
                try {
                    System.out.println("* request na thread *");
                    this.list = (ArrayList<String>) GeminiSingleton.getGemini().request(that, that.url); // gambiarra alert
                } catch (Exception e) {
                    this.exception = e;
                }
                return this.list;
            }

            @Override
            protected void onPostExecute(ArrayList<String> _ignore) {
                System.out.println("* post execute *");
                if (list != null) {
                    for (String item : list) {
                        System.out.println(item);
                    }
                }
                if (list != null) {
                    that.handleLoad(list);
                } else {
                    that.handleLoad(exception);
                }
            }
        };
        task.execute("");
    }
}