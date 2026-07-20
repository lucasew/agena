package com.biglucas.agena.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.biglucas.agena.R;
import com.biglucas.agena.utils.DebugUIHelper;
import com.biglucas.agena.utils.StacktraceDialogHandler;

import java.io.Serializable;

/**
 * Displays a Gemini page load error and optionally a stack trace dialog.
 * <p>
 * Error text and exception are stored in fragment arguments so they survive
 * configuration changes and process death recreation (the no-arg constructor
 * is used by the framework in those cases).
 */
public class PageErrorFragment extends Fragment {

    private static final String ARG_ERROR = "error";
    private static final String ARG_EXCEPTION = "exception";

    public PageErrorFragment() {
        // Required empty public constructor for fragment recreation
    }

    public PageErrorFragment(String error, Exception e) {
        Bundle args = new Bundle();
        args.putString(ARG_ERROR, error != null ? error : "");
        if (e != null) {
            args.putSerializable(ARG_EXCEPTION, e);
        }
        setArguments(args);
    }

    @NonNull
    private String errorText() {
        Bundle args = getArguments();
        if (args == null) {
            return "";
        }
        String error = args.getString(ARG_ERROR);
        return error != null ? error : "";
    }

    @NonNull
    private Exception errorException() {
        Bundle args = getArguments();
        if (args == null) {
            return new Exception();
        }
        Serializable serialized = args.getSerializable(ARG_EXCEPTION);
        if (serialized instanceof Exception) {
            return (Exception) serialized;
        }
        return new Exception();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        TextView label = view.findViewById(R.id.page_error_label);
        label.setText(errorText());

        Button moreInfoBtn = view.findViewById(R.id.more_information_button);

        // Conditionally show the "More Information" button only in a debug-like context.
        // This prevents leaking stack traces to regular users, mitigating an information
        // disclosure vulnerability.
        if (!DebugUIHelper.hasManageExternalStoragePermission(requireContext())) {
            moreInfoBtn.setVisibility(View.GONE);
            return;
        }
        moreInfoBtn.setOnClickListener(v -> {
            StacktraceDialogHandler.show(requireContext(), errorException());
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.page_error_fragment, container, false);
    }
}
