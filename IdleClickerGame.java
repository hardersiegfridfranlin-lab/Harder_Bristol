import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

public class IdleClickerGame extends JFrame {

    // --- Game Variables ---
    private int score = 0;
    private int passiveIncome = 0; // Points earned automatically per second
    private int clickPower = 1;    // Points earned per manual click
    private int upgradeCost = 10;  // Cost of the next passive income upgrade

    // --- Mineshaft Colors ---
    private static final Color MINESHAFT_BG = new Color(50, 40, 30); // Dark earthy brown
    private static final Color TEXT_COLOR = new Color(220, 200, 180); // Light, dusty gray-beige
    private static final Color DIAMOND_COLOR = new Color(100, 180, 250); // Sparkly translucent blue
    private static final Color UPGRADE_BUTTON_COLOR = new Color(90, 70, 50); // Lighter brown
    private static final Color DISABLED_UPGRADE_COLOR = new Color(70, 60, 50); // Muted disabled brown

    // --- UI Components ---
    private JLabel scoreLabel;
    private JLabel passiveIncomeLabel;
    private JButton clickButton;
    private JButton upgradeButton;
    private Timer gameTimer;

    public IdleClickerGame() {
        // Set up the main window
        setTitle("Mineshaft Diamond Clicker");
        setSize(450, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(4, 1, 10, 10)); // Simple grid layout
        setLocationRelativeTo(null); // Center on screen

        // Set overall background for the frame
        getContentPane().setBackground(MINESHAFT_BG);

        // Initialize UI Elements with Mineshaft Theme
        scoreLabel = new JLabel("Score: 0", SwingConstants.CENTER);
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 28));
        scoreLabel.setForeground(TEXT_COLOR);

        passiveIncomeLabel = new JLabel("Passive Income: 0 / sec", SwingConstants.CENTER);
        passiveIncomeLabel.setFont(new Font("Courier New", Font.PLAIN, 18)); // Monospace for numbers
        passiveIncomeLabel.setForeground(TEXT_COLOR);

        // --- Custom Diamond Clickable Button ---
        clickButton = new DiamondButton("MINE (+1)");
        clickButton.setFont(new Font("Arial", Font.BOLD, 22));
        // DiamondButton class handles its own diamond shape and color internally

        upgradeButton = new JButton("Upgrade Pickaxe (Cost: 10)");
        upgradeButton.setFont(new Font("Courier New", Font.BOLD, 18));
        upgradeButton.setBackground(UPGRADE_BUTTON_COLOR);
        upgradeButton.setForeground(TEXT_COLOR);
        upgradeButton.setFocusPainted(false); // Remove focus border on click

        // Add components to the window
        add(scoreLabel);
        add(passiveIncomeLabel);
        add(clickButton);
        add(upgradeButton);

        // --- Event Listeners ---

        // 1. Manual Click Action (triggers only on the diamond shape)
        clickButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                score += clickPower;
                updateUI();
            }
        });

        // 2. Upgrade Action
        upgradeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (score >= upgradeCost) {
                    score -= upgradeCost;       // Pay for upgrade
                    passiveIncome += 1;         // Increase passive income
                    upgradeCost *= 2;           // Double the cost for the next one
                    updateUI();
                } else {
                    // Optional: Show a quick popup
                    JOptionPane.showMessageDialog(ClickerIdleGame.this, 
                        "Need more gold!", "Cannot Upgrade", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        // 3. Game Timer (The "Idle" part)
        gameTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (passiveIncome > 0) {
                    score += passiveIncome;
                    updateUI();
                }
            }
        });
        gameTimer.start(); // Start the loop
    }

    // Helper method to refresh the text and styles on the screen
    private void updateUI() {
        scoreLabel.setText("Score: " + score);
        passiveIncomeLabel.setText("Passive Income: " + passiveIncome + " / sec");
        upgradeButton.setText("Upgrade Pickaxe (Cost: " + upgradeCost + ")");
        
        // Update upgrade button state visually
        if (score >= upgradeCost) {
            upgradeButton.setEnabled(true);
            upgradeButton.setBackground(UPGRADE_BUTTON_COLOR);
        } else {
            upgradeButton.setEnabled(false);
            upgradeButton.setBackground(DISABLED_UPGRADE_COLOR);
        }
    }

    // --- Custom Diamond Button Class ---
    // Inherits from JButton, but overrides drawing and hit-testing for the diamond shape
    private class DiamondButton extends JButton {

        private Polygon diamondShape;

        public DiamondButton(String text) {
            super(text);
            setContentAreaFilled(false); // Tell Swing not to draw standard background
            setBorderPainted(false);     // Tell Swing not to draw standard border
            setFocusPainted(false);      // Remove focus rectangle
            setForeground(Color.WHITE);  // Text color on diamond
            setBackground(DIAMOND_COLOR); // Use our diamond color for drawing
            setCursor(new Cursor(Cursor.HAND_CURSOR)); // Add hand cursor for feedback
        }

        // --- Customize hit-testing: Only register clicks inside the diamond ---
        @Override
        public boolean contains(int x, int y) {
            return diamondShape != null && diamondShape.contains(x, y);
        }

        // --- Custom Drawing ---
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // Smooth edges

            // Calculate diamond shape based on current button size
            int w = getWidth();
            int h = getHeight();
            int[] xPoints = {w / 2, w, w / 2, 0};
            int[] yPoints = {0, h / 2, h, h / 2};
            diamondShape = new Polygon(xPoints, yPoints, 4);

            // 1. Fill the diamond with color
            g2.setColor(getBackground()); // Use button background color (DIAMOND_COLOR)
            g2.fill(diamondShape);

            // 2. Draw a slightly lighter border around the diamond
            g2.setColor(DIAMOND_COLOR.brighter());
            g2.setStroke(new BasicStroke(2));
            g2.draw(diamondShape);

            // 3. Draw the button's text, centered inside the diamond
            super.paintComponent(g); // Draw standard button text/icon after custom painting
        }
    }

    // Main method to run the program
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new IdleClickerGame().setVisible(true);
            }
        });
    }
}