package com.newuniverse.nusmp.heroweapon.gem;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.bond.HeroSummon;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: this is the whole of the Choosing on the server: it gathers the facts about a player and the world, asks the pure {@link GemRules} and {@link GemRitual}
 * what they mean, and does what they decide. {@link #begin} starts a ritual (or tells the player the first thing that is wrong), {@link #tick} advances it
 * once per server tick while the use key is held, {@link #hurt} and {@link #released} end it early, and {@link #finish} is the moment the weapon answers: it
 * checks the survival route and the gates once more, tries the weapons in order (the one a Seal names, or the free Holy Weapons shuffled) through
 * {@link HeroBonds#bind}, which applies the core's limits and uniqueness rule and refuses with a reason, and only when a bind succeeded does it spend the gem
 * (a creative player keeps it) and show the title; a refusal keeps the gem and says why. The weapon is put in the hand one tick later through
 * {@link HeroSummon#summon}: the gem occupied that very slot, and the item-use code of the game is still finishing with it. The firing of the quest key
 * {@code hero.bound} is NOT done here: the core fires it inside its bind listener, for every route (this ritual, the commands), so a second firing would count
 * one bind twice. State: the running rituals and the summons waiting for their tick, per player, on the server thread only; cleared when a player leaves
 * and when the server stops.
 */
public final class GemChoosing {
    private GemChoosing() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** One running ritual: the clock, where the circle was drawn, which weapon a Seal names, and what has happened to the player since. */
    private static final class Active {
        final GemRitual ritual;
        final Vec3 anchor;
        final ResourceKey<Level> dimension;
        final HeroKind seal;
        boolean hurt;
        long lastTime;                                                                       // the game time of the last tick the ritual was stepped (a new body of the same player has its own tick counter)

        Active(GemRitual ritual, Vec3 anchor, ResourceKey<Level> dimension, HeroKind seal, long time) {
            this.ritual = ritual;
            this.anchor = anchor;
            this.dimension = dimension;
            this.seal = seal;
            this.lastTime = time;
        }
    }

    private static final Map<UUID, Active> ACTIVE = new HashMap<>();
    private static final Set<UUID> SUMMON = new HashSet<>();

    // ------------------------------------------------------------------ the facts
    /** An operator or a creative player: the admin routes stay open when the survival route is switched off. */
    static boolean privileged(ServerPlayer p) { return p.isCreative() || p.createCommandSourceStack().hasPermission(2); }

    private static boolean heldByAnOnlinePlayer(MinecraftServer server, ServerPlayer except, HeroKind kind) {
        for (ServerPlayer o : server.getPlayerList().getPlayers()) if (o != except && HeroBonds.kindOf(o) == kind) return true;
        return false;
    }

    /** The Holy Weapons this player could be given right now: the gates allow it and (config {@code holyGemFreeOnly}) no online player holds it. */
    static List<HeroKind> holyCandidates(ServerPlayer p) {
        List<HeroKind> out = new ArrayList<>();
        for (HeroKind k : HeroKind.values()) {
            if (!k.isHoly() || HeroBonds.refusal(p, k) != null) continue;
            if (GemConfig.holyFreeOnly() && p.getServer() != null && heldByAnOnlinePlayer(p.getServer(), p, k)) continue;
            out.add(k);
        }
        return out;
    }

    /** The one reason every Holy Weapon is refused for, or null when the refusals differ (the weapons are simply taken) or some weapon is allowed. */
    static String holyRefusal(ServerPlayer p) {
        List<String> reasons = new ArrayList<>();
        for (HeroKind k : HeroKind.values()) if (k.isHoly()) reasons.add(HeroBonds.refusal(p, k));
        return GemRules.commonReason(reasons);
    }

    /** How many blocks of clear space there are above the player's head, counted up to {@code need} (a sky is not required: a cavern will do). */
    static int clearAbove(ServerPlayer p, int need) {
        if (need <= 0) return 0;
        Level level = p.level();
        int top = (int) Math.floor(p.getY() + p.getBbHeight()) + 1;
        int clear = 0;
        for (int i = 0; i < need; i++) {
            BlockPos pos = BlockPos.containing(p.getX(), top + i, p.getZ());
            if (level.isOutsideBuildHeight(pos)) return need;
            BlockState state = level.getBlockState(pos);
            if (!state.getCollisionShape(level, pos).isEmpty() || !state.getFluidState().isEmpty()) break;
            clear++;
        }
        return clear;
    }

    /** Everything {@link GemRules#decide} needs to know about a player who holds a gem for {@code seal} (null: the Holy Calling Gem). */
    static GemRules.Facts facts(ServerPlayer p, HeroKind seal) {
        boolean sealed = seal != null;
        int free = 0;
        boolean refused;
        if (sealed) {
            refused = HeroBonds.refusal(p, seal) != null;
        } else {
            free = holyCandidates(p).size();
            refused = free == 0 && holyRefusal(p) != null;
        }
        return new GemRules.Facts(GemRules.routeOpen(GemConfig.recipes(), privileged(p)), HeroBonds.has(p), sealed, free, refused, p.isPassenger(), p.isUnderWater(),
                p.onGround(), Math.hypot(p.getX() - p.xo, p.getZ() - p.zo), clearAbove(p, GemConfig.openAbove()));
    }

    private static Component denialText(ServerPlayer p, GemRules.Denial d, HeroKind seal) {
        switch (d) {
            case ALREADY_BOUND: {
                HeroKind has = HeroBonds.kindOf(p);
                return GemText.msg(GemMessages.BOUND, has == null ? "hero weapon" : has.display());
            }
            case GATE_REFUSED: {
                String why = seal != null ? HeroBonds.refusal(p, seal) : holyRefusal(p);
                if (why == null) why = "not now";
                return seal != null ? GemText.msg(GemMessages.REFUSED_SEAL, seal.display(), why) : GemText.msg(GemMessages.REFUSED_GEM, why);
            }
            case CLOSED_ABOVE:
                return GemText.msg(GemMessages.CLOSED_ABOVE, String.valueOf(GemConfig.openAbove()));
            default:
                return GemText.msg(GemMessages.denialKey(d, seal != null));
        }
    }

    // ------------------------------------------------------------------ the ritual
    /** Starts a ritual for a player who just used a gem; returns {@link GemRules.Denial#NONE} when it runs, else why not (the player has been told). */
    public static GemRules.Denial begin(ServerPlayer p, CallingGemItem gem) { return begin(p, gem.kind(), facts(p, gem.kind())); }

    /** The same with the facts given, so the self-test can drive every path without depending on the blocks round a stand-in player. */
    static GemRules.Denial begin(ServerPlayer p, HeroKind seal, GemRules.Facts facts) {
        ACTIVE.remove(p.getUUID());                                                           // a new use replaces any old record
        GemRules.Denial d = GemRules.decide(facts, GemConfig.speedTolerance(), GemConfig.openAbove());
        if (d != GemRules.Denial.NONE) {
            GemFx.say(p, denialText(p, d, seal));
            return d;
        }
        GemRitual ritual = new GemRitual(GemConfig.ritualTicks(), GemConfig.moveTolerance());
        ACTIVE.put(p.getUUID(), new Active(ritual, p.position(), p.level().dimension(), seal, p.level().getGameTime()));
        GemFx.start(p, seal, ritual.total());
        return GemRules.Denial.NONE;
    }

    /** One server tick while the player holds the use key: reads how far they stand from the circle and whether the sky is open, then steps the ritual. */
    public static void tick(ServerPlayer p) {
        Active a = ACTIVE.get(p.getUUID());
        if (a == null) {                                                                      // no ritual behind this use (it was replaced, or the server restarted)
            p.stopUsingItem();
            return;
        }
        double moved = p.level().dimension().equals(a.dimension) ? p.position().distanceTo(a.anchor) : Double.MAX_VALUE;
        int need = GemConfig.openAbove();
        boolean open = need <= 0 || p.tickCount % 5 != 0 || clearAbove(p, need) >= need;     // the sky is looked at five times a second
        step(p, moved, open);
    }

    /** The ritual's tick with the inputs given (the self-test calls it directly). */
    static GemRitual.Step step(ServerPlayer p, double moved, boolean open) {
        Active a = ACTIVE.get(p.getUUID());
        if (a == null) return GemRitual.Step.INTERRUPT;
        a.lastTime = p.level().getGameTime();
        GemRitual.Step s = a.ritual.tick(moved, a.hurt, open);
        if (s == GemRitual.Step.INTERRUPT) {
            ACTIVE.remove(p.getUUID());
            GemFx.broken(p, a.ritual.reason());
            p.stopUsingItem();
        } else if (s == GemRitual.Step.CONTINUE) {
            GemFx.channel(p, a.seal, a.ritual);
        }
        return s;
    }

    /** Something hurt the player: the circle will break on the ritual's next tick. */
    public static void hurt(ServerPlayer p) {
        Active a = ACTIVE.get(p.getUUID());
        if (a != null) a.hurt = true;
    }

    /** The player let go of the use key before the gem answered. */
    public static void released(ServerPlayer p) {
        Active a = ACTIVE.remove(p.getUUID());
        if (a == null || a.ritual.state() != GemRitual.State.CHANNELING) return;
        a.ritual.interrupt(GemRitual.Interrupt.RELEASED);
        GemFx.broken(p, GemRitual.Interrupt.RELEASED);
    }

    /**
     * The game's own use timer ran out with the key still held, which is the moment a weapon answers. Returns the stack the hand holds afterwards: the same
     * stack, spent (count zero) when a weapon was bound and the player is not in creative mode, untouched when the gem refused.
     */
    public static ItemStack finish(ServerPlayer p, CallingGemItem gem, ItemStack stack) {
        Active a = ACTIVE.remove(p.getUUID());
        if (a == null || a.ritual.state() == GemRitual.State.INTERRUPTED) return stack;     // nothing running, or the player has been told it broke
        HeroKind seal = gem.kind();
        GemRules.Denial d = GemRules.decideAtEnd(facts(p, seal));
        if (d != GemRules.Denial.NONE) {
            GemFx.say(p, denialText(p, d, seal));
            return stack;
        }
        List<HeroKind> order = seal != null ? List.of(seal) : GemRules.shuffled(holyCandidates(p), p.getRandom()::nextInt);
        HeroKind bound = null;
        String why = null;
        for (HeroKind k : order) {                                                           // another player may have taken a weapon while this one held still
            why = HeroBonds.bind(p, k);
            if (why == null) {
                bound = k;
                break;
            }
        }
        if (bound == null) {
            GemFx.say(p, seal != null ? GemText.msg(GemMessages.REFUSED_SEAL, seal.display(), why == null ? "not now" : why)
                    : why == null ? GemText.msg(GemMessages.NONE_FREE) : GemText.msg(GemMessages.REFUSED_GEM, why));
            return stack;
        }
        stack.consume(1, p);                                                                 // before anything that can fail: a bound player must never keep the gem
        if (!p.isCreative() && !p.isSpectator()) SUMMON.add(p.getUUID());
        GemFx.chosen(p, bound, seal == null);
        return stack;
    }

    // ------------------------------------------------------------------ housekeeping
    /** True while any ritual runs or any summon waits: the per-tick listener does nothing otherwise. */
    static boolean busy() { return !ACTIVE.isEmpty() || !SUMMON.isEmpty(); }

    /**
     * Once per player tick while {@link #busy}: puts a newly chosen weapon in the hand, and ends a ritual whose use stopped without reaching it (another hotbar
     * slot, the gem dropped, the player died) so that no record outlives it.
     */
    static void housekeeping(ServerPlayer p) {
        UUID id = p.getUUID();
        if (SUMMON.remove(id)) {
            try {
                HeroSummon.summon(p);
            } catch (RuntimeException e) {
                LOG.warn("the chosen weapon could not be put in {}'s hand", p.getGameProfile().getName(), e);
            }
        }
        Active a = ACTIVE.get(id);
        if (a != null && p.level().getGameTime() - a.lastTime > 4) {
            ACTIVE.remove(id);
            GemFx.broken(p, GemRitual.Interrupt.LOST);
        }
    }

    /** Forgets everything about a player (they left, or died and came back). */
    public static void forget(UUID id) {
        ACTIVE.remove(id);
        SUMMON.remove(id);
    }

    /** Forgets everything (the server stopped). */
    public static void clear() {
        ACTIVE.clear();
        SUMMON.clear();
    }

    /** Whether a ritual runs for this player (the self-test asks). */
    static boolean running(UUID id) { return ACTIVE.containsKey(id); }

    /** Whether a chosen weapon waits to be put in this player's hand (the self-test asks). */
    static boolean summonPending(UUID id) { return SUMMON.contains(id); }
}
