package com.idrnd.idvoice.utils.runners;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SingleTaskRunner used to run task in a background thread and return result of work to the main thread
 */
public class SingleTaskRunner {

    private final String TAG = SingleTaskRunner.class.getSimpleName();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * Function called in the main thread. It is used for performing some operations with the background thread's work result
     * @param <R> type returned background thread's work result
     */
    public interface Callback<R> {
        void onComplete(R result);
    }

    /**
     * Function used to run task in a background thread and return result of work to the main thread
     * @param callable some work for background thread
     * @param <R> type returned background thread's work result
     */
    public <R> void execute(Callable<R> callable, Callback<R> callback) {
        executor.execute(() -> {
            final R result;
            try {
                result = callable.call();
                if(result != null)
                    handler.post(() -> callback.onComplete(result));
            } catch (Exception e) {
                Log.e(TAG, "Unknown problem", e);
            }
        });
    }

    /**
     * Function used to run task in background thread
     * @param runnable some work for background thread
     */
    public void execute(Runnable runnable) {
        executor.execute(runnable);
    }
}