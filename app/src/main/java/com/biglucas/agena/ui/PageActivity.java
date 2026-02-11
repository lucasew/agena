package com.biglucas.agena.ui;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.biglucas.agena.R;
import com.biglucas.agena.protocol.gemini.FailedGeminiRequestException;
import com.biglucas.agena.protocol.gemini.GeminiSingleton;
import com.biglucas.agena.utils.Invoker;
import com.biglucas.agena.utils.StacktraceDialogHandler;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.net.URI;
import java.lang.ref.WeakReference;
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

        Context appctx = this.getApplicationContext();
        String errText = GeminiErrorMapper.getErrorMessage(appctx, e);

        if (errText == null) {
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

        new GeminiRequestTask(this).execute();
    }

    private static class GeminiRequestTask extends AsyncTask<Void, Void, ArrayList<String>> {
        private final WeakReference<PageActivity> activityRef;
        private Exception exception;
        private ArrayList<String> list;

        GeminiRequestTask(PageActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        protected ArrayList<String> doInBackground(Void... voids) {
            PageActivity activity = activityRef.get();
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                return null;
            }

            try {
                Log.d(TAG, "* request na thread *");
                // Use activity context and URL from activity
                // Note: If activity is gone, we might still proceed but results are discarded
                this.list = (ArrayList<String>) GeminiSingleton.getGemini().request(activity, activity.url);
            } catch (Exception e) {
                this.exception = e;
            }
            return this.list;
        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {
            Log.d(TAG, "* post execute *");
            PageActivity activity = activityRef.get();
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                return;
            }

            if (result != null) {
                for (String item : result) {
                    Log.v(TAG, item);
                }
                activity.handleLoad(result);
            } else {
                activity.handleLoad(exception);
            }
        }
    }
}
