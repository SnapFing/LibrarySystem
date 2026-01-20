package panels;

import db.DBHelper;

import javax.swing.*;
import java.awt.*;

/**
 * Panel for monitoring database connection pool statistics in real-time.
 * Useful for administrators to monitor database performance and connection usage.
 */
public class DatabaseMonitorPanel extends JPanel {

    private JLabel activeConnectionsLabel;
    private JLabel idleConnectionsLabel;
    private JLabel totalConnectionsLabel;
    private JLabel threadsAwaitingLabel;
    private JLabel maxPoolSizeLabel;
    private JLabel minIdleLabel;
    private JTextArea statsTextArea;
    private Timer refreshTimer;

    public DatabaseMonitorPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // === TITLE PANEL ===
        JPanel titlePanel = new JPanel();
        JLabel titleLabel = new JLabel("📊 Database Connection Pool Monitor");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titlePanel.add(titleLabel);
        add(titlePanel, BorderLayout.NORTH);

        // === STATISTICS PANEL ===
        JPanel statsPanel = new JPanel(new GridLayout(6, 2, 10, 10));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Real-Time Statistics"));

        // Active Connections
        statsPanel.add(createLabel("Active Connections:", Font.BOLD));
        activeConnectionsLabel = createValueLabel("0");
        statsPanel.add(activeConnectionsLabel);

        // Idle Connections
        statsPanel.add(createLabel("Idle Connections:", Font.BOLD));
        idleConnectionsLabel = createValueLabel("0");
        statsPanel.add(idleConnectionsLabel);

        // Total Connections
        statsPanel.add(createLabel("Total Connections:", Font.BOLD));
        totalConnectionsLabel = createValueLabel("0");
        statsPanel.add(totalConnectionsLabel);

        // Threads Awaiting
        statsPanel.add(createLabel("Threads Awaiting:", Font.BOLD));
        threadsAwaitingLabel = createValueLabel("0");
        statsPanel.add(threadsAwaitingLabel);

        // Max Pool Size
        statsPanel.add(createLabel("Max Pool Size:", Font.BOLD));
        maxPoolSizeLabel = createValueLabel("0");
        statsPanel.add(maxPoolSizeLabel);

        // Min Idle
        statsPanel.add(createLabel("Min Idle:", Font.BOLD));
        minIdleLabel = createValueLabel("0");
        statsPanel.add(minIdleLabel);

        // === DETAILED STATS PANEL ===
        JPanel detailedPanel = new JPanel(new BorderLayout());
        detailedPanel.setBorder(BorderFactory.createTitledBorder("Detailed Pool Information"));

        statsTextArea = new JTextArea(10, 40);
        statsTextArea.setEditable(false);
        statsTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(statsTextArea);
        detailedPanel.add(scrollPane, BorderLayout.CENTER);

        // === CONTROL PANEL ===
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));

        JButton refreshButton = new JButton("🔄 Refresh Now");
        refreshButton.setToolTipText("Manually refresh statistics");
        refreshButton.addActionListener(e -> updateStatistics());

        JButton testConnectionButton = new JButton("🔌 Test Connection");
        testConnectionButton.setToolTipText("Test database connection");
        testConnectionButton.addActionListener(e -> testConnection());

        JButton autoRefreshButton = new JButton("⏱️ Toggle Auto-Refresh");
        autoRefreshButton.setToolTipText("Toggle automatic refresh every 2 seconds");
        autoRefreshButton.addActionListener(e -> toggleAutoRefresh());

        controlPanel.add(refreshButton);
        controlPanel.add(testConnectionButton);
        controlPanel.add(autoRefreshButton);

        // === LAYOUT ===
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.add(statsPanel, BorderLayout.NORTH);
        topPanel.add(detailedPanel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        // Initial update
        updateStatistics();

        // Start auto-refresh timer (every 2 seconds)
        startAutoRefresh();
    }

    private JLabel createLabel(String text, int style) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", style, 13));
        return label;
    }

    private JLabel createValueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(new Color(0, 120, 215)); // Blue color
        return label;
    }

    private void updateStatistics() {
        try {
            // Update numeric labels
            int active = DBHelper.getActiveConnections();
            int idle = DBHelper.getIdleConnections();
            int total = active + idle;

            activeConnectionsLabel.setText(String.valueOf(active));
            idleConnectionsLabel.setText(String.valueOf(idle));
            totalConnectionsLabel.setText(String.valueOf(total));

            // Color code based on usage
            if (active > 7) {
                activeConnectionsLabel.setForeground(Color.RED); // High usage
            } else if (active > 4) {
                activeConnectionsLabel.setForeground(Color.ORANGE); // Medium usage
            } else {
                activeConnectionsLabel.setForeground(new Color(0, 150, 0)); // Low usage
            }

            // Get detailed stats
            String detailedStats = DBHelper.getPoolStats();
            statsTextArea.setText(detailedStats);

            // Update pool configuration labels
            Object ds = DBHelper.getDataSource();
            if (ds != null) {
                try {
                    // Use reflection to avoid compile-time dependency on Hikari classes
                    Class<?> cls = ds.getClass();
                    java.lang.reflect.Method mMax = cls.getMethod("getMaximumPoolSize");
                    java.lang.reflect.Method mMin = cls.getMethod("getMinimumIdle");
                    Object maxVal = mMax.invoke(ds);
                    Object minVal = mMin.invoke(ds);
                    maxPoolSizeLabel.setText(String.valueOf(maxVal));
                    minIdleLabel.setText(String.valueOf(minVal));

                    java.lang.reflect.Method mMxBean = cls.getMethod("getHikariPoolMXBean");
                    Object mxBean = mMxBean.invoke(ds);
                    if (mxBean != null) {
                        Class<?> mxCls = mxBean.getClass();
                        java.lang.reflect.Method mThreads = mxCls.getMethod("getThreadsAwaitingConnection");
                        Object threads = mThreads.invoke(mxBean);
                        threadsAwaitingLabel.setText(String.valueOf(threads));

                        int waiting = Integer.parseInt(String.valueOf(threads));
                        if (waiting > 0) {
                            threadsAwaitingLabel.setForeground(Color.RED);
                        } else {
                            threadsAwaitingLabel.setForeground(new Color(0, 150, 0));
                        }
                    }

                } catch (NoSuchMethodException nsme) {
                    // Pool doesn't expose these methods - ignore
                } catch (Exception ex) {
                    // Any reflection error, log to detailed area
                    statsTextArea.append("\n(Warning) Could not read pool internals: " + ex.getMessage());
                }
            }

        } catch (Exception ex) {
            statsTextArea.setText("Error fetching statistics:\n" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void testConnection() {
        boolean success = DBHelper.testConnection();
        if (success) {
            JOptionPane.showMessageDialog(this,
                    "✅ Database connection successful!",
                    "Connection Test",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "❌ Database connection failed!\nCheck console for details.",
                    "Connection Test",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startAutoRefresh() {
        if (refreshTimer == null) {
            refreshTimer = new Timer(2000, e -> updateStatistics()); // Refresh every 2 seconds
            refreshTimer.start();
        }
    }

    private void toggleAutoRefresh() {
        if (refreshTimer != null) {
            if (refreshTimer.isRunning()) {
                refreshTimer.stop();
                JOptionPane.showMessageDialog(this,
                        "⏸️ Auto-refresh stopped",
                        "Auto-Refresh",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                refreshTimer.start();
                JOptionPane.showMessageDialog(this,
                        "▶️ Auto-refresh started (every 2 seconds)",
                        "Auto-Refresh",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    // Stop timer when panel is removed
    @Override
    public void removeNotify() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        super.removeNotify();
    }
}