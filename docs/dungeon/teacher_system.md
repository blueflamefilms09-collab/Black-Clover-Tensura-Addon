# Teacher (Mentor) System, Mega-Dungeon only (DESIGN, not built yet)

Inspired by the Log Horizon Teacher System. Status: owner answers recorded, no code written. Everything below is a proposal to confirm before building.

## Owner decisions
1. **Scale down to the apprentice:** inside the dungeon the mentor's effective power is scaled down to the apprentice's (not a fixed per-rank ceiling).
2. **Automatic offer:** when a strong and a weak player party up in the dungeon, the dungeon offers the mentoring link by itself (both players get a prompt and must accept; nobody is forced).
3. **Apprentice bonus:** left to me. Proposal: +25% skill/mastery progress and +25% dungeon experience for the apprentice while a mentor is present; the mentor gets a small reward per apprentice floor cleared (gold + a progress counter toward a "Mentor" title).
4. **Boss fights:** applies everywhere, normal rooms and boss fights ("try all"). Boss group scaling (docs/bosses/RULES.md rule 11) counts players as before; the mentor's reduced power is part of the fight, so the boss is not made stronger for it.

## Where it is active
Only in the Mega-Dungeon dimension and only while mentor and apprentice are in the same party. It switches off on: leaving the dimension, logout, death, leaving the party, the link being cancelled, or the server stopping. A login/respawn check clears any leftover modifier (no state survives a crash).

## What "scale down" means
- A **power score** is computed per player on the server (proposal: from Tensura Existence Points / ep plus max health, attack damage and armour; the exact formula is OPEN).
- The mentor is lowered toward the apprentice's score by temporary, uniquely-named attribute modifiers (max health, attack damage, armour, movement speed), never below a floor (proposal: not below 50% of own values) so the mentor is not helpless.
- The mentor's strongest skills (ultimates, instant-kills, skills on the arena ban list) are switched off while mentoring, the same list the boss arena rules use. Tensura power is skills + EP, which cannot be levelled down cleanly, so the skill gate is the practical part. OPEN: whether Tensura's own stat system can be capped directly.
- The apprentice is never changed.

## Aggro
Dungeon mobs weigh targets toward the apprentice while a mentor is active (target-selection bias in the dungeon mob AI), so a high-power mentor does not pull every monster. Bounded: one comparison when a mob picks a target, no new scans.

## Who is strong and who is weak
The offer fires when two party members' power scores differ by at least a ratio (proposal: 2x, a server config). The stronger becomes the mentor, the weaker the apprentice. A mentor can have a few apprentices (proposal: up to 3, the lowest-power one sets the scale). Registration is also possible by command for friends who want to do it on purpose.

## Server and perf (rules 9 and 11)
- All state is server-side, keyed by player UUID; client only shows a small status icon and the prompt.
- Work happens only on: party change, dimension change, login/respawn/death, accept/cancel. No per-tick scanning.
- Network: one small status packet to the two players involved, plus the prompt.
- Worst case: 1 attribute-modifier set per mentor (4 modifiers) and one stored link per player.
- Needs a self-test: link, scale, clear on logout/dimension change, and "no modifier survives".

## Decisions (owner: "do what you think is best")
- Power score = 0.4 x normalised Tensura EP + 0.2 x max health + 0.2 x attack damage + 0.2 x armour (each against a fixed reference table, server config). One pure function, unit tested.
- Mentor floor: never below 50% of own values.
- Offer fires at a power gap of 2x or more (server config); up to 3 apprentices per mentor, the weakest sets the scale.
- Switched off for the mentor while mentoring: the same arena ban list as the boss rules (teleport-out, instant-kill, time-stop) plus ultimate-class skills.
- Bonus numbers: +25% skill progress and +25% dungeon experience for the apprentice; mentor reward 1 Mentor Mark per apprentice floor cleared, a title at 100 marks. Both are server configs.
- Mentoring never changes the boss 1-of-1 skill roll: it stays damage-weighted among the players who dealt damage.
- Still unknown until built and tried in a game: whether Tensura's own stats can be capped directly (if yes, use that instead of attribute modifiers).
