// import javax.swing.*;
// import java.awt.*;
// import java.awt.event.ActionEvent;
// import java.awt.event.ActionListener;
// import java.io.File;
// import java.io.FileWriter;
// import java.io.IOException;
// import java.security.SecureRandom;

// public class GridFrame extends JFrame {
//     private JPanel[][] panels;

//     public GridFrame(int rows, int cols) {
//         super("Grid Frame");
//         setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

//         // Create a grid layout with specified rows and columns
//         GridLayout gridLayout = new GridLayout(rows, cols);
//         setLayout(gridLayout);

//         // Initialize the array to hold the panels
//         panels = new JPanel[rows][cols];

//         // Populate the grid with panels
//         for (int i = 0; i < rows; i++) {
//             for (int j = 0; j < cols; j++) {
//                 JPanel panel = new JPanel();
//                 panel.setBackground(new SecureRandom().nextInt(2) == 0 ? Color.WHITE : Color.BLACK); // Set default color to white
//                 panels[i][j] = panel;
//                 add(panel);
//             }
//         }

//         // Set JFrame size based on the grid size
//         int panelWidth = 50; // Adjust this value as needed
//         int panelHeight = 50; // Adjust this value as needed
//         int frameWidth = cols * panelWidth;
//         int frameHeight = rows * panelHeight;
//         setSize(frameWidth, frameHeight);

//         // Center JFrame on the screen
//         setLocationRelativeTo(null);

//         // Create menu bar
//         JMenuBar menuBar = new JMenuBar();
//         JMenu fileMenu = new JMenu("File");
//         JMenuItem saveMenuItem = new JMenuItem("Save");
//         fileMenu.add(saveMenuItem);
//         menuBar.add(fileMenu);
//         setJMenuBar(menuBar);

//         // Add action listener for Save menu item
//         saveMenuItem.addActionListener(new ActionListener() {
//             @Override
//             public void actionPerformed(ActionEvent e) {
//                 // Perform save operation
//                 saveToFile();
//             }
//         });
//     }

//     // Method to set the color of a specific panel at position (row, col)
//     public void setPanelColor(int row, int col, Color color) {
//         if (row >= 0 && row < panels.length && col >= 0 && col < panels[0].length) {
//             panels[row][col].setBackground(color);
//         }
//     }

//     // Method to save the grid state to a 2D int array and print it to the console
//     private void saveToFile() {
//         int[][] gridState = new int[panels.length][panels[0].length];
//         for (int i = 0; i < panels.length; i++) {
//             for (int j = 0; j < panels[0].length; j++) {
//                 Color color = panels[i][j].getBackground();
//                 gridState[i][j] = color.equals(Color.BLACK) ? 1 : 0;
//             }
//         }

//         // Print the grid state to the console
//         System.out.println("Grid state:");
//         for (int i = 0; i < gridState.length; i++) {
//             for (int j = 0; j < gridState[0].length; j++) {
//                 System.out.print(gridState[i][j] + " ");
//             }
//             System.out.println();
//         }
//         System.out.println(gridState.length);
//         JOptionPane.showMessageDialog(this, "Grid state printed to console.");
//     }


//     public static void main(String[] args) {
//         SwingUtilities.invokeLater(() -> {
//             int rows = 150;
//             int cols = 75;
//             GridFrame frame = new GridFrame(rows, cols);
//             frame.setVisible(true);
//         });
//     }
// }
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.SecureRandom;

public class GridFrame extends JFrame {
    private JButton[][] buttons;
    private int[][] buttonStates;
    public boolean isSavePressed = false;
    private GridFrame sim;
    private int rows;
    private int cols;

    public GridFrame(int cols, int rows) {
        super("Grid Frame");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create a grid layout with specified rows and columns
        GridLayout gridLayout = new GridLayout(rows, cols);
        setLayout(gridLayout);

        // Initialize the array to hold the buttons and button states
        buttons = new JButton[rows][cols];
        buttonStates = new int[rows][cols];

        // Populate the grid with buttons
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(50, 50)); // Set button size
                button.addActionListener(new ButtonClickListener(i, j));
                buttons[i][j] = button;
                add(button);
                // Set default color and state for each button
                int randomValue = new SecureRandom().nextInt(2);
                buttonStates[i][j] = randomValue;
                button.setBackground(randomValue == 0 ? Color.WHITE : Color.BLACK);
            }
        }

        // Set JFrame size based on the grid size
        int panelWidth = 50; // Adjust this value as needed
        int panelHeight = 50; // Adjust this value as needed
        int frameWidth = cols * panelWidth;
        int frameHeight = rows * panelHeight;
        setSize(frameWidth, frameHeight);

        // Center JFrame on the screen
        setLocationRelativeTo(null);

        // Create menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem saveMenuItem = new JMenuItem("Save");
        fileMenu.add(saveMenuItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // Add action listener for Save menu item
        saveMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Perform save operation
                saveToFile();
            }
        });
        this.rows = rows;
        this.cols = cols;
        setVisible(true);
    }
    public GridFrame(int cols, int rows, int[][] init) {
        super("Grid Frame");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create a grid layout with specified rows and columns
        GridLayout gridLayout = new GridLayout(rows, cols);
        setLayout(gridLayout);

        // Initialize the array to hold the panels and panel states
        JPanel[][] panels = new JPanel[rows][cols];
        int[][] panelStates = init;

        // Populate the grid with panels
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                JPanel panel = new JPanel();
                panel.setPreferredSize(new Dimension(50, 50)); // Set panel size
                panels[i][j] = panel;
                add(panel);
                // Set default color and state for each panel based on initial state array
                panel.setBackground(panelStates[i][j] == 0 ? Color.WHITE : Color.BLACK);
            }
        }

        // Set JFrame size based on the grid size
        int panelWidth = 50; // Adjust this value as needed
        int panelHeight = 50; // Adjust this value as needed
        int frameWidth = cols * panelWidth;
        int frameHeight = rows * panelHeight;
        setSize(frameWidth, frameHeight);

        // Center JFrame on the screen
        setLocationRelativeTo(null);
        this.rows = rows;
        this.cols = cols;
        setVisible(true);
    }

    public int[][] getFinalCustomPosition() {
        return buttonStates;
    }

    // ActionListener class for handling button clicks
    private class ButtonClickListener implements ActionListener {
        private int row;
        private int col;

        public ButtonClickListener(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Toggle the background color and state of the button
            JButton button = buttons[row][col];
            int currentState = buttonStates[row][col];
            int newState = (currentState == 0) ? 1 : 0;
            buttonStates[row][col] = newState;
            button.setBackground(newState == 0 ? Color.WHITE : Color.BLACK);
        }
    }

    // Method to save the grid state to a 2D int array and close the frame
    private void saveToFile() {
        // Print the grid state to the console
        System.out.println("Grid state:");
        for (int i = 0; i < buttonStates.length; i++) {
            for (int j = 0; j < buttonStates[0].length; j++) {
                System.out.print(buttonStates[i][j] + " ");
            }
            System.out.println();
        }
        isSavePressed = true;
        System.out.println(buttonStates.length);
        JOptionPane.showMessageDialog(this, "Grid state printed to console.");
        
        // Close the frame
        dispose();
    }

     public void createAndShowSimulationFrame(int[][] position) {
        int rows = position.length;
        int cols = position[0].length;
        GridFrame nextFrame = new GridFrame(cols, rows, position);
        nextFrame.setVisible(true);
    }

    // public static void main(String[] args) {
    //     SwingUtilities.invokeLater(() -> {
    //         int rows = 20;
    //         int cols = 25;
    //         GridFrame frame = new GridFrame(cols, rows);
    //         // int[][] init = frame.getFinalCustomPosition();
    //         // GridFrame sim = new GridFrame(cols, rows, init);
    //     });

    // }
    public static void main(String[] args) {
        int rows = 20;
        int cols = 25;
        GridFrame frame = new GridFrame(cols, rows);
        while (!frame.isSavePressed) {
            Thread.onSpinWait();
        }
        int[][] position = new int[rows][cols];
        while (true) {
            // Regenerate the int[][] array (buttonStates) as needed
            position = autofillArray(rows, cols);

            // Create and display the simulation frame on the EDT
            SwingUtilities.invokeLater(() -> {
                frame.createAndShowSimulationFrame(position);
            });

            // Delay between iterations (adjust as needed)
            try {
                Thread.sleep(1000); // 1 second delay
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            break;
        }
    }
    public static int[][] autofillArray(int rows, int cols) {
        int[][] array = new int[rows][cols];
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                array[i][j] = random.nextInt(2); // Generates random 0 or 1
            }
        }
        return array;
    }
}
