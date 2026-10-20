package com.piet.solar;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class CaptureSnapshots {
    public static void main(String[] args) throws Exception {
        File dir = new File("/workspaces/first_java_program/snapshots");
        dir.mkdirs();

        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        App.Database.initialize();

        // 1. Capture Login Frame
        App.LoginFrame login = new App.LoginFrame();
        login.setSize(430, 260);
        login.setVisible(true);
        Thread.sleep(800);
        save(login, new File(dir, "01_login_screen.png"));
        login.dispose();

        // 2. Capture MainFrame and its tabs
        App.MainFrame main = new App.MainFrame(1, "Project Administrator", "Administrator");
        main.setSize(1100, 700);
        main.setVisible(true);
        Thread.sleep(1200);

        JTabbedPane tabs = null;
        for (Component c : main.getContentPane().getComponents()) {
            if (c instanceof JTabbedPane tp) {
                tabs = tp;
                break;
            }
        }

        String[] names = {
            "02_dashboard_kpi.png",
            "03_equipment_tab.png",
            "04_maintenance_schedule_tab.png",
            "05_maintenance_records_tab.png",
            "06_equipment_parts_tab.png",
            "07_users_tab.png"
        };

        if (tabs != null) {
            final JTabbedPane targetTabs = tabs;
            for (int i = 0; i < targetTabs.getTabCount() && i < names.length; i++) {
                final int idx = i;
                SwingUtilities.invokeAndWait(() -> targetTabs.setSelectedIndex(idx));
                Thread.sleep(600);
                save(main, new File(dir, names[i]));
            }
        }
        main.dispose();
        System.out.println("ALL_SNAPSHOTS_DONE");
        System.exit(0);
    }

    static void save(Component comp, File file) throws Exception {
        BufferedImage image = new BufferedImage(comp.getWidth(), comp.getHeight(), BufferedImage.TYPE_INT_RGB);
        comp.paint(image.getGraphics());
        ImageIO.write(image, "PNG", file);
        System.out.println("Successfully saved: " + file.getName());
    }
}
