
package com.sleekydz86.finsight.core.global.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Retryable {
    int maxAttempts() default 3;
    long delay() default 1000;
    double multiplier() default 2.0;
    long maxDelay() default 10000;
    Class<? extends Throwable>[] retryFor() default {Exception.class};
    Class<? extends Throwable>[] noRetryFor() default {};
}