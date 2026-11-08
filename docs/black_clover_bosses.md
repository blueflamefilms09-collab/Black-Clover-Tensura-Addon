# Black Clover bosses x3 (3.64.0)

**What it does.** Every boss of this mod whose character comes from *Black Clover* has **three times its maximum health** and **deals three times the damage**. Nothing else changes: speed, armour, moves, phases, barriers, drops and the other works' bosses (DanMachi, The Seven Deadly Sins, Tensura, the Tier List) are untouched.

**Who counts.** A boss in the boss registry whose roster character (`ArcCharacter`) has role `BOSS` and source work `Black Clover`: the Dark Triad (Dante, Vanica, Zenon), Lucius Zogratis, the Devil Avatar, Zagred, the Midnight Sun elves (Fana, Rades, Rhya, Patolli), the early-arc bosses (Heath Grice, Lotus Whomalt, Mars, Revchi Salik, Vetto) and the Spade Kingdom bosses (Morris, Lucifero). New Black Clover bosses added to the roster later are picked up by the same rule. Not in it: Goliath and the Charybdis Spawn (DanMachi / Tensura), the God of Order, followers (Blood Beasts, clones, disciples).

**Setting.** `nusmp-boss-rules-server.toml`, `bossRules.blackCloverStatMultiplier` (server, default **3.0**, range 1 to 10, **1 = off** and the old numbers come back on the next time the boss loads).

**How.**
- *Health*: when a boss joins a level it gets one permanent maximum-health modifier, id `nusmp:boss_stat/health`, operation multiplied-total with amount `multiplier - 1` (x3 = +2.0). It is saved with the boss, so a reload keeps the bar; a changed setting replaces it (it is never stacked). The boss keeps its share of the bar (a new boss is full, a wounded one stays as wounded), written through the heal guard and the damage budget the way the boss's own refills are. The other health systems (arena scaling for Lucius, floor scaling for arc bosses) are separate modifiers and multiply with it. The per-second damage budget is a share of maximum health, so it grows with it.
- *Damage*: every hit whose attacker is such a boss is multiplied in the incoming-damage event, so melee, spells and Lucius's percent-of-maximum true damage are all covered. A boosted hit never takes more than half of the victim's maximum health in one blow (a hit that was already bigger is not lowered), so nothing the buff does is a one-shot from full health. Another mod boss is never the victim.

**Cost.** Nothing per tick, no scan, no spawn, no packet. One registry lookup per damage event that has an attacker, a roster lookup only when that attacker is a registered boss, one modifier per boss at join.

**Not changed on purpose.** The barrier/effect levels of the buff engine (`bosses.buffMultiplier`, 0 to 3, still 1) and Lucius's difficulty master. Raise those yourself if tripled health and damage is not enough.

**Tests.** `BossStatMathTest`, `BossStatSourceScanTest`, and the server smoke key `boss.stats` (summons one Black Clover boss and one of another work: the first carries the modifier on a full bar, the second none).

**Not seen in a game.** The balance (3x health and damage on top of numbers tuned for 1x, including Lucius's three acts and the Gates refill, which refill to the new maximum) needs play-testing; the setting is the dial.
