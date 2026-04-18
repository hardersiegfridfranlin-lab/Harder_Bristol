import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class IdleClickerGame extends JFrame {

    // --- Game Variables ---
    private int score = 0;
    private int passiveIncome = 0; // Points earned automatically per second
    private int clickPower = 1;    // Points earned per manual click
    private int upgradeCost = 10;  // Cost of the next passive income upgrade

    // --- UI Components ---
    private JLabel scoreLabel;
    private JLabel passiveIncomeLabel;
    private JButton clickButton;
    private JButton upgradeButton;
    private Timer gameTimer;

    public IdleClickerGame() {
        // Set up the main window
        setTitle("Java Idle Clicker");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(4, 1, 10, 10)); // Simple grid layout
        setLocationRelativeTo(null); // Center on screen

        // Initialize UI Elements
        scoreLabel = new JLabel("Score: 0", SwingConstants.CENTER);
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 24));

        passiveIncomeLabel = new JLabel("Passive Income: 0 per second", SwingConstants.CENTER);
        passiveIncomeLabel.setFont(new Font("Arial", Font.PLAIN, 16));

        clickButton = new JButton("CLICK ME (+1)");
        clickButton.setFont(new Font("Arial", Font.BOLD, 20));
        clickButton.setBackground(new Color(100, 200, 100)); // Light green

        upgradeButton = new JButton("Upgrade Auto-Clicker (Cost: 10)");
        upgradeButton.setFont(new Font("Arial", Font.BOLD, 16));

        // Add components to the window
        add(scoreLabel);
        add(passiveIncomeLabel);
        add(clickButton);
        add(upgradeButton);

        // --- Event Listeners ---

        // 1. Manual Click Action
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
                    // Optional: Show a quick popup if they don't have enough points
                    JOptionPane.showMessageDialog(IdleClickerGame.this, 
                        "Not enough points!", "Cannot Upgrade", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        // 3. Game Timer (The "Idle" part)
        // This timer ticks every 1000 milliseconds (1 second)
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

    // Helper method to refresh the text on the screen
    private void updateUI() {
        scoreLabel.setText("Score: " + score);
        passiveIncomeLabel.setText("Passive Income: " + passiveIncome + " per second");
        upgradeButton.setText("Upgrade Auto-Clicker (Cost: " + upgradeCost + ")");
        
        // Disable the upgrade button if the player can't afford it
        if (score >= upgradeCost) {
            upgradeButton.setEnabled(true);
        } else {
            upgradeButton.setEnabled(false);
        }
    }

    // Main method to run the program
    public static void main(String[] args) {
        // Run GUI construction on the Event-Dispatching Thread for thread safety
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new IdleClickerGame().setVisible(true);
            }
        });
    }
}