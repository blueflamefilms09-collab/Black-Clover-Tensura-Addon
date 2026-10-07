package com.newuniverse.nusmp.entity;

import com.newuniverse.nusmp.book.KotodamaWords;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * 0.52: Zagred's two signature Word Soul attacks (docs/zagred_boss_gdd.md, sections 1.2 and 1.4), plus the shared pieces of
 * his Halt ring and Fall ring.
 * <ul>
 *   <li><b>Redact</b> ("Sentence of Ruin"): a sentence of 5 to 8 glyph letters is written on the floor in a curve toward the
 *       target (that is the wind-up), then the letters detonate in reading order, 6 ticks apart; the last is larger and leaves a
 *       patch that strips buffs. An anti-magic blow on a letter cancels it and every letter after it. Players can speak it too
 *       (creative / earned Kotodama), along their line of sight.</li>
 *   <li><b>Overwrite</b> ("Rewrite the Rules"), boss only: five rule stones orbit him while he chants, then one rule holds the
 *       arena for 12 s ("No fire", "No ice", "No lightning", "No flying", "No standing still", "No healing"). Breaking it
 *       hurts the offender. Destroying three stones cuts the sentence short and staggers him.</li>
 * </ul>
 */
public final class ZagredAttacks {
    private ZagredAttacks() {}

    public enum Rule {
        NO_FIRE("No fire", "fire"), NO_ICE("No ice", "ice"), NO_LIGHTNING("No lightning", "lightning"),
        NO_FLYING("No flying", null), NO_STILL("No standing still", null), NO_HEALING("No healing", null);
        public final String text;
        final String element;
        Rule(String text, String element) { this.text = text; this.element = element; }
    }

    public static final int RULE_TICKS = 240;
    static final int STONES = 5;

    // ================================================================ Redact
    /** Where the letters go: a curve of 'n' glyphs from the caster toward its target (or where it looks), on the floor. */
    public static List<Vec3> plan(LivingEntity c, int n) {
        LivingEntity tg = c instanceof Mob m ? m.getTarget() : null;
        Vec3 flat = (tg != null ? tg.position().subtract(c.position()) : c.getViewVector(1f)).multiply(1, 0, 1);
        if (flat.lengthSqr() < 1e-4) flat = new Vec3(0, 0, 1);
        Vec3 dir = flat.normalize(), side = new Vec3(-dir.z, 0, dir.x);
        double bend = c.getRandom().nextBoolean() ? 1 : -1;
        List<Vec3> pts = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Vec3 p = c.position().add(dir.scale(3 + i * 2.6)).add(side.scale(Math.sin(i * 0.7) * 2.2 * bend));
            pts.add(ground(c.level(), p, c.getY()));
        }
        return pts;
    }

    /** The floor near y at (x, z): the first air block with solid ground below, searching from 2 above to 4 below. */
    static Vec3 ground(Level level, Vec3 p, double y) {
        BlockPos base = BlockPos.containing(p.x, y, p.z);
        for (int dy = 2; dy >= -4; dy--) {
            BlockPos b = base.offset(0, dy, 0);
            if (level.getBlockState(b).isAir() && level.getBlockState(b.below()).blocksMotion()) return new Vec3(p.x, b.getY(), p.z);
        }
        return new Vec3(p.x, y, p.z);
    }

    /** The wind-up: the sentence is written on the floor so the party can read it. */
    public static void telegraph(LivingEntity c, List<Vec3> pts) {
        if (!(c.level() instanceof ServerLevel sl)) return;
        DustParticleOptions red = new DustParticleOptions(new Vector3f(0.95f, 0.1f, 0.3f), 1.4f);
        for (int i = 0; i < pts.size(); i++) {
            Vec3 p = pts.get(i);
            boolean last = i == pts.size() - 1;
            VfxSpawn.send(sl, VfxShape.KOTO_WORDS, p.add(0, 0.15, 0), p.add(0, 1.3, 0), KotodamaWords.VIOLET, 30, last ? 1.3f : 0.9f);
            double r = last ? 4 : 2.5;
            int n = (int) (r * 7);
            for (int k = 0; k < n; k++) {
                double a = k * Math.PI * 2 / n;
                sl.sendParticles(red, p.x + Math.cos(a) * r, p.y + 0.12, p.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
            }
        }
    }

    /** The letters detonate in reading order. 'delay' is the pause before the first (0 when the wind-up already happened). */
    public static void redact(LivingEntity c, float pw, List<Vec3> pts, int delay) {
        if (!(c.level() instanceof ServerLevel sl)) return;
        boolean[] cut = new boolean[1];
        float raw = 9f * pw / 1.9f;
        for (int i = 0; i < pts.size(); i++) {
            int k = i;
            SpellRuntime.later(sl, delay + 2 + k * 6, () -> letter(sl, c, pts, k, raw, cut));
        }
    }

    private static void letter(ServerLevel sl, LivingEntity c, List<Vec3> pts, int k, float raw, boolean[] cut) {
        if (!c.isAlive() || cut[0]) return;
        Vec3 p = pts.get(k);
        boolean last = k == pts.size() - 1;
        if (c instanceof ZagredBossEntity && cutByAntiMagic(sl, p)) {
            cut[0] = true;
            VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, p.add(0, 1, 0), p, 0xFFFFFFFF, 14, 1.6f);
            sl.playSound(null, BlockPos.containing(p), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.5f, 1.4f);
            return;
        }
        double r = last ? 4 : 2.5;
        for (LivingEntity t : KotodamaWords.foes(c, p, r)) {
            KotodamaWords.hurt(c, t, last ? raw * 1.3f : raw);
            KotodamaWords.spirit(c, t, 1);
        }
        VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, p.add(0, 0.8, 0), p, KotodamaWords.VIOLET, 16, (float) (r / 2.2));
        sl.playSound(null, BlockPos.containing(p), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.2f, 0.5f);
        if (last) SpellRuntime.zone(sl, 80, 10, age -> {                         // the redacted patch: buffs struck out, mending stopped
            for (LivingEntity t : KotodamaWords.foes(c, p, 4)) {
                t.removeEffect(MobEffects.REGENERATION);
                for (MobEffectInstance e : new ArrayList<>(t.getActiveEffects()))
                    if (e.getEffect().value().isBeneficial()) { t.removeEffect(e.getEffect()); break; }
            }
        });
    }

    /** A player on the letter who is swinging anti-magic cuts the sentence there. */
    private static boolean cutByAntiMagic(ServerLevel sl, Vec3 p) {
        for (Player pl : sl.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(p, p).inflate(2.6)))
            if (pl.swinging && pl.swingTime <= 3 && ZagredBossEntity.antiMagicHeld(pl)) return true;
        return false;
    }

    // ================================================================ Halt ring and Fall ring
    /** A dust ring on the floor, shown 'times' times 'gap' ticks apart (the Halt ring closing in). */
    static void ring(ServerLevel sl, Vec3 c, double r, int times, int gap) {
        DustParticleOptions d = new DustParticleOptions(new Vector3f(0.55f, 0.25f, 1f), 1.6f);
        for (int k = 0; k < times; k++) {
            double rr = r * (1 - 0.12 * k);
            SpellRuntime.later(sl, k * gap, () -> {
                int n = (int) (rr * 6);
                for (int i = 0; i < n; i++) {
                    double a = i * Math.PI * 2 / n;
                    sl.sendParticles(d, c.x + Math.cos(a) * rr, c.y + 0.15, c.z + Math.sin(a) * rr, 1, 0, 0, 0, 0);
                }
            });
        }
    }

    /** After Fall's pull: a ring 0.8 blocks high races out from the caster; jump it. 11 damage and a shove to anyone it meets. */
    public static void crush(LivingEntity c, float pw) {
        if (!(c.level() instanceof ServerLevel sl)) return;
        float raw = 11f * pw / 1.9f;
        Vec3 o = c.position();
        DustParticleOptions d = new DustParticleOptions(new Vector3f(0.2f, 0.1f, 0.35f), 1.8f);
        for (int k = 1; k <= 9; k++) {
            double r = 2.0 * k;
            int kk = k;
            SpellRuntime.later(sl, 20 + k * 2, () -> {
                int n = (int) (r * 6);
                for (int i = 0; i < n; i++) {
                    double a = i * Math.PI * 2 / n;
                    sl.sendParticles(d, o.x + Math.cos(a) * r, o.y + 0.3, o.z + Math.sin(a) * r, 1, 0, 0.05, 0, 0);
                }
                for (LivingEntity t : KotodamaWords.foes(c, o, r + 1.5)) {
                    double dist = t.position().subtract(o).multiply(1, 0, 1).length();
                    if (Math.abs(dist - r) > 1.4 || t.getY() - o.y > 0.8) continue;       // only on the ring, and only if you stayed low
                    KotodamaWords.hurt(c, t, raw);
                    Vec3 away = t.position().subtract(o).multiply(1, 0, 1);
                    if (away.lengthSqr() > 1e-4) t.knockback(0.8, -away.x, -away.z);
                }
                if (kk == 1) sl.playSound(null, BlockPos.containing(o), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.4f, 0.4f);
            });
        }
    }

    // ================================================================ Overwrite
    /** At the start of the chant: pick the rule and raise the five stones. */
    static void overwriteStart(ZagredBossEntity z, ServerLevel sl) {
        List<Rule> rules = new ArrayList<>(List.of(Rule.values()));
        rules.remove(z.lastRule);
        z.pendingRule = rules.get(z.getRandom().nextInt(rules.size()));
        z.lastRule = z.pendingRule;
        z.stoneKills = 0;
        z.stones.clear();
        for (int i = 0; i < STONES; i++) {
            double a = i * Math.PI * 2 / STONES;
            Vec3 at = z.position().add(Math.cos(a) * 6, 1.4, Math.sin(a) * 6);
            GrimoireDaemonEntity s = GrimoireDaemonEntity.spawn(sl, z, GrimoireDaemonEntity.STONE, at);
            if (s != null) z.stones.add(s.getUUID());
        }
        z.say(sl, "Zagred begins to write...");
    }

    /** The chant ends: the rule holds the arena. */
    public static boolean overwrite(ZagredBossEntity z) {
        if (!(z.level() instanceof ServerLevel sl) || z.pendingRule == null) return false;
        z.ruleKind = z.pendingRule;
        z.ruleUntil = z.tickCount + RULE_TICKS;
        z.ruleViolation.clear();
        if (z.ruleKind == Rule.NO_HEALING) { noHealUntil = sl.getGameTime() + RULE_TICKS; noHealLevel = sl.dimension(); }
        z.say(sl, "Zagred writes: \"" + z.ruleKind.text.toUpperCase() + "\"");
        sl.playSound(null, z.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.HOSTILE, 2f, 0.5f);
        VfxSpawn.sendFollowing(sl, VfxShape.KOTO_AURA, z, z.position(), KotodamaWords.VIOLET, 60, 2.4f);
        return true;
    }

    static void endRule(ZagredBossEntity z, ServerLevel sl) {
        z.ruleKind = null;
        z.ruleUntil = 0;
        noHealUntil = 0;
        for (var id : z.stones) if (sl.getEntity(id) instanceof GrimoireDaemonEntity s) s.discard();
        z.stones.clear();
    }

    /** Every 5 ticks while a rule holds: find who is breaking it and hurt them (6 per second of breaking). */
    static void enforce(ZagredBossEntity z, ServerLevel sl) {
        long t = z.tickCount;
        if (z.ruleKind == null) return;
        if (t >= z.ruleUntil) { z.say(sl, "The rule fades."); endRule(z, sl); return; }
        Rule rule = z.ruleKind;
        if (t % 20 == 0) {
            Component line = Component.literal("RULE: " + rule.text.toUpperCase() + "  (" + (z.ruleUntil - t + 19) / 20 + "s)").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD);
            for (ServerPlayer p : sl.players()) if (p.distanceToSqr(z) < 64 * 64) p.displayClientMessage(line, true);
        }
        if (rule != Rule.NO_FLYING && rule != Rule.NO_STILL) return;                    // the others are enforced where the blow lands
        for (LivingEntity f : KotodamaWords.foes(z, z.position(), 48)) {
            if (!(f instanceof Player p)) continue;
            int[] s = z.ruleViolation.computeIfAbsent(p.getUUID(), k -> new int[3]);     // [0] airborne ticks, [1] still ticks, [2] violation ticks
            Vec3 d = p.getDeltaMovement();
            s[0] = p.onGround() || p.isInWater() || p.onClimbable() ? 0 : s[0] + 5;
            s[1] = d.horizontalDistanceSqr() < 0.0009 && p.onGround() ? s[1] + 5 : 0;
            boolean flying = p.getAbilities().flying || p.isFallFlying() || (s[0] > 20 && d.y > -0.4);
            boolean bad = rule == Rule.NO_FLYING ? flying : s[1] > 20;
            s[2] = bad ? s[2] + 5 : 0;
            if (s[2] >= 20) { s[2] = 0; punish(z, p); }
        }
    }

    /** One second of rule-breaking: spiritual damage plus a short bruise. */
    static void punish(ZagredBossEntity z, LivingEntity who) {
        KotodamaWords.spirit(z, who, 2);
        who.hurt(z.damageSources().indirectMagic(z, z), 3f);
        VfxSpawn.send((ServerLevel) z.level(), VfxShape.KOTO_SHATTER, who.getBoundingBox().getCenter(), z.getBoundingBox().getCenter(), KotodamaWords.ABYSS, 12, 0.6f);
    }

    // ---- "No healing": healing is undone as it happens
    static long noHealUntil;
    static ResourceKey<Level> noHealLevel;

    public static void onHeal(LivingHealEvent e) {
        LivingEntity t = e.getEntity();
        if (t.level().isClientSide || !(t instanceof Player) || noHealLevel == null) return;
        if (t.level().dimension() == noHealLevel && t.level().getGameTime() < noHealUntil) e.setCanceled(true);
    }
}
