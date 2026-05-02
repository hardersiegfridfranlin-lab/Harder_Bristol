import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import javax.swing.*;

/*
 * 💎 Diamond Rush: Professional Edition
 *
 * Description:
 *   A Java Swing idle clicker game where you mine gems by clicking the diamond,
 *   buy upgrades, unlock multipliers, and boost passive income.
 *
 * How to Run:
 *   Compile : javac DiamondRush.java
 *   Run     : java DiamondRush
 *
 * Controls:
 *   - Click diamond  — mine gems
 *   - Shop buttons   — buy upgrades
 *   - Mouse wheel    — scroll shop panel
 *   - Reset button   — restart game (with confirmation)
 *
 * Features:
 *   - Upgrades: Auto Miner, Build Mine, Sharpen Pickaxe
 *   - Gem multipliers: Iron -> Gold -> Ruby -> Sapphire -> Emerald -> Diamond
 *   - Auto Miner skins: Shovel -> Pickaxe -> Jackhammer
 *   - Visual effects: pulsing gem, screen shake (diamond only), floating +score text
 *   - Auto-save to diamond-rush-save.properties every 5 seconds
 *
 * Architecture Overview:
 *   - GameState    — all mutable game data; no rendering logic
 *   - GameCanvas   — left panel; draws the gem, score, and popups
 *   - ShopPanel    — right panel; upgrade buttons and live stats
 *   - GemUpgrade   — enum of purchasable click-multiplier gems
 *   - AutoMinerSkin — enum of purchasable auto-speed skins
 *   - FloatingText — lightweight DTO for animated +score popups
 */
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
        setSize(1100, 720);
        setMinimumSize(new Dimension(900, 560));
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
        shopPanel.setPreferredSize(new Dimension(340, 0));
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
            // passive income uses combined auto miner skin multiplier
            state.score += (state.passiveIncome * state.autoSkinMultiplier) / 20.0;
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

            // Stable context — NO shake here (score, badges, popups, hint all stay still)
            Graphics2D g2s = (Graphics2D) g2.create();

            // Score display
            g2s.setColor(new Color(180, 160, 120));
            g2s.setFont(new Font("Verdana", Font.PLAIN, 14));
            g2s.drawString("GEMS", 30, 50);

            g2s.setColor(Color.WHITE);
            g2s.setFont(new Font("Verdana", Font.BOLD, 38));
            g2s.drawString(formatScore(state.score), 30, 92);

            double effectivePassive = state.passiveIncome * state.autoSkinMultiplier;
            if (effectivePassive > 0) {
                g2s.setColor(new Color(120, 200, 120));
                g2s.setFont(new Font("Verdana", Font.PLAIN, 13));
                g2s.drawString("+" + formatScore(effectivePassive) + "/sec", 30, 115);
            }

            g2s.setColor(new Color(140, 180, 240));
            g2s.setFont(new Font("Verdana", Font.PLAIN, 13));
            g2s.drawString("Click power: " + formatScore(state.clickPower * state.clickMultiplier), 30, 140);

            // Active gem multiplier badge
            if (state.clickMultiplier > 1.0) {
                Color gemColor = state.getActiveGemColor();
                String gemName = state.getActiveGemName();
                g2s.setColor(new Color(0, 0, 0, 120));
                g2s.fillRoundRect(28, 150, 220, 26, 8, 8);
                g2s.setColor(gemColor);
                g2s.setFont(new Font("Verdana", Font.BOLD, 12));
                g2s.drawString(gemName + "  x" + String.format("%.1f", state.clickMultiplier) + " gem", 34, 167);
            }

            // Active skin badge
            if (state.autoSkinMultiplier > 1.0) {
                g2s.setColor(new Color(0, 0, 0, 120));
                g2s.fillRoundRect(28, 180, 220, 26, 8, 8);
                g2s.setColor(new Color(255, 180, 60));
                g2s.setFont(new Font("Verdana", Font.BOLD, 12));
                g2s.drawString(state.getActiveSkinName() + "  x" + String.format("%.0f", state.autoSkinMultiplier) + " auto speed", 34, 197);
            }

            // ── Diamond: shake applies ONLY to this context ──────────────────
            pulseAngle += 0.04f;
            int pulseMag = (int)(Math.sin(pulseAngle) * 4);
            int centerX = W / 2;
            int centerY = H / 2 + 20;
            int size = 140 + pulseMag;

            Color gemTopColor = state.getActiveDiamondTopColor();
            Color gemBotColor = state.getActiveDiamondBotColor();

            // Compute diamond polygon in stable coordinates (used for click detection)
            int[] xPts = {centerX, centerX + size / 2, centerX, centerX - size / 2};
            int[] yPts = {centerY - size / 2, centerY, centerY + size / 2, centerY};
            diamond = new Polygon(xPts, yPts, 4);

            // Create a separate child context just for the diamond so shake
            // only affects it and not the rest of the UI
            Graphics2D g2d = (Graphics2D) g2s.create();
            if (shakeIntensity > 0.5f) {
                float dx = (rand.nextFloat() - 0.5f) * shakeIntensity;
                float dy = (rand.nextFloat() - 0.5f) * shakeIntensity;
                g2d.translate(dx, dy);
            }

            // Glow layers
            for (int i = 3; i >= 1; i--) {
                int gSize = size + i * 10;
                int[] gxPts = {centerX, centerX + gSize / 2, centerX, centerX - gSize / 2};
                int[] gyPts = {centerY - gSize / 2, centerY, centerY + gSize / 2, centerY};
                Color glowC = state.getActiveGlowColor(i);
                g2d.setColor(glowC);
                g2d.fillPolygon(new Polygon(gxPts, gyPts, 4));
            }

            // Diamond fill
            GradientPaint gem = new GradientPaint(
                centerX, centerY - size / 2, gemTopColor,
                centerX, centerY + size / 2, gemBotColor
            );
            g2d.setPaint(gem);
            g2d.fill(diamond);

            // Highlight
            int[] hlX = {centerX, centerX - size / 4, centerX};
            int[] hlY = {centerY - size / 2, centerY - size / 8, centerY - size / 4};
            g2d.setColor(new Color(255, 255, 255, 80));
            g2d.fillPolygon(new Polygon(hlX, hlY, 3));

            // Outline
            g2d.setColor(new Color(200, 230, 255, 200));
            g2d.setStroke(new BasicStroke(2f));
            g2d.draw(diamond);

            g2d.dispose(); // done with shaking diamond context

            // ── Floating text popups (stable — no shake) ─────────────────────
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
        // Upgrades
        private JButton minerBtn, mineBtn, pickaxeBtn, resetBtn;
        private JLabel minerInfo, mineInfo, pickaxeInfo;

        // Gem Shop
        private JButton[] gemBtns = new JButton[GemUpgrade.values().length];
        private JLabel[] gemInfos = new JLabel[GemUpgrade.values().length];

        // Auto Miner Skins
        private JButton[] skinBtns = new JButton[AutoMinerSkin.values().length];
        private JLabel[] skinInfos = new JLabel[AutoMinerSkin.values().length];

        ShopPanel() {
            setLayout(new BorderLayout());
            setBackground(new Color(30, 25, 20));
            setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setOpaque(false);

            // ── UPGRADES ────────────────────────────────────────────────────
            content.add(makeSection("⛏  UPGRADES", new Color(200, 160, 80)));
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

            // ── GEM SHOP ────────────────────────────────────────────────────
            content.add(makeSection("💎  GEM SHOP  (click multipliers)", new Color(255, 160, 210)));
            content.add(Box.createVerticalStrut(4));
            content.add(makeDivider(new Color(160, 100, 150)));
            content.add(Box.createVerticalStrut(8));

            for (GemUpgrade gem : GemUpgrade.values()) {
                int idx = gem.ordinal();
                gemBtns[idx] = makeUpgradeButton(gem.displayName, gem.btnColor);
                final GemUpgrade g = gem;
                gemBtns[idx].addActionListener(e -> { state.buyGem(g); refresh(); });
                gemInfos[idx] = makeInfoLabel();
                content.add(makeUpgradeBlock(gemBtns[idx], gemInfos[idx], gem.description, gem.accentColor));
                content.add(Box.createVerticalStrut(8));
            }

            content.add(Box.createVerticalStrut(6));

            // ── AUTO MINER SKINS ────────────────────────────────────────────
            content.add(makeSection("🪄  AUTO MINER SKINS  (speed multipliers)", new Color(255, 200, 80)));
            content.add(Box.createVerticalStrut(4));
            content.add(makeDivider(new Color(160, 140, 60)));
            content.add(Box.createVerticalStrut(8));

            for (AutoMinerSkin skin : AutoMinerSkin.values()) {
                int idx = skin.ordinal();
                skinBtns[idx] = makeUpgradeButton(skin.displayName, skin.btnColor);
                final AutoMinerSkin s = skin;
                skinBtns[idx].addActionListener(e -> { state.buySkin(s); refresh(); });
                skinInfos[idx] = makeInfoLabel();
                content.add(makeUpgradeBlock(skinBtns[idx], skinInfos[idx], skin.description, skin.accentColor));
                content.add(Box.createVerticalStrut(8));
            }

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

            // ── Smooth Scroll Pane ──────────────────────────────────────────
            JScrollPane scroll = new JScrollPane(content);
            scroll.setBorder(null);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            JScrollBar vsb = scroll.getVerticalScrollBar();
            vsb.setUnitIncrement(20);
            vsb.setBlockIncrement(80);
            scroll.removeMouseWheelListener(scroll.getMouseWheelListeners()[0]);
            scroll.addMouseWheelListener(e -> {
                int delta = e.getWheelRotation() * vsb.getUnitIncrement();
                vsb.setValue(vsb.getValue() + delta);
            });
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

            double effectivePassive = state.passiveIncome * state.autoSkinMultiplier;
            minerInfo.setText("Miners: +" + formatScore(state.passiveIncome - state.mineProduction) + "/sec (base)");
            mineInfo.setText("Mines: +" + formatScore(state.mineProduction) + "/sec (base)");
            pickaxeInfo.setText("Pickaxe power: " + formatScore(state.clickPower) + "/click");

            // ── Gem Shop ──
            GemUpgrade[] gems = GemUpgrade.values();
            for (int i = 0; i < gems.length; i++) {
                GemUpgrade gem = gems[i];
                boolean owned    = state.isGemOwned(gem);
                boolean prereq   = (i == 0) || state.isGemOwned(gems[i - 1]);
                boolean canAfford = state.score >= gem.cost;

                JButton btn  = gemBtns[i];
                JLabel  info = gemInfos[i];

                if (owned) {
                    btn.setText("<html><center>" + gem.displayName
                        + "<br><small>✔ OWNED — " + gem.multiplierStr + " active</small></center></html>");
                    btn.setEnabled(false);
                    btn.setBackground(gem.btnColor.darker());
                    info.setText("Active multiplier: " + gem.multiplierStr);
                    info.setForeground(new Color(120, 200, 120));
                } else if (!prereq) {
                    btn.setText("<html><center>" + gem.displayName
                        + "<br><small>🔒 LOCKED — buy previous gem first</small></center></html>");
                    btn.setEnabled(false);
                    btn.setBackground(new Color(50, 45, 40));
                    info.setText("Unlock previous gem to access");
                    info.setForeground(new Color(130, 120, 110));
                } else {
                    btn.setText("<html><center>" + gem.displayName
                        + "<br><small>Cost: " + formatScore(gem.cost) + " gems  |  " + gem.multiplierStr + "</small></center></html>");
                    btn.setEnabled(canAfford);
                    btn.setBackground(gem.btnColor);
                    info.setText("Click multiplier: " + gem.multiplierStr + "  •  Resets gem count");
                    info.setForeground(new Color(160, 200, 160));
                }
            }

            // ── Auto Miner Skins ──
            AutoMinerSkin[] skins = AutoMinerSkin.values();
            for (int i = 0; i < skins.length; i++) {
                AutoMinerSkin skin = skins[i];
                boolean owned    = state.isSkinOwned(skin);
                boolean prereq   = (i == 0) || state.isSkinOwned(skins[i - 1]);
                boolean canAfford = state.score >= skin.cost;

                JButton btn  = skinBtns[i];
                JLabel  info = skinInfos[i];

                if (owned) {
                    btn.setText("<html><center>" + skin.displayName
                        + "<br><small>✔ OWNED — x" + (int)skin.speedMultiplier + " auto speed active</small></center></html>");
                    btn.setEnabled(false);
                    btn.setBackground(skin.btnColor.darker());
                    info.setText("Active auto speed: x" + (int)skin.speedMultiplier);
                    info.setForeground(new Color(255, 200, 100));
                } else if (!prereq) {
                    btn.setText("<html><center>" + skin.displayName
                        + "<br><small>🔒 LOCKED — buy previous skin first</small></center></html>");
                    btn.setEnabled(false);
                    btn.setBackground(new Color(50, 45, 40));
                    info.setText("Unlock previous skin to access");
                    info.setForeground(new Color(130, 120, 110));
                } else {
                    btn.setText("<html><center>" + skin.displayName
                        + "<br><small>Cost: " + formatScore(skin.cost) + " gems  |  x" + (int)skin.speedMultiplier + " auto speed</small></center></html>");
                    btn.setEnabled(canAfford);
                    btn.setBackground(skin.btnColor);
                    info.setText("Auto mining multiplier: x" + (int)skin.speedMultiplier);
                    info.setForeground(new Color(200, 180, 120));
                }
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
            btn.setPreferredSize(new Dimension(300, 52));
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

            stats.add(makeStatRow("Total Clicks",      () -> formatScore(state.totalClicks)));
            stats.add(makeStatRow("Gems/sec",           () -> formatScore(state.passiveIncome * state.autoSkinMultiplier)));
            stats.add(makeStatRow("Click power",        () -> formatScore(state.clickPower)));
            stats.add(makeStatRow("Gem multiplier",     () -> "x" + String.format("%.1f", state.clickMultiplier)));
            stats.add(makeStatRow("Auto skin",          () -> "x" + String.format("%.0f", state.autoSkinMultiplier)));
            stats.add(makeStatRow("Effective/click",    () -> formatScore(state.clickPower * state.clickMultiplier)));
            stats.add(makeStatRow("Mine Lv.",           () -> String.valueOf(state.mineLevel)));
            stats.add(makeStatRow("Mine prod.",         () -> formatScore(state.mineProduction) + "/sec"));
            stats.add(makeStatRow("Active Gem",         () -> state.getActiveGemName()));
            stats.add(makeStatRow("Active Skin",        () -> state.getActiveSkinName()));
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

    // ─── Gem Upgrade Enum ─────────────────────────────────────────────────────
    enum GemUpgrade {
        IRON    ("⬜ IRON GEM",     500,    1.5,  "x1.5",
                 new Color(140,140,150), new Color(200,200,210),
                 "Always available first  •  x1.5 per click"),
        GOLD    ("🟡 GOLD GEM",     800,    2.0,  "x2.0",
                 new Color(180,140,30),  new Color(240,200,80),
                 "Requires Iron  •  x2.0 per click"),
        RUBY    ("🔴 RUBY GEM",     1200,   2.5,  "x2.5",
                 new Color(190,40,40),   new Color(255,100,100),
                 "Requires Gold  •  x2.5 per click"),
        SAPPHIRE("🔵 SAPPHIRE GEM", 5000,   3.0,  "x3.0",
                 new Color(40,80,200),   new Color(100,160,255),
                 "Requires Ruby  •  x3.0 per click"),
        EMERALD ("🟢 EMERALD GEM",  10000,  4.0,  "x4.0",
                 new Color(30,160,60),   new Color(80,240,120),
                 "Requires Sapphire  •  x4.0 per click"),
        DIAMOND ("🔷 DIAMOND GEM",  25000,  5.0,  "x5.0",
                 new Color(80,160,230),  new Color(200,230,255),
                 "Requires Emerald  •  x5.0 per click");

        final String displayName;
        final int    cost;
        final double multiplier;
        final String multiplierStr;
        final Color  btnColor;
        final Color  accentColor;
        final String description;

        GemUpgrade(String displayName, int cost, double multiplier, String multiplierStr,
                   Color btnColor, Color accentColor, String description) {
            this.displayName    = displayName;
            this.cost           = cost;
            this.multiplier     = multiplier;
            this.multiplierStr  = multiplierStr;
            this.btnColor       = btnColor;
            this.accentColor    = accentColor;
            this.description    = description;
        }
    }

    // ─── Auto Miner Skin Enum ─────────────────────────────────────────────────
    enum AutoMinerSkin {
        SHOVEL     ("🪓 SHOVEL",      5_000,  2.0,
                    new Color(120, 80, 40),  new Color(180, 130, 70),
                    "Always available first  •  2× auto mining speed"),
        PICKAXE    ("⛏ PICKAXE",      10_000, 4.0,
                    new Color(80, 100, 140), new Color(120, 150, 200),
                    "Requires Shovel  •  4× auto mining speed"),
        JACKHAMMER ("🔩 JACKHAMMER",  30_000, 7.0,
                    new Color(160, 60, 20),  new Color(220, 110, 60),
                    "Requires Pickaxe  •  7× auto mining speed");

        final String displayName;
        final int    cost;
        final double speedMultiplier;
        final Color  btnColor;
        final Color  accentColor;
        final String description;

        AutoMinerSkin(String displayName, int cost, double speedMultiplier,
                      Color btnColor, Color accentColor, String description) {
            this.displayName     = displayName;
            this.cost            = cost;
            this.speedMultiplier = speedMultiplier;
            this.btnColor        = btnColor;
            this.accentColor     = accentColor;
            this.description     = description;
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

        double clickMultiplier    = 1.0;
        double autoSkinMultiplier = 1.0;

        boolean[] gemsOwned  = new boolean[GemUpgrade.values().length];
        boolean[] skinsOwned = new boolean[AutoMinerSkin.values().length];

        // ── Gem purchase ────────────────────────────────────────────────────
        boolean buyGem(GemUpgrade gem) {
            int idx = gem.ordinal();
            if (gemsOwned[idx]) return false;
            if (idx > 0 && !gemsOwned[idx - 1]) return false;
            if (score < gem.cost) return false;
            score -= gem.cost;
            gemsOwned[idx] = true;
            score = 0;
            recomputeMultipliers();
            return true;
        }

        boolean isGemOwned(GemUpgrade gem) { return gemsOwned[gem.ordinal()]; }

        // ── Skin purchase ────────────────────────────────────────────────────
        boolean buySkin(AutoMinerSkin skin) {
            int idx = skin.ordinal();
            if (skinsOwned[idx]) return false;
            if (idx > 0 && !skinsOwned[idx - 1]) return false;
            if (score < skin.cost) return false;

            score -= skin.cost;
            skinsOwned[idx] = true;
            recomputeMultipliers();
            return true;
        }

        boolean isSkinOwned(AutoMinerSkin skin) { return skinsOwned[skin.ordinal()]; }

        // ── Recompute combined multipliers ───────────────────────────────────
        void recomputeMultipliers() {
            // Gem: highest owned gem multiplier, default 1.0
            clickMultiplier = 1.0;
            GemUpgrade[] gems = GemUpgrade.values();
            for (int i = gems.length - 1; i >= 0; i--) {
                if (gemsOwned[i]) { clickMultiplier = gems[i].multiplier; break; }
            }

            // Skin: highest owned skin multiplier, default 1.0
            autoSkinMultiplier = 1.0;
            AutoMinerSkin[] skins = AutoMinerSkin.values();
            for (int i = skins.length - 1; i >= 0; i--) {
                if (skinsOwned[i]) { autoSkinMultiplier = skins[i].speedMultiplier; break; }
            }
        }

        // ── Display helpers ──────────────────────────────────────────────────
        String getActiveGemName() {
            GemUpgrade[] gems = GemUpgrade.values();
            for (int i = gems.length - 1; i >= 0; i--) {
                if (gemsOwned[i]) return gems[i].displayName.replaceAll("^\\S+ ", "");
            }
            return "None";
        }

        String getActiveSkinName() {
            AutoMinerSkin[] skins = AutoMinerSkin.values();
            for (int i = skins.length - 1; i >= 0; i--) {
                if (skinsOwned[i]) return skins[i].displayName.replaceAll("^\\S+ ", "");
            }
            return "None";
        }

        // Diamond canvas tint based on highest gem
        Color getActiveDiamondTopColor() {
            GemUpgrade[] gems = GemUpgrade.values();
            Color[] tops = {
                new Color(210,210,220), new Color(255,230,100),
                new Color(255,130,130), new Color(100,160,255),
                new Color(100,255,160), new Color(180,230,255)
            };
            for (int i = gems.length - 1; i >= 0; i--) {
                if (gemsOwned[i]) return tops[i];
            }
            return new Color(130, 200, 255);
        }

        Color getActiveDiamondBotColor() {
            GemUpgrade[] gems = GemUpgrade.values();
            Color[] bots = {
                new Color(100,100,110), new Color(160,110,10),
                new Color(160,20,20),   new Color(20,60,180),
                new Color(20,130,60),   new Color(30,100,200)
            };
            for (int i = gems.length - 1; i >= 0; i--) {
                if (gemsOwned[i]) return bots[i];
            }
            return new Color(30, 80, 160);
        }

        Color getActiveGemColor() {
            GemUpgrade[] gems = GemUpgrade.values();
            Color[] clrs = {
                new Color(210,210,220), new Color(255,220,80),
                new Color(255,100,100), new Color(100,160,255),
                new Color(80,240,120),  new Color(180,230,255)
            };
            for (int i = gems.length - 1; i >= 0; i--) {
                if (gemsOwned[i]) return clrs[i];
            }
            return Color.WHITE;
        }

        Color getActiveGlowColor(int intensity) {
            Color base = getActiveGemColor();
            return new Color(base.getRed(), base.getGreen(), base.getBlue(), 15 * intensity);
        }

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
            recomputeMultipliers();
        }
    }

    // ─── Persistence ──────────────────────────────────────────────────────────
    private void saveGame() {
        Properties p = new Properties();
        p.setProperty("score",       String.valueOf(state.score));
        p.setProperty("mineLevel",   String.valueOf(state.mineLevel));
        p.setProperty("autoLevel",   String.valueOf(state.autoLevel));
        p.setProperty("pickLevel",   String.valueOf(state.pickLevel));
        p.setProperty("totalClicks", String.valueOf(state.totalClicks));
        for (GemUpgrade gem : GemUpgrade.values()) {
            p.setProperty("gem_" + gem.name(), String.valueOf(state.gemsOwned[gem.ordinal()]));
        }
        for (AutoMinerSkin skin : AutoMinerSkin.values()) {
            p.setProperty("skin_" + skin.name(), String.valueOf(state.skinsOwned[skin.ordinal()]));
        }

        try (FileOutputStream out = new FileOutputStream(SAVE_FILE)) {
            p.store(out, "Diamond Rush Save");
        } catch (Exception ignored) {}
    }

    private void loadGame() {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(SAVE_FILE)) {
            p.load(in);
            state.score       = Double.parseDouble(p.getProperty("score",       "0"));
            state.mineLevel   = Integer.parseInt(p.getProperty("mineLevel",     "0"));
            state.autoLevel   = Integer.parseInt(p.getProperty("autoLevel",     "0"));
            state.pickLevel   = Integer.parseInt(p.getProperty("pickLevel",     "0"));
            state.totalClicks = Long.parseLong(p.getProperty("totalClicks",     "0"));

            for (GemUpgrade gem : GemUpgrade.values()) {
                state.gemsOwned[gem.ordinal()] =
                    Boolean.parseBoolean(p.getProperty("gem_" + gem.name(), "false"));
            }

            for (AutoMinerSkin skin : AutoMinerSkin.values()) {
                state.skinsOwned[skin.ordinal()] =
                    Boolean.parseBoolean(p.getProperty("skin_" + skin.name(), "false"));
            }

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
    // Lightweight data-transfer object for animated +score popup text.
    // Each instance fades upward over ~45 frames then is discarded.
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