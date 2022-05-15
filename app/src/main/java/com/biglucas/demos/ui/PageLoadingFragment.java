package com.biglucas.demos.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.biglucas.demos.R;

public class PageLoadingFragment extends Fragment {

    public PageLoadingFragment() {
        // Required empty public constructor
    }

    public static PageLoadingFragment newInstance(String param1, String param2) {
        PageLoadingFragment fragment = new PageLoadingFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.page_loading_fragment, container, false);
    }
}