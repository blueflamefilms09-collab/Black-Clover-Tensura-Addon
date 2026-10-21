package com.newuniverse.nusmp.labyrinth.mega;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

/**
 * Why: {@code /multiverse mega ...} is ONE command tree that many builders contribute to, and each of them wrote only its own branch
 * ({@code dm}, {@code faction}, {@code rest}, {@code gear}, {@code flow}, {@code system}, {@code floors}, {@code core}, {@code selftest}, the
 * generator's own words ...). The multiverse command class stays untouched except for one line that adds this node; attaching a branch is one line
 * here, in dependency order, so a missing registry shows up as a compile error in ONE place and the tree is easy to read. All branches need
 * permission 2 (every one of them checks it itself; the root does too).
 */
public final class MegaCommands {
    private MegaCommands() {}

    /** The finished {@code mega} node: the generator's own commands (status, sector, tp, expand, regen, audit, plan, unseal ...) plus every builder's branch. */
    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        LiteralArgumentBuilder<CommandSourceStack> mega = com.newuniverse.nusmp.labyrinth.mega.gen.MegaCommand.build();
        mega.then(com.newuniverse.nusmp.labyrinth.mega.enc.EncCommand.command());            // dm: the Dungeon Master (status, roll, spawn, wanderer, clear)
        mega.then(com.newuniverse.nusmp.labyrinth.mega.enc.EncCommand.encAlias());            // enc: the same tree under a second name (/multiverse mega enc why, /multiverse mega enc force)
        mega.then(com.newuniverse.nusmp.labyrinth.mega.eco.EcoRegistry.command());           // faction: factions, turf wars, the economy
        mega.then(com.newuniverse.nusmp.labyrinth.mega.safe.SafeRegistry.command());         // rest: short and long rests, anchors
        mega.then(com.newuniverse.nusmp.labyrinth.mega.guard.GuardCommand.command());        // core: the Mana Core and the Guardian
        mega.then(com.newuniverse.nusmp.labyrinth.mega.floors.FloorsRegistry.command());     // floors: floor numbers and the Directional Lock
        mega.then(com.newuniverse.nusmp.labyrinth.mega.system.SystemRegistry.command());     // system: the Solo Leveling System
        mega.then(com.newuniverse.nusmp.labyrinth.mega.flow.FlowRegistry.command());         // flow: the Rukh flow
        mega.then(com.newuniverse.nusmp.labyrinth.mega.gear.GearRegistry.command());         // gear: standard Normal / Special / Rare gear
        mega.then(com.newuniverse.nusmp.labyrinth.mega.party.PartyRegistry.command());       // party: list, of, tp, set (admin)
        mega.then(com.newuniverse.nusmp.labyrinth.mega.rift.RiftRegistry.command());         // rift: dimensional rifts, pocket realms, the waypoint (open, list, pocket, waypoint)
        mega.then(com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryRegistry.command());     // foundry: the 400 custom dungeon mobs (list, spawn, count, verify)
        mega.then(com.newuniverse.nusmp.labyrinth.mega.hub.HubRegistry.command());           // hub: Floor 0, the Sanctuary Hub (status, tp, plaza, gates)
        mega.then(com.newuniverse.nusmp.labyrinth.mega.crucible.CrucibleCommand.command());   // crucible: status, roll, reset, catastrophe
        mega.then(com.newuniverse.nusmp.labyrinth.mega.macro.MacroCommand.command());         // macro: info, sample (what the Hub generator would place)
        mega.then(com.newuniverse.nusmp.labyrinth.mega.mechanics.MechanicsRegistry.command());   // mechanic: the dungeon mechanics library (list, info, apply, clear, count, verify)
        mega.then(com.newuniverse.nusmp.labyrinth.mega.lock.LockRegistry.command());          // lock: the escape rules (list, release, unstuck, cases, reach)
        mega.then(com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleRegistry.command());         // puzzle: the puzzle rooms (list, info, apply, status, plan, clear, reset, verify)
        mega.then(com.newuniverse.nusmp.labyrinth.mega.lockdown.LockdownRegistry.command());   // lockdown: room lockdown and the ambush (status, rules, release, forget, ambush)
        mega.then(SelfTestCommand.command());                                               // selftest: the in-game self-tests the CI smoke test reads
        return mega;
    }
}
