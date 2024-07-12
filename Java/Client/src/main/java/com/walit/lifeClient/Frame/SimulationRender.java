package com.walit.lifeClient.Frame;

import javax.swing.*;
import java.awt.*;

public class SimulationRender extends JFrame {

    JPanel scene;

    public SimulationRender(int x, int y) {
        super("Game of Life Simulation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        GridLayout gridLayout = new GridLayout(x, y);
        setLayout(gridLayout);



        setVisible(true);
    }
    public void renderNextScene(int[][] state) {
        // Generate JPanels to fit in grid of size xy and add them to frame
    }
    public void shutdown() {
        setVisible(false);
        dispose();
    }
}
