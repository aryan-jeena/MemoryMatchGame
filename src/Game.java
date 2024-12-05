import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import javax.swing.Timer;

public class Game {
    // Controller class
    public static class Controller {
        final JFrame window;
        Model model;
        View view;

        public Controller(Model model) {
            this.window = new JFrame("Memory Match Game");
            this.window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            this.window.setResizable(false);
            this.reset(model);
        }

        public void reset(Model model) {
            this.model = model;
            this.view = new View(model);
            this.window.setVisible(false);

            // Add save and load buttons
            JButton saveButton = new JButton("Save Game");
            JButton loadButton = new JButton("Load Game");

            saveButton.addActionListener(e -> {
                try {
                    model.saveGame("game_save.txt");
                    JOptionPane.showMessageDialog(window, "Game saved successfully!");
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(window, "Failed to save game.");
                }
            });

            loadButton.addActionListener(e -> {
                try {
                    model.loadGame("game_save.txt");
                    view.setTries(model.getTries());
                    JOptionPane.showMessageDialog(window, "Game loaded successfully!");
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(window, "Failed to load game.");
                }
            });

            JPanel buttonPanel = new JPanel();
            buttonPanel.add(saveButton);
            buttonPanel.add(loadButton);
            this.window.add(buttonPanel, BorderLayout.SOUTH);

            this.window.setContentPane(view);
            this.window.pack();
            this.window.setLocationRelativeTo(null);

            for (JButton button : this.model.getButtons()) {
                button.addActionListener(new ButtonActionListener(this));
            }

            Utilities.timer(200, (ignored) -> this.window.setVisible(true));
        }

        public JFrame getWindow() {
            return this.window;
        }

        public Model getModel() {
            return this.model;
        }

        public View getView() {
            return this.view;
        }
    }

    // Model class
    public static class Model {
        static final String[] AVAILABLE_IMAGES = new String[]{"0.png", "1.png", "2.png", "3.png", "4.png", "5.png", "6.png", "7.png", "8.png"};
        final ArrayList<JButton> buttons;
        final int columns;
        int tries;
        boolean gameStarted;

        public Model(int columns) {
            this.columns = columns;
            this.buttons = new ArrayList<>();
            this.tries = 10;
            this.gameStarted = false;
            int numberOfImages = columns * columns;
            Vector<Integer> v = new Vector<>();
            for (int i = 0; i < numberOfImages - numberOfImages % 2; i++) {
                v.add(i % (numberOfImages / 2));
            }
            if (numberOfImages % 2 != 0) v.add(AVAILABLE_IMAGES.length - 1);
            for (int i = 0; i < numberOfImages; i++) {
                int rand = (int) (Math.random() * v.size());
                String reference = AVAILABLE_IMAGES[v.elementAt(rand)];
                this.buttons.add(new MemoryButton(reference));
                v.removeElementAt(rand);
            }
        }

        public int getColumns() {
            return columns;
        }

        public ArrayList<JButton> getButtons() {
            return buttons;
        }

        public int getTries() {
            return tries;
        }

        public void decrementTries() {
            this.tries--;
        }

        public boolean isGameStarted() {
            return this.gameStarted;
        }

        public void startGame() {
            this.gameStarted = true;
        }

        public void saveGame(String fileName) throws IOException {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
                writer.write("Tries: " + this.tries + "\n");
                writer.write("Board:\n");
                for (int i = 0; i < buttons.size(); i++) {
                    ReferencedIcon icon = (ReferencedIcon) buttons.get(i).getDisabledIcon();
                    writer.write(icon.getReference());
                    if ((i + 1) % columns == 0) writer.newLine();
                    else writer.write(",");
                }
                writer.write("State:\n");
                for (JButton button : buttons) {
                    writer.write(button.isEnabled() ? "F" : "T");
                    writer.write(",");
                }
                writer.newLine();
            }
        }

        public void loadGame(String fileName) throws IOException {
            try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
                this.tries = Integer.parseInt(reader.readLine().split(": ")[1]);
                reader.readLine(); // Skip "Board:" line
                ArrayList<String> board = new ArrayList<>();
                for (int i = 0; i < columns; i++) {
                    board.addAll(Arrays.asList(reader.readLine().split(",")));
                }
                reader.readLine(); // Skip "State:" line
                ArrayList<Boolean> states = new ArrayList<>();
                for (String state : reader.readLine().split(",")) {
                    states.add(state.equals("T"));
                }
                for (int i = 0; i < buttons.size(); i++) {
                    JButton button = buttons.get(i);
                    ReferencedIcon icon = (ReferencedIcon) button.getDisabledIcon();
                    icon.reference = board.get(i);
                    if (states.get(i)) {
                        button.setEnabled(false);
                    } else {
                        button.setEnabled(true);
                        button.setIcon(new ImageIcon(MemoryButton.NO_IMAGE));
                    }
                }
            }
        }
    }

    public static class View extends JPanel {
        final JLabel tries;

        public View(Model model) {
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            this.tries = new JLabel("", SwingConstants.CENTER);
            this.tries.setFont(new Font("MV Boli", Font.BOLD, 30));
            this.tries.setForeground(Color.WHITE);

            JPanel imagePanel = new JPanel();
            int columns = model.getColumns();
            imagePanel.setLayout(new GridLayout(columns, columns));
            for (JButton button : model.getButtons()) {
                imagePanel.add(button);
            }
            this.setTries(model.getTries());
            JPanel triesPanel = new JPanel();
            triesPanel.add(this.tries);
            triesPanel.setAlignmentX(CENTER_ALIGNMENT);
            triesPanel.setBackground(new Color(0X8946A6));
            this.add(triesPanel);
            this.add(imagePanel);
        }

        public void setTries(int triesLeft) {
            this.tries.setText("Tries left : " + triesLeft);
        }
    }

    public static class MemoryButton extends JButton {
        static final String IMAGE_PATH = "images/";
        static final Image NO_IMAGE = Utilities.loadImage(IMAGE_PATH + "no_image.png");

        public MemoryButton(String reference) {
            Image image = Utilities.loadImage(IMAGE_PATH + reference);
            Dimension dimension = new Dimension(120, 120);
            this.setPreferredSize(dimension);
            this.setIcon(new ImageIcon(NO_IMAGE));
            this.setDisabledIcon(new ReferencedIcon(image, reference));
        }
    }

    public static class ReferencedIcon extends ImageIcon {
        String reference;

        public ReferencedIcon(Image image, String reference) {
            super(image);
            this.reference = reference;
        }

        public String getReference() {
            return reference;
        }
    }

    public static class ButtonActionListener implements ActionListener {
        final Controller controller;
        final Model model;
        final View view;
        final JFrame window;
        static int disabledButtonCount = 0;
        static JButton lastDisabledButton = null;

        public ButtonActionListener(Controller controller) {
            this.controller = controller;
            this.model = controller.getModel();
            this.view = controller.getView();
            this.window = controller.getWindow();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JButton button = (JButton) e.getSource();
            button.setEnabled(false);
            ReferencedIcon thisIcon = (ReferencedIcon) button.getDisabledIcon();
            disabledButtonCount++;
            if (!model.isGameStarted()) model.startGame();
            if (disabledButtonCount == 2) {
                ReferencedIcon thatIcon = (ReferencedIcon) lastDisabledButton.getDisabledIcon();
                boolean isPair = thisIcon.getReference().equals(thatIcon.getReference());
                if (!isPair) {
                    model.decrementTries();
                    view.setTries(model.getTries());
                    JButton lastButton = lastDisabledButton;
                    Utilities.timer(500, (ignored) -> {
                        button.setEnabled(true);
                        lastButton.setEnabled(true);
                    });
                }
                disabledButtonCount = 0;
            }
            ArrayList<JButton> enabledButtons = model.getButtons().stream()
                    .filter(Component::isEnabled)
                    .collect(Collectors.toList());
            if (enabledButtons.isEmpty()) {
                controller.reset(new Model(controller.getModel().getColumns()));
                JOptionPane.showMessageDialog(window, "Congrats, you won!");
            }
            lastDisabledButton = button;
            if (model.getTries() == 0) {
                controller.reset(new Model(controller.getModel().getColumns()));
                JOptionPane.showMessageDialog(window, "You lost, try again!");
                Utilities.timer(1000, (ignored) -> model.getButtons().forEach(btn -> btn.setEnabled(false)));
            }
        }
    }

    public static class Utilities {
        static final ClassLoader cl = Utilities.class.getClassLoader();

        public static void timer(int delay, ActionListener listener) {
            Timer t = new Timer(delay, listener);
            t.setRepeats(false);
            t.start();
        }

        public static Image loadImage(String s) {
            Image image = null;
            try {
                InputStream resourceStream = cl.getResourceAsStream(s);
                if (resourceStream != null) {
                    ImageInputStream imageStream = ImageIO.createImageInputStream(resourceStream);
                    image = ImageIO.read(imageStream);
                    // Resize image to 120x120 pixels
                    if (image != null) {
                        image = image.getScaledInstance(120, 120, Image.SCALE_SMOOTH);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return image;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Controller(new Model(4)));
    }
}
