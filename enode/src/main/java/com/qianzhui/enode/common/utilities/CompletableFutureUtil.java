package com.qianzhui.enode.common.utilities;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Created by junbo_xu on 2016/7/5.
 */
public class CompletableFutureUtil {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
            1,
            new ThreadFactoryBuilder()
                    .setDaemon(false)
                    .setNameFormat("failAfter-%d")
                    .build());

    private static <T> CompletableFuture<T> failAfter(Duration duration) {
        final CompletableFuture<T> promise = new CompletableFuture<>();

        scheduler.schedule(() -> {
            final TimeoutException ex = new TimeoutException("Timeout after " + duration);
            promise.completeExceptionally(ex);
        }, duration.toMillis(), TimeUnit.MILLISECONDS);

        return promise;
    }

    public static <T> CompletableFuture<T> within(CompletableFuture<T> future, Duration duration) {
        final CompletableFuture<T> timeout = failAfter(duration);
        return future.applyToEither(timeout, Function.identity());
    }
}
