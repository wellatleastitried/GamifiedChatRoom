package com.walit.lifeClient.Frame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class InitialPosition extends JFrame {

    private final JButton[][] buttons;
    public int[][] buttonStates;
    public boolean isSavePressed = false;

    public InitialPosition(int cols, int rows) {
        super("Grid Frame");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        GridLayout gridLayout = new GridLayout(rows, cols);
        setLayout(gridLayout);

        buttons = new JButton[rows][cols];
        buttonStates = new int[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                JButton button = new JButton();
                // button.setPreferredSize(new Dimension(50, 50));
                button.addActionListener(new ButtonClickListener(i, j));
                buttons[i][j] = button;
                add(button);
                int randomValue = 0;
                buttonStates[i][j] = randomValue;
                button.setBackground(randomValue == 0 ? Color.WHITE : Color.BLACK);
            }
        }

        int panelWidth = 50;
        int panelHeight = 50;
        int frameWidth = cols * panelWidth;
        int frameHeight = rows * panelHeight;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = (int) screenSize.getWidth();
        int screenHeight = (int) screenSize.getHeight();
        setSize(Math.min(frameWidth, screenWidth), Math.min(frameHeight, screenHeight));

        setLocationRelativeTo(null);

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem saveMenuItem = new JMenuItem("Save");
        fileMenu.add(saveMenuItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        saveMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveToFile();
            }
        });
        setVisible(true);
    }

    public int[][] getFinalCustomPosition() {
        return isSavePressed ? buttonStates : null;
    }

    private class ButtonClickListener implements ActionListener {
        private final int row;
        private final int col;

        public ButtonClickListener(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JButton button = buttons[row][col];
            int currentState = buttonStates[row][col];
            int newState = currentState == 0 ? 1 : 0;
            buttonStates[row][col] = newState;
            button.setBackground(newState == 0 ? Color.WHITE : Color.BLACK);
        }
    }

    public void saveToFile() {
        isSavePressed = true;
        JOptionPane.showMessageDialog(this, "Grid state sent to server, you may close this window.");
        dispose();
    }
}
