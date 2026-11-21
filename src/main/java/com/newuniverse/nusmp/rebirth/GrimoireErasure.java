package com.newuniverse.nusmp.rebirth;

import com.newuniverse.nusmp.blackclover.GrimoireAcceptance;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.GrimoireSlot;
import com.newuniverse.nusmp.mana.Signature;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: erasing a grimoire has two sizes. {@link #skills} is what the admin command {@code /anime_multiverse grimoire reset} always did (forget every grimoire skill, which takes its pages and fusion
 * with it, and let the player roll again); the command now calls it, unchanged in effect. {@link #everything} is the Sigil of Rebirth: that, plus the player's own grimoire items (inventory and Grimoire Slot)
 * and the Signature. The ender chest, other players' grimoires and every non-grimoire skill are never touched. One-off server work, bounded by the inventory size.
 */
public final class GrimoireErasure {
    private GrimoireErasure() {}

    /** The command's reset: forget every grimoire skill (pages and fusion live in the skill's data) and mark the player as not yet rolled. */
    public static void skills(ServerPlayer p) {
        for (var id : NUSkills.allGrimoireSkillIds()) SkillAPI.getSkillsFrom(p).forgetSkill(id);
        GrimoireAcceptance.markRolled(p, false);
    }

    /** The grimoire skills the player has now (one per grimoire). */
    public static List<ManasSkillInstance> owned(ServerPlayer p) {
        List<ManasSkillInstance> out = new ArrayList<>();
        var skills = SkillAPI.getSkillsFrom(p);
        for (var id : NUSkills.allGrimoireSkillIds()) skills.getSkill(id).ifPresent(out::add);
        return out;
    }

    /** The highest leaf tier among the player's grimoires (0 = none). */
    public static int bestTier(ServerPlayer p) {
        int best = 0;
        for (ManasSkillInstance i : owned(p)) best = Math.max(best, GrimoirePages.coverOf(i).tier);
        return best;
    }

    /** The Sigil of Rebirth's erasure. Returns how many grimoire items were taken. */
    public static int everything(ServerPlayer p) {
        skills(p);
        int taken = 0;
        Inventory inv = p.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++)
            if (GrimoireItem.isOwnedBy(inv.getItem(slot), p.getUUID())) { inv.setItem(slot, ItemStack.EMPTY); taken++; }
        if (GrimoireItem.isOwnedBy(GrimoireSlot.get(p), p.getUUID())) { GrimoireSlot.set(p, ItemStack.EMPTY); taken++; }
        Signature.clear(p);
        return taken;
    }
}
