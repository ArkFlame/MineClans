package com.arkflame.mineclans.providers;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DatabaseExecutor {
    private final ExecutorService executor;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public DatabaseExecutor() {
        this.executor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "MineClans-Database-" + System.nanoTime());
            t.setDaemon(false);
            return t;
        });
    }

    public <T> CompletableFuture<T> supply(java.util.concurrent.Callable<T> callable) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return callable.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new java.util.concurrent.CompletionException(e);
            }
        }, executor);
    }

    public CompletableFuture<Void> run(CheckedRunnable runnable) {
        return CompletableFuture.runAsync(() -> {
            try {
                runnable.run();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new java.util.concurrent.CompletionException(e);
            }
        }, executor);
    }

    public void shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    @FunctionalInterface
    public interface CheckedRunnable {
        void run() throws Exception;
    }
}
