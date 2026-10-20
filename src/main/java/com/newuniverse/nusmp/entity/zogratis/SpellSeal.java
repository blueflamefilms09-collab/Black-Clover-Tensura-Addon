package com.newuniverse.nusmp.entity.zogratis;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.data.TensuraTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.joml.Vector3f;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.IntConsumer;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * Spatial Mana Domination (Absolute Space): the cube in which Zenon Zogratis severs everyone else's connection to mana. While a
 * cube stands, every living thing inside it that is not of the Zogratis family is <b>sealed</b>:
 * <ul>
 *   <li><b>grimoire spells</b> cannot be cast: the cube writes the mod's own {@code nusmp_sealed_until} flag into each player inside
 *       every two ticks (so the cast path refuses at once, with the sealing message, even with no other hook), and
 *       {@link #sealed(LivingEntity)} is the exact one-line check for the top of {@code GrimoireBook#castPage};</li>
 *   <li><b>Tensura and ManasCore skills</b> cannot be activated, released or toggled ({@link #vetoesSkill}, bound to ManasCore's skill
 *       events from NUSMP). Grimoire pages are exempt here because the cast path above already refuses them, with the right message,
 *       and leaves the grimoire's utility pages (summon, store a weapon) alone;</li>
 *   <li><b>spell damage</b> a sealed attacker still manages to land (a spell already in flight, a summon's spell) is cut to a fifth
 *       ({@link #onIncomingDamage}).</li>
 * </ul>
 * Weapons, items, movement and ordinary blows are untouched, and the Zogratis family is exempt (it is Zenon's space: his own bone and
 * spatial magic keep working inside).
 *
 * <p><b>Counter-play.</b> The cube is telegraphed by the move's wind-up, has a size and a duration ({@link DomainMath}), closes for
 * {@link DomainClock#FORM_TICKS} ticks before the seal bites, and its twelve edges are drawn in dust the whole time, with a shimmer
 * on the wall nearest a sealed player and an action-bar line naming the way out. It can be left on foot in the time the telegraph
 * gives. And it can be broken: every point of damage dealt to Zenon while it stands wears it down ({@link DomainIntegrity}, anti-magic
 * counts double), from inside or outside. It also ends with its master.
 *
 * <p>State is server side and in memory only (a world reload ends every cube); the cubes belong to the level they were opened in.
 */
public final class SpellSeal {
    private SpellSeal() {}

    /** What a sealed caster is told when a spell is refused. */
    public static final String MESSAGE = "Spatial Mana Domination has severed your mana. Leave the cube, or break the domain.";
    /** The mod's own grimoire-sealing flag (a game tick until which a player's grimoire is shut). */
    public static final String SEALED_FLAG = "nusmp_sealed_until";
    /** Spell damage a sealed attacker deals gets this factor. */
    public static final float SPELL_FACTOR = 0.2f;

    private static final String NOTE_KEY = "nusmp_seal_note";
    private static final int STEP = 2, MAX_CUBES = 6, EDGE_PERIOD = 4, FLAG_TICKS = 4;
    private static final double EDGE_SPACING = 1.6, SHIMMER_REACH = 3.5;
    private static final int INDIGO = DevilStyle.color(Devil.BEELZEBUB), ACCENT = DevilStyle.accent(Devil.BEELZEBUB);
    private static final Random FX = new Random();

    /** One standing cube. */
    private static final class Cube {
        final WeakReference<ServerLevel> level;
        final DomainCube geo;
        final DomainClock clock;
        final long start;
        final UUID source;
        final DomainIntegrity integrity;
        final Set<UUID> inside = new HashSet<>(), warned = new HashSet<>();
        volatile boolean ended;

        Cube(ServerLevel level, DomainCube geo, DomainClock clock, UUID source, DomainIntegrity integrity) {
            this.level = new WeakReference<>(level);
            this.geo = geo;
            this.clock = clock;
            this.start = level.getGameTime();
            this.source = source;
            this.integrity = integrity;
        }

        int age(Level at) { return (int) (at.getGameTime() - start); }

        boolean sealingNow(Level at) { return !ended && clock.sealing(age(at)); }
    }

    private static final CopyOnWriteArrayList<Cube> CUBES = new CopyOnWriteArrayList<>();

    // ================================================================ the questions
    /**
     * Is this entity inside a cube whose seal has bitten? False for the Zogratis family, for spectators, for the dead, and always on
     * the client. The check is a handful of comparisons per standing cube (usually none).
     */
    public static boolean sealed(LivingEntity e) { return cubeOf(e) != null; }

    /** Ticks of seal left for an entity inside a cube (the whole remaining seal), or 0 if it is not sealed. */
    public static int sealLeft(LivingEntity e) {
        Cube c = cubeOf(e);
        return c == null ? 0 : c.clock.sealLeft(c.age(e.level()));
    }

    private static Cube cubeOf(LivingEntity e) {
        if (e == null || CUBES.isEmpty() || e.level().isClientSide || !e.isAlive() || e.isSpectator() || ZogratisEntities.isFamily(e)) return null;
        Level lv = e.level();
        double y = e.getY() + e.getBbHeight() * 0.5;
        for (Cube c : CUBES) if (c.level.get() == lv && c.sealingNow(lv) && c.geo.contains(e.getX(), y, e.getZ())) return c;
        return null;
    }

    /** A cube that has not ended and whose time is not up (a cube whose own tasks died still stops counting when its clock runs out). */
    private static boolean live(Cube c) {
        ServerLevel l = c.level.get();
        return !c.ended && l != null && !c.clock.over(c.age(l));
    }

    /** Does this caster have a cube standing (closing or sealing)? */
    public static boolean active(UUID source) {
        for (Cube c : CUBES) if (live(c) && c.source.equals(source)) return true;
        return false;
    }

    /** Number of standing cubes (for commands and tests). */
    public static int standing() {
        int n = 0;
        for (Cube c : CUBES) if (live(c)) n++;
        return n;
    }

    // ================================================================ opening and closing a cube
    /**
     * Opens a cube for 'ticks' ticks for the caster 'source' (a boss's UUID). 'centre' is the middle of the cube on the ground: the
     * cube is centred on it horizontally, its floor lies {@link DomainCube#BELOW_FLOOR} blocks under {@code centre.y} and it reaches
     * {@code 2 * half - 3} blocks above it, so a point on the ground is well inside. A caster has one cube at a time (a new one
     * replaces the old). The cube ends when its time is up, when the caster dies or leaves, or when it is broken.
     */
    public static void seal(ServerLevel level, Vec3 centre, double half, int ticks, UUID source) {
        if (level == null || centre == null || source == null) return;
        purge();
        clear(source);
        if (CUBES.size() >= MAX_CUBES) return;
        DomainCube geo = DomainCube.around(centre.x, centre.y, centre.z, half);
        DomainClock clock = DomainClock.of(ticks);
        double max = 120;
        if (level.getEntity(source) instanceof ZogratisBoss b) max = DomainIntegrity.maxFor(b.getMaxHealth(), b.power());
        Cube c = new Cube(level, geo, clock, source, new DomainIntegrity(max));
        CUBES.add(c);
        Vec3 floor = new Vec3(geo.cx(), geo.minY(), geo.cz());
        VfxSpawn.send(level, VfxShape.SPACE_CUBE, floor, floor.add(0, 1, 0), 0xFF000000 | ACCENT, DomainClock.FORM_TICKS + 8, (float) geo.half());
        level.playSound(null, BlockPos.containing(geo.cx(), geo.cy(), geo.cz()), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 3f, 0.5f);
        level.playSound(null, BlockPos.containing(geo.cx(), geo.cy(), geo.cz()), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 3f, 0.4f);
        SpellRuntime.zone(level, clock.total() + STEP, STEP, safe("domain", age -> step(c, level)));
        SpellRuntime.later(level, clock.total(), safe("domain end", () -> end(c, level, "Spatial Mana Domination fades. Mana flows again.")));
    }

    /** Ends every cube this caster has, at once (it died, was reset, or leaves the fight). */
    public static void clear(UUID source) {
        if (source == null) return;
        for (Cube c : CUBES) {
            if (c.ended || !c.source.equals(source)) continue;
            ServerLevel sl = c.level.get();
            if (sl == null) { c.ended = true; CUBES.remove(c); }
            else end(c, sl, "Spatial Mana Domination collapses. Mana flows again.");
        }
    }

    /** Wears the caster's cube down by a blow of 'amount' dealt to the caster; true if this blow shattered it. */
    public static boolean hurtDomain(UUID source, float amount, boolean antiMagic) {
        if (source == null) return false;
        for (Cube c : CUBES) {
            if (!live(c) || !c.source.equals(source)) continue;
            if (!c.integrity.hit(amount, antiMagic)) return false;
            ServerLevel sl = c.level.get();
            if (sl == null) return false;
            Vec3 mid = new Vec3(c.geo.cx(), c.geo.cy(), c.geo.cz());
            sl.playSound(null, BlockPos.containing(mid), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3f, 0.5f);
            Tells.burst(sl, mid, ACCENT, 140);
            end(c, sl, "Spatial Mana Domination shatters! Mana flows again.");
            return true;
        }
        return false;
    }

    /** How much of the caster's cube is left, 1 untouched .. 0 broken (0 if there is none). */
    public static double integrity(UUID source) {
        for (Cube c : CUBES) if (live(c) && c.source.equals(source)) return c.integrity.fraction();
        return 0;
    }

    private static void purge() {
        for (Cube c : CUBES) {
            ServerLevel l = c.level.get();
            if (c.ended || l == null || c.clock.over(c.age(l) - 40)) CUBES.remove(c);
        }
    }

    private static void end(Cube c, ServerLevel sl, String why) {
        if (c.ended) return;
        c.ended = true;
        CUBES.remove(c);
        Vec3 mid = new Vec3(c.geo.cx(), c.geo.cy(), c.geo.cz());
        Tells.burst(sl, mid, ACCENT, 60);
        for (UUID id : c.inside) {
            if (sl.getPlayerByUUID(id) instanceof ServerPlayer p) p.displayClientMessage(Component.literal("Your mana returns.").withStyle(ChatFormatting.AQUA), true);
        }
        c.inside.clear();
        if (why != null && sl.getEntity(c.source) instanceof ZogratisBoss b) b.announce(why);
    }

    // ================================================================ the cube, every two ticks
    private static void step(Cube c, ServerLevel sl) {
        if (c.ended) return;
        int age = c.age(sl);
        if (!(sl.getEntity(c.source) instanceof LivingEntity master) || !master.isAlive() || master.isRemoved()) {
            end(c, sl, "Spatial Mana Domination collapses with its master. Mana flows again.");
            return;
        }
        if (c.clock.over(age)) {
            end(c, sl, "Spatial Mana Domination fades. Mana flows again.");
            return;
        }
        boolean forming = c.clock.forming(age), sealing = c.clock.sealing(age);
        drawWalls(c, sl, age, forming);
        long now = sl.getGameTime();
        for (ServerPlayer p : sl.players()) {
            double d2 = p.distanceToSqr(c.geo.cx(), c.geo.cy(), c.geo.cz());
            double reach = c.geo.half() * 1.8 + 4;
            boolean in = d2 < reach * reach && p.isAlive() && !p.isSpectator() && !ZogratisEntities.isFamily(p)
                    && c.geo.contains(p.getX(), p.getY() + p.getBbHeight() * 0.5, p.getZ());
            if (!in) {
                if (c.inside.remove(p.getUUID()) && sealing) p.displayClientMessage(Component.literal("Your mana returns.").withStyle(ChatFormatting.AQUA), true);
                continue;
            }
            boolean note = age % 10 < STEP;
            if (forming) {
                if (note) p.displayClientMessage(Component.literal("The cube is closing! Leave it now: " + exitOf(c, p) + ".").withStyle(ChatFormatting.AQUA), true);
                shimmer(c, sl, p);
                continue;
            }
            if (!sealing) continue;
            if (c.inside.add(p.getUUID())) {
                if (c.warned.add(p.getUUID())) {
                    p.sendSystemMessage(Component.literal(MESSAGE).withStyle(ChatFormatting.AQUA));
                    p.sendSystemMessage(Component.literal("Grimoire spells, Tensura skills and spell damage are negated inside. Weapons, items and movement still work. Nearest way out: "
                            + exitOf(c, p) + ".").withStyle(ChatFormatting.DARK_AQUA));
                }
                sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5f, 0.5f);
            }
            long cur = p.getPersistentData().getLong(SEALED_FLAG);
            if (cur < now + FLAG_TICKS) p.getPersistentData().putLong(SEALED_FLAG, now + FLAG_TICKS);
            if (note) {
                p.displayClientMessage(Component.literal("Mana severed: " + Math.max(1, (c.clock.sealLeft(age) + 19) / 20) + " s. Way out: " + exitOf(c, p) + ". Hurt its caster to break it.")
                        .withStyle(ChatFormatting.AQUA), true);
            }
            shimmer(c, sl, p);
        }
    }

    private static String exitOf(Cube c, ServerPlayer p) { return c.geo.exitHint(p.getX(), p.getY() + p.getBbHeight() * 0.5, p.getZ()); }

    // ================================================================ the walls
    private static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f(DevilStyle.red(rgb), DevilStyle.green(rgb), DevilStyle.blue(rgb)), size);
    }

    /** The twelve edges in dust (all of them while the cube closes, three at a time after, so each is redrawn every 16 ticks), and warped-space sparks on the faces. */
    private static void drawWalls(Cube c, ServerLevel sl, int age, boolean forming) {
        DomainCube g = c.geo;
        if (age % EDGE_PERIOD < STEP) {
            DustParticleOptions edge = dust(forming ? ACCENT : INDIGO, forming ? 1.9f : 1.6f);
            int per = forming ? DomainCube.EDGES : 3, first = forming ? 0 : (age / EDGE_PERIOD * 3) % DomainCube.EDGES;
            for (int k = 0; k < per; k++)
                for (double[] p : g.edgeSamples((first + k) % DomainCube.EDGES, EDGE_SPACING)) sl.sendParticles(edge, p[0], p[1], p[2], 1, 0, 0, 0, 0);
        }
        for (int i = 0; i < 4; i++) {
            int face = FX.nextInt(6);
            double u = (FX.nextDouble() * 2 - 1) * g.half(), v = (FX.nextDouble() * 2 - 1) * g.half();
            double[] p = switch (face) {
                case DomainCube.WEST -> new double[]{g.minX(), g.cy() + u, g.cz() + v};
                case DomainCube.EAST -> new double[]{g.maxX(), g.cy() + u, g.cz() + v};
                case DomainCube.DOWN -> new double[]{g.cx() + u, g.minY(), g.cz() + v};
                case DomainCube.UP -> new double[]{g.cx() + u, g.maxY(), g.cz() + v};
                case DomainCube.NORTH -> new double[]{g.cx() + u, g.cy() + v, g.minZ()};
                default -> new double[]{g.cx() + u, g.cy() + v, g.maxZ()};
            };
            Fx.spray(sl, FxThemes.BOSS_LUCIUS, FxOld.REVERSE_PORTAL, p[0], p[1], p[2], 2, 0.2, 0.2, 0.2, 0.05);
        }
    }

    /** A patch of dust on the side wall nearest to a player who is within a few blocks of it, shown to that player only. */
    private static void shimmer(Cube c, ServerLevel sl, ServerPlayer p) {
        DomainCube g = c.geo;
        double px = p.getX(), py = p.getY() + p.getBbHeight() * 0.5, pz = p.getZ();
        int face = g.nearestSideFace(px, pz);
        if (g.faceDistances(px, py, pz)[face] > SHIMMER_REACH) return;
        DustParticleOptions d = dust(ACCENT, 1.3f);
        double[] w = g.wallPoint(face, px, py, pz);
        boolean xWall = face == DomainCube.WEST || face == DomainCube.EAST;
        for (int i = 0; i < 10; i++) {
            double along = (FX.nextDouble() * 2 - 1) * 2.5, up = (FX.nextDouble() * 2 - 1) * 1.8;
            sl.sendParticles(p, d, false, xWall ? w[0] : w[0] + along, w[1] + up, xWall ? w[2] + along : w[2], 1, 0, 0, 0, 0);
        }
    }

    // ================================================================ the veto on skills (bound by NUSMP to ManasCore's skill events)
    /**
     * Should this skill activation, release or toggle be refused? True when 'user' is sealed and the skill is not one of the mod's
     * grimoires (those are refused by the cast itself). Tells the user, at most once a second.
     */
    public static boolean vetoesSkill(ManasSkillInstance inst, LivingEntity user) {
        try {
            if (inst == null || user == null || !sealed(user)) return false;
            if (inst.getSkill() instanceof GrimoireBook) return false;
            note(user, "Your skill falls silent: the domain has severed your mana.");
            return true;
        } catch (RuntimeException failed) {           // a skill event must never break because of the seal: when in doubt the skill works
            LogUtils.getLogger().warn("[nusmp] the spell seal could not judge a skill: {}", failed.toString());
            return false;
        }
    }

    // ================================================================ spell damage (registered by NUSMP on the game bus)
    /**
     * A sealed attacker's spell damage is cut to {@link #SPELL_FACTOR}: what leaks through the seal is only a fifth of a spell. And a
     * blow that lands on any other Zogratis boss who holds a cube (Zenon wears his own down in his defence) wears that cube down.
     */
    public static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (CUBES.isEmpty() || e.getAmount() <= 0) return;
        try {
            LivingEntity victim = e.getEntity();
            if (victim instanceof ZogratisBoss && !(victim instanceof ZenonBoss) && !victim.level().isClientSide && active(victim.getUUID()))
                hurtDomain(victim.getUUID(), e.getAmount(), antiMagicHit(e.getSource()));
            if (!(e.getSource().getEntity() instanceof LivingEntity attacker) || attacker.level().isClientSide) return;
            if (!isSpellDamage(e.getSource()) || !sealed(attacker)) return;
            e.setAmount(e.getAmount() * SPELL_FACTOR);
            note(attacker, "Your spell unravels in the domain.");
        } catch (RuntimeException failed) {
            LogUtils.getLogger().warn("[nusmp] the spell seal could not judge a blow: {}", failed.toString());
        }
    }

    /** Spell damage: vanilla magic, or any Tensura damage type that is not physical. */
    public static boolean isSpellDamage(DamageSource s) {
        if (s.is(DamageTypes.MAGIC) || s.is(DamageTypes.INDIRECT_MAGIC)) return true;
        var key = s.typeHolder().unwrapKey();
        return key.isPresent() && "tensura".equals(key.get().location().getNamespace()) && !s.is(TensuraTags.DamageTypes.IS_PHYSICAL);
    }

    /** A demon sword in hand, or the Anti-Magic Lord's own blow: anti-magic cuts the domain twice as deep. */
    public static boolean antiMagicHit(DamageSource s) {
        if (!(s.getEntity() instanceof Player p)) return false;
        if (BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem()).getPath().startsWith("demon_")) return true;
        return AntiMagic.lord(p).isPresent() && s.getDirectEntity() == p;
    }

    private static void note(LivingEntity who, String line) {
        if (!(who instanceof ServerPlayer p)) return;
        long now = p.level().getGameTime();
        if (p.getPersistentData().getLong(NOTE_KEY) > now) return;
        p.getPersistentData().putLong(NOTE_KEY, now + 20);
        p.displayClientMessage(Component.literal(line).withStyle(ChatFormatting.AQUA), true);
    }

    // ================================================================ safety
    /** Zone steps and delayed tasks run inside the server tick: one that throws is logged and stops, it never takes the tick down. */
    private static IntConsumer safe(String what, IntConsumer step) {
        boolean[] dead = {false};
        return age -> {
            if (dead[0]) return;
            try {
                step.accept(age);
            } catch (Throwable t) {
                dead[0] = true;
                LogUtils.getLogger().warn("[nusmp] Zenon's {} stopped: {}", what, t.toString());
            }
        };
    }

    private static Runnable safe(String what, Runnable task) {
        IntConsumer guarded = safe(what, age -> task.run());
        return () -> guarded.accept(0);
    }
}
