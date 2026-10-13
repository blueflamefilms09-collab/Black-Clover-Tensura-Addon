package com.newuniverse.nusmp.dungeon;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Skills lent by cooked monster meals (docs/living_dungeon.md section 4). {@link #grant} teaches a Tensura skill through ManasCore's
 * skill list and takes it away again when the time is up, unless the player owned it before. Every lent skill is written down first, in
 * the player's persistent data ({@link SkillsData}, copied on death), and checked once a second and at login, so logging out or dying
 * cannot make it permanent. A skill id the registry does not know does nothing; the pure rules are {@link SkillLease}.
 */
public final class MealSkills {
    private MealSkills() {}

    /** What a grant came to: lent now, lent longer, already theirs for good, or not possible. */
    public enum Outcome { GRANTED, EXTENDED, OWNED, UNAVAILABLE }

    private static final String LEASES = "Leases";

    /** Is there a skill with this id in the skill registry? */
    public static boolean available(String skillId) {
        ResourceLocation id = ResourceLocation.tryParse(skillId);
        if (id == null) return false;
        try {
            return SkillAPI.getSkillRegistry().get(id) != null;
        } catch (Throwable noSkills) { /* the skill registry is not there */ }
        return false;
    }

    /** Lends a skill for {@code ticks}. A skill the player already owns is left alone; one already lent has its time extended, never cut. */
    public static Outcome grant(Player p, String skillId, int ticks) { return grant(p, skillId, ticks, null); }

    /**
     * The same, but the chat lines name {@code lender} instead of "The meal" ("Your relic lends you X." / "Your relic's gift fades: X."): for the
     * relics, whose lent skills are not food. A null lender is the meal wording of {@link #grant(Player, String, int)}; nothing else differs.
     */
    public static Outcome grant(Player p, String skillId, int ticks, String lender) {
        ResourceLocation id = ResourceLocation.tryParse(skillId);
        if (id == null) return Outcome.UNAVAILABLE;
        try {
            ManasSkill skill = SkillAPI.getSkillRegistry().get(id);
            Skills skills = SkillAPI.getSkillsFrom(p);
            if (skill == null || skills == null) return Outcome.UNAVAILABLE;
            CompoundTag leases = SkillsData.part(p, LEASES);
            CompoundTag old = leases.contains(skillId, Tag.TAG_COMPOUND) ? leases.getCompound(skillId) : null;
            boolean have = skills.getSkill(id).isPresent();
            SkillLease.Plan plan = SkillLease.grant(have, old != null, old == null ? 0L : old.getLong("Until"), p.level().getGameTime(), ticks);
            if (plan.action() == SkillLease.Action.NONE) return have ? Outcome.OWNED : Outcome.UNAVAILABLE;
            CompoundTag lease = new CompoundTag();
            lease.putLong("Until", plan.until());
            if (lender != null && !lender.isBlank()) lease.putString("By", lender);          // the fade line names the same giver
            leases.put(skillId, lease);                                  // written before the skill is: whatever happens next, it cannot stay for good
            if (plan.action() == SkillLease.Action.EXTEND) return Outcome.EXTENDED;
            boolean learned;
            try {
                learned = skills.learnSkill(skill.createDefaultInstance(), Component.literal(lender == null || lender.isBlank() ? "The meal lends you " : lender + " lends you ").append(skill.getName()).append(".").withStyle(ChatFormatting.AQUA));
            } catch (RuntimeException refused) {
                learned = skills.getSkill(id).isPresent();
            }
            if (!learned) {
                leases.remove(skillId);
                return Outcome.UNAVAILABLE;
            }
            return Outcome.GRANTED;
        } catch (Throwable noSkills) { /* Tensura's skill list is not there: nothing is lent */ }
        return Outcome.UNAVAILABLE;
    }

    /** Takes back every lent skill whose time is up. */
    static void sweep(ServerPlayer p) {
        CompoundTag leases = SkillsData.peek(p, LEASES);
        if (leases == null || leases.isEmpty()) return;
        Map<String, Long> until = new LinkedHashMap<>();
        for (String id : leases.getAllKeys()) until.put(id, leases.getCompound(id).getLong("Until"));
        for (String id : SkillLease.expired(until, p.level().getGameTime())) {
            String by = leases.getCompound(id).getString("By");
            if (!forget(p, id)) continue;                                // the skill list would not let go: the lease stays and it is tried again
            leases.remove(id);
            p.displayClientMessage(Component.literal((by.isBlank() ? "The meal's" : by + "'s") + " gift fades: " + label(id) + ".").withStyle(ChatFormatting.GRAY), true);
        }
    }

    private static boolean forget(Player p, String skillId) {
        ResourceLocation id = ResourceLocation.tryParse(skillId);
        if (id == null) return true;
        try {
            Skills skills = SkillAPI.getSkillsFrom(p);
            if (skills == null || skills.getSkill(id).isEmpty()) return true;
            skills.forgetSkill(id);
            return skills.getSkill(id).isEmpty();                        // an event that vetoed the forgetting leaves the lease in place, to be tried again
        } catch (Throwable noSkills) { /* try again next time */ }
        return false;
    }

    private static String label(String skillId) {
        for (MealTable.Perk perk : MealTable.ALL) if (perk.skill().equals(skillId)) return perk.skillName();
        return skillId.substring(skillId.indexOf(':') + 1).replace('_', ' ');
    }

    // ---------------------------------------------------------------- game bus
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p && p.tickCount % 20 == 9) sweep(p);
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) sweep(p);
    }

    /** The lease book goes with the player to their new body. */
    public static void onClone(PlayerEvent.Clone e) { SkillsData.copy(e.getOriginal(), e.getEntity()); }
}
