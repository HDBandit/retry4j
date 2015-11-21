package com.evanlennick.retry4j.annotation;

import com.evanlennick.retry4j.CallExecutor;
import com.evanlennick.retry4j.CallResults;
import com.evanlennick.retry4j.exception.RetriesExhaustedException;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.temporal.ChronoUnit;

public class RetryMethodTest {

    @RetryMethod(delay = 1,
            timeUnit = ChronoUnit.MILLIS,
            maxNumberOfTries = 5,
            backoffStrategy = BackoffStrategyEnums.FIXED,
            retryOnExceptions = {RuntimeException.class, InvocationTargetException.class})
    public String methodToRetry() {
        System.out.println("Inside retry method!");
        throw new RuntimeException("test");
    }

    @Test(expectedExceptions = RetriesExhaustedException.class)
    public void testAnnotation() throws ClassNotFoundException, NoSuchMethodException {
        Class c = Class.forName("com.evanlennick.retry4j.annotation.RetryMethodTest");
        System.out.println("c = " + c);
        Method m = c.getDeclaredMethod("methodToRetry", new Class[0]);
        System.out.println("m = " + m);

        CallResults results = new CallExecutor().execute(c, m);
        System.out.println("results = " + results);
    }
}
