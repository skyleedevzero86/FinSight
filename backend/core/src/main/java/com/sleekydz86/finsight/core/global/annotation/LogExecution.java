package com.sleekydz86.finsight.core.global.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecution {
    String value() default "";
    boolean includeArgs() default true;
    boolean includeResult() default true;
    LogLevel level() default LogLevel.INFO;

    enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
}