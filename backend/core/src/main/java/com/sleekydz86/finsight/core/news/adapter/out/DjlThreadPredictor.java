package com.sleekydz86.finsight.core.news.adapter.out;

import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.ZooModel;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class DjlThreadPredictor<I, O> implements AutoCloseable {

    private final ZooModel<I, O> model;
    private final Set<Predictor<I, O>> opened = ConcurrentHashMap.newKeySet();
    private final ThreadLocal<Predictor<I, O>> current = new ThreadLocal<>();

    public DjlThreadPredictor(ZooModel<I, O> model) {
        this.model = model;
    }

    public Predictor<I, O> borrow() {
        Predictor<I, O> predictor = current.get();
        if (predictor != null) {
            return predictor;
        }
        Predictor<I, O> created = model.newPredictor();
        opened.add(created);
        current.set(created);
        return created;
    }

    @Override
    public void close() {
        for (Predictor<I, O> predictor : opened) {
            predictor.close();
        }
        opened.clear();
        current.remove();
    }
}
