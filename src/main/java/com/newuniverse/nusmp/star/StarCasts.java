package com.newuniverse.nusmp.star;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.book.pages15.Pages15Kit;
import com.newuniverse.nusmp.book.pages15.Pages15Kit.Ctx;
import com.newuniverse.nusmp.book.pages15.Pages15Rules.Tier;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.skilltree.kit.KitAllies;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Star Magic nodes: the casts (docs/vfx_remake/star_ingame.md). Every cast goes through {@link Pages15Kit#cast}, so its damage is {@code Ctx.hit} (the book's balance law, clamped to 1.5 hits of the tier), its
 * targets are at most the tier's number, its delayed parts are {@code Ctx.later} (cancelled when the caster dies or leaves), and nothing here spawns more than the stars, one pane and one pane per player.
 * Effects go through {@link StarFx} (at most ten packets per cast, to nearby players only).
 *
 * <p>Worst case per cast: Seed 1 packet, 1 entity; Conjunction 1 packet, 1 ray, up to 5 box tests, up to 5 allies moved; Scutum 4 or 5 packets, 1 entity; Hasta 5 or 6 packets, 1 segment query; Flagellum 4 or 5
 * packets plus 3, 12 or 15 segment queries over 12 ticks; Black Neverland 1 packet, 5 pulses of one sphere query each (the anti-magic dome adds one shatterBarriers per cancelling pulse, 4 of the 5). The old versions of Conjunction and Flagellum are kept as the fallback.
 */
public final class StarCasts {
    private StarCasts() {}

    private static final String NOT_HEIR = "Only a Grinberryall heir can cast this (a Spade Star grimoire at full mastery, after the Spade Kingdom Raid).";

    private static Vec3 hand(ServerPlayer p) { return p.getEyePosition().add(p.getViewVector(1f).scale(0.6)).add(0, -0.4, 0); }

    private static boolean gate(Ctx c, String id) {
        if (!StarConfig.nodes()) return c.fail("Star nodes are switched off on this server.");
        if (StarAccess.needsHeir(id, StarConfig.vfxForAll()) && !StarGame.isHeir(c.caster)) return c.fail(NOT_HEIR);      // star.vfxForAll: only Black Neverland is heir-only; false = the 3.7.0 table
        return true;
    }

    private static List<StarMath.P> positions(List<StarNodes.Node> nodes) {
        List<StarMath.P> out = new ArrayList<>(nodes.size());
        for (StarNodes.Node n : nodes) out.add(n.pos());
        return out;
    }

    // ------------------------------------------------------------------------------------------------ Star Seed
    public static BookPage.Cast seed() {
        return Pages15Kit.cast(Tier.STARTER, c -> {
            if (!gate(c, StarRules.SEED)) return false;
            StarGame.Placed placed = StarGame.seed(c.caster);
            if (placed.node() == null) return c.fail(placed.problem());
            ServerLevel level = c.caster.serverLevel();
            c.circle(0.5f);
            StarFx.send(new StarFx.Budget(), level, VfxShape.STAR_NODE, hand(c.caster), StarGame.v(placed.node().pos()), 0, 16, 1f);
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------ Conjunction (REPLACES the dash behind star.nodes; the dash is the fallback)
    public static BookPage.Cast conjunction(BookPage.Cast dash) {
        BookPage.Cast nodes = Pages15Kit.cast(Tier.MID, c -> teleport(c, dash));
        return (b, i, p, mode) -> StarConfig.nodes() ? nodes.cast(b, i, p, mode) : dash.cast(b, i, p, mode);
    }

    private static boolean teleport(Ctx c, BookPage.Cast dash) {
        ServerPlayer p = c.caster;
        ServerLevel level = p.serverLevel();
        List<StarNodes.Node> live = StarGame.live(p);
        int idx = StarMath.pickByLook(positions(live), StarGame.p(p.getEyePosition()), StarGame.p(p.getViewVector(1f)), StarRules.PICK_ANGLE_DEG, StarRules.PICK_RANGE);
        if (idx < 0) return StarConfig.conjunctionFallback() ? dash.cast(c.book, c.inst, p, c.mode) : c.fail("No star to go to.");
        StarNodes.Node star = live.get(idx);
        Vec3 ground = StarGame.groundBelow(p, StarGame.v(star.pos()));
        if (ground == null) return c.fail("There is no ground under that star.");
        Vec3 dest = fit(level, p, ground);
        if (dest == null) return c.fail("The ground under that star is blocked.");
        Vec3 from = p.position();
        Vec3 delta = dest.subtract(from);
        List<LivingEntity> bring = alliesToBring(c, p, from);
        StarGame.consume(p, List.of(star));
        c.circle(0.6f);
        p.teleportTo(dest.x, dest.y, dest.z);
        p.fallDistance = 0f;
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, StarRules.EVADE_TICKS, 4, false, false));
        int brought = 0;
        for (LivingEntity ally : bring) {
            if (!level.noCollision(ally, ally.getBoundingBox().move(delta))) continue;
            Vec3 at = ally.position().add(delta);
            ally.teleportTo(at.x, at.y, at.z);
            ally.fallDistance = 0f;
            brought++;
        }
        StarFx.send(new StarFx.Budget(), level, VfxShape.STAR_CONJ, from.add(0, 1, 0), dest.add(0, 1, 0), Math.min(brought, 9), 20, 1f);
        return true;
    }

    /** The point on or beside the wanted one where the caster's box fits: the spot itself, then the four neighbours, each at ground level and one block up. */
    static Vec3 fit(ServerLevel level, ServerPlayer p, Vec3 ground) {
        double[][] offsets = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (double lift : new double[]{0.02, 1.02}) {
            for (double[] o : offsets) {
                Vec3 at = new Vec3(ground.x + o[0], ground.y + lift, ground.z + o[1]);
                if (level.isLoaded(net.minecraft.core.BlockPos.containing(at)) && level.noCollision(p, p.getBoundingBox().move(at.subtract(p.position())))) return at;
            }
        }
        return null;
    }

    /** The allies within 3 blocks (party, team, tamed pets of those: the mod's own ally rule), nearest first, at most the setting and never more than the tier allows. */
    private static List<LivingEntity> alliesToBring(Ctx c, ServerPlayer p, Vec3 centre) {
        int limit = StarRules.allyLimit(StarConfig.conjunctionAllies(), c.tier.maxTargets);
        if (limit <= 0) return List.of();
        List<LivingEntity> near = new ArrayList<>(KitAllies.around(p, centre, StarRules.ALLY_RADIUS));
        near.removeIf(e -> e == p || !e.isAlive() || e.isSpectator());
        near.sort(Comparator.comparingDouble(e -> e.distanceToSqr(centre)));
        return StarRules.firstN(near, limit);
    }

    // ------------------------------------------------------------------------------------------------ the quartet links
    private static void links(StarFx.Budget budget, ServerLevel level, List<StarMath.P> loop, int ticks) {
        for (int i = 0; i < loop.size(); i++) {
            StarMath.P a = loop.get(i), b = loop.get((i + 1) % loop.size());
            StarFx.send(budget, level, VfxShape.STAR_LINK, StarGame.v(a), StarGame.v(b), i, ticks, 1f);
        }
    }

    private static List<StarNodes.Node> take(Ctx c, int n) {
        String[] why = {""};
        List<StarNodes.Node> q = StarGame.quartet(c.caster, n, why);
        if (q == null) c.fail(why[0]);
        return q;
    }

    // ------------------------------------------------------------------------------------------------ Quartile Scutum and Quintile: Atrum Scutum
    public static BookPage.Cast scutum(Tier tier, int stars, int lifeTicks) {
        String id = stars == 4 ? StarRules.SCUTUM : StarRules.QUINTILE_SCUTUM;
        return Pages15Kit.cast(tier, c -> {
            if (!gate(c, id)) return false;
            List<StarNodes.Node> q = take(c, stars);
            if (q == null) return false;
            List<StarMath.P> loop = StarMath.orderAround(positions(q));
            StarMath.Pane geom = StarMath.pane(loop, StarGame.p(c.caster.position()), stars == 4 ? StarMath.PANE_MAX_W : StarMath.PANE_MAX_W_QUINTILE, stars == 4 ? StarMath.PANE_MAX_H : StarMath.PANE_MAX_H_QUINTILE);
            ServerLevel level = c.caster.serverLevel();
            MagicPropEntity body = MagicProps.spawn(level, PropKind.STAR_PANE, StarGame.v(geom.center()), StarRules.entityYawDeg(geom.normalYaw()), 1f, lifeTicks,
                    StarRules.packPane(100, geom.width(), geom.height()), c.caster);
            if (body == null) return c.fail("The pane would not form.");
            List<StarPaneBook.Pane> old = new ArrayList<>(StarGame.PANES.add(body.getUUID(), c.caster.getUUID(), StarGame.dimOf(level), geom, StarRules.panePool(c.tier.hit, stars), level.getGameTime() + lifeTicks));
            StarGame.discardPanes(level.getServer(), old);
            c.circle(0.8f);
            links(new StarFx.Budget(), level, loop, 24);
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------ Quartile Hasta and Quintile: Atrum Hasta
    public static BookPage.Cast hasta(Tier tier, int stars, double width) {
        String id = stars == 4 ? StarRules.HASTA : StarRules.QUINTILE_HASTA;
        return Pages15Kit.cast(tier, c -> {
            if (!gate(c, id)) return false;
            List<StarNodes.Node> q = take(c, stars);
            if (q == null) return false;
            ServerPlayer p = c.caster;
            ServerLevel level = p.serverLevel();
            List<StarMath.P> loop = StarMath.orderAround(positions(q));
            StarMath.P centre = StarMath.centroid(loop);
            float raw = StarRules.hastaRaw(c.tier.hit, stars, c.book.masteryFrac(c.inst), StarGame.isHeir(p));      // the 1.15 stays the heir's (with star.vfxForAll = false only an heir gets here, so it is the 3.7.0 number)
            StarGame.consume(p, q);                                  // the stars overload at once: nothing else can use them while the spear charges
            c.circle(0.8f);
            StarFx.Budget budget = new StarFx.Budget();
            links(budget, level, loop, StarRules.HASTA_CHARGE_TICKS + 4);
            c.later(StarRules.HASTA_CHARGE_TICKS, () -> {
                Vec3 a = StarGame.v(centre);
                Vec3 aim = c.aim(32);
                Vec3 end = aim.distanceTo(a) < 2.0 ? a.add(p.getViewVector(1f).scale(32)) : StarGame.v(StarMath.lanceEnd(centre, StarGame.p(aim), 32));
                for (LivingEntity t : c.foesAlong(a, end, width)) {
                    c.hit(t, raw);
                    Pages15Kit.knock(0.6).apply(t, p);
                }
                StarFx.send(budget, level, VfxShape.STAR_LANCE, a, end, 0, 14, (float) width);
                c.impact(end, 0.8f);
            });
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------ Quartile Flagellum (REPLACES the cone for an heir) and Quintile: Atrum Flagellum
    public static BookPage.Cast flagellum(BookPage.Cast cone) {
        BookPage.Cast whip = Pages15Kit.cast(Tier.STARTER, c -> {
            List<StarNodes.Node> q = StarGame.quartet(c.caster, 4, new String[]{""});
            if (q == null) return cone.cast(c.book, c.inst, c.caster, c.mode);       // no room for four stars: the old cone
            return lash(c, q);
        });
        return (b, i, p, mode) -> StarConfig.nodes() && (StarConfig.vfxForAll() || StarGame.isHeir(p)) ? whip.cast(b, i, p, mode) : cone.cast(b, i, p, mode);      // star.vfxForAll: every Star mage whips; false = only the heir (StarAccess.whip)
    }

    public static BookPage.Cast atrumFlagellum() {
        return Pages15Kit.cast(Tier.ZONE, c -> {
            if (!gate(c, StarRules.QUINTILE_FLAGELLUM)) return false;
            List<StarNodes.Node> q = take(c, 5);
            return q != null && lash(c, q);
        });
    }

    private static boolean lash(Ctx c, List<StarNodes.Node> stars) {
        ServerPlayer p = c.caster;
        ServerLevel level = p.serverLevel();
        StarMath.P hand = StarGame.p(hand(p));
        List<StarMath.P> chain = StarMath.chain(hand, positions(stars));
        List<StarMath.P> legs = new ArrayList<>(chain.size() + 1);
        legs.add(hand);
        legs.addAll(chain);
        StarFx.Budget budget = new StarFx.Budget();
        for (int i = 0; i + 1 < legs.size(); i++) StarFx.send(budget, level, VfxShape.STAR_WHIP, StarGame.v(legs.get(i)), StarGame.v(legs.get(i + 1)), i, 16, 1f);
        c.circle(0.7f);
        Map<UUID, Integer> hits = new HashMap<>();
        float raw = StarRules.whipRaw(c.tier.hit);
        for (int step = 1; step <= StarRules.WHIP_STEPS; step++) {
            final int s = step;
            c.later(4 * step, () -> {
                for (int i = 0; i + 1 < legs.size(); i++) {
                    for (LivingEntity t : c.foesAlong(StarGame.v(legs.get(i)), StarGame.v(legs.get(i + 1)), 0.6)) {
                        if (hits.merge(t.getUUID(), 1, Integer::sum) <= StarRules.WHIP_MAX_HITS_PER_TARGET) c.hit(t, raw);
                    }
                }
                if (s == StarRules.WHIP_STEPS) c.impact(StarGame.v(legs.get(legs.size() - 1)), 0.5f);
            });
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------------ Black Neverland
    /**
     * Black Neverland (manga: Yuno's Star + Sylph Union Magic with Asta's anti-magic, ch. 381 to 383; rules in {@link StarNeverlandRules}). The Grinberryall heir mark is the base gate; while
     * {@code star.neverlandCanonGate} is true the Sylph bond and the anti-magic source are needed too. The dome (8 blocks, 100 ticks, a pulse every 20) weakens and slows the foes inside, ends their spell
     * bolts, quickens and strengthens the caster's allies, and, for an anti-magic caster, cancels the foes' magic (silence, jammed skills, shattered barriers) until the anti-magic fades after 80 ticks.
     */
    public static BookPage.Cast blackNeverland() {
        return Pages15Kit.cast(Tier.ZONE, c -> {
            if (!gate(c, StarRules.BLACK_NEVERLAND)) return false;
            ServerPlayer p = c.caster;
            StarNeverlandRules.Plan plan = StarNeverlandRules.plan(true, StarGame.sylphBond(p), StarGame.antiMagicSource(p), StarConfig.neverlandCanonGate());
            if (!plan.ok()) return c.fail(plan.problem());
            ServerLevel level = p.serverLevel();
            final Vec3 centre = p.position();
            final double radius = StarNeverlandRules.RADIUS;
            final boolean[] anti = {plan.antiMagic()};
            if (!c.zone(100, 20, age -> pulse(c, level, centre, radius, anti[0], age))) return false;
            if (anti[0] && !EnergyBridge.aura(p, StarNeverlandRules.AURA_FRAC, StarNeverlandRules.AURA_FLOOR)) {
                anti[0] = false;                                      // the dome rises, but without the anti-magic: it costs nothing and cancels nothing
                p.displayClientMessage(Component.literal(StarNeverlandRules.NO_AURA + " The dome holds without anti-magic.").withStyle(ChatFormatting.GRAY), true);
            }
            c.circle(1.0f);
            StarFx.send(new StarFx.Budget(), level, VfxShape.STAR_DOME, centre, centre.add(0, 1, 0), 0, 100, (float) radius);
            return true;
        });
    }

    /**
     * One pulse of the dome (every 20 ticks for 5 seconds): the foes inside are weakened and hurt a little, the mage's allies are strengthened, and the magic of others fades (their bolts end). For an
     * anti-magic dome, while it has not faded (age under 80): the foes are silenced, every second pulse their skills are jammed for 3 seconds, and the barriers and stars of others inside shatter
     * ({@code Nullification}, temporary for players: it never touches learned skills, mastery or max EP). Cost per pulse: one foe query (up to 8 targets), one ally query, at most 6 bolts, and for the
     * anti-magic dome one {@code shatterBarriers} (one entity query of the sphere, one pass over the star registry).
     */
    private static void pulse(Ctx c, ServerLevel level, Vec3 centre, double radius, boolean anti, int age) {
        ServerPlayer p = c.caster;
        boolean cancel = anti && StarNeverlandRules.cancelsAt(age);
        boolean jam = cancel && StarNeverlandRules.jamsAt(age);
        for (LivingEntity t : c.foes(centre, radius)) {
            c.hit(t, c.tier.pulse * 0.6f);
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
            if (cancel) {
                EnergyBridge.effect(t, "silence", StarNeverlandRules.SILENCE_TICKS, 0);
                if (jam) Nullification.jamAll(t, StarNeverlandRules.JAM_SECONDS, StarNeverlandRules.JAM_MISFIRE);
            }
        }
        if (cancel) Nullification.shatterBarriers(level, centre, radius, p);
        SpellRuntime.dissolveBolts(level, centre, radius, p, 6);
        List<LivingEntity> friends = new ArrayList<>(KitAllies.around(p, centre, radius));
        friends.removeIf(e -> !e.isAlive() || e.isSpectator());
        friends.sort(Comparator.comparingDouble(e -> e.distanceToSqr(centre)));
        for (LivingEntity f : StarRules.firstN(friends, c.tier.maxTargets)) {
            f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, false, false));
            f.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, false, false));
        }
    }
}
