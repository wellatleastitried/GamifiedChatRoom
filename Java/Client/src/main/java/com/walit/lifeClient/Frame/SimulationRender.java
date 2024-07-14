package com.walit.lifeClient.Frame;

import javax.swing.*;
import java.awt.*;

public class SimulationRender extends JFrame {

    JPanel[][] scene;

    public SimulationRender(int x, int y) {
        super("Game of Life Simulation");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        GridLayout gridLayout = new GridLayout(x, y);
        setLayout(gridLayout);

        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                scene[i][j] = new JPanel();
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
        // Generate JPanels to fit in grid of size xy and add them to frame
        for (int i = 0; i < state.length; i++) {
            for (int j = 0; j < state[i].length; j++) {
                scene[i][j].setBackground(state[i][j] == 1 ? Color.BLACK : Color.WHITE);
                add(scene[i][j]);
            }
        }
        revalidate();
    }
    public void shutdown() {
        setVisible(false);
        dispose();
    }
}