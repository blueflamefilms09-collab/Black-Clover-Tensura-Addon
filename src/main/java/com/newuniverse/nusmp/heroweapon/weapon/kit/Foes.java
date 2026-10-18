package com.newuniverse.nusmp.heroweapon.weapon.kit;

import com.newuniverse.nusmp.arc.ArcNpcEntity;
import com.newuniverse.nusmp.avatar.FtbTeamHelper;
import com.newuniverse.nusmp.corpse.Corpses;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.soul.Paladins;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Why: the hero weapons' techniques strike "foes", and the owner's rule is exact about who is not one: never a creative or spectator player, never an ally, never
 * the wielder's own summons or tamed pets, never the wielder. {@code HeroCtx.around} already drops the wielder, the dead, spectators and anything {@code isAlliedTo};
 * the rest of the rule (creative players, armour stands, villagers and the friendly characters of the arcs, a pet or a body or a Paladin that belongs to the
 * wielder or to somebody on the wielder's team, a relic summon, a friendly player of the wielder's FTB team or party) lives here, once, so that no technique of
 * the four weapons can forget it. Every query returns at most {@link Budget#MAX_TARGETS} foes, nearest first. Server thread only.
 */
public final class Foes {
    private Foes() {}

    /** The persistent-data key a relic summon carries (it is package-private in {@code relics.Fx}); a relic summon, of anyone, is never a target. */
    private static final String RELIC_OWNER = "nusmp_relic_owner";

    /** True when {@code e} may be struck by a technique of {@code p}'s weapon. */
    public static boolean isFoe(ServerPlayer p, LivingEntity e) {
        if (e == null || e == p || !e.isAlive() || e.isSpectator()) return false;
        if (e instanceof ArmorStand || e instanceof Npc || e instanceof ArcNpcEntity) return false;
        if (e.isAlliedTo(p)) return false;
        if (e instanceof Player o) return !o.isCreative() && p.canHarmPlayer(o) && !FtbTeamHelper.sameTeam(p.getUUID(), o.getUUID());
        return !belongsToSide(p, e);
    }

    /** True when {@code e} is a pet, a summon, a body or a Paladin of the wielder, of a relic, or of somebody on the wielder's team. */
    public static boolean belongsToSide(ServerPlayer p, LivingEntity e) {
        UUID me = p.getUUID();
        if (e.getPersistentData().hasUUID(RELIC_OWNER)) return true;
        if (e instanceof OwnableEntity own && own.getOwnerUUID() != null && mine(me, own.getOwnerUUID())) return true;
        UUID corpse = Corpses.ownerOf(e);
        if (corpse != null && mine(me, corpse)) return true;
        UUID paladin = Paladins.ownerOf(e);
        return paladin != null && mine(me, paladin);
    }

    private static boolean mine(UUID me, UUID owner) { return me.equals(owner) || FtbTeamHelper.sameTeam(me, owner); }

    /** True when {@code e} should be helped by a heal or a buff of {@code p}: the wielder, an ally, a teammate, a pet or a summon of the wielder's side. */
    public static boolean isAlly(ServerPlayer p, LivingEntity e) {
        if (e == null || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand) return false;
        if (e == p || e.isAlliedTo(p)) return true;
        if (e instanceof Player o) return FtbTeamHelper.sameTeam(p.getUUID(), o.getUUID());
        UUID me = p.getUUID();
        if (e instanceof OwnableEntity own && own.getOwnerUUID() != null && mine(me, own.getOwnerUUID())) return true;
        UUID corpse = Corpses.ownerOf(e);
        return corpse != null && mine(me, corpse);
    }

    /** Foes within {@code r} of {@code center}, nearest first, at most {@code max}. */
    public static List<LivingEntity> around(HeroCtx c, Vec3 center, double r, int max) {
        List<LivingEntity> list = new ArrayList<>(c.around(center, r));
        list.removeIf(e -> !isFoe(c.player, e));
        list.sort(Comparator.comparingDouble(e -> e.distanceToSqr(center)));
        return cap(list, max);
    }

    /** Foes within {@code width} of the segment a to b, nearest to a first, at most {@code max}. */
    public static List<LivingEntity> along(HeroCtx c, Vec3 a, Vec3 b, double width, int max) {
        List<LivingEntity> list = new ArrayList<>(c.along(a, b, width));
        list.removeIf(e -> !isFoe(c.player, e));
        list.sort(Comparator.comparingDouble(e -> e.getBoundingBox().getCenter().distanceToSqr(a)));
        return cap(list, max);
    }

    /** Foes within {@code range} of the wielder and within {@code halfAngleDeg} of the look direction, nearest first, at most {@code max}. */
    public static List<LivingEntity> cone(HeroCtx c, double range, double halfAngleDeg, int max) {
        List<LivingEntity> list = new ArrayList<>(c.cone(range, halfAngleDeg));
        list.removeIf(e -> !isFoe(c.player, e));
        Vec3 from = c.player.position();
        list.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));
        return cap(list, max);
    }

    /** The first foe on the look line within {@code range} (a wall stops the line), or null. {@code width} is how far from the line a foe may stand. */
    public static LivingEntity aimed(HeroCtx c, double range, double width) {
        Vec3 from = c.eye();
        Vec3 end = Rays.reach(c, from, c.look(), range).end();
        List<LivingEntity> hit = along(c, from, end, width, 1);
        return hit.isEmpty() ? null : hit.get(0);
    }

    /** The wielder first, then allies (players of the team, pets, summons) within {@code r} of {@code center}, nearest first, at most {@code max} in all. */
    public static List<LivingEntity> alliesAround(HeroCtx c, Vec3 center, double r, int max) {
        List<LivingEntity> list = new ArrayList<>(c.level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(r),
                e -> e != c.player && e.distanceToSqr(center) <= r * r && isAlly(c.player, e)));
        list.sort(Comparator.comparingDouble(e -> e.distanceToSqr(center)));
        List<LivingEntity> out = new ArrayList<>();
        out.add(c.player);
        out.addAll(list);
        return cap(out, max);
    }

    private static List<LivingEntity> cap(List<LivingEntity> list, int max) {
        int n = Math.max(0, Math.min(max, Budget.MAX_TARGETS));
        return list.size() > n ? new ArrayList<>(list.subList(0, n)) : list;
    }
}
