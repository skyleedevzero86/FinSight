package com.sleekydz86.finsight.core.global.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Transactional {
    boolean readOnly() default false;
    int timeout() default -1;
    String[] rollbackFor() default {};
    String[] noRollbackFor() default {};
    Propagation propagation() default Propagation.REQUIRED;

    enum Propagation {
        REQUIRED, REQUIRES_NEW, SUPPORTS, NOT_SUPPORTED, NEVER, MANDATORY, NESTED
    }
}