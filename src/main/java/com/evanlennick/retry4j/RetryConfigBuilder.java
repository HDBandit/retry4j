package com.evanlennick.retry4j;

import com.evanlennick.retry4j.backoff.*;
import com.evanlennick.retry4j.exception.InvalidRetryConfigException;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.time.temporal.ChronoUnit.*;

public class RetryConfigBuilder {

    private boolean exceptionStrategySpecified;
    private RetryConfig config;
    private boolean validationEnabled;

    public final static String MUST_SPECIFY_BACKOFF__ERROR_MSG
            = "Retry config must specify a backoff strategy!";
    public final static String MUST_SPECIFY_MAX_TRIES__ERROR_MSG
            = "Retry config must specify a maximum number of tries!";
    public final static String MUST_SPECIFY_DELAY__ERROR_MSG
            = "Retry config must specify the delay between retries!";
    public final static String CAN_ONLY_SPECIFY_ONE_BACKOFF_STRAT__ERROR_MSG
            = "Retry config cannot specify more than one backoff strategy!";
    public final static String CAN_ONLY_SPECIFY_ONE_EXCEPTION_STRAT__ERROR_MSG
            = "Retry config cannot specify more than one exception strategy!";

    public static RetryConfigBuilder newConfig() {
        return new RetryConfigBuilder();
    }

    public static RetryConfigBuilder newConfig(boolean validationEnabled) {
        return new RetryConfigBuilder(validationEnabled);
    }

    private RetryConfigBuilder() {
        this.config = new RetryConfig();
        this.exceptionStrategySpecified = false;
        this.validationEnabled = true;
    }

    private RetryConfigBuilder(boolean validationEnabled) {
        this();
        this.validationEnabled = validationEnabled;
    }

    public boolean isValidationEnabled() {
        return validationEnabled;
    }

    public void setValidationEnabled(boolean validationEnabled) {
        this.validationEnabled = validationEnabled;
    }

    public RetryConfigBuilder retryOnAnyException() {
        validateExceptionStrategyAddition();

        config.setRetryOnAnyException(true);

        exceptionStrategySpecified = true;
        return this;
    }

    public RetryConfigBuilder failOnAnyException() {
        validateExceptionStrategyAddition();

        config.setRetryOnAnyException(false);
        config.setRetryOnSpecificExceptions(new HashSet<>());

        exceptionStrategySpecified = true;
        return this;
    }

    @SafeVarargs
    public final RetryConfigBuilder retryOnSpecificExceptions(Class<? extends Exception>... exceptions) {
        validateExceptionStrategyAddition();

        Set<Class<? extends Exception>> setOfExceptions = new HashSet<>(Arrays.asList(exceptions));
        config.setRetryOnSpecificExceptions(setOfExceptions);

        exceptionStrategySpecified = true;
        return this;
    }

    public RetryConfigBuilder withMaxNumberOfTries(int max) {
        config.setMaxNumberOfTries(max);
        return this;
    }

    public RetryConfigBuilder withDelayBetweenTries(Duration duration) {
        config.setDelayBetweenRetries(duration);
        return this;
    }

    public RetryConfigBuilder withDelayBetweenTries(long amount, ChronoUnit time) {
        config.setDelayBetweenRetries(Duration.of(amount, time));
        return this;
    }

    public RetryConfigBuilder withBackoffStrategy(BackoffStrategy backoffStrategy) {
        validateBackoffStrategyAddition();
        config.setBackoffStrategy(backoffStrategy);
        return this;
    }

    public RetryConfigBuilder withFixedBackoff() {
        validateBackoffStrategyAddition();
        config.setBackoffStrategy(new FixedBackoffStrategy());
        return this;
    }

    public RetryConfigBuilder withExponentialBackoff() {
        validateBackoffStrategyAddition();
        config.setBackoffStrategy(new ExponentialBackoffStrategy());
        return this;
    }

    public RetryConfigBuilder withFibonacciBackoff() {
        validateBackoffStrategyAddition();
        config.setBackoffStrategy(new FibonacciBackoffStrategy());
        return this;
    }

    public RetryConfigBuilder withNoWaitBackoff() {
        validateBackoffStrategyAddition();
        config.setBackoffStrategy(new NoWaitBackoffStrategy());
        return this;
    }

    public RetryConfigBuilder withRandomBackoff() {
        validateBackoffStrategyAddition();
        config.setBackoffStrategy(new RandomBackoffStrategy());
        return this;
    }

    public RetryConfigBuilder withRandomExponentialBackoff() {
        validateBackoffStrategyAddition();
        config.setBackoffStrategy(new RandomExponentialBackoffStrategy());
        return this;
    }

    public RetryConfig build() {
        validateConfig();

        return config;
    }

    private void validateConfig() {
        if(!validationEnabled) {
            return;
        }

        if(null == config.getBackoffStrategy()) {
            throw new InvalidRetryConfigException(MUST_SPECIFY_BACKOFF__ERROR_MSG);
        }

        if(null == config.getMaxNumberOfTries()) {
            throw new InvalidRetryConfigException(MUST_SPECIFY_MAX_TRIES__ERROR_MSG);
        }

        if(null == config.getDelayBetweenRetries()) {
            throw new InvalidRetryConfigException(MUST_SPECIFY_DELAY__ERROR_MSG);
        }
    }

    private void validateBackoffStrategyAddition() {
        if(!validationEnabled) {
            return;
        }

        if(null != config.getBackoffStrategy()) {
            throw new InvalidRetryConfigException(CAN_ONLY_SPECIFY_ONE_BACKOFF_STRAT__ERROR_MSG);
        }
    }

    private void validateExceptionStrategyAddition() {
        if(!validationEnabled) {
            return;
        }

        if(exceptionStrategySpecified) {
            throw new InvalidRetryConfigException(CAN_ONLY_SPECIFY_ONE_EXCEPTION_STRAT__ERROR_MSG);
        }
    }

    public RetryConfigBuilder fixedBackoff5Tries10Sec() {
        return new RetryConfigBuilder()
                .retryOnAnyException()
                .withMaxNumberOfTries(5)
                .withDelayBetweenTries(10, SECONDS)
                .withFixedBackoff();
    }

    public RetryConfigBuilder exponentialBackoff5Tries5Sec() {
        return new RetryConfigBuilder()
                .retryOnAnyException()
                .withMaxNumberOfTries(5)
                .withDelayBetweenTries(5, SECONDS)
                .withExponentialBackoff();
    }

    public RetryConfigBuilder fiboBackoff7Tries5Sec() {
        return new RetryConfigBuilder()
                .retryOnAnyException()
                .withMaxNumberOfTries(7)
                .withDelayBetweenTries(5, SECONDS)
                .withFibonacciBackoff();
    }

    public RetryConfigBuilder randomExpBackoff10Tries60Sec() {
        return new RetryConfigBuilder()
                .retryOnAnyException()
                .withMaxNumberOfTries(10)
                .withDelayBetweenTries(60, SECONDS)
                .withRandomExponentialBackoff();
    }

}
