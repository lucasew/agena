package com.biglucas.agena.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.biglucas.agena.R;
import com.biglucas.agena.utils.StacktraceDialogHandler;

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
    public void onViewCreated(View view, Bundle savedInstanceState) {
        TextView label = this.getActivity().findViewById(R.id.page_error_label);
        label.setText(this.error);

        Button moreInfoBtn = this.getActivity().findViewById(R.id.more_information_button);
        moreInfoBtn.setOnClickListener(v -> {
            System.out.println("* click *");
            new StacktraceDialogHandler(this.exception).show(v);
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.page_error_fragment, container, false);
    }
}