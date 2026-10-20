package com.newuniverse.nusmp.labyrinth.mega.safe;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.Marker;
import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.quests.NusmpEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Why: the brief asks for "the custom capability allowing players to trigger D&amp;D-style Short/Long rests only when physically interacting
 * with these specific seating/cooking stations within designated Safe Zones". This is that capability: a right-click on a block whose cell
 * above holds a SEAT, HEARTH or BED marker (the generator saved every marker of the sector, so nothing is scanned in the world) starts a
 * channelled rest, a check twice a second watches it, and it ends in a heal and a mana refill, or in a message saying what broke it.
 * Everything that is a RULE (what refuses a rest, what breaks one, the numbers) is {@link RestRules}, tested without a server; this class only
 * gathers plain values from the world, asks it, and applies the answer. The three PUBLIC entry points ({@link #interact}, {@link #advance},
 * {@link #interruptByDamage}) are exactly what the event handlers call, with the game time passed IN, so the in-game self-tests drive the real
 * code path by calling them in a loop instead of waiting ten real seconds. Choices worth knowing: (1) an EMPTY hand or any non-food item starts a
 * rest, but food in hand on a hearth is left to the campfire or the Farmer's Delight stove (the whole point of the compat blocks is that they
 * cook), a furnace-type hearth (furnace, smoker, blast furnace) keeps its menu for a plain click and rests only on a sneaking click, and sneaking with
 * an item in hand is left alone (that is "place a block"). (2) The Mega-Dungeon's dimension type has
 * {@code bed_works = false}, where vanilla EXPLODES any bed that is right-clicked, in the player's face; since beds are rest anchors, every
 * right-click on a bed in the dimension is taken here (a marked bed starts a long rest, any other bed refuses politely), so no decorative bed
 * of any template can hurt a player ({@code protectBeds}). (3) The rest is measured from where the player sat down, not from the block, because
 * a click can come from four blocks away. (4) A long rest that rolled an ambush announces it to {@link RestAmbushListener}s part-way through;
 * this class spawns nothing. Never throws out of a handler: a failed rest is logged and ends.
 */
public final class RestEvents {
    private RestEvents() {}

    private static final Logger LOG = LogUtils.getLogger();
    /** The player's persistent int the generator's travel code reads: the deepest Landing reached (a long rest in a sector raises it to that sector). */
    public static final String LANDING_KEY = "nusmp_mega_landing";
    private static final String QUEST_RESTED = "mega.rested";
    /** The debuffs of exhaustion a long rest lifts: dark, weak, slow, fatigued, hungry, dizzy, blind. */
    private static final List<Holder<MobEffect>> FATIGUE = List.of(MobEffects.DARKNESS, MobEffects.WEAKNESS, MobEffects.MOVEMENT_SLOWDOWN,
            MobEffects.DIG_SLOWDOWN, MobEffects.HUNGER, MobEffects.CONFUSION, MobEffects.BLINDNESS);
    private static final List<RestAmbushListener> AMBUSH_LISTENERS = new CopyOnWriteArrayList<>();

    /** Registers a listener for long-rest ambushes (once, from a registry's init; never removed). */
    public static void addAmbushListener(RestAmbushListener l) { if (l != null && !AMBUSH_LISTENERS.contains(l)) AMBUSH_LISTENERS.add(l); }

    // ---------------------------------------------------------------- the verdicts the entry points return
    /**
     * What a right-click came to: {@link Action#PASS} (not ours: the event is left alone), {@link Action#STARTED} (a rest began), {@link Action#REFUSED}
     * (an anchor, but the rest cannot start: the player was told why, the click is consumed) or {@link Action#BED_REFUSED} (a bed that is no anchor:
     * consumed so vanilla cannot explode it). {@code refusal} is the reason for a refusal (and {@code NONE} otherwise), {@code kind} the rest.
     */
    public record Click(Action action, RestRules.Refusal refusal, RestKind kind) {
        public enum Action { PASS, STARTED, REFUSED, BED_REFUSED }

        static final Click PASS = new Click(Action.PASS, RestRules.Refusal.NONE, null);
    }

    /** What one look at a resting player came to: nothing to look at, still resting, finished this call, or broken this call (and why). */
    public record Step(Phase phase, RestRules.Interrupt interrupt) {
        public enum Phase { IDLE, RESTING, DONE, BROKEN }

        static final Step IDLE = new Step(Phase.IDLE, RestRules.Interrupt.NONE);
        static final Step RESTING = new Step(Phase.RESTING, RestRules.Interrupt.NONE);
        static final Step DONE = new Step(Phase.DONE, RestRules.Interrupt.NONE);
    }

    // ---------------------------------------------------------------- starting
    public static void onRightClick(PlayerInteractEvent.RightClickBlock e) {
        try {
            if (e.getLevel().isClientSide() || e.getHand() != InteractionHand.MAIN_HAND || !(e.getEntity() instanceof ServerPlayer p)) return;
            Click c = interact(p, e.getPos(), e.getItemStack(), p.serverLevel().getGameTime());
            if (c.action() != Click.Action.PASS) {
                e.setCanceled(true);
                e.setCancellationResult(InteractionResult.SUCCESS);
            }
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] A rest click failed and was ignored", ex);
        }
    }

    /**
     * A right-click of {@code p}'s main hand on the block at {@code pos} while holding {@code held}, at game time {@code now}: starts a rest on a seat,
     * hearth or bed anchor, refuses politely, protects a vanilla bed from exploding, or passes. Server side only; the event handler and the
     * self-tests call this one.
     */
    public static Click interact(ServerPlayer p, BlockPos pos, ItemStack held, long now) {
        ServerLevel level = p.serverLevel();
        if (!Labyrinth.inMega(level)) return Click.PASS;
        if (p.isShiftKeyDown() && !held.isEmpty()) return Click.PASS;                          // sneaking with an item: the player means to place it
        BlockState state = level.getBlockState(pos);
        boolean vanillaBed = state.getBlock() instanceof BedBlock;
        Sector sector = MegaDungeon.sectorAt(level, pos);
        Marker anchor = sector == null ? null : findAnchor(level, sector, pos, state);
        if (anchor == null) {
            if (vanillaBed && SafeConfig.get(SafeConfig.PROTECT_BEDS)) {                       // vanilla would explode it (bed_works = false)
                say(p, "bed_no_use");
                return new Click(Click.Action.BED_REFUSED, RestRules.Refusal.NONE, null);
            }
            return Click.PASS;
        }
        RestKind kind = RestRules.kindOf(anchor.kind());
        if (kind == null) return Click.PASS;
        if (anchor.kind() == MarkerKind.HEARTH) {
            if (held.has(DataComponents.FOOD)) return Click.PASS;                                // food on a hearth is cooking, not resting
            if (state.getBlock() instanceof AbstractFurnaceBlock && !p.isShiftKeyDown()) return Click.PASS;   // a furnace, smoker or blast furnace keeps its menu: sneak and click to rest at it
        }
        return start(p, level, sector, kind, pos, vanillaBed, now);
    }

    /** The rest anchor for a click on {@code pos}: the marker in the cell above, or (for a two-block bed) above the other half. */
    private static Marker findAnchor(ServerLevel level, Sector sector, BlockPos pos, BlockState state) {
        Marker m = RestAnchors.at(sector, pos.above());
        if (m != null) return m;
        for (Direction d : Direction.Plane.HORIZONTAL) {                                      // the clicked half of a bed may be the head; the marker is above the foot
            BlockPos n = pos.relative(d);
            if (level.getBlockState(n).getBlock() != state.getBlock()) continue;
            Marker other = RestAnchors.at(sector, n.above());
            if (other != null && other.kind() == MarkerKind.BED) return other;
        }
        return null;
    }

    private static Click start(ServerPlayer p, ServerLevel level, Sector sector, RestKind kind, BlockPos clicked, boolean vanillaBed, long now) {
        RestState st = p.getData(SafeRegistry.REST_STATE);
        double radius = SafeConfig.get(SafeConfig.HOSTILE_RADIUS);
        boolean busy = st.channel != null;
        boolean zone = MegaDungeon.inSafeZone(level, p.blockPosition());
        double dist = p.position().distanceTo(Vec3.atCenterOf(clicked));
        double hostile = busy || !zone || dist > RestRules.START_REACH ? Double.POSITIVE_INFINITY : nearestHostile(level, p, radius);   // the scan only when it can matter
        int cooldown = SafeConfig.get(SafeConfig.LONG_REST_COOLDOWN_TICKS);
        RestRules.Start s = new RestRules.Start(SafeConfig.get(SafeConfig.REST_ENABLED), !p.isCreative() && !p.isSpectator(), busy, zone, dist, hostile,
                RestRules.shortRestsLeft(SafeConfig.get(SafeConfig.SHORT_REST_USES), st.shortRestsUsed), now, st.lastLongRest);
        RestRules.Refusal r = RestRules.checkStart(kind, s, radius, cooldown);
        if (r != RestRules.Refusal.NONE) {
            switch (r) {
                case DISABLED, NOT_SURVIVAL -> { return vanillaBed ? new Click(Click.Action.REFUSED, r, kind) : new Click(Click.Action.PASS, r, kind); }   // vanilla may carry on, but a vanilla bed would explode
                case BUSY -> { }                                                                // a second click while resting: swallowed silently
                case NOT_SAFE_ZONE -> say(p, "not_safe_zone");
                case TOO_FAR -> say(p, "too_far");
                case HOSTILE_NEAR -> say(p, "hostile_near", Math.round(radius));
                case NO_SHORT_RESTS -> say(p, "no_short_rests");
                case LONG_COOLDOWN -> {
                    long seconds = RestRules.longCooldownRemaining(now, st.lastLongRest, cooldown) / 20L + 1L;
                    say(p, "long_cooldown", String.format("%d:%02d", seconds / 60L, seconds % 60L));
                }
                default -> { }
            }
            return new Click(Click.Action.REFUSED, r, kind);
        }
        int total = RestRules.ticksFor(kind, SafeConfig.get(SafeConfig.SHORT_REST_SECONDS), SafeConfig.get(SafeConfig.LONG_REST_SECONDS));
        long ambushAt = -1L;
        if (kind == RestKind.LONG) {
            double chance = RestRules.ambushChance(Math.max(sector.threat, MegaDungeon.threatAt(level, p.blockPosition())), SafeConfig.get(SafeConfig.AMBUSH_CHANCE), SafeConfig.get(SafeConfig.AMBUSH_MAX_CHANCE));   // 1.55.0: the floor's threat, not only the sector's
            if (RestRules.rollAmbush(chance, level.getRandom().nextDouble())) ambushAt = RestRules.ambushDelay(total, level.getRandom().nextDouble());
        }
        st.channel = new RestState.Channel(kind, now, total, p.position(), clicked.immutable(), sector.id, ambushAt);
        level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, 0.7f);
        say(p, kind == RestKind.LONG ? "start_long" : "start_short");
        com.newuniverse.nusmp.compat.maps.MapShare.safeRoom(p, clicked);                                   // compat-maps: a clickable waypoint line for the safe room (once per anchor, opt-in, rate limited)
        return new Click(Click.Action.STARTED, RestRules.Refusal.NONE, kind);
    }

    // ---------------------------------------------------------------- the channel
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % RestRules.CHECK_INTERVAL != 0) return;
        if (!p.hasData(SafeRegistry.REST_STATE)) return;                                      // nobody who never rested gets an attachment created here
        advance(p, p.serverLevel().getGameTime());
    }

    /**
     * One look at {@code p}'s rest at game time {@code now} (the tick handler calls it every {@link RestRules#CHECK_INTERVAL} ticks): it either goes
     * on (progress on the action bar), finishes (heal, mana, state, quest hook) or breaks (a message saying why). Never throws.
     */
    public static Step advance(ServerPlayer p, long now) {
        if (!p.hasData(SafeRegistry.REST_STATE)) return Step.IDLE;
        RestState st = p.getData(SafeRegistry.REST_STATE);
        RestState.Channel ch = st.channel;
        if (ch == null) return Step.IDLE;
        try {
            return step(p, st, ch, now);
        } catch (RuntimeException ex) {
            st.channel = null;
            LOG.warn("[nusmp] A rest failed and was ended", ex);
            return new Step(Step.Phase.BROKEN, RestRules.Interrupt.STATE);
        }
    }

    private static Step step(ServerPlayer p, RestState st, RestState.Channel ch, long now) {
        ServerLevel level = p.serverLevel();
        double radius = SafeConfig.get(SafeConfig.HOSTILE_RADIUS);
        boolean valid = p.isAlive() && !p.isCreative() && !p.isSpectator() && Labyrinth.inMega(level);
        boolean zone = valid && MegaDungeon.inSafeZone(level, p.blockPosition());
        double moved = p.position().distanceTo(ch.origin);
        double hostile = valid && zone && moved <= RestRules.MOVE_LIMIT ? nearestHostile(level, p, radius) : Double.POSITIVE_INFINITY;
        RestRules.Interrupt why = RestRules.checkChannel(new RestRules.Sample(valid, zone, moved, false, hostile), radius);
        if (why != RestRules.Interrupt.NONE) {
            interrupt(p, st, why);
            return new Step(Step.Phase.BROKEN, why);
        }
        long elapsed = now - ch.start;
        if (ch.kind == RestKind.LONG && ch.ambushAt >= 0L && !ch.ambushDone && elapsed >= ch.ambushAt) {
            ch.ambushDone = true;
            fireAmbush(p, level, ch);
        }
        if (RestRules.finished(elapsed, ch.total)) {
            complete(p, st, ch, level, now);
            return Step.DONE;
        }
        p.displayClientMessage(Component.translatable("message.nusmp.mega.safe.progress", RestRules.progressPercent(elapsed, ch.total)), true);
        level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 2, 0.3, 0.4, 0.3, 0.01);
        return Step.RESTING;
    }

    /** A hit ends the rest at once (not at the next check): the damage event calls this. */
    public static void onDamage(LivingDamageEvent.Post e) {
        try {
            if (e.getEntity() instanceof ServerPlayer p && e.getNewDamage() > 0f) interruptByDamage(p);
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] A rest damage check failed and was ignored", ex);
        }
    }

    /** Ends the rest of {@code p} (if any) because they were hurt. Returns whether a rest was broken. */
    public static boolean interruptByDamage(ServerPlayer p) {
        if (!p.hasData(SafeRegistry.REST_STATE)) return false;
        RestState st = p.getData(SafeRegistry.REST_STATE);
        if (st.channel == null) return false;
        interrupt(p, st, RestRules.Interrupt.DAMAGED);
        return true;
    }

    /** Leaving the dimension ends a rest silently (the saved state is untouched). */
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && p.hasData(SafeRegistry.REST_STATE)) p.getData(SafeRegistry.REST_STATE).channel = null;
    }

    private static void interrupt(ServerPlayer p, RestState st, RestRules.Interrupt why) {
        st.channel = null;
        String key = switch (why) {
            case DAMAGED -> "interrupt_damaged";
            case MOVED -> "interrupt_moved";
            case LEFT_ZONE -> "interrupt_left";
            case HOSTILE -> "interrupt_hostile";
            default -> null;                                                                 // STATE: the player died or left, nobody to tell
        };
        if (key == null) return;
        say(p, key);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.CANDLE_EXTINGUISH, SoundSource.PLAYERS, 0.8f, 0.9f);
    }

    private static void complete(ServerPlayer p, RestState st, RestState.Channel ch, ServerLevel level, long now) {
        RestKind kind = ch.kind;
        st.channel = null;
        float heal = RestRules.healAmount(p.getHealth(), p.getMaxHealth(), RestRules.healthShare(kind, SafeConfig.get(SafeConfig.SHORT_REST_HEALTH)));
        if (heal > 0f) p.heal(heal);
        RestMana.restore(p, RestRules.manaShare(kind, SafeConfig.get(SafeConfig.SHORT_REST_MANA)), SafeConfig.get(SafeConfig.REST_AURA));
        st.lastRest = now;
        Sector sector = MegaDungeon.data(level.getServer()).sector(ch.sectorId);
        if (kind == RestKind.LONG) {
            st.shortRestsUsed = 0;
            st.lastLongRest = now;
            for (Holder<MobEffect> fx : FATIGUE) p.removeEffect(fx);
            // 1.99.0, THE HUNGER BAN: a rest recovers health, mana (and the effects above) only; it never feeds or starves, so the food bar's exhaustion is left alone
            if (sector != null) {
                CompoundTag data = p.getPersistentData();
                if (!data.contains(LANDING_KEY) || sector.id > data.getInt(LANDING_KEY)) data.putInt(LANDING_KEY, sector.id);
            }
            say(p, "done_long");
            level.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.2, p.getZ(), 6, 0.4, 0.4, 0.4, 0.0);
        } else {
            st.shortRestsUsed++;
            say(p, "done_short", RestRules.shortRestsLeft(SafeConfig.get(SafeConfig.SHORT_REST_USES), st.shortRestsUsed));
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1.0, p.getZ(), 14, 0.5, 0.6, 0.5, 0.0);
        level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 1.2f);
        try {
            NusmpEvents.fire(p, QUEST_RESTED, kind.id());
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] The quest hook {} failed and was ignored", QUEST_RESTED, ex);       // a quest hiccup must never undo a rest
        }
    }

    private static void fireAmbush(ServerPlayer p, ServerLevel level, RestState.Channel ch) {
        Sector sector = MegaDungeon.data(level.getServer()).sector(ch.sectorId);
        if (sector == null) return;
        boolean handled = false;
        for (RestAmbushListener l : AMBUSH_LISTENERS) {
            try {
                handled |= l.onAmbush(p, sector, Math.max(sector.threat, MegaDungeon.threatAt(level, p.blockPosition())));
            } catch (RuntimeException ex) {
                LOG.warn("[nusmp] A rest ambush listener failed and was ignored", ex);
            }
        }
        if (handled) say(p, "ambush");
    }

    // ---------------------------------------------------------------- the world, as plain numbers
    /** Distance to the nearest hostile mob within {@code radius} of the player, or {@link Double#POSITIVE_INFINITY}. */
    static double nearestHostile(ServerLevel level, Player p, double radius) {
        double best = Double.POSITIVE_INFINITY;
        AABB box = p.getBoundingBox().inflate(radius);
        for (Mob m : level.getEntitiesOfClass(Mob.class, box, mob -> isHostile(mob, p))) {
            double d = Math.sqrt(m.distanceToSqr(p));
            if (d < best) best = d;
        }
        return best;
    }

    /**
     * Is this mob a threat to a resting player? A vanilla {@link Enemy}, anything in the monster category, or anything that has set its eyes on a
     * player; never a mob with no AI (a statue, a captive), an ally, or an owned pet. A heuristic, said so in the docs: a modded monster outside
     * all three groups does not stop a rest until it targets someone.
     */
    static boolean isHostile(Mob m, Player p) {
        if (!m.isAlive() || m.isNoAi() || m.isAlliedTo(p)) return false;
        if (m instanceof OwnableEntity pet && pet.getOwnerUUID() != null) return false;
        if (m.getTags().contains("nusmp_servant")) return false;        // 1.55.0: a resting player's own Shadow minion (an Enemy by type) must not block the rest
        return m instanceof Enemy || m.getType().getCategory() == MobCategory.MONSTER || m.getTarget() instanceof Player;
    }

    // ---------------------------------------------------------------- small helpers
    private static void say(ServerPlayer p, String key, Object... args) {
        p.displayClientMessage(Component.translatable("message.nusmp.mega.safe." + key, args), true);
    }
}
