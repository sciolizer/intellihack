package com.sciolizer.intellihack;

import org.junit.Test;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CountDownLatch;

// First created by Joshua Ball on 12/31/13 at 9:32 AM
public class SwingSandbox {

    @Test
    public void testShowLabel() throws Exception {
        showComponent(new JLabel("Hello"));

    }

    @Test
    public void testTextBoxInPanel() throws Exception {
        JPanel jPanel = new JPanel();
        jPanel.add(new JTextArea(""));
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.X_AXIS));
        showComponent(jPanel);
    }

    private void showComponent(JComponent component) throws InterruptedException {
        JFrame frame = new JFrame("Component shower");
        frame.add(component);
        frame.pack();
        frame.setVisible(true);
        final CountDownLatch cdl = new CountDownLatch(1);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cdl.countDown();
            }
        });
        cdl.await();
    }
}
