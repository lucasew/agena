package com.biglucas.demos;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.net.URI;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PageContentFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PageContentFragment extends Fragment {

    private final ArrayList<String> content;
    private final URI oldURI;

    public PageContentFragment(ArrayList<String> list, URI oldURI) {
        this.content = list;
        this.oldURI = oldURI;
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
                String buttonURI = tokenizer.nextToken();
                String label = "";
                while (tokenizer.hasMoreElements()) {
                    label = String.format("%s %s", label, tokenizer.nextToken());
                }
                label = label.trim(); // remove spaces around
                buttonURI = buttonURI.trim();
                if (label.length() == 0) {
                    label = buttonURI;
                }
                Button button = new Button(this.getContext());
                button.setText(label);
                String finalButtonURI = buttonURI;
                PageContentFragment that = this;
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        System.out.println(finalButtonURI);
                        URI newURI = that.oldURI.resolve(URI.create(finalButtonURI));

                        new Invoker(that.getActivity(), newURI.toString()).invoke();
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