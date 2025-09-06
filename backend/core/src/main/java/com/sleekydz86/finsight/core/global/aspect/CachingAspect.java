package com.sleekydz86.finsight.core.global.aspect;

import com.sleekydz86.finsight.core.global.annotation.Cacheable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

@Aspect
@Component
public class CachingAspect {

    private static final Logger logger = LoggerFactory.getLogger(CachingAspect.class);

    @Autowired
    private CacheManager cacheManager;

    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(cacheable)")
    public Object cache(ProceedingJoinPoint joinPoint, Cacheable cacheable) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();

        String cacheKey = generateCacheKey(cacheable, joinPoint, className, methodName);
        String cacheName = className + "." + methodName;

        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            logger.warn("Cache '{}' not found, proceeding without caching", cacheName);
            return joinPoint.proceed();
        }

        Cache.ValueWrapper cachedValue = cache.get(cacheKey);
        if (cachedValue != null && !cacheable.refresh()) {
            logger.debug("Cache hit for key: {}", cacheKey);
            return cachedValue.get();
        }

        logger.debug("Cache miss for key: {}, executing method", cacheKey);
        Object result = joinPoint.proceed();

        if (result != null) {
            cache.put(cacheKey, result);
            logger.debug("Cached result for key: {}", cacheKey);
        }

        return result;
    }

    private String generateCacheKey(Cacheable cacheable, ProceedingJoinPoint joinPoint,
                                    String className, String methodName) {
        if (!cacheable.key().isEmpty()) {
            return evaluateSpelExpression(cacheable.key(), joinPoint);
        }

        Object[] args = joinPoint.getArgs();
        return className + "." + methodName + "(" + Arrays.toString(args) + ")";
    }

    private String evaluateSpelExpression(String expression, ProceedingJoinPoint joinPoint) {
        try {
            Expression exp = parser.parseExpression(expression);
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("args", joinPoint.getArgs());
            context.setVariable("target", joinPoint.getTarget());
            return exp.getValue(context, String.class);
        } catch (Exception e) {
            logger.warn("Failed to evaluate SpEL expression: {}, using default key", expression);
            return "default";
        }
    }
}