package com.evanlennick.retry4j.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.temporal.ChronoUnit;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RetryMethod {

    Class<? extends Exception>[] retryOnExceptions() default {};

    int maxNumberOfTries();

    int delay();

    ChronoUnit timeUnit() default ChronoUnit.SECONDS;

    BackoffStrategyEnums backoffStrategy() default BackoffStrategyEnums.FIXED;

}
