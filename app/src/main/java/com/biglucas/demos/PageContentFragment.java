package com.biglucas.demos;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.resources.TextAppearance;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PageContentFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PageContentFragment extends Fragment {

    private final ArrayList<String> content;

    public PageContentFragment() {
        this.content = new ArrayList<>();
    }

    public PageContentFragment(ArrayList<String> list) {
        this.content = list;
    }

    public static PageContentFragment newInstance(List<String> content) {
        return new PageContentFragment();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        LinearLayout contentColumn = this.getView().findViewById(R.id.content_column);
        contentColumn.removeAllViewsInLayout();
        TextView tv = new TextView(this.getContext());
        float textSizeBaseline = 14; //tv.getTextSize();
        System.out.printf("Default text size: %f", tv.getTextSize());
        for (String item : this.content) {
            if (item.startsWith("=>")) { // TODO: arrumar esse regex cagado
                Pattern pattern = Pattern.compile("=> *([^ ]*) *([^$]*)");
                try {
                    Matcher matcher = pattern.matcher(item);
                    String url = matcher.group(1);
                    String label = matcher.group(2);
                    Button button = new Button(this.getContext());
                    button.setText(label);
                    // TODO: add handler
                    contentColumn.addView(button);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    System.out.printf("failed: %s\n", item);
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
            System.out.println(headingLevels);
            switch (headingLevels) {
                case 1: tv.setTextSize(textSizeBaseline * (20f/11f)); break;
                case 2: tv.setTextSize(textSizeBaseline * (16f/11f)); break;
                case 3: tv.setTextSize(textSizeBaseline * (14f/11f)); break;
                case 4: tv.setTextSize(textSizeBaseline * (12f/11f)); break;
                default: tv.setTextSize(textSizeBaseline);
            }
            System.out.println(tv.getTextSize());
            tv.setText(item.substring(cutoutLevels));
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