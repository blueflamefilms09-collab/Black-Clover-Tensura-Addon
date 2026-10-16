package com.newuniverse.nusmp.thread;

import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Why: Rouge is one cat per caster, called by a page, replaced on a recast and paid for in the caster's mana, and the damage events
 * need a quick answer to "is a Rouge up for anyone?". This class is the single place that knows who has a Rouge, how a new one is
 * summoned (the old one unravels first), how the hand-picked ally list is read and edited in the grimoire's skill tag, and how mana
 * is read and spent. Everything is server side and static; entries are removed when the cat is removed, so nothing leaks.
 */
public final class RougeManager {
    private RougeManager() {}

    /** caster id -> the cat that guards them. */
    private static final Map<UUID, RougeEntity> ACTIVE = new HashMap<>();

    /** The skill-tag key of the hand-picked allies (ids separated by commas). */
    public static final String ALLIES_TAG = "RougeAllies";

    public static boolean anyActive() {
        if (ACTIVE.isEmpty()) return false;
        ACTIVE.values().removeIf(RougeEntity::isRemoved);
        return !ACTIVE.isEmpty();
    }

    /** Snapshot of the live cats (the events walk this; a copy so a cat that vanishes mid-walk is harmless). */
    public static List<RougeEntity> cats() {
        List<RougeEntity> out = new ArrayList<>();
        for (Iterator<RougeEntity> it = ACTIVE.values().iterator(); it.hasNext(); ) {
            RougeEntity r = it.next();
            if (r.isRemoved()) it.remove(); else out.add(r);
        }
        return out;
    }

    public static RougeEntity of(UUID caster) {
        RougeEntity r = ACTIVE.get(caster);
        return r == null || r.isRemoved() ? null : r;
    }

    /** Called by the cat when it is removed. */
    static void forget(UUID caster, RougeEntity cat) { ACTIVE.remove(caster, cat); }

    /** Calls Rouge for this caster; any earlier cat of theirs unravels. */
    public static RougeEntity summon(ServerPlayer p) {
        RougeEntity old = ACTIVE.remove(p.getUUID());
        if (old != null && !old.isRemoved()) old.beginUnravel(null);
        ServerLevel sl = p.serverLevel();
        RougeEntity cat = ThreadEntities.ROUGE.get().create(sl);
        if (cat == null) return null;
        Vec3 at = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(0.9));
        Vec3 spot = SafeSpot.ok(sl, cat, at) ? at : p.position();
        cat.moveTo(spot.x, spot.y, spot.z, p.getYRot(), 0f);
        cat.setOwner(p);
        cat.setLifetime(sl.getGameTime() + ThreadConfig.rougeTicks());
        cat.refreshCaches(p);
        threadBook(p).ifPresent(i -> {                                   // 1.46.0: a recast keeps the name the caster gave her
            String name = RougePerch.cleanName(i.getOrCreateTag().getString(RougePerch.NAME_TAG));
            if (name != null) cat.applyName(name);
        });
        sl.addFreshEntity(cat);
        ACTIVE.put(p.getUUID(), cat);
        return cat;
    }

    /** Dismisses the caster's cat at once (it unravels). */
    public static void dismiss(UUID caster) {
        RougeEntity r = ACTIVE.remove(caster);
        if (r != null && !r.isRemoved()) r.beginUnravel(null);
    }

    // ================================================================ mana
    public static double mana(LivingEntity e) {
        var ex = TensuraStorages.getExistenceFrom(e);
        return ex == null ? 0.0 : ex.getMagicule();
    }

    public static double maxMana(LivingEntity e) { return EnergyHelper.getMaxMagicule(e); }

    /** Takes 'cost' mana if the whole of it is there. Returns false (and takes nothing) otherwise. */
    public static boolean pay(LivingEntity e, double cost) {
        var ex = TensuraStorages.getExistenceFrom(e);
        if (ex == null || !FateRules.canPay(ex.getMagicule(), cost)) return false;
        ex.setMagicule(ex.getMagicule() - cost);
        ex.markDirty();
        return true;
    }

    // ================================================================ the hand-picked allies (saved in the thread grimoire's skill tag)
    public static Optional<ManasSkillInstance> threadBook(ServerPlayer p) {
        return SkillAPI.getSkillsFrom(p).getSkill(NUSkills.BOOK_THREAD.getId());
    }

    public static List<UUID> allies(ServerPlayer p) {
        return threadBook(p).map(i -> ProtectedSet.clamp(ProtectedSet.parse(i.getOrCreateTag().getString(ALLIES_TAG)), ThreadConfig.rougeMaxAllies())).orElseGet(ArrayList::new);
    }

    /** May this one be named? Players and anyone's pets (an owner is on record). Never a hostile mob. */
    public static boolean nameable(ServerPlayer caster, LivingEntity target) {
        if (target == caster) return false;
        return target instanceof net.minecraft.world.entity.player.Player || target instanceof OwnableEntity o && o.getOwnerUUID() != null;
    }

    /** Sneak + cast on a player or pet: adds or removes them from the list. Tells the caster. Returns true if the list changed. */
    public static boolean toggleAlly(ServerPlayer p, ManasSkillInstance inst, LivingEntity target) {
        if (!nameable(p, target)) {
            p.displayClientMessage(Component.literal("Rouge only watches over people and pets.").withStyle(ChatFormatting.RED), true);
            return false;
        }
        List<UUID> list = new ArrayList<>(allies(p));
        ProtectedSet.Toggle result = ProtectedSet.toggle(list, target.getUUID(), ThreadConfig.rougeMaxAllies());
        String name = target.getDisplayName().getString();
        switch (result) {
            case FULL -> { p.displayClientMessage(Component.literal("Rouge cannot watch over more than " + ThreadConfig.rougeMaxAllies() + " chosen allies.").withStyle(ChatFormatting.RED), true); return false; }
            case ADDED -> p.displayClientMessage(Component.literal("Rouge will now watch over " + name + ".").withStyle(ChatFormatting.RED), true);
            case REMOVED -> p.displayClientMessage(Component.literal("Rouge will no longer watch over " + name + ".").withStyle(ChatFormatting.GRAY), true);
        }
        inst.getOrCreateTag().putString(ALLIES_TAG, ProtectedSet.format(list));
        inst.markDirty();
        RougeEntity cat = of(p.getUUID());
        if (cat != null) cat.refreshCaches(p);
        return true;
    }
}
