package com.biglucas.agena.ui;

import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.biglucas.agena.R;
import com.biglucas.agena.protocol.gemini.GeminiUriHelper;
import com.biglucas.agena.utils.Invoker;
import com.biglucas.agena.utils.StacktraceDialogHandler;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.IllegalFormatConversionException;
import java.util.StringTokenizer;

/**
 * Fragment responsible for parsing and rendering `text/gemini` content.
 * <p>
 * This class takes a list of raw Gemini strings and converts them into native Android Views
 * (TextViews, Buttons) to build the page UI dynamically.
 */
public class GeminiPageContentFragment extends Fragment {

    private static final String TAG = "GeminiPageContent";
    private final ArrayList<String> content;
    private final Uri oldURI;
    private final float textSizeBaseline = 14;

    public GeminiPageContentFragment(ArrayList<String> list, Uri oldURI) {
        this.content = list;
        this.oldURI = oldURI;
    }

    public GeminiPageContentFragment() {
        this(new ArrayList<>(), Uri.parse("gemini://example.com"));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.page_content_fragment, container, false);
    }

    /**
     * Main parsing loop. Iterates through the content lines and builds the UI.
     * <p>
     * The parsing logic handles:
     * <ul>
     *     <li><b>Preformatted Text (```):</b> Toggles a state flag. Text inside is collected and rendered in a monospace font inside a HorizontalScrollView.</li>
     *     <li><b>Links (=>):</b> Parsed using {@link StringTokenizer}. Resolves relative URIs against the current page URI.
     *         <ul>
     *             <li>Includes a fallback mechanism for malformed URIs using {@link GeminiUriHelper}.</li>
     *             <li>Applies a custom {@link GestureDetector} to handle Single Tap (navigate), Double Tap (new window), and Long Press (show URL).</li>
     *         </ul>
     *     </li>
     *     <li><b>Headings (#):</b> Adjusted text size based on heading level (1-3).</li>
     *     <li><b>List Items (*):</b> Prefixed with a bullet point.</li>
     *     <li><b>Regular Text:</b> Rendered as standard paragraphs.</li>
     * </ul>
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        LinearLayout contentColumn = this.requireView().findViewById(R.id.content_column);
        contentColumn.removeAllViewsInLayout();

        String monospaceText = null;

        for (String item : this.content) {
            // Handle preformatted text block start/end
            if (item.startsWith("```")) {
                if (monospaceText != null) {
                    addPreformattedBlock(contentColumn, monospaceText);
                    monospaceText = null;
                } else {
                    monospaceText = "";
                }
                continue;
            }

            // Accumulate preformatted text
            if (monospaceText != null) {
                monospaceText = String.format("%s\n%s", monospaceText, item);
                continue;
            }

            // Handle Links
            if (item.startsWith("=>")) {
                addLinkButton(contentColumn, item);
                continue;
            }

            // Handle Headings, Lists, and Regular Text
            addTextElement(contentColumn, item);
        }
    }

    private void addPreformattedBlock(LinearLayout container, String text) {
        HorizontalScrollView horizontalScrollView = new HorizontalScrollView(getContext());
        TextView txt = new TextView(this.getContext());
        txt.setText(text);
        txt.setTypeface(Typeface.MONOSPACE);
        txt.setClickable(false);
        txt.setCursorVisible(false);
        txt.setFocusable(false);
        horizontalScrollView.addView(txt);
        container.addView(horizontalScrollView);
    }

    private void addLinkButton(LinearLayout container, String item) {
        try {
            StringTokenizer tokenizer = new StringTokenizer(item.substring(2));
            if (!tokenizer.hasMoreTokens()) {
                return;
            }
            String buttonURI = tokenizer.nextToken().trim();
            String label = "";
            while (tokenizer.hasMoreElements()) {
                label = String.format("%s %s", label, tokenizer.nextToken());
            }
            label = label.trim();
            if (label.isEmpty()) {
                label = buttonURI;
            }

            MaterialButton button = new MaterialButton(this.requireContext());
            button.setText(label);
            button.setAllCaps(false);

            // Use GeminiUriHelper for resolution
            String resolvedUriString = GeminiUriHelper.resolve(this.oldURI.toString(), buttonURI);
            final Uri uri = Uri.parse(resolvedUriString);

            button.setOnTouchListener(createLinkTouchListener(uri));
            container.addView(button);
        } catch (IllegalFormatConversionException e) {
            StacktraceDialogHandler.show(getContext(), e);
        }
    }

    private View.OnTouchListener createLinkTouchListener(final Uri uri) {
        return new View.OnTouchListener() {
            private final GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(@NonNull MotionEvent e) {
                    Invoker.invokeNewWindow(getActivity(), uri);
                    return super.onDoubleTap(e);
                }

                @Override
                public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                    Invoker.invoke(getActivity(), uri);
                    return super.onSingleTapConfirmed(e);
                }

                @Override
                public void onLongPress(@NonNull MotionEvent e) {
                    Toast.makeText(getContext(), uri.toString(), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "long press");
                    super.onLongPress(e);
                }
            });

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                view.performClick();
                gestureDetector.onTouchEvent(motionEvent);
                return true;
            }
        };
    }

    private void addTextElement(LinearLayout container, String item) {
        TextView tv = new TextView(this.getContext());

        int headingLevels = 0;
        int cutoutLevels = 0;
        for (int i = 0; i < item.length(); i++) {
            if (item.charAt(i) != '#') {
                break;
            }
            headingLevels++;
        }
        if (headingLevels > 0) {
            cutoutLevels += headingLevels;
        }

        switch (headingLevels) {
            case 1: tv.setTextSize(textSizeBaseline * (20f/11f)); break;
            case 2: tv.setTextSize(textSizeBaseline * (16f/11f)); break;
            case 3: tv.setTextSize(textSizeBaseline * (14f/11f)); break;
            case 4: tv.setTextSize(textSizeBaseline * (12f/11f)); break;
            default: tv.setTextSize(textSizeBaseline);
        }

        if (item.startsWith("*")) {
            cutoutLevels += 1;
        }

        String labelText = item.substring(cutoutLevels).trim();
        if (item.startsWith("*")) {
            labelText = String.format("○ %s", labelText);
        }

        tv.setText(labelText);
        container.addView(tv);
    }
}
