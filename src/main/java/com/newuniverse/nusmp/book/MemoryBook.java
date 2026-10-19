package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.ext.ExclusiveExt;
import com.newuniverse.nusmp.prop.MemoryRebuildProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.newuniverse.nusmp.book.BookPage.*;

/**
 * 1.93.0 Memory Magic, rebuilt as the magic of what was: replays, echoes, forgetting, the film of a moment run again. The five generic pages keep their ids and their order
 * (attribute_bolt, attribute_cut, attribute_field, attribute_domain, attribute_guard); four pages are appended (memory_echo, memory_forget, memory_recall, memory_archive): 9
 * family pages of the 24 allowed. Every successful cast puts the 5 second Reel Vestments on the caster (Aura.MEMORY, client.aura.MemoryAura). Memory Magic has no named user
 * in the sources of this repo and its canon text could not be fetched; the spells are original, built on the one idea the name gives (a moment kept and shown again).
 * <pre>
 *   attribute_bolt    Recollection Bolt  an orb (MEMORY_2): raw 7 on the first foe it meets, and 16 ticks later the same blow again (raw 4.2: the echo)
 *   attribute_cut     Replay Line        a line of 14 blocks: raw 5 on everything along it, and the same line replayed 10 ticks later (raw 5)
 *   attribute_field   Forgetting Field   a field of radius 5 for 6 s: every second raw 2.5 on foes, mobs forget what they hunted (their target is cleared) and see dimly (Blindness 1.5 s,
 *                                        none on bosses); allies in it shake off one harmful effect and heal 1.5 percent
 *   attribute_domain  Flashback Domain   a burst of radius 8: raw 10 and Blindness 3 s (1 s on bosses), and the burst replays 20 ticks later (raw 8)
 *   attribute_guard   Memory Veil        Night Vision, a melee bonus and thorns for 9 s, and the next hit you take within 8 s is forgotten (cancelled; a boss's hit is cut by 70 percent)
 *   memory_echo       Echo               an afterimage of you (MEMORY_1) for 15 s: it blinks beside the nearest foe every 1.5 s, strikes (raw 5) and replays the blow 8 ticks later (raw 3)
 *   memory_forget     Forget             the foe in your sights forgets you: its target is cleared every half second for 6 s and it is slowed; a player gets Weakness and Blindness.
 *                                        Uses the shared hard-control lock (BalanceLaw.beginControl) and its control time law
 *   memory_recall     Recall             back to where and how you were 3 to 7 seconds ago (a sample per second is kept): position, and health up to that moment's if it was higher;
 *                                        harmful effects are shaken off. Refuses when no sample is old enough or you changed dimension
 *   memory_archive    Archive            every foe within 16 blocks (at most 8) is archived for 6 s: everything that hurts it is written down, and at the end 60 percent of it
 *                                        is replayed on it (capped at 35 percent of its health, half for a boss)
 * </pre>
 */
public final class MemoryBook extends ElementBook {
    private static final int LAV = 0xFFBCA8FF;

    public MemoryBook() {
        super(MagicType.MEMORY, WikiBooks.color(MagicType.MEMORY), TensuraDamageTypes.MAGIC_GENERIC, familyList());
    }

    public static List<BookPage> familyList() {
        Aura a = Aura.MEMORY;
        List<BookPage> l = new ArrayList<>(List.of(
                starter("attribute_bolt", "Memory: Recollection Bolt", SongRebuildKit.armoured(a, MemoryBook::bolt)),
                mid("attribute_cut", "Memory: Replay Line", SongRebuildKit.armoured(a, MemoryBook::replayLine)),
                zone("attribute_field", "Memory: Forgetting Field", SongRebuildKit.armoured(a, MemoryBook::forgettingField)),
                signature("attribute_domain", "Memory: Flashback Domain", SongRebuildKit.armoured(a, MemoryBook::flashback)).withCooldown(900),
                mid("attribute_guard", "Memory: Memory Veil", MemoryBook::veil)));
        l.addAll(ExclusiveExt.pages(MagicType.MEMORY));
        l.add(mid("memory_echo", "Memory: Echo", SongRebuildKit.armoured(a, MemoryBook::echo)).withCooldown(320));
        l.add(mid("memory_forget", "Memory: Forget", SongRebuildKit.armoured(a, MemoryBook::forget)).withCooldown(300));
        l.add(mid("memory_recall", "Memory: Recall", SongRebuildKit.armoured(a, MemoryBook::recall)).withCooldown(600));
        l.add(signature("memory_archive", "Memory: Archive", SongRebuildKit.armoured(a, MemoryBook::archive)).withCooldown(1500).withAnim("signature"));
        return List.copyOf(l);
    }

    @Override
    public void castCircle(ServerPlayer p, float power) {
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.MEMORY_FX1, p, p.getEyePosition(), LAV, 12, 0.6f * power);
    }

    @Override
    public void impact(ServerPlayer p, Vec3 at, float power) {
        VfxSpawn.send(p.serverLevel(), VfxShape.MEMORY_FX3, at, at.add(0, 1, 0), LAV, 20, power);
    }

    private static void dim(LivingEntity t, int ticks) {
        t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BalanceLaw.isBoss(t) ? Math.min(ticks, 20) : ticks, 0, false, true));
    }

    // ---------------------------------------------------------------- the five generic pages
    private static boolean bolt(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        ServerLevel level = p.serverLevel();
        Vec3 start = p.getEyePosition().add(p.getViewVector(1f).scale(0.6)).add(0, -0.25, 0);
        b.castCircle(p, 1f);
        SongRebuildKit.projectile(p, PropKind.MEMORY_2, 0, start, p.getViewVector(1f), 1.6, 0.55 * s, 24, false,
                (bolt, t) -> { },
                (bolt, at) -> {
                    for (LivingEntity t : GrimoireBook.around(p, at, 1.5 * s)) {
                        b.hurt(i, p, t, mode, SongRebuildKit.safe(t, 7f));
                        SpellRuntime.later(level, 16, () -> {                              // the echo of the blow
                            if (!t.isAlive() || !p.isAlive()) return;
                            b.hurt(i, p, t, mode, SongRebuildKit.safe(t, 4.2f));
                            VfxSpawn.send(level, VfxShape.MEMORY_FX3, t.position(), t.position().add(0, 1, 0), LAV, 14, 0.4f);
                        });
                    }
                    b.impact(p, at, 0.7f);
                }, 0.9f * s);
        return true;
    }

    private static void lineHit(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, Vec3 a, Vec3 end) {
        int n = 0;
        for (LivingEntity t : GrimoireBook.along(p, a, end, 1.1)) {
            if (n++ >= SongRebuildKit.MAX_TARGETS) break;
            b.hurt(i, p, t, mode, SongRebuildKit.safe(t, 5f));
        }
        VfxSpawn.send(p.serverLevel(), VfxShape.MEMORY_FX1, a, end, LAV, 14, 1.3f);
    }

    private static boolean replayLine(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 a = p.getEyePosition().add(0, -0.2, 0), end = a.add(p.getViewVector(1f).scale(14 * s));
        lineHit(b, i, p, mode, a, end);
        b.castCircle(p, 0.8f);
        SpellRuntime.later(p.serverLevel(), 10, () -> { if (p.isAlive()) lineHit(b, i, p, mode, a, end); });
        return true;
    }

    private static boolean forgettingField(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        ServerLevel level = p.serverLevel();
        Vec3 c = GrimoireBook.aim(p, 24);
        double r = 5 * s;
        int ticks = 120;
        b.castCircle(p, 1f);
        VfxSpawn.send(level, VfxShape.MEMORY_FX2, c, c.add(0, 1, 0), LAV, ticks, (float) r);
        SpellRuntime.zone(level, ticks, 20, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                b.hurt(i, p, t, mode, SongRebuildKit.safe(t, 2.5f));
                dim(t, 30);
                if (t instanceof Mob m) { m.setTarget(null); m.getNavigation().stop(); }
            }
            for (LivingEntity a : SongRebuildKit.allies(p, c, r)) {
                for (MobEffectInstance e : new ArrayList<>(a.getActiveEffects())) if (!e.getEffect().value().isBeneficial()) { a.removeEffect(e.getEffect()); break; }
                if (!BalanceLaw.isBoss(a)) BalanceLaw.heal(a, a.getMaxHealth() * 0.015f);
            }
        });
        return true;
    }

    private static void flashbackHit(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, Vec3 c, double r, float raw, boolean blind) {
        for (LivingEntity t : GrimoireBook.around(p, c, r)) {
            b.hurt(i, p, t, mode, SongRebuildKit.safe(t, raw));
            if (blind) dim(t, 60);
        }
        VfxSpawn.send(p.serverLevel(), VfxShape.MEMORY_FX3, c, c.add(0, 1, 0), LAV, 26, (float) r / 3f * 1.2f);
    }

    private static boolean flashback(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 c = p.position();
        double r = 8 * s;
        flashbackHit(b, i, p, mode, c, r, 10f, true);
        b.castCircle(p, 1.2f);
        SpellRuntime.later(p.serverLevel(), 20, () -> { if (p.isAlive()) flashbackHit(b, i, p, mode, c, r, 8f, false); });
        return true;
    }

    private static boolean veil(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 180;
        var tag = i.getOrCreateTag();
        long now = p.level().getGameTime();
        tag.putLong("EmpowerUntil", now + ticks);
        tag.putFloat("EmpowerBonus", 4f);
        tag.putLong("ThornsUntil", now + ticks);
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, ticks + 200, 0, false, false));
        p.getPersistentData().putLong(MemoryRebuildProps.VEIL_UNTIL, now + 160);
        SongRebuildKit.armour(p, Aura.MEMORY, ticks);
        b.castCircle(p, 1f);
        VfxSpawn.send(p.serverLevel(), VfxShape.MEMORY_FX2, p.position(), p.position().add(0, 1, 0), LAV, 24, 1.6f);
        return true;
    }

    // ---------------------------------------------------------------- the appended pages
    private static boolean echo(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        SongRebuildKit.dismiss(p, PropKind.MEMORY_1, -1, 0);
        Vec3 at = p.position().add(-p.getViewVector(1f).x * 0.8, 0, -p.getViewVector(1f).z * 0.8);
        if (SongRebuildKit.spawn(p, PropKind.MEMORY_1, at, p.getYRot(), 0.95f * Math.min(1.3f, SongRebuildKit.size(p)), SongRebuildKit.span(300, p), 0, b, i, mode) == null) {
            GrimoireBook.fail(p, "The echo would not form.");
            return false;
        }
        b.castCircle(p, 1.2f);
        VfxSpawn.send(p.serverLevel(), VfxShape.MEMORY_FX3, at, at.add(0, 1, 0), LAV, 20, 0.8f);
        return true;
    }

    private static boolean forget(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 20);
        if (t == null || !SongRebuildKit.foes(p, t.position(), 1.5).contains(t)) { GrimoireBook.fail(p, "No foe in your sights."); return false; }
        if (!GrimoireBook.control(p)) return false;
        ServerLevel level = p.serverLevel();
        int ticks = BalanceLaw.controlTicks(t, 120);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 0, false, true));
        if (t instanceof Mob) {
            for (int k = 0; k < 6; k++) SpellRuntime.later(level, 10 * k, () -> { if (t instanceof Mob m && m.isAlive()) { m.setTarget(null); m.getNavigation().stop(); } });
        } else {
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0, false, true));
            dim(t, ticks);
        }
        b.castCircle(p, 1f);
        VfxSpawn.send(level, VfxShape.MEMORY_FX1, p.getEyePosition(), t.getBoundingBox().getCenter(), LAV, 14, 1.0f);
        VfxSpawn.sendFollowing(level, VfxShape.MEMORY_FX2, t, t.position(), LAV, ticks, 1.6f);
        return true;
    }

    private static boolean recall(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        MemoryRewind.Sample s = MemoryRebuildProps.ring(p).recall(level.getGameTime(), level.dimension().location().toString());
        if (s == null) { GrimoireBook.fail(p, "There is nothing old enough to remember yet."); return false; }
        Vec3 from = p.position(), to = new Vec3(s.x(), s.y(), s.z());
        if (!level.noCollision(p, p.getBoundingBox().move(to.subtract(from)))) { GrimoireBook.fail(p, "That place is not free any more."); return false; }
        b.castCircle(p, 1f);
        VfxSpawn.send(level, VfxShape.MEMORY_FX3, from, from.add(0, 1, 0), LAV, 22, 1.0f);
        p.teleportTo(to.x, to.y, to.z);
        p.fallDistance = 0;
        if (s.health() > p.getHealth()) p.setHealth(Math.min(p.getMaxHealth(), s.health()));
        for (MobEffectInstance e : new ArrayList<>(p.getActiveEffects())) if (!e.getEffect().value().isBeneficial()) p.removeEffect(e.getEffect());
        VfxSpawn.send(level, VfxShape.MEMORY_FX3, to, to.add(0, 1, 0), LAV, 22, 1.0f);
        return true;
    }

    private static boolean archive(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        List<LivingEntity> marked = new ArrayList<>();
        for (LivingEntity t : GrimoireBook.around(p, p.position(), 16 * GrimoireBook.size(i, p))) {
            if (marked.size() >= 8) break;
            marked.add(t);
            MemoryRebuildProps.archive(t, now + 125);
            VfxSpawn.sendFollowing(level, VfxShape.MEMORY_FX2, t, t.position(), LAV, 120, 1.4f);
        }
        if (marked.isEmpty()) { GrimoireBook.fail(p, "No foe to archive."); return false; }
        b.castCircle(p, 1.4f);
        SpellRuntime.later(level, 120, () -> {
            for (LivingEntity t : marked) {
                double rec = MemoryRebuildProps.takeArchive(t);
                if (!t.isAlive() || rec <= 0 || !p.isAlive()) continue;
                float dmg = MemoryRewind.replay(rec, t.getMaxHealth(), BalanceLaw.isBoss(t));
                if (dmg > 0) t.hurt(p.damageSources().magic(), dmg);
                VfxSpawn.send(level, VfxShape.MEMORY_FX3, t.position(), t.position().add(0, 1, 0), LAV, 24, 0.9f);
            }
        });
        return true;
    }
}
