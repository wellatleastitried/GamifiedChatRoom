package com.walit.lifeClient.Frame;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.concurrent.TimeUnit;

public class SimulationRender extends JFrame {

    JPanel[][] scene;

    public SimulationRender(int y, int x) {
        super("Game of Life Simulation");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        scene = new JPanel[x][y];

        GridLayout gridLayout = new GridLayout(x, y);
        setLayout(gridLayout);

        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                JPanel panel = new JPanel();
                panel.setBackground(Color.WHITE);
                scene[i][j] = panel;
                Border border = BorderFactory.createLineBorder(Color.GRAY, 1);
                panel.setBorder(border);
                add(panel);
            }
        }

        int panelWidth = 50;
        int panelHeight = 50;
        int frameWidth = x * panelWidth;
        int frameHeight = y * panelHeight;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = (int) screenSize.getWidth();
        int screenHeight = (int) screenSize.getHeight();
        setSize(Math.min(frameWidth, screenWidth), Math.min(frameHeight, screenHeight));

        setLocationRelativeTo(null);

        setVisible(true);
    }

    public void renderNextScene(int[][] state) {
        for (int i = 0; i < state.length; i++) {
            for (int j = 0; j < state[i].length; j++) {
                scene[i][j].setBackground(state[i][j] == 1 ? Color.BLACK : Color.WHITE);
            }
        }
        revalidate();
        repaint();
    }

    public void shutdown() {
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException iE) {
            Thread.currentThread().interrupt();
        }
        setVisible(false);
        dispose();
    }
}
