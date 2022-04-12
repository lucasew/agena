package com.biglucas.demos;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

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
        for (String item : this.content) {
            TextView tv = new TextView(this.getContext());
            tv.setText(item);
            contentColumn.addView(tv);
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.page_content_fragment, container, false);
    }
}