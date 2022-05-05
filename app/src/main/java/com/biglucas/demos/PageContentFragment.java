package com.biglucas.demos;

import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.net.URI;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class PageContentFragment extends Fragment {

    private final ArrayList<String> content;
    private final Uri oldURI;

    public PageContentFragment(ArrayList<String> list, Uri oldURI) {
        this.content = list;
        this.oldURI = oldURI;
    }

    public PageContentFragment() {
        this(new ArrayList<>(), Uri.parse("gemini://example.com"));
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        LinearLayout contentColumn = this.getView().findViewById(R.id.content_column);
        contentColumn.removeAllViewsInLayout();
        TextView tv = new TextView(this.getContext());
        float textSizeBaseline = 14; //tv.getTextSize();
        System.out.printf("Default text size: %f\n", tv.getTextSize());
        String monospaceText = null;
        for (String item : this.content) {
            if (item.startsWith("```")) {
                if (monospaceText != null) {
                    TextView txt = new TextView(this.getContext());
                    txt.setText(monospaceText);
                    txt.setTypeface(Typeface.MONOSPACE);
                    contentColumn.addView(txt);
                    monospaceText = null;
                } else {
                    monospaceText = "";
                }
                continue;
            }
            if (monospaceText != null) {
                monospaceText = String.format("%s\n%s", monospaceText, item);
            }
            if (item.startsWith("=>")) { // TODO: arrumar esse regex cagado
                StringTokenizer tokenizer = new StringTokenizer(item.substring(2));
                String buttonURI = tokenizer.nextToken().trim();
                String label = "";
                while (tokenizer.hasMoreElements()) {
                    label = String.format("%s %s", label, tokenizer.nextToken());
                }
                label = label.trim(); // remove spaces around
                if (label.length() == 0) {
                    label = buttonURI;
                }
                Button button = new Button(this.getContext());
                button.setText(label);
                button.setAllCaps(false);

                String oldURINormalized = this.oldURI.toString();
                if (!oldURINormalized.endsWith("/")) {
                    oldURINormalized = String.format("%s/", oldURINormalized);
                }
                PageContentFragment that = this;
                String finalOldURINormalized = oldURINormalized.trim();
                    button.setOnTouchListener(new View.OnTouchListener() {
                        private Uri getFinalButtonURI() {
                            try {
                                System.out.print(buttonURI);
                                Uri u = Uri.parse(URI.create(finalOldURINormalized.trim()).resolve(buttonURI.trim()).toString());
                                //System.out.println(u.toString());
                                return u;
                            } catch (IllegalArgumentException e) {
                                System.out.printf("uri '%s' %d\n", finalOldURINormalized, finalOldURINormalized.charAt(0));
                                System.out.println("wtf, man");
                                e.printStackTrace();
                                return Uri.parse(buttonURI.trim());
                            }
                        }
                        private Invoker getInvoker() {
                            return new Invoker(getActivity(), getFinalButtonURI());
                        }
                        private GestureDetector gestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
                            @Override
                            public boolean onDoubleTap(MotionEvent e) {
                                getInvoker().invokeNewWindow();
                                return super.onDoubleTap(e);
                            }

                            @Override
                            public boolean onSingleTapConfirmed(MotionEvent e) {
                                getInvoker().invoke();
                                return super.onSingleTapConfirmed(e);
                            }

                            @Override
                            public void onLongPress(MotionEvent e) {
                                Toast.makeText(that.getContext(), getFinalButtonURI().toString(), Toast.LENGTH_SHORT)
                                        .show();
                                System.out.println("long press");
                                super.onLongPress(e);
                            }
                        });

                        @Override
                        public boolean onTouch(View view, MotionEvent motionEvent) {
                            gestureDetector.onTouchEvent(motionEvent);
                            return true;
                        }

                    });

                System.out.printf("label='%s' uri='%s'", label, buttonURI);
                // TODO: add handler
                contentColumn.addView(button);
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
            System.out.println(headingLevels);
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
            System.out.println(tv.getTextSize());
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