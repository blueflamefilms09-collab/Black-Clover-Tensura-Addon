## 3.52.0 - Rank Mana Cores, Beast Will in the main menu, Grimoires at 20,000 EP

This file covers everything since the last CurseForge upload (3.48.0): 3.49.0 to 3.52.0.

**New**
- 24 rank Mana Cores: ranks E, D, C, B, A, AA, S, SS, each as Standard, Beast and Ancient. Real 3D orbs in hand. Press = right-click, Crouch + Press = shift + right-click; passives work while the core is held. Drops from wild kills, pocket-rift clears and vaults. Admin: `/manacore give|list`, config `nusmp-manacore-server.toml`.
- SS Ancient core "World Line Core / Dragon Fragment": World of Still Seconds (75,000 MP, 10 s freeze, players max 3 s, bosses 1 s) and Last Second of Creation (120,000 MP, 30 s countdown, 600 s cooldown).
- All eight beast cores (E to SS) now use the Beast Will system (Acquire / Integrate, assimilation, mastery, strain). Four new wills: Ridge-Wolf, Crested Boar, Wind-Skimmer Raptor, Flame-Serpent (only from their cores). Beast cores A and up plug into it too, with a new Thunder-Fang Chimera will.
- Beast Will page inside the Multiverse status menu: will, bars, abilities with cooldowns, Acquire / Integrate / Swap buttons. The absorb question opens inside the menu. No extra screens on your HUD.
- Grimoires only call a soul of at least 20,000 EP (server setting `multiverse.minEp`, 0 = off). Every grimoire block says what is missing.
- Pocket rifts: clearing one has a 6 % chance (`rift.djinnSealChance`, 0 = off) to give a Djinn Seal to a random player inside.
- Multiverse status menu: new Soul block (EP vs the grimoire need) and Djinn block (name, phase, trust).
- Nen rework: seven modes added (Shu, In, Ryu, Hatsu Transmutation, Conjuration, Manipulation and more), Water Divination type per player, new aura visuals (server setting `nenVfx`).
- Abyssal floors that fail to plan now re-plan themselves up to 3 times (`mega.autoRetries`).
- Fix: a rift's chests can no longer be farmed by re-entering the same rift (3.48.3).

**Replaced**
- The second active of the E to B beast cores is now the Beast Will form (old texts kept in the lang file).
- The always-on Beast Will side panel is off by default (client setting `[beastwill] hud = true` brings it back).
- Nen aura particle puffs replaced by flowing aura visuals (`nenVfx = false` restores the old puffs).

**Kept**
- The sector-key Mana Core, the original Beast Will wills, the five Beast Will keys and Codex page, the 10 original Nen modes, every other item and skill.

**Known issues**
- Not yet seen in a running game: the orb in-hand placement, the Beast Will page at every GUI scale, balance of the new wills and cores.
- Not built yet: Fragile Resonance (max MP) and Cosmic Infusion; some core passives are simplified.
- Abyssal floors 15 and deeper can still fail on some seeds; a root cause was not found.

**Update note:** remove any older `multiverse-of-anime-*.jar` from `mods` first. Install on both client and server.
