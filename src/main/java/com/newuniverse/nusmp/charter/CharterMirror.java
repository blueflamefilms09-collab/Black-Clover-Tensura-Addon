package com.newuniverse.nusmp.charter;

import com.newuniverse.nusmp.dungeon.MealSkills;
import com.newuniverse.nusmp.item.CuriosLink;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Skill mirroring: while a Charter Grimoire is in a hand or worn in a Curios slot, the skills of its boss are lent to the holder for sixty ticks,
 * renewed once a second (staggered by entity id). The lending is {@link MealSkills#grant} with the lender "Charter Grimoire": a skill the
 * player owns is left alone, a lent one is only ever extended, and the lease is written down before the skill is, so it ends by itself a few
 * seconds after the book is put away, survives relog and death without becoming permanent, and never replaces, resets or removes anything of the
 * player's own ({@link MirrorPolicy} cannot even express that).
 */
public final class CharterMirror {
    private CharterMirror() {}

    /** The skills each online player currently holds on loan from a grimoire (so a lent skill is not mistaken for an owned one). */
    private static final Map<UUID, Set<String>> LENT = new HashMap<>();

    /** Once a second per player (the tick is offset by the entity id so the whole server does not pass in the same tick). */
    public static void pass(ServerPlayer p) {
        if ((p.tickCount + p.getId()) % 20 != 0) return;
        if (!p.isAlive() || p.isSpectator() || !CharterConfig.mirror()) return;
        List<CharterTable.Boss> held = held(p);
        if (held.isEmpty()) {
            if (!LENT.isEmpty()) LENT.remove(p.getUUID());                      // put away: the leases run out by themselves
            return;
        }
        Set<String> lent = LENT.get(p.getUUID());
        Set<String> owned = new HashSet<>();
        if (lent == null) {                                                         // the first pass since login: the lender knows what is owned and what is on a lease
            lent = new LinkedHashSet<>();
            LENT.put(p.getUUID(), lent);
        } else {
            owned.addAll(learned(p));                                               // what the player has, minus what a grimoire lent them
            owned.removeAll(lent);
        }
        for (MirrorPolicy.Lease lease : MirrorPolicy.plan(owned, MirrorPolicy.union(held))) {
            MealSkills.Outcome o = MealSkills.grant(p, lease.skillId(), lease.ticks(), "Charter Grimoire");
            if (o == MealSkills.Outcome.GRANTED || o == MealSkills.Outcome.EXTENDED) lent.add(lease.skillId());
            else lent.remove(lease.skillId());
        }
    }

    /** The grimoires in either hand or in a Curios slot, each boss once. */
    static List<CharterTable.Boss> held(ServerPlayer p) {
        List<CharterTable.Boss> out = new ArrayList<>(2);
        add(out, p.getMainHandItem());
        add(out, p.getOffhandItem());
        if (CuriosLink.available()) for (ItemStack s : CuriosLink.equipped(p)) add(out, s);
        return out;
    }

    private static void add(List<CharterTable.Boss> out, ItemStack s) {
        if (s.isEmpty() || !(s.getItem() instanceof CharterGrimoireItem g)) return;
        if (!out.contains(g.boss())) out.add(g.boss());
    }

    /** The ids of the skills on the player now (lent ones included). */
    private static Set<String> learned(ServerPlayer p) {
        Set<String> out = new HashSet<>();
        try {
            Skills skills = SkillAPI.getSkillsFrom(p);
            if (skills == null) return out;
            for (ManasSkillInstance i : new ArrayList<>(skills.getLearnedSkills())) {
                ResourceLocation id = i.getSkill().getRegistryName();
                if (id != null) out.add(id.toString());
            }
        } catch (Throwable noSkills) { /* Tensura's skill list is not there: nothing is owned, nothing is lent either */ }
        return out;
    }

    public static void forget(UUID player) { LENT.remove(player); }

    public static void clear() { LENT.clear(); }
}
