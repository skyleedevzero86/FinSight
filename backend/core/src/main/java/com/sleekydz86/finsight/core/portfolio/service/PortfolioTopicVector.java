package com.sleekydz86.finsight.core.portfolio.service;

public final class PortfolioTopicVector {

    private PortfolioTopicVector() {
    }

    public static float[] embed(String label, String message, int dims) {
        int size = Math.min(4096, Math.max(8, dims));
        float[] vector = new float[size];
        add(vector, label, 4f);
        add(vector, message, 1f);
        normalize(vector);
        return vector;
    }

    public static double cosine(float[] left, float[] right) {
        if (left == null || right == null || left.length == 0 || left.length != right.length) {
            return 0;
        }
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int index = 0; index < left.length; index++) {
            dot += left[index] * right[index];
            leftNorm += left[index] * left[index];
            rightNorm += right[index] * right[index];
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / Math.sqrt(leftNorm * rightNorm);
    }

    public static double fromElasticsearchScore(double score) {
        return score * 2.0 - 1.0;
    }

    private static void add(float[] vector, String text, float weight) {
        if (text == null || text.isBlank()) {
            return;
        }
        int[] points = text.trim().toLowerCase(java.util.Locale.ROOT).codePoints()
                .filter(point -> !Character.isWhitespace(point))
                .toArray();
        for (int index = 0; index < points.length; index++) {
            bump(vector, points[index], weight);
            if (index + 1 < points.length) {
                bump(vector, points[index] * 31 + points[index + 1], weight * 1.5f);
            }
        }
    }

    private static void bump(float[] vector, int hash, float weight) {
        int index = Math.floorMod(hash, vector.length);
        float sign = (hash & 1) == 0 ? weight : -weight;
        vector[index] += sign;
    }

    private static void normalize(float[] vector) {
        double norm = 0;
        for (float value : vector) {
            norm += value * value;
        }
        if (norm == 0) {
            return;
        }
        float scale = (float) (1.0 / Math.sqrt(norm));
        for (int index = 0; index < vector.length; index++) {
            vector[index] *= scale;
        }
    }
}
