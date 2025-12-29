package com.biglucas.agena.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.biglucas.agena.BuildConfig;
import com.biglucas.agena.R;
import com.biglucas.agena.utils.StacktraceDialogHandler;

import java.util.Objects;

public class PageErrorFragment extends Fragment {

    private final Exception exception;
    private final String error;

    public PageErrorFragment() {
        this.exception = new Exception();
        this.error = "";
    }

    public PageErrorFragment(String error, Exception e) {
        this.error = error;
        this.exception = e;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        TextView label = this.requireActivity().findViewById(R.id.page_error_label);
        label.setText(this.error);

        Button moreInfoBtn = requireActivity().findViewById(R.id.more_information_button);

        if (BuildConfig.DEBUG) {
            moreInfoBtn.setVisibility(View.VISIBLE);
            moreInfoBtn.setOnClickListener(v -> {
                new StacktraceDialogHandler(this.exception).show(v);
            });
        } else {
            moreInfoBtn.setVisibility(View.GONE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.page_error_fragment, container, false);
    }
}