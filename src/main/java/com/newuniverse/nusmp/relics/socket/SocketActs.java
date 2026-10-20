package com.newuniverse.nusmp.relics.socket;

import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.newuniverse.nusmp.particle.fx.FxRelic;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * The active abilities of the sockets: one {@link Act} per catalog entry, built from the spec's numbers (cooldown, magicule cost, power) and run by the
 * relic engine's own {@code activate}, so attunement, cooldown, cost, creative and spectator handling are the engine's and not repeated here. All damage
 * and healing go through {@link Fx} (the balance law and the relics' caps), all targets through {@link Fx#foesAround} (never creative or spectator
 * players, allies, pets or summons), and every area is small and bounded.
 */
public final class SocketActs {
    private SocketActs() {}

    private static final Map<String, Act> ACTS = new HashMap<>();
    private static final String[] TENSURA_CURSES = {"curse", "fatal_poison", "magicule_poison", "corrosion", "infection", "fear", "burden", "silence", "fragility", "frost", "webbed", "paralysis"};

    static {
        for (SocketDef d : SocketCatalog.all()) {
            ActSpec s = d.act();
            ACTS.put(d.id(), Act.of(s.key(), s.name(), ctx -> run(d, ctx)).cooldown(s.cooldownTicks()).mana(s.manaPercent()));
        }
    }

    /** The act of a socket id, or null. */
    public static Act of(String socketId) { return socketId == null ? null : ACTS.get(socketId); }

    private static void run(SocketDef d, ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        float amt = (float) SocketRules.actAmount(d.act().power(), SocketGrades.tier(ctx.stack()));
        FxRelic.ring(l, p.position(), 2.0, d.rgb());
        switch (d.id()) {
            case "spirit:fire", "devil:nahamah" -> flame(p, d, amt);
            case "spirit:water" -> {
                Fx.aoe(p, 6.0, amt, "minecraft:magic", "minecraft:slowness", 80, 1);
                splash(l, p, com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_SOCKET, FxTone.LIGHT), 40, 3.0);
            }
            case "spirit:wind" -> {
                Fx.aoe(p, 3.0, amt, "minecraft:magic", null, 0, 0);
                Fx.dash(p, 8.0);
                Fx.addEffect(p, "minecraft:speed", 60, 1);
                splash(l, p, com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_SOCKET, FxTone.LIGHT), 24, 1.5);
            }
            case "spirit:earth" -> {
                Fx.shield(p, 10f, 200);
                Fx.addEffect(p, "minecraft:resistance", 160, 0);
                Fx.aoe(p, 4.0, 0f, null, "minecraft:slowness", 60, 1);
                splash(l, p, com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_SOCKET, FxTone.LIGHT), 24, 2.0);
            }
            case "spirit:light" -> {
                mend(p, amt, 6.0);
                for (LivingEntity f : Fx.foesAround(p, 8.0)) Fx.addEffect(f, "minecraft:glowing", 100, 0);
                splash(l, p, com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_SOCKET, FxTone.LIGHT), 28, 2.0);
            }
            case "spirit:dark" -> {
                Fx.addEffect(p, "minecraft:invisibility", 100, 0);
                Fx.addEffect(p, "minecraft:speed", 100, 0);
                Fx.aoe(p, 6.0, 0f, null, "minecraft:blindness", 60, 0);
                splash(l, p, com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_SOCKET, FxTone.DARK), 30, 2.0);
            }
            case "devil:liebe" -> {
                cleanse(p);
                Fx.shield(p, 8f, 200);
                splash(l, p, com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_SOCKET, FxTone.LIGHT), 28, 2.0);
            }
            case "devil:zagred" -> {
                Fx.aoe(p, 8.0, 0f, null, "minecraft:slowness", 60, 4);
                Fx.aoe(p, 8.0, 0f, null, "minecraft:weakness", 100, 1);
                splash(l, p, com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_SOCKET, FxTone.LIGHT), 40, 3.0);
            }
            case "devil:megicula" -> curseWarding(p);
            case "devil:lucifero" -> {
                for (LivingEntity t : Fx.foesAround(p, 6.0)) {
                    Fx.damage(p, t, amt, "minecraft:magic");
                    Fx.addEffect(t, "minecraft:slowness", 80, 2);
                    t.push(0.0, -0.6, 0.0);
                    t.hurtMarked = true;
                }
                splash(l, p, com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_SOCKET, FxTone.DARK), 30, 3.0);
            }
            case "devil:beelzebub" -> {
                splash(l, p, com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_SOCKET, FxTone.DARK), 40, 1.0);
                if (Fx.teleportLook(p, 14.0)) splash(l, p, com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_SOCKET, FxTone.DARK), 40, 1.0);
            }
            case "devil:astaroth" -> {
                Fx.addEffect(p, "minecraft:speed", 100, 2);
                Fx.addEffect(p, "minecraft:haste", 100, 1);
                Fx.aoe(p, 8.0, 0f, null, "minecraft:slowness", 80, 2);
                splash(l, p, com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_SOCKET, FxTone.LIGHT), 30, 3.0);
            }
            case "devil:lilith" -> {
                for (LivingEntity t : Fx.foesAround(p, 6.0)) {
                    Fx.damage(p, t, amt, "minecraft:freeze");
                    Fx.addEffect(t, "minecraft:slowness", 80, 3);
                    t.setTicksFrozen(t.getTicksRequiredToFreeze() + 40);
                }
                splash(l, p, com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_SOCKET, FxTone.LIGHT), 40, 3.0);
            }
            case "devil:walgner" -> {
                float healed = 0f;
                List<LivingEntity> foes = Fx.foesAround(p, 5.0);
                for (int i = 0; i < foes.size() && i < 3; i++) {
                    Fx.damage(p, foes.get(i), amt, "minecraft:magic");
                    healed += amt * 0.5f;
                }
                if (healed > 0f) Fx.heal(p, healed);
                splash(l, p, com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_SOCKET, FxTone.DARK), 20, 2.0);
            }
            default -> { }
        }
        l.playSound(null, p.blockPosition(), sound(d), SoundSource.PLAYERS, 1.0f, d.kind() == SocketKind.DEVIL ? 0.7f : 1.2f);
    }

    private static SoundEvent sound(SocketDef d) {
        return switch (d.element()) {
            case FIRE -> SoundEvents.BLAZE_SHOOT;
            case WATER -> SoundEvents.PLAYER_SPLASH;
            case WIND -> SoundEvents.PHANTOM_FLAP;
            case EARTH -> SoundEvents.ANVIL_LAND;
            case LIGHT -> SoundEvents.BEACON_ACTIVATE;
            case DARK -> SoundEvents.WITHER_AMBIENT;
        };
    }

    /** Hellflare / Scorching Aura: a cone (Hellflare) or a ring (Nahamah) of fire that burns the foes in it. */
    private static void flame(ServerPlayer p, SocketDef d, float amt) {
        boolean cone = d.kind() == SocketKind.SPIRIT;
        double reach = cone ? 8.0 : 5.0;
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        for (LivingEntity t : Fx.foesAround(p, reach)) {
            if (cone) {
                Vec3 to = t.getBoundingBox().getCenter().subtract(eye);
                double len = to.length();
                if (len > 1.0e-3 && to.scale(1.0 / len).dot(look) < 0.55) continue;     // about 57 degrees either side of the look
            }
            Fx.damage(p, t, amt, "minecraft:on_fire");
            t.setRemainingFireTicks(Math.max(t.getRemainingFireTicks(), 100));
        }
        ServerLevel l = p.serverLevel();
        if (cone) {
            for (int i = 1; i <= 8; i++) {
                Vec3 at = eye.add(look.scale(i));
                com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_SOCKET, FxTone.LIGHT, at.x, at.y - 0.2, at.z, 4, 0.15 * i, 0.15 * i, 0.15 * i, 0.02);
            }
        } else {
            splash(l, p, com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_SOCKET, FxTone.LIGHT), 48, 3.0);
        }
    }

    /** Radiant Mend: heals the caster and teammates nearby and takes the curses off them. */
    private static void mend(ServerPlayer p, float amt, double radius) {
        cleanse(p);
        Fx.heal(p, amt);
        for (LivingEntity f : friends(p, radius)) {
            if (f == p) continue;
            cleanse(f);
            if (f instanceof ServerPlayer sp) Fx.heal(sp, amt * 0.7f);
        }
    }

    /** Curse-Warding Magic: a ward round the caster and the curses taken off every friend near. */
    private static void curseWarding(ServerPlayer p) {
        cleanse(p);
        Fx.shield(p, 6f, 200);
        for (LivingEntity f : friends(p, 8.0)) {
            if (f == p) continue;
            cleanse(f);
            if (f instanceof ServerPlayer) Fx.shield((ServerPlayer) f, 4f, 160);
        }
        splash(p.serverLevel(), p, com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_SOCKET, FxTone.DARK), 36, 2.5);
    }

    // ---------------------------------------------------------------- helpers
    /** The caster and its friends within r (at most 12): teammates, never creative or spectator players. */
    static List<LivingEntity> friends(ServerPlayer p, double r) {
        Vec3 c = p.position();
        List<LivingEntity> out = p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r),
                e -> e.isAlive() && !e.isSpectator() && !(e instanceof Player o && o.isCreative() && o != p) && (e == p || e.isAlliedTo(p)) && e.distanceToSqr(c) <= r * r);
        return out.size() > 12 ? new ArrayList<>(out.subList(0, 12)) : out;
    }

    /** Takes the harmful effects, the Tensura curses, frost and fire off an entity. */
    static void cleanse(LivingEntity t) {
        List<Holder<MobEffect>> bad = new ArrayList<>();
        for (MobEffectInstance e : t.getActiveEffects()) if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) bad.add(e.getEffect());
        for (Holder<MobEffect> h : bad) t.removeEffect(h);
        for (String id : TENSURA_CURSES)
            BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.fromNamespaceAndPath("tensura", id)).ifPresent(t::removeEffect);
        if (t.getTicksFrozen() > 0) t.setTicksFrozen(0);
        if (t.isOnFire()) t.clearFire();
    }

    private static void splash(ServerLevel l, ServerPlayer p, ParticleOptions what, int count, double spread) {
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_SOCKET, com.newuniverse.nusmp.particle.fx.Fx.toneOf(what), p.getX(), p.getY() + 1.0, p.getZ(), count, spread, 0.8, spread, 0.04);
    }
}
