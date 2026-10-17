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
        mega.then(com.newuniverse.nusmp.labyrinth.mega.eco.EcoRegistry.command());           // faction: factions, turf wars, the economy
        mega.then(com.newuniverse.nusmp.labyrinth.mega.safe.SafeRegistry.command());         // rest: short and long rests, anchors
        mega.then(com.newuniverse.nusmp.labyrinth.mega.guard.GuardCommand.command());        // core: the Mana Core and the Guardian
        mega.then(com.newuniverse.nusmp.labyrinth.mega.floors.FloorsRegistry.command());     // floors: floor numbers and the Directional Lock
        mega.then(com.newuniverse.nusmp.labyrinth.mega.system.SystemRegistry.command());     // system: the Solo Leveling System
        mega.then(com.newuniverse.nusmp.labyrinth.mega.flow.FlowRegistry.command());         // flow: the Rukh flow
        mega.then(com.newuniverse.nusmp.labyrinth.mega.gear.GearRegistry.command());         // gear: standard Normal / Special / Rare gear
        mega.then(SelfTestCommand.command());                                               // selftest: the in-game self-tests the CI smoke test reads
        return mega;
    }
}
