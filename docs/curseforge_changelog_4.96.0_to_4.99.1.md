# Multiverse of Anime: what is new since 4.95.1 (4.96.0 to 4.99.1)

For players and server owners. Almost every change has a setting that turns it off, so you can go back to the old behaviour. Old worlds keep working.
The earlier list (3.105.0 to 4.95.1) is in `docs/curseforge_changelog_4.0.0_to_4.95.1.md`.

---

## SHORT VERSION (paste this into the CurseForge changelog box)

**4.96.0 to 4.99.1**

**Goku:** every form now has its own health bar. A bar runs down to zero, Goku powers up, and the next, stronger form comes back with a full bar. He now fights with his fists and hits much harder.
**Bosses:** no boss starves to death any more, and no boss can steal your magicule, aura or skills.
**Boss arena:** you are no longer thrown out in the middle of a fight. You go home when you win, use a lodestone, or use the Chronicle Sigil.
**Creative menu:** a creative tab for every anime verse, plus `/multiverse verses` for server owners.
**Spirit Dive:** the Wind, Fire, Water and Earth grimoires can dive without a Tensura Lord level.

---

## FULL CHANGELOG

### 4.99.1: this changelog
Text only, no game change.

### 4.99.0: Spirit Dive opens for every grimoire wielder
- Before, the Spirit Dive page only opened for a Lord of that spirit in Tensura's own spirit data, so most mages never saw it. Now any wielder of a Wind, Fire, Water or Earth grimoire can use it.
- Still needed: the spirit answers you, it is not bonded to another mage, the first dive costs a Spirit Charm, and the dive costs magicule while it lasts.
- Setting: server `spiritDive.requireLordLevel` (default off). Turn it on to get the old rule back.
- All dive looks and tiers (Yuno, Fuegoleon, Noelle) are unchanged. Light, Dark and Space spirits still have no dive.

### 4.98.0: Goku punches, no more being thrown out, bosses take nothing from you
- **Goku fights with his fists.** Between his moves he runs at you and punches. His physical blows hit much harder (4x by default, never more than 75 percent of a player's health in one blow). Beams and the Spirit Bomb keep their own limits. Settings: `goku_boss.punches`, `punchMultiplier`, `punchHitShare`, `punchGapTicks`.
- **No more being kicked out of the arena mid-fight.** The 30-minute fight timer no longer sends you home, and a fight where the boss simply vanished no longer carries you away. You go home when you win, when you click a lodestone, or with the Chronicle Sigil. Settings: `bossRealm.timeoutEvicts` and `bossRealm.returnOnlyAfterWin`.
- **Bosses cannot steal from you.** No boss absorbs or drains your magicule, aura or skills. Goku Omega's Energy Drain and Skill Seal are off, and a boss cannot use a skill whose name says drain, steal, siphon, absorb, leech or plunder. Setting: `bossRules.noPoolTheft`.

### 4.97.0: a health bar for every Goku form
- Like Lucifero's phases: the bar of a form runs to zero, Goku cannot be hurt while he powers up, then the next, stronger form returns with a full bar.
- One bar is smaller than the old single bar, so ten forms do not make the fight ten times longer (`goku_boss.formBarShare`, default 0.4). Turn `goku_boss.formBars` off for the old single bar.

### 4.96.0: bosses stop killing themselves, and a creative tab for every verse
- **Fixed:** Goku and Kakashi spend Ki (magicule), ran dry, and Tensura's own energy rule then hurt them for their full health with no attacker. Every boss now keeps its pools topped up. Setting: `bossRules.keepPoolsFull`.
- **New:** one creative tab per anime verse (28 more tabs: Dragon Ball, Shield Hero, DanMachi, Seven Deadly Sins, Naruto, Bleach, One Piece and many more). Server owners can run `/multiverse verses` to see how full each tab is.
- The seven older tabs and all their items are still there.

---

## Good to know
- Server settings change gameplay; client settings only change how things look.
- Existing config files keep their old values; delete a line from a config file to get the new default.
- The new work is light on public servers: no new entities, a few small packets, and effects only for nearby players.
- Not everything has been seen in a game by the author. Please report anything odd with your `latest.log`.

*Small build-fix and test-fix releases in between are not listed separately.*
