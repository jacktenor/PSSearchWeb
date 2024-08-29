package PSSearchApp;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class PSSearchApp {
    private JFrame frame;
    private JTable processTable;
    private DefaultTableModel tableModel;

    public static void main(String[] args) {
        PSSearchApp app = new PSSearchApp();
        app.createAndShowGUI();
    }

    private void createAndShowGUI() {
        frame = new JFrame("PSSearch - Process Search");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        JPanel panel = new JPanel(new BorderLayout());
        JButton refreshButton = new JButton("Refresh Processes");
        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refreshProcessList();
            }
        });

        tableModel = new DefaultTableModel(new Object[]{"PID", "Process Name"}, 0);
        processTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(processTable);

        panel.add(refreshButton, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        frame.add(panel);
        frame.setVisible(true);

        refreshProcessList();
        setupContextMenu();
    }

    private void refreshProcessList() {
        tableModel.setRowCount(0);  // Clear the table
        List<String[]> processes = getRunningProcesses();
        for (String[] process : processes) {
            tableModel.addRow(process);
        }
    }

    private List<String[]> getRunningProcesses() {
        List<String[]> processes = new ArrayList<>();
        try {
            String line;
            // Execute the `ps -e` command
            Process p = Runtime.getRuntime().exec("ps -e -o pid,comm");
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            input.readLine(); // Skip the header line
            while ((line = input.readLine()) != null) {
                String[] processDetails = line.trim().split("\\s+", 2);
                if (processDetails.length == 2) {
                    processes.add(processDetails);
                }
            }
            input.close();
        } catch (Exception err) {
            err.printStackTrace();
        }
        return processes;
    }
    private void setupContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem searchMenuItem = new JMenuItem("Search Google for this process");
        searchMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selectedRow = processTable.getSelectedRow();
                if (selectedRow != -1) {
                    String processName = (String) tableModel.getValueAt(selectedRow, 1);
                    searchGoogle(processName);
                }
            }
        });
        contextMenu.add(searchMenuItem);

        processTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }

            private void showContextMenu(MouseEvent e) {
                int row = processTable.rowAtPoint(e.getPoint());
                processTable.setRowSelectionInterval(row, row);
                contextMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }

    private void searchGoogle(String processName) {
        try {
            // Encode the process name to make it URL-safe
            String encodedProcessName = URLEncoder.encode(processName, StandardCharsets.UTF_8.toString());
            String url = "https://www.google.com/search?q=" + encodedProcessName;

            // Use Desktop.browse() for platforms where it is supported
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(java.net.URI.create(url));
            } else {
                // Fallback for Linux platforms
                Runtime.getRuntime().exec(new String[]{"firefox", url});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}