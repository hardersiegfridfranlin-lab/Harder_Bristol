import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import javax.swing.*;

public class DiamondRush extends JFrame {

    // ─── State ────────────────────────────────────────────────────────────────
    private GameState state;
    private GameCanvas canvas;
    private ShopPanel shopPanel;

    private final String SAVE_FILE = "diamond-rush-save.properties";
    private final ArrayList<FloatingText> popups = new ArrayList<>();
    private float shakeIntensity = 0;
    private final Random rand = new Random();

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
        setSize(1050, 660);
        setMinimumSize(new Dimension(850, 520));
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
        shopPanel.setPreferredSize(new Dimension(320, 0));
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
        // Apply click multiplier from gem shop
        double earned = state.clickPower * state.clickMultiplier;
        state.score += earned;
        state.totalClicks++;
        shakeIntensity = 6f;

        Color popupColor = state.clickMultiplier >= 3.0 ? new Color(185, 242, 255)
                         : state.clickMultiplier >= 2.0 ? new Color(100, 255, 120)
                         : state.clickMultiplier >= 1.5 ? new Color(100, 180, 255)
                         : state.clickMultiplier >= 1.0 ? new Color(255, 215, 0)
                         : Color.WHITE;
        popups.add(new FloatingText("+" + formatScore(earned), p.x, p.y, popupColor));
        shopPanel.refresh();
    }

    // ─── Game Loop ────────────────────────────────────────────────────────────
    private void startGameLoop() {
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
    static String formatScore(double value) {
        if (value < 1_000)         return String.valueOf((int) value);
        if (value < 1_000_000)     return String.format("%.1fK", value / 1_000);
        if (value < 1_000_000_000) return String.format("%.1fM", value / 1_000_000);
        return String.format("%.1fB", value / 1_000_000_000);
    }

    // ─── Rendering: Game Canvas ───────────────────────────────────────────────
    private class GameCanvas extends JPanel {
        private Polygon diamond;
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

            GradientPaint bg = new GradientPaint(0, 0, new Color(28, 22, 16), 0, H, new Color(12, 9, 6));
            g2.setPaint(bg);
            g2.fillRect(0, 0, W, H);

            g2.setColor(new Color(255, 255, 255, 10));
            g2.setStroke(new BasicStroke(0.5f));
            for (int x = 0; x < W; x += 40) g2.drawLine(x, 0, x, H);
            for (int y = 0; y < H; y += 40) g2.drawLine(0, y, W, y);

            Graphics2D g2s = (Graphics2D) g2.create();
            if (shakeIntensity > 0.5f) {
                float dx = (rand.nextFloat() - 0.5f) * shakeIntensity;
                float dy = (rand.nextFloat() - 0.5f) * shakeIntensity;
                g2s.translate(dx, dy);
            }

            // Score display
            g2s.setColor(new Color(180, 160, 120));
            g2s.setFont(new Font("Verdana", Font.PLAIN, 14));
            g2s.drawString("GEMS", 30, 50);

            g2s.setColor(Color.WHITE);
            g2s.setFont(new Font("Verdana", Font.BOLD, 38));
            g2s.drawString(formatScore(state.score), 30, 92);

            if (state.passiveIncome > 0) {
                g2s.setColor(new Color(120, 200, 120));
                g2s.setFont(new Font("Verdana", Font.PLAIN, 13));
                g2s.drawString("+" + formatScore(state.passiveIncome) + "/sec", 30, 115);
            }

            g2s.setColor(new Color(140, 180, 240));
            g2s.setFont(new Font("Verdana", Font.PLAIN, 13));
            g2s.drawString("Click power: " + formatScore(state.clickPower), 30, 140);

            // Active gem multiplier badge
            if (state.clickMultiplier > 1.0) {
                Color gemColor = state.getActiveGemColor();
                String gemName = state.getActiveGemName();
                g2s.setColor(new Color(0, 0, 0, 120));
                g2s.fillRoundRect(28, 150, 200, 26, 8, 8);
                g2s.setColor(gemColor);
                g2s.setFont(new Font("Verdana", Font.BOLD, 12));
                g2s.drawString(gemName + "  x" + String.format("%.1f", state.clickMultiplier) + " multiplier", 34, 167);
            }

            // Diamond
            pulseAngle += 0.04f;
            int pulseMag = (int)(Math.sin(pulseAngle) * 4);
            int centerX = W / 2;
            int centerY = H / 2 + 10;
            int size = 140 + pulseMag + (int)(shakeIntensity * 1.5f);

            // Tint diamond color based on active gem
            Color gemTopColor = state.getActiveDiamondTopColor();
            Color gemBotColor = state.getActiveDiamondBotColor();

            int[] xPts = {centerX, centerX + size / 2, centerX, centerX - size / 2};
            int[] yPts = {centerY - size / 2, centerY, centerY + size / 2, centerY};
            diamond = new Polygon(xPts, yPts, 4);

            for (int i = 3; i >= 1; i--) {
                int gSize = size + i * 10;
                int[] gxPts = {centerX, centerX + gSize / 2, centerX, centerX - gSize / 2};
                int[] gyPts = {centerY - gSize / 2, centerY, centerY + gSize / 2, centerY};
                Color glowC = state.getActiveGlowColor(i);
                g2s.setColor(glowC);
                g2s.fillPolygon(new Polygon(gxPts, gyPts, 4));
            }

            GradientPaint gem = new GradientPaint(
                centerX, centerY - size / 2, gemTopColor,
                centerX, centerY + size / 2, gemBotColor
            );
            g2s.setPaint(gem);
            g2s.fill(diamond);

            int[] hlX = {centerX, centerX - size / 4, centerX};
            int[] hlY = {centerY - size / 2, centerY - size / 8, centerY - size / 4};
            g2s.setColor(new Color(255, 255, 255, 80));
            g2s.fillPolygon(new Polygon(hlX, hlY, 3));

            g2s.setColor(new Color(200, 230, 255, 200));
            g2s.setStroke(new BasicStroke(2f));
            g2s.draw(diamond);

            // Floating text popups
            g2s.setFont(new Font("Arial", Font.BOLD, 18));
            for (FloatingText ft : popups) {
                float alpha = Math.max(0, ft.alpha);
                g2s.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2s.setColor(Color.BLACK);
                g2s.drawString(ft.text, (int)ft.x + 1, (int)ft.y + 1);
                g2s.setColor(ft.color);
                g2s.drawString(ft.text, (int)ft.x, (int)ft.y);
            }
            g2s.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

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
        private JButton minerBtn, mineBtn, pickaxeBtn, resetBtn;
        private JLabel minerInfo, mineInfo, pickaxeInfo;

        // Gem Shop buttons & info labels
        private JButton ironBtn, goldBtn, rubyBtn, sapphireBtn, emeraldBtn, diamondBtn;
        private JLabel ironInfo, goldInfo, rubyInfo, sapphireInfo, emeraldInfo, diamondInfo;

        ShopPanel() {
            setLayout(new BorderLayout());
            setBackground(new Color(30, 25, 20));
            setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setOpaque(false);

            // ── UPGRADES section ────────────────────────────────────────────
            JLabel title = makeSection("⛏  UPGRADES", new Color(200, 160, 80));
            content.add(title);
            content.add(Box.createVerticalStrut(4));
            content.add(makeDivider(new Color(80, 70, 55)));
            content.add(Box.createVerticalStrut(10));

            minerBtn = makeUpgradeButton("AUTO-MINER", new Color(60, 100, 180));
            minerBtn.addActionListener(e -> { state.buyAutoMiner(); refresh(); });
            minerInfo = makeInfoLabel();
            content.add(makeUpgradeBlock(minerBtn, minerInfo, "Generates gems passively while idle.", new Color(80, 120, 200)));
            content.add(Box.createVerticalStrut(10));

            mineBtn = makeUpgradeButton("BUILD MINE", new Color(90, 60, 30));
            mineBtn.addActionListener(e -> { state.buyMine(); refresh(); });
            mineInfo = makeInfoLabel();
            content.add(makeUpgradeBlock(mineBtn, mineInfo, "Deep mines generate gems underground.", new Color(120, 90, 50)));
            content.add(Box.createVerticalStrut(10));

            pickaxeBtn = makeUpgradeButton("SHARPEN PICKAXE", new Color(160, 90, 30));
            pickaxeBtn.addActionListener(e -> { state.buyPickaxe(); refresh(); });
            pickaxeInfo = makeInfoLabel();
            content.add(makeUpgradeBlock(pickaxeBtn, pickaxeInfo, "Increases gems per click.", new Color(200, 120, 50)));
            content.add(Box.createVerticalStrut(14));

            // ── GEM SHOP section ────────────────────────────────────────────
            content.add(makeSection("💎 GEM SHOP  (click multipliers)", new Color(255, 160, 210)));
            content.add(Box.createVerticalStrut(4));
            content.add(makeDivider(new Color(160, 100, 150)));
            content.add(Box.createVerticalStrut(8));

            // Iron
            ironBtn = makeUpgradeButton("⬜ IRON GEM", new Color(140, 140, 150));
            ironBtn.addActionListener(e -> {
                if (state.buyGem(GameState.GEM_IRON)) refresh();
            });
            ironInfo = makeInfoLabel();
            content.add(makeUpgradeBlock(ironBtn, ironInfo,
                "Always available first  •  x0.5 per click", new Color(200, 200, 210)));
            content.add(Box.createVerticalStrut(8));

            // Gold
            goldBtn = makeUpgradeButton("🟡 GOLD GEM", new Color(180, 140, 30));
            goldBtn.addActionListener(e -> {
                if (state.buyGem(GameState.GEM_GOLD)) refresh();
            });
            goldInfo = makeInfoLabel();
            content.add(makeUpgradeBlock(goldBtn, goldInfo,
                "Requires Iron  •  x1.0 per click (reset)", new Color(240, 200, 80)));
            content.add(Box.createVerticalStrut(8));

            // Ruby
            rubyBtn = makeUpgradeButton("🔴 RUBY GEM", new Color(190, 40, 40));
            rubyBtn.addActionListener(e -> {
                if (state.buyGem(GameState.GEM_RUBY)) refresh();
            });
            rubyInfo = makeInfoLabel();
            content.add(makeUpgradeBlock(rubyBtn, rubyInfo,
                "Requires Gold  •  x1.5 per click (reset)", new Color(255, 100, 100)));
            content.add(Box.createVerticalStrut(8));

            // Sapphire
            sapphireBtn = makeUpgradeButton("🔵 SAPPHIRE GEM", new Color(40, 80, 200));
            sapphireBtn.addActionListener(e -> {
                if (state.buyGem(GameState.GEM_SAPPHIRE)) refresh();
            });
            sapphireInfo = makeInfoLabel();
            content.add(makeUpgradeBlock(sapphireBtn, sapphireInfo,
                "Requires Ruby  •  x2.0 per click (reset)", new Color(100, 160, 255)));
            content.add(Box.createVerticalStrut(8));

            // Emerald
            emeraldBtn = makeUpgradeButton("🟢 EMERALD GEM", new Color(30, 160, 60));
            emeraldBtn.addActionListener(e -> {
                if (state.buyGem(GameState.GEM_EMERALD)) refresh();
            });
            emeraldInfo = makeInfoLabel();
            content.add(makeUpgradeBlock(emeraldBtn, emeraldInfo,
                "Requires Sapphire  •  x2.5 per click (reset)", new Color(80, 240, 120)));
            content.add(Box.createVerticalStrut(8));

            // Diamond
            diamondBtn = makeUpgradeButton("🔷 DIAMOND GEM", new Color(80, 160, 230));
            diamondBtn.addActionListener(e -> {
                if (state.buyGem(GameState.GEM_DIAMOND)) refresh();
            });
            diamondInfo = makeInfoLabel();
            content.add(makeUpgradeBlock(diamondBtn, diamondInfo,
                "Requires Emerald  •  x3.0 per click (reset)", new Color(200, 230, 255)));
            content.add(Box.createVerticalStrut(10));

            content.add(Box.createVerticalGlue());

            // Stats panel
            content.add(makeStatsPanel());
            content.add(Box.createVerticalStrut(10));

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

            // Wrap in scroll pane
            JScrollPane scroll = new JScrollPane(content);
            scroll.setBorder(null);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            add(scroll, BorderLayout.CENTER);

            refresh();
        }

        /** Refresh all button states and labels. */
        void refresh() {
            // ── Upgrades ──
            int minerCost = state.nextAutoMinerCost();
            int mineCost  = state.nextMineCost();
            int pickCost  = state.nextPickaxeCost();

            minerBtn.setText("<html><center>AUTO-MINER Lv." + state.autoLevel
                + "<br><small>Cost: " + formatScore(minerCost) + " gems</small></center></html>");
            minerBtn.setEnabled(state.score >= minerCost);

            mineBtn.setText("<html><center>MINES Lv." + state.mineLevel
                + "<br><small>Cost: " + formatScore(mineCost) + " gems</small></center></html>");
            mineBtn.setEnabled(state.score >= mineCost);

            pickaxeBtn.setText("<html><center>PICKAXE Lv." + state.pickLevel
                + "<br><small>Cost: " + formatScore(pickCost) + " gems</small></center></html>");
            pickaxeBtn.setEnabled(state.score >= pickCost);

            minerInfo.setText("Miners: +" + formatScore(state.passiveIncome - state.mineProduction) + "/sec");
            mineInfo.setText("Mines: +" + formatScore(state.mineProduction) + "/sec");
            pickaxeInfo.setText("Pickaxe power: " + formatScore(state.clickPower) + "/click");

            // ── Gem Shop ──
            // Iron – always unlocked, but show OWNED if bought
            refreshGemButton(ironBtn, ironInfo,
                "⬜ IRON GEM", GameState.GEM_IRON,
                GameState.IRON_COST, state.ironBought,
                true, "x0.5", new Color(140, 140, 150));

            refreshGemButton(goldBtn, goldInfo,
                "🟡 GOLD GEM", GameState.GEM_GOLD,
                GameState.GOLD_COST, state.goldBought,
                state.ironBought, "x1.0", new Color(180, 140, 30));

            refreshGemButton(rubyBtn, rubyInfo,
                "🔴 RUBY GEM", GameState.GEM_RUBY,
                GameState.RUBY_COST, state.rubyBought,
                state.goldBought, "x1.5", new Color(190, 40, 40));

            refreshGemButton(sapphireBtn, sapphireInfo,
                "🔵 SAPPHIRE GEM", GameState.GEM_SAPPHIRE,
                GameState.SAPPHIRE_COST, state.sapphireBought,
                state.rubyBought, "x2.0", new Color(40, 80, 200));

            refreshGemButton(emeraldBtn, emeraldInfo,
                "🟢 EMERALD GEM", GameState.GEM_EMERALD,
                GameState.EMERALD_COST, state.emeraldBought,
                state.sapphireBought, "x2.5", new Color(30, 160, 60));

            refreshGemButton(diamondBtn, diamondInfo,
                "🔷 DIAMOND GEM", GameState.GEM_DIAMOND,
                GameState.DIAMOND_COST, state.diamondBought,
                state.emeraldBought, "x3.0", new Color(80, 160, 230));
        }

        /** Helper to update a single gem button's text, enabled state, and info label. */
        private void refreshGemButton(JButton btn, JLabel info,
                                      String baseName, int gemId,
                                      int cost, boolean bought,
                                      boolean prereqMet, String multiplierStr,
                                      Color baseColor) {
            if (bought) {
                btn.setText("<html><center>" + baseName
                    + "<br><small>✔ OWNED — " + multiplierStr + " active</small></center></html>");
                btn.setEnabled(false);
                btn.setBackground(baseColor.darker());
                info.setText("Active multiplier: " + multiplierStr);
                info.setForeground(new Color(120, 200, 120));
            } else if (!prereqMet) {
                btn.setText("<html><center>" + baseName
                    + "<br><small>🔒 LOCKED</small></center></html>");
                btn.setEnabled(false);
                btn.setBackground(new Color(50, 45, 40));
                info.setText("Buy previous gem to unlock");
                info.setForeground(new Color(130, 120, 110));
            } else {
                btn.setText("<html><center>" + baseName
                    + "<br><small>Cost: " + formatScore(cost) + " gems  |  " + multiplierStr + "</small></center></html>");
                btn.setEnabled(state.score >= cost);
                btn.setBackground(baseColor);
                info.setText("Click multiplier: " + multiplierStr + "  (resets gem count)");
                info.setForeground(new Color(160, 200, 160));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(new Color(30, 25, 20));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(new Color(80, 60, 30));
            g2.fillRect(0, 0, 2, getHeight());
        }

        // ── Builder helpers ──────────────────────────────────────────────────

        private JLabel makeSection(String text, Color color) {
            JLabel lbl = new JLabel(text);
            lbl.setForeground(color);
            lbl.setFont(new Font("Verdana", Font.BOLD, 13));
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            return lbl;
        }

        private JSeparator makeDivider(Color color) {
            JSeparator sep = new JSeparator();
            sep.setForeground(color);
            sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            return sep;
        }

        private JButton makeUpgradeButton(String text, Color baseColor) {
            JButton btn = new JButton(text);
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
            btn.setPreferredSize(new Dimension(280, 52));
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
            block.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

            JLabel desc = new JLabel(description);
            desc.setForeground(new Color(160, 150, 130));
            desc.setFont(new Font("Arial", Font.PLAIN, 11));
            desc.setAlignmentX(Component.LEFT_ALIGNMENT);

            block.add(btn);
            block.add(Box.createVerticalStrut(3));
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
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
            ));

            stats.add(makeStatRow("Total Clicks",  () -> formatScore(state.totalClicks)));
            stats.add(makeStatRow("Gems/sec",       () -> formatScore(state.passiveIncome)));
            stats.add(makeStatRow("Click power",    () -> formatScore(state.clickPower)));
            stats.add(makeStatRow("Gem multiplier", () -> "x" + String.format("%.1f", state.clickMultiplier)));
            stats.add(makeStatRow("Effective/click",() -> formatScore(state.clickPower * state.clickMultiplier)));
            stats.add(makeStatRow("Mine Lv.",       () -> String.valueOf(state.mineLevel)));
            stats.add(makeStatRow("Mine prod.",     () -> formatScore(state.mineProduction) + "/sec"));
            stats.add(makeStatRow("Active Gem",     () -> state.getActiveGemName()));
            return stats;
        }

        private JPanel makeStatRow(String label, java.util.function.Supplier<String> valueSupplier) {
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            JLabel lbl = makeStatLabel(label);
            JLabel val = makeStatLabel(valueSupplier.get());
            val.setHorizontalAlignment(SwingConstants.RIGHT);
            row.add(lbl, BorderLayout.WEST);
            row.add(val, BorderLayout.EAST);
            Timer t = new Timer(500, e -> val.setText(valueSupplier.get()));
            t.start();
            return row;
        }

        private JLabel makeStatLabel(String text) {
            JLabel l = new JLabel(text);
            l.setForeground(new Color(180, 170, 150));
            l.setFont(new Font("Arial", Font.PLAIN, 12));
            return l;
        }
    }

    // ─── Game State ───────────────────────────────────────────────────────────
    static class GameState {
        double score        = 0;
        int passiveIncome   = 0;
        int mineLevel       = 0;
        int mineProduction  = 0;
        int autoLevel       = 0;
        double clickPower   = 1.0;
        int pickLevel       = 0;
        long totalClicks    = 0;

        // ── Click multiplier (from Gem Shop) ────────────────────────────────
        double clickMultiplier = 1.0;

        // Gem purchase flags
        boolean ironBought     = false;
        boolean goldBought     = false;
        boolean rubyBought     = false;
        boolean sapphireBought = false;
        boolean emeraldBought  = false;
        boolean diamondBought  = false;

        // Gem IDs
        static final int GEM_IRON     = 0;
        static final int GEM_GOLD     = 1;
        static final int GEM_RUBY     = 2;
        static final int GEM_SAPPHIRE = 3;
        static final int GEM_EMERALD  = 4;
        static final int GEM_DIAMOND  = 5;

        // Gem costs (fixed one-time purchase)
        static final int IRON_COST     = 50;
        static final int GOLD_COST     = 200;
        static final int RUBY_COST     = 750;
        static final int SAPPHIRE_COST = 2500;
        static final int EMERALD_COST  = 8000;
        static final int DIAMOND_COST  = 25000;

        // Gem multipliers
        static final double IRON_MULT     = 0.5;
        static final double GOLD_MULT     = 1.0;  // resets; new baseline
        static final double RUBY_MULT     = 1.5;
        static final double SAPPHIRE_MULT = 2.0;
        static final double EMERALD_MULT  = 2.5;
        static final double DIAMOND_MULT  = 3.0;

        /**
         * Attempts to buy a gem. Returns true if the purchase succeeded.
         * Resets the score to 0 on buy (gem count reset mechanic) and
         * sets the new click multiplier permanently.
         */
        boolean buyGem(int gemId) {
            switch (gemId) {
                case GEM_IRON:
                    if (!ironBought && score >= IRON_COST) {
                        score -= IRON_COST;
                        ironBought = true;
                        clickMultiplier = IRON_MULT;
                        score = 0; // reset gem count
                        return true;
                    }
                    break;
                case GEM_GOLD:
                    if (ironBought && !goldBought && score >= GOLD_COST) {
                        score -= GOLD_COST;
                        goldBought = true;
                        clickMultiplier = GOLD_MULT;
                        score = 0;
                        return true;
                    }
                    break;
                case GEM_RUBY:
                    if (goldBought && !rubyBought && score >= RUBY_COST) {
                        score -= RUBY_COST;
                        rubyBought = true;
                        clickMultiplier = RUBY_MULT;
                        score = 0;
                        return true;
                    }
                    break;
                case GEM_SAPPHIRE:
                    if (rubyBought && !sapphireBought && score >= SAPPHIRE_COST) {
                        score -= SAPPHIRE_COST;
                        sapphireBought = true;
                        clickMultiplier = SAPPHIRE_MULT;
                        score = 0;
                        return true;
                    }
                    break;
                case GEM_EMERALD:
                    if (sapphireBought && !emeraldBought && score >= EMERALD_COST) {
                        score -= EMERALD_COST;
                        emeraldBought = true;
                        clickMultiplier = EMERALD_MULT;
                        score = 0;
                        return true;
                    }
                    break;
                case GEM_DIAMOND:
                    if (emeraldBought && !diamondBought && score >= DIAMOND_COST) {
                        score -= DIAMOND_COST;
                        diamondBought = true;
                        clickMultiplier = DIAMOND_MULT;
                        score = 0;
                        return true;
                    }
                    break;
            }
            return false;
        }

        /** Returns a display name for the currently active gem multiplier. */
        String getActiveGemName() {
            if (diamondBought)  return "Diamond";
            if (emeraldBought)  return "Emerald";
            if (sapphireBought) return "Sapphire";
            if (rubyBought)     return "Ruby";
            if (goldBought)     return "Gold";
            if (ironBought)     return "Iron";
            return "None";
        }

        // Colors used to tint the main gem on the canvas
        Color getActiveDiamondTopColor() {
            if (diamondBought)  return new Color(180, 230, 255);
            if (emeraldBought)  return new Color(100, 255, 160);
            if (sapphireBought) return new Color(100, 160, 255);
            if (rubyBought)     return new Color(255, 130, 130);
            if (goldBought)     return new Color(255, 230, 100);
            if (ironBought)     return new Color(210, 210, 220);
            return new Color(130, 200, 255);
        }

        Color getActiveDiamondBotColor() {
            if (diamondBought)  return new Color(30, 100, 200);
            if (emeraldBought)  return new Color(20, 130, 60);
            if (sapphireBought) return new Color(20, 60, 180);
            if (rubyBought)     return new Color(160, 20, 20);
            if (goldBought)     return new Color(160, 110, 10);
            if (ironBought)     return new Color(100, 100, 110);
            return new Color(30, 80, 160);
        }

        Color getActiveGemColor() {
            if (diamondBought)  return new Color(180, 230, 255);
            if (emeraldBought)  return new Color(80, 240, 120);
            if (sapphireBought) return new Color(100, 160, 255);
            if (rubyBought)     return new Color(255, 100, 100);
            if (goldBought)     return new Color(255, 220, 80);
            if (ironBought)     return new Color(210, 210, 220);
            return Color.WHITE;
        }

        Color getActiveGlowColor(int intensity) {
            Color base = getActiveGemColor();
            return new Color(base.getRed(), base.getGreen(), base.getBlue(), 15 * intensity);
        }

        // ── Original upgrade costs ───────────────────────────────────────────
        int nextAutoMinerCost() { return (int)(10 * Math.pow(1.15, autoLevel)); }
        int nextPickaxeCost()   { return (int)(15 * Math.pow(1.5,  pickLevel)); }
        int nextMineCost()      { return (int)(8  * Math.pow(1.12, mineLevel)); }

        void buyAutoMiner() {
            int cost = nextAutoMinerCost();
            if (score >= cost) {
                score -= cost;
                autoLevel++;
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

        void buyMine() {
            int cost = nextMineCost();
            if (score >= cost) {
                score -= cost;
                mineLevel++;
                mineProduction = (int)(mineLevel * 0.8 + (mineLevel / 10));
                passiveIncome  = autoLevel + (autoLevel / 5) * 2 + mineProduction;
            }
        }

        void recomputeStats() {
            mineProduction = (int)(mineLevel * 0.8 + (mineLevel / 10));
            passiveIncome  = autoLevel + (autoLevel / 5) * 2 + mineProduction;
            clickPower     = 1 + (pickLevel * 2) + (pickLevel / 3) * 3;

            // Re-derive multiplier from flags
            if      (diamondBought)  clickMultiplier = DIAMOND_MULT;
            else if (emeraldBought)  clickMultiplier = EMERALD_MULT;
            else if (sapphireBought) clickMultiplier = SAPPHIRE_MULT;
            else if (rubyBought)     clickMultiplier = RUBY_MULT;
            else if (goldBought)     clickMultiplier = GOLD_MULT;
            else if (ironBought)     clickMultiplier = IRON_MULT;
            else                     clickMultiplier = 1.0;
        }
    }

    // ─── Persistence ──────────────────────────────────────────────────────────
    private void saveGame() {
        Properties p = new Properties();
        p.setProperty("score",          String.valueOf(state.score));
        p.setProperty("mineLevel",      String.valueOf(state.mineLevel));
        p.setProperty("autoLevel",      String.valueOf(state.autoLevel));
        p.setProperty("pickLevel",      String.valueOf(state.pickLevel));
        p.setProperty("totalClicks",    String.valueOf(state.totalClicks));
        // Gem shop flags
        p.setProperty("ironBought",     String.valueOf(state.ironBought));
        p.setProperty("goldBought",     String.valueOf(state.goldBought));
        p.setProperty("rubyBought",     String.valueOf(state.rubyBought));
        p.setProperty("sapphireBought", String.valueOf(state.sapphireBought));
        p.setProperty("emeraldBought",  String.valueOf(state.emeraldBought));
        p.setProperty("diamondBought",  String.valueOf(state.diamondBought));
        try (FileOutputStream out = new FileOutputStream(SAVE_FILE)) {
            p.store(out, "Diamond Rush Save");
        } catch (Exception ignored) {}
    }

    private void loadGame() {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(SAVE_FILE)) {
            p.load(in);
            state.score          = Double.parseDouble(p.getProperty("score",          "0"));
            state.mineLevel      = Integer.parseInt(p.getProperty("mineLevel",        "0"));
            state.autoLevel      = Integer.parseInt(p.getProperty("autoLevel",        "0"));
            state.pickLevel      = Integer.parseInt(p.getProperty("pickLevel",        "0"));
            state.totalClicks    = Long.parseLong(p.getProperty("totalClicks",        "0"));
            state.ironBought     = Boolean.parseBoolean(p.getProperty("ironBought",     "false"));
            state.goldBought     = Boolean.parseBoolean(p.getProperty("goldBought",     "false"));
            state.rubyBought     = Boolean.parseBoolean(p.getProperty("rubyBought",     "false"));
            state.sapphireBought = Boolean.parseBoolean(p.getProperty("sapphireBought", "false"));
            state.emeraldBought  = Boolean.parseBoolean(p.getProperty("emeraldBought",  "false"));
            state.diamondBought  = Boolean.parseBoolean(p.getProperty("diamondBought",  "false"));
            state.recomputeStats();
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