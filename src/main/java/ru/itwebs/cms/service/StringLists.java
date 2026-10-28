package ru.itwebs.cms.service;

import java.util.*;

/** Ordered labels: trim whitespace, remove duplicates, retain original spelling. */
public final class StringLists {
    private StringLists() { }
    public static List<String> normalize(Collection<String> source) {
        if (source == null) return List.of();
        Map<String, String> labels = new LinkedHashMap<>();
        for (String item : source) {
            if (item == null || item.isBlank()) continue;
            String label = item.trim();
            labels.putIfAbsent(label.toLowerCase(Locale.ROOT), label);
        }
        return List.copyOf(labels.values());
    }
}
