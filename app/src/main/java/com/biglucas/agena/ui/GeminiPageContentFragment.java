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
import com.biglucas.agena.protocol.gemini.GemtextParser;
import com.biglucas.agena.utils.Invoker;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

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
     * Builds the page UI from gemtext source lines.
     * <p>
     * Structural parsing (fences, links, headings, lists) is delegated to
     * {@link GemtextParser}; this method only maps elements to Android views:
     * <ul>
     *     <li><b>Preformatted:</b> Monospace text in a {@link HorizontalScrollView}.</li>
     *     <li><b>Links:</b> {@link MaterialButton} with {@link GestureDetector}
     *         for single tap (navigate), double tap (new window), long press (show URL).</li>
     *     <li><b>Headings:</b> Text size scaled by heading level.</li>
     *     <li><b>List Items:</b> Prefixed with a bullet.</li>
     *     <li><b>Regular Text:</b> Standard paragraphs.</li>
     * </ul>
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        LinearLayout contentColumn = this.requireView().findViewById(R.id.content_column);
        contentColumn.removeAllViewsInLayout();

        for (GemtextParser.Element element : GemtextParser.parse(this.content)) {
            if (element instanceof GemtextParser.Preformatted) {
                addPreformattedBlock(contentColumn, ((GemtextParser.Preformatted) element).text);
            } else if (element instanceof GemtextParser.Link) {
                addLinkButton(contentColumn, (GemtextParser.Link) element);
            } else if (element instanceof GemtextParser.Heading) {
                addHeading(contentColumn, (GemtextParser.Heading) element);
            } else if (element instanceof GemtextParser.ListItem) {
                addTextView(contentColumn, String.format("○ %s", ((GemtextParser.ListItem) element).text),
                        textSizeBaseline);
            } else if (element instanceof GemtextParser.Text) {
                addTextView(contentColumn, ((GemtextParser.Text) element).raw, textSizeBaseline);
            }
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

    private void addLinkButton(LinearLayout container, GemtextParser.Link link) {
        MaterialButton button = new MaterialButton(this.requireContext());
        button.setText(link.label);
        button.setAllCaps(false);

        String resolvedUriString = GeminiUriHelper.resolve(this.oldURI.toString(), link.target);
        final Uri uri = Uri.parse(resolvedUriString);

        button.setOnTouchListener(createLinkTouchListener(uri));
        container.addView(button);
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

    private void addHeading(LinearLayout container, GemtextParser.Heading heading) {
        float size;
        switch (heading.level) {
            case 1: size = textSizeBaseline * (20f / 11f); break;
            case 2: size = textSizeBaseline * (16f / 11f); break;
            case 3: size = textSizeBaseline * (14f / 11f); break;
            case 4: size = textSizeBaseline * (12f / 11f); break;
            default: size = textSizeBaseline;
        }
        addTextView(container, heading.text, size);
    }

    private void addTextView(LinearLayout container, String text, float size) {
        TextView tv = new TextView(this.getContext());
        tv.setTextSize(size);
        tv.setText(text);
        container.addView(tv);
    }
}
