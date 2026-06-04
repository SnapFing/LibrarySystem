package com.librarysystem.utils;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

public class RefreshManager {
    private static final Logger LOG = Logger.getLogger(RefreshManager.class.getName());
    private static final RefreshManager INSTANCE = new RefreshManager();
    private final Map<String, CopyOnWriteArrayList<RefreshListener>> listeners = new HashMap<>();
    private final Map<String, java.util.Timer> debounceTimers = new HashMap<>();

    public static RefreshManager getInstance() { return INSTANCE; }

    public synchronized void addRefreshListener(String panelType, RefreshListener listener) {
        listeners.computeIfAbsent(panelType, k -> new CopyOnWriteArrayList<>()).add(listener);
        LOG.fine("Listener added: " + panelType);
    }

    public synchronized void removeRefreshListener(String panelType, RefreshListener listener) {
        List<RefreshListener> list = listeners.get(panelType);
        if (list != null) list.remove(listener);
    }

    public void notifyRefresh(String panelType) {
        List<RefreshListener> list = listeners.get(panelType);
        if (list == null || list.isEmpty()) return;

        // Debounce: cancel pending and schedule new in 100ms
        java.util.Timer timer = debounceTimers.get(panelType);
        if (timer != null) timer.cancel();
        timer = new java.util.Timer("RefreshDebounce-" + panelType, true);
        debounceTimers.put(panelType, timer);
        timer.schedule(new java.util.TimerTask() {
            @Override public void run() {
                SwingUtilities.invokeLater(() -> {
                    for (RefreshListener l : list) {
                        try { l.onRefresh(); } catch (Exception e) {
                            LOG.log(Level.WARNING, "Refresh failed", e);
                        }
                    }
                });
            }
        }, 100);
    }

    public void notifyRefreshAll() {
        for (String type : listeners.keySet()) notifyRefresh(type);
    }

    public interface RefreshListener { void onRefresh(); }

    // Constants
    public static final String PANEL_MEMBERS = "members";
    public static final String PANEL_BOOKS = "books";
    public static final String PANEL_BORROW = "borrow";
    public static final String PANEL_FINES = "fines";
    public static final String PANEL_USERS = "users";
    public static final String PANEL_DASHBOARD = "dashboard";
    public static final String PANEL_STUDENT_BOOKS = "student_books";
    public static final String PANEL_MY_BORROWED = "my_borrowed";
    public static final String PANEL_STUDENT_PROFILE = "student_profile";
}