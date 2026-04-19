import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;

public class DiamondRush extends JFrame {

    // ─── State ────────────────────────────────────────────────────────────────
    private GameState state;
    private GameCanvas canvas;
    private ShopPanel shopPanel;

    private final String SAVE_FILE = "diamond-rush-save.properties";
    private final ArrayList<FloatingText> popups = new ArrayList<>();
    private float shakeIntensity = 0;
    private final Random rand = new Random();

    // Save throttle: only save every 5 seconds, not every frame
    private long lastSaveTime = 0;
    private static final long SAVE_INTERVAL_MS = 5000;

    // ─── Entry ────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DiamondRush().setVisible(true));
    }

    public DiamondRush() {
        state = new GameState();
        loadGame();

        setTitle("Diamond Rush: Professional Edition");
        setSize(950, 620);
        setMinimumSize(new Dimension(750, 500));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);

        setupUI();
        startGameLoop();
    }

    // ─── UI Setup ─────────────────────────────────────────────────────────────
    private void setupUI() {
        setLayout(new BorderLayout());

        canvas = new GameCanvas();
        add(canvas, BorderLayout.CENTER);

        shopPanel = new ShopPanel();
        shopPanel.setPreferredSize(new Dimension(280, 0));
        add(shopPanel, BorderLayout.EAST);

        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (canvas.getDiamondBounds().contains(e.getPoint())) {
                    mineAction(e.getPoint());
                }
            }
        });
    }

    // ─── Game Actions ─────────────────────────────────────────────────────────
    private void mineAction(Point p) {
        state.score += state.clickPower;
        state.totalClicks++;
        shakeIntensity = 6f;

        // Show floating text at click location with color based on power level
        Color popupColor = state.clickPower >= 20 ? new Color(255, 215, 0)
                         : state.clickPower >= 5  ? new Color(150, 220, 255)
                         : Color.WHITE;
        popups.add(new FloatingText("+" + formatScore(state.clickPower), p.x, p.y, popupColor));
        shopPanel.refresh();
    }

    // ─── Game Loop ────────────────────────────────────────────────────────────
    private void startGameLoop() {
        // Logic tick: passive income at 20 TPS
        Timer logicTimer = new Timer(50, e -> {
            state.score += state.passiveIncome / 20.0;
            updateEffects();

            long now = System.currentTimeMillis();
            if (now - lastSaveTime > SAVE_INTERVAL_MS) {
                saveGame();
                lastSaveTime = now;
            }
        });
        logicTimer.start();

        // Render tick: 60 FPS
        Timer renderTimer = new Timer(16, e -> {
            canvas.repaint();
            shopPanel.repaint();
        });
        renderTimer.start();
    }

    private void updateEffects() {
        if (shakeIntensity > 0) shakeIntensity *= 0.85f;
        for (int i = popups.size() - 1; i >= 0; i--) {
            FloatingText ft = popups.get(i);
            ft.y -= 1.5f;
            ft.alpha -= 0.022f;
            if (ft.alpha <= 0) popups.remove(i);
        }
    }

    // ─── Score Formatting ─────────────────────────────────────────────────────
    /**
     * Formats large numbers into readable abbreviations.
     * e.g. 1500 → "1.5K", 2000000 → "2.0M"
     */
    static String formatScore(double value) {
        if (value < 1_000)         return String.valueOf((int) value);
        if (value < 1_000_000)     return String.format("%.1fK", value / 1_000);
        if (value < 1_000_000_000) return String.format("%.1fM", value / 1_000_000);
        return String.format("%.1fB", value / 1_000_000_000);
    }

    // ─── Rendering: Game Canvas ───────────────────────────────────────────────
    private class GameCanvas extends JPanel {
        private Polygon diamond;
        // Pulse animation for the diamond
        private float pulseAngle = 0;

        GameCanvas() {
            setBackground(new Color(20, 16, 12));
        }

        Rectangle getDiamondBounds() {
            return diamond != null ? diamond.getBounds() : new Rectangle();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int W = getWidth(), H = getHeight();

            // ── Background ──────────────────────────────────────────────────
            GradientPaint bg = new GradientPaint(0, 0, new Color(28, 22, 16), 0, H, new Color(12, 9, 6));
            g2.setPaint(bg);
            g2.fillRect(0, 0, W, H);

            // Subtle grid lines
            g2.setColor(new Color(255, 255, 255, 10));
            g2.setStroke(new BasicStroke(0.5f));
            for (int x = 0; x < W; x += 40) g2.drawLine(x, 0, x, H);
            for (int y = 0; y < H; y += 40) g2.drawLine(0, y, W, y);

            // ── Shake transform (save/restore to isolate it) ─────────────────
            Graphics2D g2s = (Graphics2D) g2.create();
            if (shakeIntensity > 0.5f) {
                float dx = (rand.nextFloat() - 0.5f) * shakeIntensity;
                float dy = (rand.nextFloat() - 0.5f) * shakeIntensity;
                g2s.translate(dx, dy);
            }

            // ── Score display ────────────────────────────────────────────────
            g2s.setColor(new Color(180, 160, 120));
            g2s.setFont(new Font("Verdana", Font.PLAIN, 14));
            g2s.drawString("GEMS", 30, 50);

            g2s.setColor(Color.WHITE);
            g2s.setFont(new Font("Verdana", Font.BOLD, 38));
            g2s.drawString(formatScore(state.score), 30, 92);

            // Passive income per second display
            if (state.passiveIncome > 0) {
                g2s.setColor(new Color(120, 200, 120));
                g2s.setFont(new Font("Verdana", Font.PLAIN, 13));
                g2s.drawString("+" + formatScore(state.passiveIncome) + "/sec", 30, 115);
            }

            // Click power display
            g2s.setColor(new Color(140, 180, 240));
            g2s.setFont(new Font("Verdana", Font.PLAIN, 13));
            g2s.drawString("Click power: " + formatScore(state.clickPower), 30, 140);

            // ── Diamond (pulsing, centered) ──────────────────────────────────
            pulseAngle += 0.04f;
            int pulseMag = (int)(Math.sin(pulseAngle) * 4);
            int centerX = W / 2;
            int centerY = H / 2 + 10;
            int size = 140 + pulseMag + (int)(shakeIntensity * 1.5f);

            int[] xPts = {centerX, centerX + size / 2, centerX, centerX - size / 2};
            int[] yPts = {centerY - size / 2, centerY, centerY + size / 2, centerY};
            diamond = new Polygon(xPts, yPts, 4);

            // Outer glow effect (layered semi-transparent fills)
            for (int i = 3; i >= 1; i--) {
                int gSize = size + i * 10;
                int[] gxPts = {centerX, centerX + gSize / 2, centerX, centerX - gSize / 2};
                int[] gyPts = {centerY - gSize / 2, centerY, centerY + gSize / 2, centerY};
                g2s.setColor(new Color(60, 130, 220, 15 * i));
                g2s.fillPolygon(new Polygon(gxPts, gyPts, 4));
            }

            // Main diamond body
            GradientPaint gem = new GradientPaint(
                centerX, centerY - size / 2, new Color(130, 200, 255),
                centerX, centerY + size / 2, new Color(30, 80, 160)
            );
            g2s.setPaint(gem);
            g2s.fill(diamond);

            // Facet highlight (top-left shine)
            int[] hlX = {centerX, centerX - size / 4, centerX};
            int[] hlY = {centerY - size / 2, centerY - size / 8, centerY - size / 4};
            g2s.setColor(new Color(255, 255, 255, 80));
            g2s.fillPolygon(new Polygon(hlX, hlY, 3));

            // Diamond border
            g2s.setColor(new Color(200, 230, 255, 200));
            g2s.setStroke(new BasicStroke(2f));
            g2s.draw(diamond);

            // ── Floating text popups ─────────────────────────────────────────
            g2s.setFont(new Font("Arial", Font.BOLD, 18));
            for (FloatingText ft : popups) {
                float alpha = Math.max(0, ft.alpha);
                g2s.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                // Draw shadow
                g2s.setColor(Color.BLACK);
                g2s.drawString(ft.text, (int)ft.x + 1, (int)ft.y + 1);
                // Draw text
                g2s.setColor(ft.color);
                g2s.drawString(ft.text, (int)ft.x, (int)ft.y);
            }
            g2s.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

            // ── "Click the gem" hint (if no clicks yet) ──────────────────────
            if (state.totalClicks == 0) {
                g2s.setColor(new Color(255, 255, 255, 120));
                g2s.setFont(new Font("Arial", Font.ITALIC, 14));
                String hint = "Click the gem to mine!";
                FontMetrics fm = g2s.getFontMetrics();
                g2s.drawString(hint, centerX - fm.stringWidth(hint) / 2, centerY + size / 2 + 40);
            }

            g2s.dispose();
        }
    }

    // ─── Rendering: Shop Panel ────────────────────────────────────────────────
    private class ShopPanel extends JPanel {
        private JLabel scoreLabel;
        private JButton minerBtn, pickaxeBtn, resetBtn;
        private JLabel minerInfo, pickaxeInfo;

        ShopPanel() {
            setLayout(new BorderLayout());
            setBackground(new Color(30, 25, 20));
            setBorder(BorderFactory.createEmptyBorder(20, 16, 20, 16));

            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setOpaque(false);

            // Title
            JLabel title = new JLabel("⛏  UPGRADES");
            title.setForeground(new Color(200, 160, 80));
            title.setFont(new Font("Verdana", Font.BOLD, 14));
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(title);
            content.add(Box.createVerticalStrut(4));

            // Divider
            JSeparator sep = new JSeparator();
            sep.setForeground(new Color(80, 70, 55));
            sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            content.add(sep);
            content.add(Box.createVerticalStrut(18));

            // Auto-Miner upgrade
            minerBtn = makeUpgradeButton("AUTO-MINER", new Color(60, 100, 180));
            minerBtn.addActionListener(e -> {
                state.buyAutoMiner();
                refresh();
            });
            minerInfo = makeInfoLabel();
            content.add(makeUpgradeBlock(
                minerBtn, minerInfo,
                "Generates gems passively while idle.",
                new Color(80, 120, 200)
            ));
            content.add(Box.createVerticalStrut(16));

            // Pickaxe upgrade
            pickaxeBtn = makeUpgradeButton("SHARPEN PICKAXE", new Color(160, 90, 30));
            pickaxeBtn.addActionListener(e -> {
                state.buyPickaxe();
                refresh();
            });
            pickaxeInfo = makeInfoLabel();
            content.add(makeUpgradeBlock(
                pickaxeBtn, pickaxeInfo,
                "Increases gems per click.",
                new Color(200, 120, 50)
            ));

            content.add(Box.createVerticalGlue());

            // Stats panel
            content.add(makeStatsPanel());
            content.add(Box.createVerticalStrut(16));

            // Reset button
            resetBtn = new JButton("Reset Progress");
            resetBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
            resetBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            resetBtn.setBackground(new Color(80, 30, 30));
            resetBtn.setForeground(new Color(220, 100, 100));
            resetBtn.setFont(new Font("Arial", Font.PLAIN, 12));
            resetBtn.setFocusPainted(false);
            resetBtn.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            resetBtn.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure? All progress will be lost.",
                    "Reset Game", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) resetGame();
            });
            content.add(resetBtn);

            add(content, BorderLayout.CENTER);
            refresh();
        }

        /** Called whenever state changes to update button labels/states. */
        void refresh() {
            int minerCost = state.nextAutoMinerCost();
            int pickCost  = state.nextPickaxeCost();

            minerBtn.setText("<html><center>AUTO-MINER Lv." + state.autoLevel
                + "<br><small>Cost: " + formatScore(minerCost) + " gems</small></center></html>");
            minerBtn.setEnabled(state.score >= minerCost);

            pickaxeBtn.setText("<html><center>PICKAXE Lv." + state.pickLevel
                + "<br><small>Cost: " + formatScore(pickCost) + " gems</small></center></html>");
            pickaxeBtn.setEnabled(state.score >= pickCost);

            minerInfo.setText("Passive: +" + formatScore(state.passiveIncome) + "/sec");
            pickaxeInfo.setText("Click power: +" + formatScore(state.clickPower) + "/click");
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(new Color(30, 25, 20));
            g2.fillRect(0, 0, getWidth(), getHeight());
            // Left border accent
            g2.setColor(new Color(80, 60, 30));
            g2.fillRect(0, 0, 2, getHeight());
        }

        // ── Builder helpers ──────────────────────────────────────────────────

        private JButton makeUpgradeButton(String text, Color baseColor) {
            JButton btn = new JButton(text);
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
            btn.setPreferredSize(new Dimension(240, 56));
            btn.setBackground(baseColor);
            btn.setForeground(Color.WHITE);
            btn.setFont(new Font("Verdana", Font.BOLD, 11));
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(baseColor.brighter(), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
            ));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return btn;
        }

        private JLabel makeInfoLabel() {
            JLabel lbl = new JLabel(" ");
            lbl.setForeground(new Color(140, 180, 140));
            lbl.setFont(new Font("Arial", Font.ITALIC, 11));
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            return lbl;
        }

        private JPanel makeUpgradeBlock(JButton btn, JLabel info, String description, Color accentColor) {
            JPanel block = new JPanel();
            block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
            block.setOpaque(false);
            block.setAlignmentX(Component.LEFT_ALIGNMENT);
            block.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

            JLabel desc = new JLabel(description);
            desc.setForeground(new Color(160, 150, 130));
            desc.setFont(new Font("Arial", Font.PLAIN, 11));
            desc.setAlignmentX(Component.LEFT_ALIGNMENT);

            block.add(btn);
            block.add(Box.createVerticalStrut(4));
            block.add(desc);
            block.add(Box.createVerticalStrut(2));
            block.add(info);
            return block;
        }

        private JPanel makeStatsPanel() {
            JPanel stats = new JPanel(new GridLayout(0, 1, 0, 4));
            stats.setOpaque(false);
            stats.setAlignmentX(Component.LEFT_ALIGNMENT);
            stats.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 60, 45), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));

            scoreLabel = makeStatLabel("Gems: 0");
            stats.add(makeStatRow("Total Clicks", () -> formatScore(state.totalClicks)));
            stats.add(makeStatRow("Gems/sec", () -> formatScore(state.passiveIncome)));
            stats.add(makeStatRow("Click power", () -> formatScore(state.clickPower)));
            return stats;
        }

        private JLabel makeStatLabel(String text) {
            JLabel l = new JLabel(text);
            l.setForeground(new Color(180, 170, 150));
            l.setFont(new Font("Arial", Font.PLAIN, 12));
            return l;
        }

        private JPanel makeStatRow(String label, java.util.function.Supplier<String> valueSupplier) {
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            JLabel lbl = makeStatLabel(label);
            JLabel val = makeStatLabel(valueSupplier.get());
            val.setHorizontalAlignment(SwingConstants.RIGHT);
            row.add(lbl, BorderLayout.WEST);
            row.add(val, BorderLayout.EAST);

            // Refresh the value label on repaint
            row.addPropertyChangeListener(e -> val.setText(valueSupplier.get()));
            // Use a timer-driven repaint callback
            Timer t = new Timer(500, e -> val.setText(valueSupplier.get()));
            t.start();
            return row;
        }
    }

    // ─── Game State ───────────────────────────────────────────────────────────
    static class GameState {
        double score       = 0;
        int passiveIncome  = 0;
        int autoLevel      = 0;
        int clickPower     = 1;
        int pickLevel      = 0;
        long totalClicks   = 0;

        /** Cost scales with level: 10 * 1.15^level */
        int nextAutoMinerCost() {
            return (int)(10 * Math.pow(1.15, autoLevel));
        }

        /** Cost scales with level: 15 * 1.5^level */
        int nextPickaxeCost() {
            return (int)(15 * Math.pow(1.5, pickLevel));
        }

        void buyAutoMiner() {
            int cost = nextAutoMinerCost();
            if (score >= cost) {
                score -= cost;
                autoLevel++;
                // Each miner adds 1 per second, scaling up at milestones
                passiveIncome = autoLevel + (autoLevel / 5) * 2;
            }
        }

        void buyPickaxe() {
            int cost = nextPickaxeCost();
            if (score >= cost) {
                score -= cost;
                pickLevel++;
                clickPower = 1 + (pickLevel * 2) + (pickLevel / 3) * 3;
            }
        }
    }

    // ─── Persistence ──────────────────────────────────────────────────────────
    private void saveGame() {
        Properties p = new Properties();
        p.setProperty("score",        String.valueOf(state.score));
        p.setProperty("autoLevel",    String.valueOf(state.autoLevel));
        p.setProperty("pickLevel",    String.valueOf(state.pickLevel));
        p.setProperty("totalClicks",  String.valueOf(state.totalClicks));
        try (FileOutputStream out = new FileOutputStream(SAVE_FILE)) {
            p.store(out, "Diamond Rush Save");
        } catch (Exception ignored) {}
    }

    private void loadGame() {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(SAVE_FILE)) {
            p.load(in);
            state.score       = Double.parseDouble(p.getProperty("score",       "0"));
            state.autoLevel   = Integer.parseInt(p.getProperty("autoLevel",     "0"));
            state.pickLevel   = Integer.parseInt(p.getProperty("pickLevel",     "0"));
            state.totalClicks = Long.parseLong(p.getProperty("totalClicks",     "0"));
            // Recompute derived stats from levels
            state.passiveIncome = state.autoLevel + (state.autoLevel / 5) * 2;
            state.clickPower    = 1 + (state.pickLevel * 2) + (state.pickLevel / 3) * 3;
        } catch (Exception ignored) {}
    }

    private void resetGame() {
        state = new GameState();
        saveGame();
        popups.clear();
        shopPanel.refresh();
        canvas.repaint();
    }

    // ─── Floating Text ────────────────────────────────────────────────────────
    private static class FloatingText {
        String text;
        float x, y;
        float alpha = 1.0f;
        Color color;

        FloatingText(String text, int x, int y, Color color) {
            this.text  = text;
            this.x     = x;
            this.y     = y;
            this.color = color;
        }
    }
}