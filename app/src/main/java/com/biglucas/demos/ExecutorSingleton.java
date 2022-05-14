package com.biglucas.demos;

import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecutorSingleton {
    private static ExecutorService executor = Executors.newSingleThreadExecutor();
    public static ExecutorService getExecutor() {
        if (ExecutorSingleton.executor == null) {
            ExecutorSingleton.executor = Executors.newSingleThreadExecutor();
        }
        return ExecutorSingleton.executor;
    }
}
