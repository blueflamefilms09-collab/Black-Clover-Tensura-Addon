package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 1.93.0 Slash Magic rebuild (Black Clover: Jack the Ripper's Slash Magic: mana blades that cut anything, long blades from the forearms, projected slashes, and
 * blades that adapt to what they cut, "even space, healing and gravity"). The four old pages keep their ids and order (SlashBook): they gain the Ripper's armour
 * (client.aura.SlashAura), the rebuilt effects (SLASH_FX1..3, SlashRipFxLayer) and the summon models (PropKind.SLASH_1: the crescent, or with param 1 the dash
 * afterimage; SLASH_2: the Death Scythe). This class holds what is new:
 * <ul>
 *   <li><b>Death Scythe: Lunatic Slash</b> (wiki, appended): the forearm blades throw a chaotic flurry of ten flying slashes in 20 ticks over a 80 degree cone,
 *       each a piercing crescent for 6 x EP scale and an adaptive cut; uses aura.</li>
 *   <li><b>Adaptive Edge</b> (appended): the blades read the foe at once: three adaptation marks are laid on it, the barriers within 5 blocks of it are cut
 *       down and its magic is jammed for 4 s (a boss is jammed half as likely and keeps its own capped damage).</li>
 *   <li>The Ripper Dash leaves {@link #afterimages} along its path.</li>
 * </ul>
 */
public final class SlashWorks {
    private SlashWorks() {}

    private static final int GREEN = 0xFF48FF7A;

    /** Glass afterimages of the dasher every 1.6 blocks from {@code from} to {@code to} (they fade over 14 ticks). */
    static void afterimages(ServerPlayer p, Vec3 from, Vec3 to) {
        ServerLevel sl = p.serverLevel();
        Vec3 d = to.subtract(from);
        double len = d.length();
        int n = (int) Math.min(7, Math.max(1, len / 1.6));
        for (int k = 0; k < n; k++) {
            Vec3 at = from.add(d.scale((k + 0.5) / n));
            MagicProps.spawn(sl, PropKind.SLASH_1, at, p.getYRot(), 1f, 14 + k, 1, p);
        }
    }

    /** Death Scythe: Lunatic Slash: a flurry of flying slashes. */
    static boolean lunaticSlash(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!EnergyBridge.aura(p, 0.03, 10)) { GrimoireBook.fail(p, "Not enough aura to throw the flurry."); return false; }
        ServerLevel sl = p.serverLevel();
        float pw = EnergyBridge.scale(p);
        b.castCircle(p, 1.4f);
        VfxSpawn.send(sl, VfxShape.SLASH_FX2, p.position(), p.position().add(0, 1, 0), GREEN, 26, 3.2f);
        for (int k = 0; k < 10; k++) {
            SpellRuntime.later(sl, k * 2, () -> {
                if (!p.isAlive()) return;
                Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
                double yaw = Math.atan2(look.z, look.x) + (p.getRandom().nextDouble() - 0.5) * Math.toRadians(80);
                double pitch = (p.getRandom().nextDouble() - 0.5) * 0.5;
                Vec3 dir = new Vec3(Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch), Math.sin(yaw) * Math.cos(pitch)).normalize();
                Vec3 start = eye.add(dir.scale(0.8));
                float yawDeg = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
                MagicProps.spawn(sl, PropKind.SLASH_1, start.subtract(0, 0.2, 0), yawDeg, 0.8f * pw, 9, 0, p);
                VfxSpawn.send(sl, VfxShape.SLASH_FX1, eye.add(0, -0.25, 0), start.add(dir.scale(7)), GREEN, 8, 0.8f * pw);
                SpellRuntime.bolt(p, start, dir.scale(1.15), 1.0, 9, true, null, (bolt, t) -> SlashBook.cut(b, i, p, t, mode, 6f), null);
                sl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 0.9f + p.getRandom().nextFloat() * 0.5f);
            });
        }
        return true;
    }

    /** Adaptive Edge: the blades read the foe and cut its defences down at once. */
    static boolean adaptiveEdge(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 14);
        if (t == null) { GrimoireBook.fail(p, "No one to read."); return false; }
        ServerLevel sl = p.serverLevel();
        CompoundTag d = t.getPersistentData();
        d.putInt(SlashBook.K_ADAPT, Math.min(10, d.getInt(SlashBook.K_ADAPT) + 3));
        Vec3 c = t.getBoundingBox().getCenter();
        Nullification.shatterBarriers(sl, c, 5, p);
        Nullification.interfere(t, BalanceLaw.isBoss(t) ? 2 : 4, BalanceLaw.isBoss(t) ? 0.5f : 1f, 0f);
        SlashBook.cut(b, i, p, t, mode, 6f);
        VfxSpawn.send(sl, VfxShape.SLASH_FX3, c, c.add(p.getViewVector(1f)), GREEN, 24, 1.2f * EnergyBridge.scale(p));
        VfxSpawn.send(sl, VfxShape.SLASH_FX1, p.getEyePosition().add(0, -0.25, 0), c, GREEN, 10, 1.0f);
        b.castCircle(p, 0.9f);
        return true;
    }

    /** The appended pages of the book (SlashExt hands them to the book). */
    public static List<BookPage> pages() {
        return List.of(
                BookPage.signature("lunatic_slash", "Death Scythe: Lunatic Slash", SpatialArmourCast.armoured(SlashWorks::lunaticSlash, Aura.SLASH, 1)).withCooldown(700),
                BookPage.mid("adaptive_edge", "Adaptive Edge", SpatialArmourCast.armoured(SlashWorks::adaptiveEdge, Aura.SLASH, 0)).withCooldown(240));
    }
}
