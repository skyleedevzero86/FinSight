package com.sleekydz86.finsight.core.global.aspect;

import com.sleekydz86.finsight.core.global.annotation.Cacheable;
import com.sleekydz86.finsight.core.global.cache.AdvancedCacheManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class CachingAspect {

    private static final Logger logger = LoggerFactory.getLogger(CachingAspect.class);

    @Autowired
    private AdvancedCacheManager cacheManager;

    @Around("@annotation(cacheable)")
    public Object cache(ProceedingJoinPoint joinPoint, Cacheable cacheable) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        Object[] args = joinPoint.getArgs();

        String cacheName = getCacheName(cacheable, className);
        String cacheKey = generateCacheKey(cacheable, className, methodName, args);

        if (cacheable.refresh()) {
            logger.debug("Refreshing cache for {}.{}", className, methodName);
            Object result = joinPoint.proceed();
            cacheManager.put(cacheName, cacheKey, result);
            return result;
        }

        Object cachedResult = cacheManager.get(cacheName, cacheKey, Object.class);
        if (cachedResult != null) {
            logger.debug("Cache hit for {}.{}", className, methodName);
            return cachedResult;
        }

        logger.debug("Cache miss for {}.{}", className, methodName);
        Object result = joinPoint.proceed();
        cacheManager.put(cacheName, cacheKey, result);
        return result;
    }

    private String getCacheName(Cacheable cacheable, String className) {
        if (!cacheable.value().isEmpty()) {
            return cacheable.value();
        }
        return className.toLowerCase() + "Cache";
    }

    private String generateCacheKey(Cacheable cacheable, String className, String methodName, Object[] args) {
        if (!cacheable.key().isEmpty()) {
            return cacheable.key();
        }

        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(className).append(".").append(methodName);

        if (args != null && args.length > 0) {
            keyBuilder.append(":");
            for (Object arg : args) {
                keyBuilder.append(arg != null ? arg.toString() : "null").append(",");
            }
            keyBuilder.setLength(keyBuilder.length() - 1);
        }

        return keyBuilder.toString();
    }
}