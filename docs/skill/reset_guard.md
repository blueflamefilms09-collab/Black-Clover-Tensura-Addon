# Reset Scroll guard (3.42.0)

Owner request: "make it so none of my skill can't be reroll with the basic tensura skill and full reset".

**How it is read:** Tensura Reincarnated's *Reset Scrolls* (the skill reset and the full reset) must no longer remove any skill of this mod. The sentence has a double negative; the reading used is "none of my skills can be rerolled / reset by those two scrolls". If the owner meant the opposite, the server setting below turns the guard off.

## What it does

- Every skill this mod registers lives in the namespace `nusmp` (grimoire books and pages, Shishou's skills, the five Ultimate skills, the Evolution and Unique skills, ...). A removal of one of them that comes out of Tensura's `ResetScrollItem` is **refused**; the player sees one action-bar line ("The skills of this world cannot be reset by a scroll.", at most one per 40 ticks).
- Tensura's own skills and every other mod's skills are reset exactly as before.
- **Not blocked** (they never run through the scroll class): this mod's own removals (a grimoire swap, `/multiverse ... take`, revoking Shishou, the Convergence), and the admin command `/tensura reset`.

## How it works (two layers)

1. **The event.** ManasCore fires a cancellable `SkillEvents.REMOVE_SKILL` for every skill removal. `ResetGuard.onRemove` passes at once unless the owner is a server player and the skill is in namespace `nusmp` and the guard is on; only then does it walk the call stack once (`StackWalker`) looking for a frame of `ResetScrollItem` (or a class nested in it), and cancels the removal with `EventResult.interruptFalse()`. The pure decision is `ResetRules.refuses(guardOn, namespace, fromScroll)` (unit-tested).
2. **Tensura's list (best effort).** After the server starts, the ids of this mod's skills are added to `ResetScrollItem.NO_RESET`, the list Tensura keeps of skills a scroll leaves alone. If that list is immutable the attempt is logged and layer 1 still holds.

## Settings

Server config `world/serverconfig/nusmp-multiverse-server.toml`, section `guard`: `resetScrollGuard` (default `true`). `false` restores the old behaviour (the scrolls reset our skills like any other).

## Worst-case cost (rule 11)

- Work happens only when a skill of this mod is removed, which is rare: one stack walk, no packets other than one action-bar line, no per-tick work, no world scan.
- State: a message throttle of at most 64 player ids, oldest dropped; nothing is stored per player, nothing needs cleaning on logout.
- Server-side only; no client classes are touched (`ServerSafetyTest`, `ResetRulesTest`).

## Tests

- `ResetRulesTest`: the namespace, scroll-class and nested-class rules, the stack rule, the decision table, and source scans (hook on `REMOVE_SKILL`, cancels through the event, server player only, no client classes, registered in `NUSMP`, config key, self-test registered and in the smoke list).
- CI self-test `skill.reset_guard`: a stand-in player is given a skill of this mod (Hercules); this mod's own removal must still work; then Tensura's skill reset, skill reset (all) and full reset are run on the stand-in, and the skill must survive each. A Tensura skill is learned as a control and its fate is reported in the result. A Tensura call that throws on a stand-in (no connection, no race data) is told in the result, not counted as a failure.

## What is not verified

- **Nothing was seen in a game.** It rests on two assumptions read from the Tensura and ManasCore stubs: the scrolls remove skills through `Skills.forgetSkill`, which fires `REMOVE_SKILL`; and `NO_RESET` means "skills a scroll does not reset". If Tensura removes skills some other way (writing the storage directly), the event never fires and a scroll could still reset them: the CI self-test is meant to show that, and the result line says whether the Tensura reset calls ran at all.
- If the Tensura calls cannot run on a stand-in, the self-test passes with a NOTE that survival was not exercised.
- A scroll that resets the *race* (not a skill reset) is out of scope: it only removes race-bound skills through Tensura's race rules; if any of those are ours the same event guard applies.
