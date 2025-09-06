package com.sleekydz86.finsight.core.global.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PerformanceMonitor {
    long threshold() default 1000;
    boolean includeArgs() default false;
    boolean includeResult() default false;
    String metricName() default "";
}