package com.sleekydz86.finsight.core.global.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SecurityAudit {
    String action() default "";
    String resource() default "";
    boolean logRequest() default true;
    boolean logResponse() default false;
    SecurityLevel level() default SecurityLevel.INFO;

    enum SecurityLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}