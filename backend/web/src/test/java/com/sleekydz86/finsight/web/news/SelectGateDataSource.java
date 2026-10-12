package com.sleekydz86.finsight.web.news;

import org.springframework.jdbc.datasource.ConnectionProxy;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

class SelectGateDataSource extends DelegatingDataSource {

    private volatile CyclicBarrier barrier;
    private final AtomicInteger awaiting = new AtomicInteger();

    SelectGateDataSource(DataSource target) {
        super(target);
    }

    void arm(int parties) {
        awaiting.set(parties);
        barrier = new CyclicBarrier(parties);
    }

    void disarm() {
        barrier = null;
        awaiting.set(0);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return gate(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return gate(super.getConnection(username, password));
    }

    private Connection gate(Connection connection) {
        InvocationHandler handler = (proxy, method, args) -> invoke(connection, proxy, method, args);
        return (Connection) Proxy.newProxyInstance(
                SelectGateDataSource.class.getClassLoader(),
                new Class<?>[]{Connection.class, ConnectionProxy.class},
                handler);
    }

    private Object invoke(Connection connection, Object proxy, java.lang.reflect.Method method, Object[] args)
            throws Throwable {
        if ("getTargetConnection".equals(method.getName())) {
            return connection;
        }
        if ("unwrap".equals(method.getName()) && args != null && args[0] instanceof Class<?> type && type.isInstance(connection)) {
            return connection;
        }
        if ("isWrapperFor".equals(method.getName()) && args != null && args[0] instanceof Class<?> type) {
            return type.isInstance(connection) || type.isInstance(proxy);
        }
        if (method.getName().startsWith("prepare") && args != null && args[0] instanceof String sql
                && isSourceUrlLookup(sql)) {
            return gateStatement(call(connection, method, args));
        }
        return call(connection, method, args);
    }

    private PreparedStatement gateStatement(Object statement) {
        PreparedStatement prepared = (PreparedStatement) statement;
        InvocationHandler handler = (proxy, method, args) -> {
            Object result = call(prepared, method, args);
            if (method.getName().startsWith("execute")) {
                passAfterLookup();
            }
            return result;
        };
        return (PreparedStatement) Proxy.newProxyInstance(
                SelectGateDataSource.class.getClassLoader(),
                new Class<?>[]{PreparedStatement.class},
                handler);
    }

    private void passAfterLookup() throws SQLException {
        CyclicBarrier open = barrier;
        if (open == null || awaiting.getAndDecrement() <= 0) {
            return;
        }
        try {
            open.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SQLException("동시 조회 대기가 중단되었습니다", exception);
        } catch (BrokenBarrierException | TimeoutException exception) {
            throw new SQLException("동시 조회 대기가 맞지 않았습니다", exception);
        }
    }

    private static boolean isSourceUrlLookup(String sql) {
        return sql.toLowerCase(Locale.ROOT).contains("source_url in");
    }

    private static Object call(Object target, java.lang.reflect.Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }
}
