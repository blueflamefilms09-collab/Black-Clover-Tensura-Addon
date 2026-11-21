package com.newuniverse.nusmp.shishou;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.entity.TensuraCaster;
import com.newuniverse.nusmp.perf.PerfParticles;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

import java.util.List;
import java.util.Locale;

/**
 * Why: the SOFT compatibility glue that lets Shishou use skills of other mods with the SWORD's mana ({@link ShishouBorrowTable}: base Tensura Reincarnated, TR Nightmares, Tensura Mysticism, and echoes of this
 * mod's grimoire magics). Base Tensura and ManasCore are hard dependencies of the mod, so their API is linked; TR Nightmares and Tensura Mysticism are NEVER linked: an entry is only an id string, looked up in the
 * skill registry at the moment of the cast and only when {@code ModList.isLoaded(namespace)}; a skill that is not there is simply unavailable and nothing is logged per tick.
 *
 * <p>Who casts: the thrown body (a pet; held in the hand he lends nothing, his own skills are the hand's). A registered skill is taught to the body and driven like a player's key by
 * {@link TensuraCaster#cast} (press, hold, release; a skill that throws for a mob is dropped for good); an echo is the grimoire magic's own particle and hit effect fired by the sword (milder than the grimoire
 * page, never a lightning entity, never a block). The pool that pays is the sword's own ({@code ShishouRecord.spend}); the body also gets a magicule pool of its own so the other mod's skill is allowed to run.</p>
 *
 * <p>Worst case: one cast per body per {@code compat.borrowGapTicks} (default 40, at least 10) and the once-a-second upkeep that asks; a cast looks through at most {@link ShishouBorrowTable#MAX} entries
 * (cheap field checks first, one registry read only for the entry it tries), at most one box query (echo: {@code SPELL_TARGETS} creatures) and at most {@link #FX_MAX} particles. No packet of its own.</p>
 */
public final class ShishouBorrow {
    private ShishouBorrow() {}

    /** The farthest target a borrowed cast is aimed at, the owner's safety distance (area skills of other mods may not know the wielder is a friend), and the particle cap of one cast. */
    public static final double RANGE = 24.0, OWNER_SAFE = 5.0;
    public static final int FX_MAX = 20, POOL = 2000;

    // ---------------------------------------------------------------- availability
    /** The magic an echo id names, or null. */
    static MagicType magicOf(String echoId) {
        if (echoId == null || !echoId.startsWith("echo:")) return null;
        try {
            return MagicType.valueOf(echoId.substring(5).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException none) {
            return null;
        }
    }

    /** Can this entry be cast on this server right now: its source is on, and its skill (or magic) exists? One registry read. */
    public static boolean usableNow(ShishouBorrowTable.Entry en) {
        if (en == null || !ShishouConfig.compatSource(en.source())) return false;
        if (en.kind() == ShishouBorrowTable.Kind.ECHO) return magicOf(en.id()) != null;
        return skillOf(en) != null;
    }

    private static ManasSkill skillOf(ShishouBorrowTable.Entry en) {
        ResourceLocation id = ResourceLocation.tryParse(en.id());
        if (id == null) return null;
        if (!ShishouBorrowTable.TENSURA.equals(en.source()) && !ModList.get().isLoaded(id.getNamespace())) return null;          // an optional mod: only when it is installed
        try {
            return SkillAPI.getSkillRegistry().get(id);
        } catch (RuntimeException none) {
            return null;
        }
    }

    // ---------------------------------------------------------------- Skill Eater
    /** A creature died to the sword: may Skill Eater take a borrowed skill from it? Returns the entry when it is new, else null. */
    public static ShishouBorrowTable.Entry steal(ShishouRecord rec, LivingEntity victim) {
        if (!ShishouConfig.compat()) return null;
        String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString();
        ShishouBorrowTable.Entry e = ShishouBorrowTable.stealMatch(ShishouBorrowTable.entries(), typeId, rec.level(), rec.borrowed());
        if (e == null || !ShishouConfig.compatSource(e.source())) return null;
        if (!ShishouBorrowTable.steals(e, ShishouConfig.stealScale(), victim.getRandom().nextDouble()) || !usableNow(e)) return null;
        return rec.learnBorrowed(e) ? e : null;
    }

    // ---------------------------------------------------------------- casting
    /** The thrown body fires one borrowed skill at its target when it has the mana, the entry is ready and the gap has passed. At most one a gap. */
    public static void autoCast(ShishouEntity e, ServerPlayer owner, ShishouRecord rec, ServerLevel sl, long now, Vec3 look) {
        LivingEntity t = e.getTarget();
        if (t == null || !t.isAlive() || !ShishouConfig.compat() || !e.borrowGate(now, ShishouConfig.borrowGap())) return;
        List<ShishouBorrowTable.Entry> open = ShishouBorrowTable.availableList(ShishouBorrowTable.entries(), rec.level(), rec.borrowed());
        int n = open.size();
        if (n == 0) return;
        int start = (int) ((now / 20L + e.getId()) % n);                                      // a rotating start, so every open entry gets its turn
        for (int k = 0; k < n; k++) if (tryCast(e, owner, rec, sl, now, open.get((start + k) % n), t, look)) return;
    }

    /** The wielder asks for the selected borrowed skill (the Borrowed mode): only while the sword is thrown. Returns true when something was cast. */
    public static boolean castSelected(ServerPlayer p) {
        ShishouData d = ShishouData.get();
        ShishouCap cap = d.cap();
        ShishouRecord rec = d.record();
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        if (!ShishouConfig.compat() || !cap.isOwner(p.getUUID())) return false;
        ShishouEntity body = cap.form() == ShishouCap.Form.MINION && p.getServer() != null ? bodyOf(p, cap.entity()) : null;
        if (body == null) {
            say(p, "shishou.msg.borrow_throw", "Throw me first: I only lend my borrowed skills as a thrown sword.");
            return false;
        }
        ShishouBorrowTable.Entry en = ShishouBorrowTable.current(ShishouBorrowTable.entries(), rec.borrowedSel(), rec.level(), rec.borrowed());
        if (en == null || !usableNow(en)) {
            say(p, "shishou.msg.borrow_none", "I have no borrowed skill to lend.");
            return false;
        }
        LivingEntity t = body.getTarget();
        if (t == null || !t.isAlive()) {
            List<Mob> near = ShishouTargets.hostilesNear(sl, p, p.position(), RANGE, 1);
            t = near.isEmpty() ? null : near.get(0);
        }
        if (t == null) {
            say(p, "shishou.msg.nothing", "Nothing answers.");
            return false;
        }
        Vec3 look = t.getEyePosition().subtract(body.position().add(0, 0.3, 0));
        boolean ok = tryCast(body, p, rec, sl, now, en, t, look);
        if (!ok) say(p, "shishou.msg.borrow_wait", "That skill is not ready, or my mana is too low.");
        return ok;
    }

    /** The next borrowed skill that can be cast now (the Borrowed mode with sneak). */
    public static void cycle(ServerPlayer p) {
        ShishouData d = ShishouData.get();
        ShishouRecord rec = d.record();
        if (!ShishouConfig.compat() || !d.cap().isOwner(p.getUUID())) return;
        int cur = rec.borrowedSel();
        ShishouBorrowTable.Entry pick = null;
        for (int i = 0; i < ShishouBorrowTable.MAX && pick == null; i++) {
            ShishouBorrowTable.Entry next = ShishouBorrowTable.cycle(ShishouBorrowTable.entries(), cur, rec.level(), rec.borrowed());
            if (next == null) break;
            cur = next.idx();
            if (usableNow(next)) pick = next;
            else if (next.idx() == rec.borrowedSel()) break;
        }
        if (pick == null) {
            say(p, "shishou.msg.borrow_none", "I have no borrowed skill to lend.");
            return;
        }
        rec.selectBorrowed(pick.idx());
        d.touch();
        p.displayClientMessage(Component.translatableWithFallback("shishou.msg.borrow_pick", "Borrowed skill: %s (%s mana).", pick.label(), pick.mana()).withStyle(ChatFormatting.AQUA), true);
    }

    private static void say(ServerPlayer p, String key, String fallback) { p.displayClientMessage(Component.translatableWithFallback(key, fallback).withStyle(ChatFormatting.GRAY), true); }

    private static ShishouEntity bodyOf(ServerPlayer p, java.util.UUID id) {
        for (ServerLevel l : p.getServer().getAllLevels()) if (l.getEntity(id) instanceof ShishouEntity se) return se;
        return null;
    }

    /** Casts one entry now if it is ready and paid; false when nothing was cast (nothing is paid then). */
    static boolean tryCast(ShishouEntity e, ServerPlayer owner, ShishouRecord rec, ServerLevel sl, long now, ShishouBorrowTable.Entry en, LivingEntity t, Vec3 look) {
        if (!ShishouConfig.compatSource(en.source()) || !e.borrowReady(en.idx(), now) || rec.mana(now) < en.mana()) return false;
        if (e.distanceToSqr(t) > RANGE * RANGE) return false;
        boolean ok;
        if (en.kind() == ShishouBorrowTable.Kind.ECHO) ok = echo(e, owner, rec, sl, now, en, t, look);
        else {
            if (owner != null && owner.distanceToSqr(t) < OWNER_SAFE * OWNER_SAFE) return false;                // another mod's area skill may not spare the wielder: not close to him
            ok = skill(e, en, t);
        }
        if (!ok) return false;
        rec.spend(now, en.mana());
        e.borrowStart(en.idx(), now, en.cooldown());
        ShishouData.get().touch();
        return true;
    }

    /** A registered skill of another mod, driven like a player's key by the body. */
    private static boolean skill(ShishouEntity e, ShishouBorrowTable.Entry en, LivingEntity t) {
        ManasSkill skill = skillOf(en);
        if (skill == null) return false;
        ResourceLocation id = ResourceLocation.tryParse(en.id());
        try {
            var skills = SkillAPI.getSkillsFrom(e);
            if (skills.getSkill(id).isEmpty()) {
                skills.learnSkill(skill.createDefaultInstance());
                skills.markDirty();
            }
            TensuraCaster.ensureMana(e, POOL);
            return TensuraCaster.cast(e, t, List.of(id), en.hold()) != null;
        } catch (RuntimeException | LinkageError broken) {
            return false;                                                                       // never a crash of the tick: the next entry is tried next time
        }
    }

    /** An echo of a grimoire magic: its own particle and hit effect, fired by the sword at the target, up to {@code SPELL_TARGETS} hostile creatures within 3 blocks of it. */
    private static boolean echo(ShishouEntity e, ServerPlayer owner, ShishouRecord rec, ServerLevel sl, long now, ShishouBorrowTable.Entry en, LivingEntity t, Vec3 look) {
        MagicType m = magicOf(en.id());
        if (m == null) return false;
        ShishouSpells.Caster c = new ShishouSpells.Caster(sl, owner, e, e.position().add(0, 0.3, 0), look, rec.level(), rec, now, ShishouEntity.selfKey());
        double dmg = ShishouRules.spellDamage(rec.level(), 6.0, 0.25, 30.0);
        if (m.hit == MagicType.HitEffect.PIERCE) dmg *= 1.2;
        for (Mob h : ShishouTargets.hostilesNear(sl, owner, t.position(), 3.0, ShishouRules.SPELL_TARGETS)) {
            if (!ShishouSpells.hit(c, h, dmg)) continue;
            Vec3 away = h.position().subtract(e.position()).multiply(1, 0, 1);
            Vec3 dir = away.lengthSqr() > 0.0001 ? away.normalize() : Vec3.ZERO;
            switch (m.hit) {
                case BURN -> h.setRemainingFireTicks(80);
                case FREEZE -> {
                    h.setTicksFrozen(h.getTicksRequiredToFreeze() + 60);
                    h.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, false));
                }
                case SLOW -> h.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2, false, false));
                case SHOCK -> ShishouSpells.hit(c, h, dmg * 0.3);
                case POISON -> h.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0, false, false));
                case WITHER -> h.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0, false, false));
                case BLIND -> h.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
                case LEVITATE -> h.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 0, false, false));
                case PULL -> h.knockback(0.8, dir.x, dir.z);
                case PUSH -> h.knockback(0.9, -dir.x, -dir.z);
                case WEAKEN -> h.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1, false, false));
                default -> { }
            }
        }
        Vec3 from = e.position().add(0, 0.3, 0), to = t.position().add(0, t.getBbHeight() * 0.5, 0);
        int n = Math.min(FX_MAX - 8, 12);
        for (int i = 0; i <= n; i++) {
            double f = i / (double) n;
            PerfParticles.send(sl, m.particle(), from.x + (to.x - from.x) * f, from.y + (to.y - from.y) * f, from.z + (to.z - from.z) * f, 1, 0.05, 0.05, 0.05, 0.0);
        }
        PerfParticles.send(sl, m.particle(), to.x, to.y, to.z, 8, 0.5, 0.5, 0.5, 0.05);
        return true;
    }
}
