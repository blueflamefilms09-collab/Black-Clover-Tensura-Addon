package com.newuniverse.nusmp.entity.zogratis;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import com.newuniverse.nusmp.book.SpellRuntime;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Megicula's Curse of Decay in the world (the pure rules are {@link CurseBrand}). A sigil is drawn on the ground; everyone standing
 * in it at the moment it closes is branded:
 * <ul>
 *   <li>the brand is the effect {@link #DECAY} (so it shows in the HUD and a purification, milk or a cleanse, strips it), plus Weakness
 *       (Weakness II from level 2) so the victim's offence suffers, and every heal is cut to 60%, 40% or 25% by level ({@link #onHeal});</li>
 *   <li>once a second it bites for {@link VanicaTable#CURSE_DOT} through the boss's damage law, harder the longer the victim stays in
 *       the sigil, and what it takes feeds the boss a little (blood drain);</li>
 *   <li>it breaks when the victim leaves the sigil, when they fight with anti-magic (a demon sword, the Anti-Magic Lord), when they are
 *       cleansed, when it runs out, or when the boss falls.</li>
 * </ul>
 * Wire it in NUSMP: {@code VanicaCurse.EFFECTS.register(modEventBus)} and {@code NeoForge.EVENT_BUS.addListener(VanicaCurse::onHeal)}.
 * If the effect is not registered the curse still works (Weakness, the bite, the three ways out except the purification).
 */
public final class VanicaCurse {
    private VanicaCurse() {}

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, NUSMP.MODID);

    /** The brand itself: a harmful mark with no attribute change of its own; the amplifier is the curse's level minus one. */
    public static final DeferredHolder<MobEffect, MobEffect> DECAY = EFFECTS.register("megicula_decay", () -> new Mark(MobEffectCategory.HARMFUL, 0x7A1020));

    private static final class Mark extends MobEffect {
        Mark(MobEffectCategory category, int color) { super(category, color); }
    }

    /** Loads this class (and so registers the effect) before the effect register's event; call it from the NUSMP constructor. */
    public static void init() {}

    private static final int CURSE_ARGB = 0xFFFF5A4A, BREAK_ARGB = 0xFFC08AFF, MARK_TICKS = 210, MAX_BRAND_TICKS = 600;

    /** One live brand: who, whose sigil, and the rules. */
    private static final class Active {
        final UUID victim, boss;
        final CurseBrand brand;
        final Vec3 centre;
        final double radius;
        long lastSeen;
        int steps;
        boolean markSeen;

        Active(UUID victim, UUID boss, CurseBrand brand, Vec3 centre, double radius, long now) {
            this.victim = victim;
            this.boss = boss;
            this.brand = brand;
            this.centre = centre;
            this.radius = radius;
            this.lastSeen = now;
        }

        /** A brand whose zone stopped (a server that shut down mid-fight, another world) is stale. */
        boolean fresh(long now) { return now >= lastSeen && now - lastSeen <= 40; }
    }

    private static final Map<UUID, Active> ACTIVE = new HashMap<>();

    // ================================================================ healing is cut
    /** While branded every heal is cut to the level's share (60%, 40%, 25%). */
    public static void onHeal(LivingHealEvent e) {
        LivingEntity t = e.getEntity();
        if (t.level().isClientSide || !DECAY.isBound()) return;
        MobEffectInstance mark = t.getEffect(DECAY);
        if (mark == null) return;
        e.setAmount(e.getAmount() * (float) VanicaMath.healFactor(mark.getAmplifier() + 1));
    }

    /** Is this entity under a Curse of Decay (by the live brand or by the mark)? */
    public static boolean branded(LivingEntity e) {
        Active a = ACTIVE.get(e.getUUID());
        if (a != null && a.fresh(e.level().getGameTime()) && !a.brand.ended()) return true;
        return DECAY.isBound() && e.hasEffect(DECAY);
    }

    /** The fight is over (the boss fell or gave up): every brand it cast is lifted now and the marks are stripped; the zones notice and stop. */
    static void liftAll(ZogratisBoss boss) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        UUID me = boss.getUUID();
        for (Active a : new ArrayList<>(ACTIVE.values())) {
            if (!me.equals(a.boss)) continue;
            ACTIVE.remove(a.victim, a);
            if (sl.getEntity(a.victim) instanceof LivingEntity v) release(sl, v, CurseBrand.End.EXPIRED, true);
        }
    }

    // ================================================================ the sigil
    /**
     * Draws a sigil of the level's radius at 'centre' and brands every foe standing in it (at most six), for 'ticks' ticks at 'level'.
     * 'target' is the one it was aimed at (it is only branded if it is inside the sigil). Returns how many were branded.
     */
    static int sigil(ZogratisBoss boss, Vec3 centre, LivingEntity target, int rawTicks, int level) {
        if (!(boss.level() instanceof ServerLevel sl) || centre == null) return 0;
        final int ticks = Math.max(40, Math.min(MAX_BRAND_TICKS, rawTicks));          // a caller's number can never make an endless brand
        final long now = sl.getGameTime();
        ACTIVE.values().removeIf(a -> !a.fresh(now));                              // brands whose zone stopped (a world that closed, a failed step)
        final int lv = Math.max(1, Math.min(3, level));
        final double r = VanicaMath.sigilRadius(lv);
        List<LivingEntity> in = new ArrayList<>();
        if (target != null && target.isAlive() && boss.isFoe(target) && inside(target, centre, r)) in.add(target);
        for (LivingEntity e : boss.foesAround(centre, r + 2)) {
            if (in.size() >= 6) break;
            if (e != target && inside(e, centre, r)) in.add(e);
        }
        VfxSpawn.send(sl, VfxShape.CURSE_FX2, centre.add(0, 0.05, 0), centre.add(0, 1, 0), CURSE_ARGB, Math.max(40, Math.min(160, ticks)), (float) r);
        VfxSpawn.send(sl, VfxShape.CURSE_FX3, centre.add(0, 0.2, 0), centre.add(0, 1, 0), CURSE_ARGB, 24, (float) (r / 3));
        sl.playSound(null, BlockPos.containing(centre), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.4f, 0.7f);
        for (LivingEntity e : in) brand(boss, sl, e, centre, r, ticks, lv);
        return in.size();
    }

    private static boolean inside(LivingEntity e, Vec3 c, double r) {
        double dx = e.getX() - c.x, dz = e.getZ() - c.z;
        return dx * dx + dz * dz <= r * r && Math.abs(e.getY() - c.y) <= 6;
    }

    private static void brand(ZogratisBoss boss, ServerLevel sl, LivingEntity victim, Vec3 centre, double radius, int ticks, int level) {
        long now = sl.getGameTime();
        Active old = ACTIVE.get(victim.getUUID());
        int lv = level, life = ticks;
        if (old != null && old.fresh(now) && !old.brand.ended()) {              // already branded: the stronger and the longer win, the new sigil takes over
            lv = Math.max(level, old.brand.level());
            life = Math.max(ticks, old.brand.life());
        }
        final Active me = new Active(victim.getUUID(), boss.getUUID(), new CurseBrand(life, lv), centre, radius, now);
        ACTIVE.put(victim.getUUID(), me);
        applyMarks(victim, lv);
        if (DECAY.isBound() && victim.hasEffect(DECAY)) me.markSeen = true;      // a purification in the first moments already counts
        if (victim instanceof ServerPlayer p)
            p.displayClientMessage(Component.literal("Megicula's curse brands you. Step out of the sigil, fight it with anti-magic, or cleanse it!").withStyle(ChatFormatting.DARK_RED), true);
        SpellRuntime.zone(sl, life + 40, CurseBrand.STEP, VanicaMoves.safe("curse of decay", age -> step(me, sl)));
    }

    private static void applyMarks(LivingEntity victim, int level) {
        victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, VanicaMath.weaknessAmp(level)));
        if (DECAY.isBound()) victim.addEffect(new MobEffectInstance(DECAY, MARK_TICKS, level - 1));      // over 200 ticks, so the icon does not blink; release() strips it
    }

    private static void step(Active a, ServerLevel sl) {
        if (ACTIVE.get(a.victim) != a) return;                                    // replaced by a newer brand, or finished
        long now = sl.getGameTime();
        a.lastSeen = now;
        LivingEntity v = sl.getEntity(a.victim) instanceof LivingEntity le && le.isAlive() ? le : null;
        ZogratisBoss boss = sl.getEntity(a.boss) instanceof ZogratisBoss b && b.isAlive() ? b : null;
        if (v == null || boss == null) {
            ACTIVE.remove(a.victim, a);
            if (v != null) release(sl, v, CurseBrand.End.EXPIRED, true);              // its caster fell: the curse goes with it
            return;
        }
        if (DECAY.isBound()) {
            if (v.hasEffect(DECAY)) a.markSeen = true;
            else if (a.markSeen) a.brand.cleanse();                              // milk, a cleanse: the mark is gone
        }
        CurseBrand.Result r = a.brand.step(inside(v, a.centre, a.radius), antiMagic(v));
        if (r.ended()) {
            ACTIVE.remove(a.victim, a);
            release(sl, v, r.reason(), true);
            return;
        }
        applyMarks(v, a.brand.level());
        Tells.gather(sl, v.position().add(0, v.getBbHeight() * 0.6, 0), 0.5, DevilStyle.color(boss.devil()), 3);
        if (++a.steps % 4 == 0) bite(boss, v, VanicaTable.CURSE_DOT * (float) r.multiplier());
    }

    /** The curse's bite: the boss's damage law, as Tensura's curse damage when the data pack has it; a little of it feeds the boss. */
    private static void bite(ZogratisBoss boss, LivingEntity v, float base) {
        float amount = boss.bossDamage(v, base);
        if (amount <= 0f) return;
        DamageSource src = boss.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(TensuraDamageTypes.CURSE)
                .<DamageSource>map(h -> new DamageSource(h, boss))
                .orElseGet(() -> boss.damageSources().indirectMagic(boss, boss));
        float before = v.getHealth() + v.getAbsorptionAmount();
        if (v.hurt(src, amount)) VanicaMoves.drain(boss, v, Math.max(0f, before - (v.getHealth() + v.getAbsorptionAmount())));
    }

    private static void release(ServerLevel sl, LivingEntity v, CurseBrand.End why, boolean say) {
        if (DECAY.isBound()) v.removeEffect(DECAY);
        if (!say) return;
        Vec3 at = v.position();
        VfxSpawn.send(sl, VfxShape.CURSE_WARDING_FX3, at.add(0, 0.8, 0), at.add(0, 1.8, 0), BREAK_ARGB, 20, 0.7f);
        if (v instanceof ServerPlayer p) {
            String line = switch (why) {
                case LEFT_SIGIL -> "You left the sigil: the curse breaks.";
                case ANTI_MAGIC -> "Anti-magic tears the curse apart.";
                case CLEANSED -> "The curse is cleansed.";
                default -> "The curse of decay fades.";
            };
            p.displayClientMessage(Component.literal(line).withStyle(ChatFormatting.GRAY), true);
        }
    }

    /** A demon sword in hand, or the Anti-Magic Lord: what the Curse of Decay cannot hold on to. */
    static boolean antiMagic(LivingEntity e) {
        if (!(e instanceof Player p)) return false;
        try {
            return AntiMagic.lord(p).isPresent() || BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem()).getPath().startsWith("demon_");
        } catch (RuntimeException ex) {
            LogUtils.getLogger().debug("[nusmp] anti-magic check failed: {}", ex.toString());
            return false;
        }
    }
}
