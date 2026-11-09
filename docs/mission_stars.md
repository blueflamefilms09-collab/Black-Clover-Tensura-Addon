# Mission stars (3.67.0)

Gold stars come from missions automatically. A mission is a questline advancement of this mod (`nusmp:` namespace, shown in the advancement tree, not on the exclude list).

| Shown as | Gold stars (default) | Setting |
|---|---|---|
| Task | 1 | `starsTask` |
| Goal | 2 | `starsGoal` |
| Challenge | 5 | `starsChallenge` |

- Config: `nusmp-multiverse-server.toml`, group `[missions]`: `enabled`, the three numbers (0..100), `exclude` (paths such as `root`, folders with a trailing slash such as `magic/`).
- Once per player per mission: the ids are kept in the player's profile (at most 4096), so revoking and earning a mission again pays nothing.
- The stars use `MultiverseProfile.addStars`: social-class bonus, grimoire mastery, squad score, automatic promotion (`starsPerClass`, 10 by default) and the Wizard King count all follow.
- Shipped advancements: 249 tasks, 144 goals, 60 challenges (about 840 stars at the defaults). Senior 1st Class is 140 net stars.
- Not given for advancements finished before the update; an admin can still use `/multiverse stars add`.
- Cost: one advancement event per earned advancement, no per-tick work, no packet; server side only; fake players never earn them.
- Code: `MissionStarRules` (pure), `MissionStars` (event), `MissionStarsSelfTests`, tests `MissionStarRulesTest`.
