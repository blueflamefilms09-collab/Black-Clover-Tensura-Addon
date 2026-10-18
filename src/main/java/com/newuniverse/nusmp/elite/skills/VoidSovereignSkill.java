package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.elite.skills.rules.UltimateRules;
import com.newuniverse.nusmp.skill.EliteBridge;
import com.newuniverse.nusmp.skill.SkillMsg;
import com.newuniverse.nusmp.skill.SkillText;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Void Sovereign (Ultimate, designed for this mod; the original addon's source was not available): the one who nullifies space. Mode 1, Void Domain: for ten seconds (fifteen mastered) a sphere of
 * nothing surrounds you (10 blocks, 12 mastered): foes inside it cannot teleport (ender pearls, Endermen, chorus fruit; commands are not affected), and every foreign projectile that enters it is
 * erased ({@code voidNullifyTeleport}, {@code voidNullifyProjectiles} in the config). Mode 2, Void Step: step 20 blocks along your sight to the last free spot (the void behind you stays for a
 * heartbeat) and cut everything on the line for armour-ignoring damage. Mode 3, Collapse: space itself closes on the foes within seven blocks: each loses 12% of its maximum health (at least
 * 20 damage; a boss only a fifth of that; capped for players by the balance law). Numbers: {@link UltimateRules.VoidSovereign}. Void Sovereign is the evolution of Void Edge in
 * data/nusmp/elite/skills/evolutions.json. The teleport and projectile rules are applied by {@link EliteSkillEvents} through {@link #nullifiesTeleport} and {@link #domainTick}.
 */
public class VoidSovereignSkill extends EliteSkill {
    static final SkillText TEXT = SkillText.of("void_sovereign", "Void Sovereign",
            "Ultimate. Void Domain: foes inside cannot teleport and foreign projectiles are erased. Void Step cuts a line of space while you cross it. Collapse closes space on the foes around you.");

    private record Domain(long until, double radius) {}

    private static final Map<UUID, Domain> DOMAINS = new HashMap<>();

    public VoidSovereignSkill() { super(SkillType.ULTIMATE, TEXT, true); }

    @Override
    protected List<EliteMode> modes() {
        return List.of(EliteMode.of("void_sovereign.domain", "Void Domain", UltimateRules.VoidSovereign.DOMAIN_COST, UltimateRules.VoidSovereign.DOMAIN_COOLDOWN),
                EliteMode.of("void_sovereign.step", "Void Step", UltimateRules.VoidSovereign.STEP_COST, UltimateRules.VoidSovereign.STEP_COOLDOWN),
                EliteMode.of("void_sovereign.collapse", "Collapse", UltimateRules.VoidSovereign.COLLAPSE_COST, UltimateRules.VoidSovereign.COLLAPSE_COOLDOWN));
    }

    @Override protected int color() { return 0xFF3A2A6A; }

    @Override
    protected boolean use(ManasSkillInstance instance, ServerPlayer player, int mode) {
        boolean ownMastered = masteredBy(instance, player);
        ServerLevel level = player.serverLevel();
        double mastery = masteryFrac(instance);
        switch (mode) {
            case 0 -> {
                int ticks = UltimateRules.VoidSovereign.domainTicks(ownMastered);
                double radius = UltimateRules.VoidSovereign.domainRadius(ownMastered);
                DOMAINS.put(player.getUUID(), new Domain(level.getGameTime() + ticks, radius));
                EliteBridge.follow(level, VfxShape.NIHILITY_ZONE, player, player.position().add(0, 1, 0), 0xFF3A2A6A, ticks, (float) (radius / 5.0));
                EliteBridge.burst(level, player.position().add(0, 1, 0), true, 18, radius * 0.3);
                EliteBridge.sound(level, player.position(), SoundEvents.END_PORTAL_SPAWN, 0.6f, 1.6f);
                EliteBridge.actionbar(player, SkillMsg.t("void_sovereign.domain", "Space is nullified around you.").withStyle(ChatFormatting.DARK_PURPLE));
                return true;
            }
            case 1 -> {
                Vec3 start = player.position();
                Vec3 eye = player.getEyePosition();
                Vec3 look = player.getViewVector(1.0F);
                Vec3 target = EliteBridge.lookPoint(player, UltimateRules.VoidSovereign.STEP_RANGE);
                Vec3 dest = null;
                double total = target.distanceTo(eye);
                for (double back = 0.6; back <= total && dest == null; back += 0.5) {                    // walk back from the wall to the first spot the body fits
                    Vec3 c = eye.add(look.scale(Math.max(0.0, total - back)));
                    Vec3 feet = new Vec3(c.x, c.y - player.getEyeHeight(), c.z);
                    if (level.noCollision(player, player.getBoundingBox().move(feet.subtract(start)))) dest = feet;
                }
                if (dest == null || dest.distanceToSqr(start) < 4.0) {
                    EliteBridge.fail(player, SkillMsg.t("void_sovereign.blocked", "There is no room to step."));
                    return false;
                }
                for (LivingEntity t : EliteBridge.foesAlong(player, eye, dest.add(0, player.getEyeHeight(), 0), UltimateRules.VoidSovereign.STEP_WIDTH)) {
                    float dmg = BalanceLaw.damage(player, t, (float) (UltimateRules.VoidSovereign.STEP_DAMAGE * EliteSkillsConfig.damageScale()), mastery);
                    t.hurt(player.damageSources().indirectMagic(player, player), dmg);
                }
                EliteBridge.shape(level, VfxShape.SPACE_TEAR, start.add(0, 1, 0), dest.add(0, 1, 0), 0xFF3A2A6A, 16, 1.0f);
                EliteBridge.burst(level, start.add(0, 1, 0), true, 10, 0.5);
                player.teleportTo(dest.x, dest.y, dest.z);
                player.fallDistance = 0.0f;
                EliteBridge.burst(level, dest.add(0, 1, 0), true, 10, 0.5);
                EliteBridge.sound(level, dest, SoundEvents.ENDERMAN_TELEPORT, 0.8f, 0.5f);
                return true;
            }
            default -> {
                Vec3 at = player.position();
                int hit = 0;
                for (LivingEntity t : EliteBridge.foes(player, at, UltimateRules.VoidSovereign.COLLAPSE_RADIUS)) {
                    double dmg = UltimateRules.VoidSovereign.collapseDamage(t.getMaxHealth(), BalanceLaw.isBoss(t)) * EliteSkillsConfig.damageScale();
                    t.hurt(player.damageSources().indirectMagic(player, player), BalanceLaw.cap(t, (float) dmg));
                    hit++;
                }
                if (hit == 0) {
                    EliteBridge.fail(player, SkillMsg.t("void_sovereign.nobody", "Nobody is within the collapse."));
                    return false;
                }
                EliteBridge.shape(level, VfxShape.SPACE_SHATTER, at, at.add(0, 1, 0), 0xFF3A2A6A, 24, (float) (UltimateRules.VoidSovereign.COLLAPSE_RADIUS / 4.0));
                EliteBridge.burst(level, at.add(0, 1, 0), true, 20, 1.5);
                EliteBridge.sound(level, at, SoundEvents.GENERIC_EXPLODE.value(), 0.7f, 0.5f);
                EliteBridge.actionbar(player, SkillMsg.t("void_sovereign.collapse", "Space closes on %s foes.", hit).withStyle(ChatFormatting.DARK_PURPLE));
                return true;
            }
        }
    }

    // ---------------------------------------------------------------- the domain (used by the elite handlers)
    /** The owner of a live domain that nullifies teleporting of this entity (a foe of the owner, inside the sphere), or null. */
    static ServerPlayer nullifiesTeleport(MinecraftServer server, Entity who, Vec3 from, Vec3 to) {
        if (DOMAINS.isEmpty() || server == null || !(who instanceof LivingEntity living)) return null;
        for (var en : DOMAINS.entrySet()) {
            ServerPlayer owner = server.getPlayerList().getPlayer(en.getKey());
            if (owner == null || owner == who || owner.level() != who.level() || owner.level().getGameTime() >= en.getValue().until()) continue;
            double r = en.getValue().radius();
            if (!EliteBridge.isFoe(owner, living)) continue;
            Vec3 c = owner.position();
            if (from.distanceToSqr(c) <= r * r || to.distanceToSqr(c) <= r * r) return owner;
        }
        return null;
    }

    /** Every second tick for an owner: erases foreign projectiles inside a live domain; drops the domain when it has run out. */
    static void domainTick(ServerPlayer owner) {
        Domain d = DOMAINS.get(owner.getUUID());
        if (d == null) return;
        long now = owner.level().getGameTime();
        if (now >= d.until() || !owner.isAlive()) {
            DOMAINS.remove(owner.getUUID());
            return;
        }
        if (!EliteSkillsConfig.get(EliteSkillsConfig.VOID_NULLIFY_PROJECTILES)) return;
        AABB box = owner.getBoundingBox().inflate(d.radius());
        ServerLevel level = owner.serverLevel();
        for (Projectile p : level.getEntitiesOfClass(Projectile.class, box, e -> e.isAlive() && e.position().distanceToSqr(owner.position()) <= d.radius() * d.radius())) {
            Entity shooter = p.getOwner();
            if (shooter == owner || (shooter instanceof LivingEntity l && !EliteBridge.isFoe(owner, l))) continue;      // yours and your side's pass
            EliteBridge.burst(level, p.position(), true, 4, 0.2);
            p.discard();
        }
    }

    static void forget(UUID id) { DOMAINS.remove(id); }

    static void clearAll() { DOMAINS.clear(); }

    static int live() { return DOMAINS.size(); }
}
