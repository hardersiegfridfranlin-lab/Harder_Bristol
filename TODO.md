# DiamondRush Gem Shop Implementation Plan
Approved: Add Gem Shop with 4 gems for click multipliers (Ruby +0.5, Sapphire +1, Emerald +2, Diamond +5 per level/boost).

## Steps:
1. [x] Add new fields to GameState: rubyLevel, sapphireLevel, emeraldLevel, diamondLevel (int=0).
2. [x] Add next*Cost() methods for each gem (e.g., ruby: 20 * 1.18^level).
3. [x] Add buy*() methods for each gem (deduct cost, ++level, call recomputeStats()).
4. [x] Update GameState.recomputeStats(): clickPower += rubyLevel*0.5 + sapphire*1 + emerald*2 + diamond*5 (add to existing).
5. [x] Update saveGame(): Serialize new levels (p.setProperty(\"rubyLevel\", ...)).
6. [x] Update loadGame(): Parse new levels (default 0), call recomputeStats().
7. [x] In ShopPanel constructor: Add \"GEM SHOP\" title + 4 new buttons/info (after pickaxe block).
8. [x] In ShopPanel.refresh(): Update 4 new btn texts/enabled/info labels (like existing).
9. [x] Optional: Add gem power stats to stats panel.
10. [x] Test: javac DiamondRush.java && java DiamondRush - compiles and runs, gems boost click multipliers correctly, saves/loads.
11. [ ] Update Readme.md.
8. [ ] In ShopPanel.refresh(): Update 4 new btn texts/enabled/info labels (like existing).
9. [ ] Optional: Add gem power stats to stats panel.
10. [ ] Test: javac DiamondRush.java && java DiamondRush - buy gems, verify clickPower/persist/UI.
11. [ ] Update Readme.md.
7. [ ] In ShopPanel constructor: Add \"GEM SHOP\" title + 4 new buttons/info (after pickaxe block).
8. [ ] In ShopPanel.refresh(): Update 4 new btn texts/enabled/info labels (like existing).
9. [ ] Optional: Add gem power stats to stats panel.
10. [ ] Test: javac DiamondRush.java && java DiamondRush - buy gems, verify clickPower/persist/UI.
11. [ ] Update Readme.md.

Progress updated after each step. Mines plan complete (step 8 pending, low prio)."

