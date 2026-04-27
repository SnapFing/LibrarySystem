package com.librarysystem.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton class to manage refresh events across different panels in the application.
 * This allows one panel to trigger refresh on another panel when data changes.
 *
 * Usage:
 * 1. Panels implement RefreshListener interface
 * 2. Register with RefreshManager: RefreshManager.getInstance().addRefreshListener(this)
 * 3. When data changes, call: RefreshManager.getInstance().notifyRefresh("members")
 */
public class RefreshManager {
    private static RefreshManager instance;
    private Map<String, List<RefreshListener>> listeners = new HashMap<>();

    private RefreshManager() {}

    /**
     * Get the singleton instance
     */
    public static RefreshManager getInstance() {
        if (instance == null) {
            instance = new RefreshManager();
        }
        return instance;
    }

    /**
     * Register a listener for a specific panel type
     * @param panelType The type of panel (e.g., "members", "books", "borrow")
     * @param listener The listener to register
     */
    public void addRefreshListener(String panelType, RefreshListener listener) {
        if (!listeners.containsKey(panelType)) {
            listeners.put(panelType, new ArrayList<>());
        }

        if (!listeners.get(panelType).contains(listener)) {
            listeners.get(panelType).add(listener);
            System.out.println("✅ Registered listener for: " + panelType);
        }
    }

    /**
     * Remove a listener
     */
    public void removeRefreshListener(String panelType, RefreshListener listener) {
        if (listeners.containsKey(panelType)) {
            listeners.get(panelType).remove(listener);
            System.out.println("❌ Removed listener for: " + panelType);
        }
    }

    /**
     * Notify all listeners of a specific type to refresh
     * @param panelType The type of panel to refresh
     */
    public void notifyRefresh(String panelType) {
        if (listeners.containsKey(panelType)) {
            System.out.println("🔄 Notifying refresh for: " + panelType + " (" + listeners.get(panelType).size() + " listeners)");
            for (RefreshListener listener : listeners.get(panelType)) {
                try {
                    listener.onRefresh();
                } catch (Exception e) {
                    System.err.println("Error refreshing listener: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Notify all registered listeners to refresh (refresh everything)
     */
    public void notifyRefreshAll() {
        System.out.println("🔄 Notifying refresh for ALL panels");
        for (String panelType : listeners.keySet()) {
            notifyRefresh(panelType);
        }
    }

    /**
     * Interface for panels that want to be notified of refresh events
     */
    public interface RefreshListener {
        /**
         * Called when this panel should refresh its data
         */
        void onRefresh();
    }

    /**
     * Common panel types (constants for consistency)
     */
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