package com.evanlennick.retry4j;

import com.evanlennick.retry4j.annotation.RetryMethod;
import com.evanlennick.retry4j.backoff.FixedBackoffStrategy;
import com.evanlennick.retry4j.exception.InvalidRetryMethod;
import com.evanlennick.retry4j.exception.RetriesExhaustedException;
import com.evanlennick.retry4j.exception.UnexpectedException;
import com.evanlennick.retry4j.listener.AfterFailedTryListener;
import com.evanlennick.retry4j.listener.BeforeNextTryListener;
import com.evanlennick.retry4j.listener.RetryListener;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class CallExecutor {

    private RetryConfig config;

    private AfterFailedTryListener afterFailedTryListener;

    private BeforeNextTryListener beforeNextTryListener;

    private CallResults results = new CallResults();

    public CallExecutor() {
        this(new RetryConfigBuilder().fixedBackoff5Tries10Sec().build());
    }

    public CallExecutor(RetryConfig config) {
        this.config = config;
    }

    public CallResults execute(Class clazz, Method method) {
        return execute(clazz, method, new Object[0]);
    }

    public CallResults execute(Class clazz, Method method, Object... params) {
        RetryMethod[] annotations = method.getAnnotationsByType(RetryMethod.class);
        if (annotations.length != 1) {
            throw new InvalidRetryMethod("You must specify the @RetryMethod annotation on the invoked method!");
        }

        RetryMethod retryMethod = annotations[0];
        RetryConfig config = parseAndGetConfigFromRetryMethodAnnotation(retryMethod);
        this.config = config;

        Callable callable = () -> method.invoke(clazz.newInstance(), params);
        return execute(callable);
    }

    private RetryConfig parseAndGetConfigFromRetryMethodAnnotation(RetryMethod retryMethod) {
        RetryConfig thisConfig = new RetryConfig();

        Duration duration = Duration.of(retryMethod.delay(), retryMethod.timeUnit());
        thisConfig.setDelayBetweenRetries(duration);
        thisConfig.setMaxNumberOfTries(retryMethod.maxNumberOfTries());

        Set<Class<? extends Exception>> retryOnExceptions = new HashSet<>();
        Collections.addAll(retryOnExceptions, retryMethod.retryOnExceptions());
        thisConfig.setRetryOnSpecificExceptions(retryOnExceptions);

        thisConfig.setBackoffStrategy(new FixedBackoffStrategy()); //TODO make this work

        return thisConfig;
    }

    public CallResults execute(Callable<?> callable) throws RetriesExhaustedException, UnexpectedException {
        long start = System.currentTimeMillis();
        results.setStartTime(start);

        int maxTries = config.getMaxNumberOfTries();
        long millisBetweenTries = config.getDelayBetweenRetries().toMillis();
        this.results.setCallName(callable.toString());

        Optional<Object> result = Optional.empty();
        int tries;

        for (tries = 0; tries < maxTries && !result.isPresent(); tries++) {
            result = tryCall(callable);

            if (!result.isPresent()) {
                handleRetry(millisBetweenTries, tries + 1);
            }
        }

        refreshRetryResults(result.isPresent(), tries);
        results.setEndTime(System.currentTimeMillis());

        if (!result.isPresent()) {
            String failureMsg = String.format("Call '%s' failed after %d tries!", callable.toString(), maxTries);
            throw new RetriesExhaustedException(failureMsg, results);
        } else {
            results.setResult(result.get());
            return results;
        }
    }

    private Optional<Object> tryCall(Callable<?> callable) throws UnexpectedException {
        try {
            Object result = callable.call();
            return Optional.of(result);
        } catch (Exception e) {
            if (shouldThrowException(e)) {
                throw new UnexpectedException(e);
            } else {
                return Optional.empty();
            }
        }
    }

    private void handleRetry(long millisBetweenTries, int tries) {
        refreshRetryResults(false, tries);

        if (null != afterFailedTryListener) {
            afterFailedTryListener.immediatelyAfterFailedTry(results);
        }

        sleep(millisBetweenTries, tries);

        if (null != beforeNextTryListener) {
            beforeNextTryListener.immediatelyBeforeNextTry(results);
        }
    }

    private void refreshRetryResults(boolean success, int tries) {
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - results.getStartTime();

        results.setTotalTries(tries);
        results.setTotalElapsedDuration(Duration.of(elapsed, ChronoUnit.MILLIS));
        results.setSuccessful(success);
    }

    private void sleep(long millis, int tries) {
        Duration duration = Duration.of(millis, ChronoUnit.MILLIS);
        long millisToSleep = config.getBackoffStrategy().getMillisToWait(tries, duration);

        try {
            TimeUnit.MILLISECONDS.sleep(millisToSleep);
        } catch (InterruptedException ignored) {
        }
    }

    private boolean shouldThrowException(Exception e) {
        if (this.config.isRetryOnAnyException()) {
            return false;
        }

        for (Class<? extends Exception> exceptionInSet : this.config.getRetryOnSpecificExceptions()) {
            if (e.getClass().isAssignableFrom(exceptionInSet)) {
                return false;
            }
        }

        return true;
    }

    public void registerRetryListener(RetryListener listener) {
        if (listener instanceof AfterFailedTryListener) {
            this.afterFailedTryListener = (AfterFailedTryListener) listener;
        } else if (listener instanceof BeforeNextTryListener) {
            this.beforeNextTryListener = (BeforeNextTryListener) listener;
        } else {
            throw new IllegalArgumentException("Tried to register an unrecognized RetryListener!");
        }
    }
}
