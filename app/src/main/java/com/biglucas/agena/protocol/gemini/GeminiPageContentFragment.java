package com.biglucas.agena.protocol.gemini;

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
import com.biglucas.agena.utils.Invoker;
import com.biglucas.agena.utils.StacktraceDialogHandler;
import com.google.android.material.button.MaterialButton;

import java.net.URI;
import java.util.ArrayList;
import java.util.IllegalFormatConversionException;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class GeminiPageContentFragment extends Fragment {

    private static final String TAG = "GeminiPageContentFragment";
    private final ArrayList<String> content;
    private final Uri oldURI;

    public GeminiPageContentFragment(ArrayList<String> list, Uri oldURI) {
        this.content = list;
        this.oldURI = oldURI;
    }

    public GeminiPageContentFragment() {
        this(new ArrayList<>(), Uri.parse("gemini://example.com"));
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        LinearLayout contentColumn = this.requireView().findViewById(R.id.content_column);
        contentColumn.removeAllViewsInLayout();
        TextView tv = new TextView(this.getContext());
        float textSizeBaseline = 14; //tv.getTextSize();
        Log.d(TAG, "Default text size: " + tv.getTextSize());
        String monospaceText = null;
        for (String item : this.content) {
            if (item.startsWith("```")) {
                if (monospaceText != null) {
                    HorizontalScrollView horizontalScrollView = new HorizontalScrollView(getContext());
                    TextView txt = new TextView(this.getContext());
                    txt.setText(monospaceText);
                    txt.setTypeface(Typeface.MONOSPACE);
                    txt.setClickable(false);
                    txt.setCursorVisible(false);
                    txt.setFocusable(false);
                    horizontalScrollView.addView(txt);
                    contentColumn.addView(horizontalScrollView);
                    monospaceText = null;
                } else {
                    monospaceText = "";
                }
                continue;
            }
            if (monospaceText != null) {
                monospaceText = String.format("%s\n%s", monospaceText, item);
                continue;
            }
            if (item.startsWith("=>")) {
                StringTokenizer tokenizer = new StringTokenizer(item.substring(2));
                if (!tokenizer.hasMoreTokens()) {
                    continue;
                }
                String buttonURI = tokenizer.nextToken().trim();
                String label = "";
                while (tokenizer.hasMoreElements()) {
                    label = String.format("%s %s", label, tokenizer.nextToken());
                }
                label = label.trim(); // remove spaces around
                if (label.isEmpty()) {
                    label = buttonURI;
                }
                MaterialButton button = new MaterialButton(this.requireContext());
                button.setText(label);
                button.setAllCaps(false);

                String oldURINormalized = this.oldURI.toString();
                if (!Objects.requireNonNull(this.oldURI.getPath()).endsWith("/") && !this.oldURI.getPath().endsWith(".gmi")) {
                    oldURINormalized = String.format("%s/", oldURINormalized);
                }
                GeminiPageContentFragment that = this;
                String finalOldURINormalized = oldURINormalized.trim();
                Uri u;
                try {
                    try {
                        u = Uri.parse(URI.create(finalOldURINormalized.trim()).resolve(buttonURI.trim()).toString().trim());
                    } catch (IllegalArgumentException e) {
                        // Some sites screw up with links and i think it would be nice a fallback behaviour in these cases
                        final String regex = "[^a-zA-Z0-9:/.-]*";
                        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
                        final Matcher matcher = pattern.matcher(buttonURI);
                        final String res = matcher.replaceAll("");
                        try {
                            u = Uri.parse(URI.create(finalOldURINormalized.trim()).resolve(res).toString());
                        } catch (IllegalArgumentException ex) {
                            Log.e(TAG, "Failed to parse URI: " + ex.getMessage());
                            continue;
                        }
                    }
                    final Uri uri = u;
                    button.setOnTouchListener(new View.OnTouchListener() {
                        private final GestureDetector gestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
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
                                Toast.makeText(that.getContext(), uri.toString(), Toast.LENGTH_SHORT)
                                        .show();
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

                    });
                    contentColumn.addView(button);
                } catch (IllegalFormatConversionException e) {
                    StacktraceDialogHandler.show(getContext(), e);
                }
                continue;
            }
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
            Log.d(TAG, "Heading levels: " + headingLevels);
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
            Log.d(TAG, "Text size: " + tv.getTextSize());
            contentColumn.addView(tv);
            tv = new TextView(this.getContext());
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.page_content_fragment, container, false);
    }
}