package com.newuniverse.nusmp.labyrinth.squad;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.squad.SquadGeometry.V2;
import com.newuniverse.nusmp.labyrinth.squad.SquadMind.Cause;
import com.newuniverse.nusmp.labyrinth.squad.SquadMind.Reaction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the state the members of a squad SHARE, in one object, so that "who leads, who is the target, who needs covering, has the tank engaged, did the nerve break" is
 * decided once per think instead of by every member for itself (which is what makes a horde a horde: six mobs that each pick the nearest player pick six answers).
 * The squad holds its members weakly (it must never keep a mob alive or a chunk loaded), thinks at most once every {@code thinkEveryTicks} (the server tick calls
 * {@link #think}; the goals only READ what the think left behind), prunes the dead and the unloaded as it goes, and is forgotten by {@link SquadManager} when nobody
 * is left. Each member carries a role tag in its persistent data ({@link #ID}, {@link #ROLE}, {@link #SLOT}, {@link #SIZE}), so a mob that comes back from a saved chunk
 * can be adopted again by {@link SquadManager#adopt}. Server thread only.
 */
public final class Squad {
    /** The scoreboard tag and the persistent-data keys of a squad member. */
    public static final String TAG = "nusmp_squad";
    public static final String ID = "nusmp_squad_id", ROLE = "nusmp_squad_role", SLOT = "nusmp_squad_slot", SEED = "nusmp_squad_seed", SIZE = "nusmp_squad_size";
    static final ResourceLocation ENRAGE_SPEED = ResourceLocation.fromNamespaceAndPath("nusmp", "squad_enrage_speed"), ENRAGE_DAMAGE = ResourceLocation.fromNamespaceAndPath("nusmp", "squad_enrage_damage");
    /** A target is dropped from the picture when it is farther than this from the squad's centre. */
    static final double THREAT_RANGE = 32.0;
    /** Ticks without a tank near the target before the engagement counts as over. */
    private static final int ENGAGE_GRACE = 30;

    /** One member as the squad sees it. */
    public static final class Member {
        public final UUID id;
        final WeakReference<Mob> ref;
        SquadRole role = SquadRole.DPS_MELEE;
        int slot;
        SquadFormation.Offset offset = new SquadFormation.Offset(0, 0);
        boolean goals, dead;
        int waited;
        boolean flip;
        long healReady, buffReady, reactUntil;
        Reaction reaction = Reaction.HOLD;

        Member(Mob mob) {
            this.id = mob.getUUID();
            this.ref = new WeakReference<>(mob);
        }

        public SquadRole role() { return role; }

        public int slot() { return slot; }

        public boolean hasGoals() { return goals; }

        public Reaction reaction() { return reaction; }

        public boolean fleeing(long now) { return reaction == Reaction.FLEE && now < reactUntil; }

        Mob mob() { return ref.get(); }
    }

    private final UUID id;
    private final long seed;
    private int size;
    private final SquadPlan.Plan plan;
    private final boolean armed;
    private final Map<UUID, Member> members = new LinkedHashMap<>();
    private final long createdAt;
    private UUID leader, targetId, wardId;
    private WeakReference<LivingEntity> target = new WeakReference<>(null);
    private int deaths, passes;
    private long engagedSince = -1L, lastEngagedAt = Long.MIN_VALUE, lastAliveAt, brokeAt;
    private V2 centre = new V2(0, 0), leaderPos;
    private double yaw;
    private Cause broken = Cause.NONE;
    private boolean leaderKnown;
    private final List<UUID> wards = new ArrayList<>();

    /** A squad of {@code size} members; {@code plan} may be null (a squad adopted from saved mobs); {@code armed} false switches the morale off for such a squad. */
    Squad(UUID id, long seed, int size, SquadPlan.Plan plan, boolean armed, long now) {
        this.id = id;
        this.seed = seed;
        this.size = Math.max(1, size);
        this.plan = plan;
        this.armed = armed;
        this.createdAt = now;
        this.lastAliveAt = now;
    }

    public UUID id() { return id; }

    public long seed() { return seed; }

    public int size() { return size; }

    /** The squad turned out smaller than planned (some places were not found): its morale is judged against the real headcount. */
    void resize(int n) { size = Math.max(1, n); }

    /** Strips the squad tags from every member (the squad was dissolved before it began). */
    void dissolve() {
        for (Member m : members.values()) {
            Mob mob = m.mob();
            if (mob == null) continue;
            mob.removeTag(TAG);
            CompoundTag d = mob.getPersistentData();
            d.remove(ID);
            d.remove(ROLE);
            d.remove(SLOT);
            d.remove(SEED);
            d.remove(SIZE);
        }
        members.clear();
    }

    public SquadPlan.Plan plan() { return plan; }

    public long createdAt() { return createdAt; }

    public Cause broken() { return broken; }

    public boolean isBroken() { return broken != Cause.NONE; }

    public int deaths() { return deaths; }

    public int memberCount() { return members.size(); }

    public Member member(UUID mob) { return members.get(mob); }

    public SquadRole roleOf(UUID mob) {
        Member m = members.get(mob);
        return m == null ? null : m.role;
    }

    public UUID leaderId() { return leader; }

    public LivingEntity target() {
        LivingEntity t = target.get();
        return t != null && t.isAlive() && !t.isRemoved() ? t : null;
    }

    public V2 centre() { return centre; }

    public boolean tankEngaged(long now) { return engagedSince >= 0L && now - lastEngagedAt <= ENGAGE_GRACE; }

    /** The mobs of the squad that are loaded and alive, in formation order. */
    public List<Mob> alive() {
        List<Mob> out = new ArrayList<>(members.size());
        for (Member m : members.values()) {
            Mob mob = m.mob();
            if (!m.dead && mob != null && mob.isAlive() && !mob.isRemoved()) out.add(mob);
        }
        return out;
    }

    public int aliveCount() { return alive().size(); }

    // ---------------------------------------------------------------- joining
    /** Adds a mob (idempotent) and tags it; the role stays what it was until {@link #assign}. */
    Member attach(Mob mob) { return attach(mob, null, 0); }

    /** Adds a mob with the role and slot it was saved with (null role = the default); a mob already in the squad keeps what it has. */
    Member attach(Mob mob, SquadRole role, int slot) {
        Member m = members.get(mob.getUUID());
        if (m == null) {
            m = new Member(mob);
            if (role != null) m.role = role;
            m.slot = slot;
            members.put(m.id, m);
        }
        mob.addTag(TAG);
        stamp(mob, m);
        return m;
    }

    private void stamp(Mob mob, Member m) {
        CompoundTag d = mob.getPersistentData();
        d.putString(ID, id.toString());
        d.putString(ROLE, m.role.id());
        d.putInt(SLOT, m.slot);
        d.putLong(SEED, seed);
        d.putInt(SIZE, size);
    }

    /** Gives the mobs their roles (one per mob, same order), picks the leader, lays out the formation and writes the role tags. */
    void assign(List<Mob> mobs, SquadRole[] roles) {
        for (int i = 0; i < mobs.size(); i++) {
            Member m = attach(mobs.get(i));
            m.role = i < roles.length && roles[i] != null ? roles[i] : SquadRole.DPS_MELEE;
        }
        layout();
        elect();
        leaderKnown = leader != null;
        for (Member m : members.values()) {
            Mob mob = m.mob();
            if (mob != null) stamp(mob, m);
        }
    }

    /** Orders the members front to back by role and gives each its formation slot. */
    void layout() {
        List<Member> order = new ArrayList<>(members.values());
        order.sort(Comparator.comparingInt((Member m) -> rank(m.role)));
        List<SquadRole> roles = new ArrayList<>(order.size());
        for (Member m : order) roles.add(m.role);
        List<SquadFormation.Offset> offsets = SquadFormation.offsets(roles, seed);
        for (int i = 0; i < order.size(); i++) {
            order.get(i).slot = i;
            order.get(i).offset = offsets.get(i);
        }
    }

    private static int rank(SquadRole r) {
        return switch (r) {
            case LEADER -> 0;
            case TANK -> 1;
            case DPS_MELEE -> 2;
            case FLANKER -> 3;
            case DPS_RANGED -> 4;
            case SUPPORT -> 5;
        };
    }

    /** The leader: the LEADER role, else the toughest tank, else the toughest alive member. */
    private void elect() {
        Member best = null;
        double bestScore = -1.0;
        for (Member m : members.values()) {
            Mob mob = m.mob();
            if (m.dead || mob == null || !mob.isAlive()) continue;
            double s = (m.role == SquadRole.LEADER ? 1e6 : m.role == SquadRole.TANK ? 1e3 : 0.0) + mob.getMaxHealth();
            if (s > bestScore) {
                bestScore = s;
                best = m;
            }
        }
        leader = best == null ? null : best.id;
    }

    // ---------------------------------------------------------------- dying
    /** A member died (the death event): counted now, pruned at the next think. */
    void memberDied(UUID mob) {
        Member m = members.get(mob);
        if (m == null || m.dead) return;
        m.dead = true;
        deaths++;
    }

    // ---------------------------------------------------------------- the think
    /**
     * One pass: prune, elect, read the situation (centre, facing, threats, the focus target, the wards, the tank's engagement), judge morale. Cheap by construction:
     * everything is a loop over at most eight members and the handful of players near the squad. Never throws into the server tick (the manager catches).
     */
    void think(ServerLevel level, long now) {
        passes++;
        prune();
        List<Mob> alive = alive();
        if (alive.isEmpty()) return;
        lastAliveAt = now;
        Member lead = leader == null ? null : members.get(leader);
        if (lead == null || lead.dead || lead.mob() == null || !lead.mob().isAlive()) elect();
        List<V2> pts = new ArrayList<>(alive.size());
        for (Mob m : alive) pts.add(new V2(m.getX(), m.getZ()));
        centre = SquadGeometry.centroid(pts);
        Member lm = leader == null ? null : members.get(leader);
        Mob leaderMob = lm == null ? null : lm.mob();
        leaderPos = leaderMob == null ? null : new V2(leaderMob.getX(), leaderMob.getZ());
        retarget(level, alive, now);
        LivingEntity t = target();
        yaw = t != null ? SquadGeometry.yawToward(centre, new V2(t.getX(), t.getZ())) : leaderMob != null ? leaderMob.getYRot() : yaw;
        chooseWards(alive);
        engagement(alive, t, now);
        morale(level, alive, now);
    }

    private void prune() {
        members.values().removeIf(m -> {
            Mob mob = m.mob();
            if (mob == null) return true;                                         // collected: gone
            Entity.RemovalReason why = mob.getRemovalReason();
            if (why != null) {
                if (why.shouldDestroy() && !m.dead) deaths++;                      // killed or discarded in a way the death event did not report
                return true;                                                       // unloaded: out of the picture, the tag brings it back
            }
            if (mob.isDeadOrDying() && !m.dead) {
                m.dead = true;
                deaths++;
            }
            return m.dead && !mob.isAlive();
        });
    }

    private void retarget(ServerLevel level, List<Mob> alive, long now) {
        LivingEntity current = target();
        boolean anyAttacked = false;
        List<LivingEntity> cands = new ArrayList<>();
        List<Boolean> avenged = new ArrayList<>();
        for (Mob m : alive) {
            LivingEntity t = m.getTarget();
            if (usable(level, t, m)) add(cands, avenged, t, false);
            LivingEntity h = m.getLastHurtByMob();
            if (usable(level, h, m) && m.tickCount - m.getLastHurtByMobTimestamp() < 100) {
                add(cands, avenged, h, true);
                anyAttacked = true;
            }
        }
        if (cands.isEmpty() || current == null) {
            AABB box = new AABB(centre.x() - THREAT_RANGE, level.getMinBuildHeight(), centre.z() - THREAT_RANGE, centre.x() + THREAT_RANGE, level.getMaxBuildHeight(), centre.z() + THREAT_RANGE);
            Mob any = alive.get(0);
            for (Player p : level.getEntitiesOfClass(Player.class, box)) if (usable(level, p, any) && !p.isCreative()) add(cands, avenged, p, false);
        }
        boolean redo = current == null || passes % 4 == 0 || anyAttacked || !cands.contains(current);
        if (!redo) return;
        V2 ward = wardPos();
        List<SquadMind.ThreatView> views = new ArrayList<>(cands.size());
        for (int i = 0; i < cands.size(); i++) {
            LivingEntity c = cands.get(i);
            V2 p = new V2(c.getX(), c.getZ());
            views.add(new SquadMind.ThreatView(c.getId(), p.dist(centre), ward == null ? p.dist(centre) : p.dist(ward), c.getMaxHealth() <= 0 ? 1.0 : c.getHealth() / c.getMaxHealth(), avenged.get(i), c instanceof Player));
        }
        int best = SquadMind.focusTarget(views);
        LivingEntity chosen = best < 0 ? null : cands.get(best);
        target = new WeakReference<>(chosen);
        targetId = chosen == null ? null : chosen.getUUID();
    }

    private static void add(List<LivingEntity> cands, List<Boolean> avenged, LivingEntity e, boolean hurtAlly) {
        int i = cands.indexOf(e);
        if (i >= 0) {
            if (hurtAlly) avenged.set(i, true);
            return;
        }
        cands.add(e);
        avenged.add(hurtAlly);
    }

    /** May the squad treat this entity as a threat: alive, not a spectator, not in a safe room, not one of the dungeon's own monsters, near enough? */
    private boolean usable(ServerLevel level, LivingEntity t, Mob by) {
        if (t == null || !t.isAlive() || t.isRemoved() || t == by) return false;
        if (t instanceof Player p && p.isSpectator()) return false;
        if (MegaDungeon.isDungeonMonster(t) || t.getTags().contains(TAG)) return false;
        if (new V2(t.getX(), t.getZ()).dist(centre) > THREAT_RANGE * 1.5) return false;
        return !MegaDungeon.inSafeZone(level, t.blockPosition());
    }

    /** The softest members, in order: the wards the tanks stand in front of (the first tank covers the softest, the second the next). */
    private void chooseWards(List<Mob> alive) {
        wards.clear();
        wardId = null;
        List<SquadMind.WardView> views = new ArrayList<>(alive.size());
        List<UUID> ids = new ArrayList<>(alive.size());
        for (Mob m : alive) {
            Member mem = members.get(m.getUUID());
            if (mem == null) continue;
            views.add(new SquadMind.WardView(ids.size(), mem.role, m.getMaxHealth() <= 0 ? 1.0 : m.getHealth() / m.getMaxHealth()));
            ids.add(m.getUUID());
        }
        List<SquadMind.WardView> left = new ArrayList<>(views);
        for (int k = 0; k < 3; k++) {
            int w = SquadMind.chooseWard(left, -1);
            if (w < 0) break;
            wards.add(ids.get(w));
            final int gone = w;
            left.removeIf(v -> v.index() == gone);
        }
        if (!wards.isEmpty()) wardId = wards.get(0);
    }

    private void engagement(List<Mob> alive, LivingEntity t, long now) {
        if (t == null) {
            engagedSince = -1L;
            return;
        }
        for (Mob m : alive) {
            Member mem = members.get(m.getUUID());
            if (mem == null || !mem.role.isFrontLine()) continue;
            if (m.distanceTo(t) <= 6.0) {
                if (engagedSince < 0L) engagedSince = now;
                lastEngagedAt = now;
                return;
            }
        }
        if (now - lastEngagedAt > ENGAGE_GRACE) engagedSince = -1L;
    }

    // ---------------------------------------------------------------- morale
    private void morale(ServerLevel level, List<Mob> alive, long now) {
        if (broken != Cause.NONE || !armed) return;
        boolean leaderAlive = false;
        if (leader != null) {
            Member lm = members.get(leader);
            leaderAlive = lm != null && !lm.dead && lm.mob() != null && lm.mob().isAlive();
        }
        Cause c = SquadMind.check(size, size - deaths, leaderKnown, leaderAlive, SquadConfig.get(SquadConfig.MORALE_FRACTION));
        if (c == Cause.NONE) return;
        broken = c;
        brokeAt = now;
        SquadMind.Mode mode = SquadMind.Mode.of(SquadConfig.get(SquadConfig.MORALE_BREAK));
        int left = alive.size();
        for (Mob m : alive) {
            Member mem = members.get(m.getUUID());
            if (mem == null) continue;
            mem.reaction = SquadMind.react(mode, mem.role, left);
            mem.reactUntil = now + SquadConfig.get(SquadConfig.ENRAGE_TICKS);
            if (mem.reaction == Reaction.ENRAGE) enrage(m);
            else if (mem.reaction == Reaction.FLEE) m.setTarget(null);
        }
        SquadAnnounce.broken(level, this, c, centre);
    }

    private static void enrage(Mob m) {
        AttributeInstance speed = m.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.addOrReplacePermanentModifier(new AttributeModifier(ENRAGE_SPEED, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        AttributeInstance dmg = m.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) dmg.addOrReplacePermanentModifier(new AttributeModifier(ENRAGE_DAMAGE, 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    // ---------------------------------------------------------------- what the goals read
    /** The place of the ward the given mob covers, or null. A tank covers the ward matching its rank among the tanks. */
    V2 wardPosFor(Mob mob, Member me) {
        if (wards.isEmpty() || me == null) return null;
        int tankRank = 0;
        for (Member m : members.values()) {
            if (m == me) break;
            if (m.role.isFrontLine() && !m.dead) tankRank++;
        }
        UUID w = wards.get(Math.min(tankRank, wards.size() - 1));
        if (w.equals(mob.getUUID())) return null;
        Member wm = members.get(w);
        Mob wmob = wm == null ? null : wm.mob();
        return wmob == null ? null : new V2(wmob.getX(), wmob.getZ());
    }

    V2 wardPos() {
        if (wardId == null) return null;
        Member wm = members.get(wardId);
        Mob wmob = wm == null ? null : wm.mob();
        return wmob == null ? null : new V2(wmob.getX(), wmob.getZ());
    }

    /** The snapshot a role decision reads for this mob, or null when it is not in this squad. */
    public RoleMotion.Ctx contextFor(Mob mob, long now) {
        Member me = members.get(mob.getUUID());
        if (me == null) return null;
        LivingEntity t = target();
        V2 self = new V2(mob.getX(), mob.getZ());
        V2 tp = t == null ? null : new V2(t.getX(), t.getZ());
        double hp = mob.getMaxHealth() <= 0 ? 1.0 : mob.getHealth() / mob.getMaxHealth();
        boolean engaged = tankEngaged(now);
        int since = engaged && engagedSince >= 0L ? (int) Math.min(100000L, now - engagedSince) : 0;
        V2 threat = tp;
        return new RoleMotion.Ctx(me.role, me.slot, self, hp, tp, t == null ? 0.0 : t.getYHeadRot(), wardPosFor(mob, me), centre, yaw, leaderPos, me.offset, engaged, since,
                me.waited, threat, me.flip, mob.getBbWidth() * 2.0 + 0.6);
    }

    /** True once the squad has a target to fight (the goals that only matter in a fight ask this first). */
    public boolean fighting() { return target() != null; }

    /** The ticks since the squad was formed. */
    public long age(long now) { return now - createdAt; }

    long lastAliveAt() { return lastAliveAt; }

    long brokeAt() { return brokeAt; }

    UUID targetId() { return targetId; }

    boolean armed() { return armed; }

    /** "6: 2 tank, 1 melee, ..." from the roles the members really hold. */
    public String describe() {
        int[] n = new int[SquadRole.values().length];
        int total = 0;
        for (Member m : members.values()) {
            if (m.dead) continue;
            n[m.role.ordinal()]++;
            total++;
        }
        StringBuilder b = new StringBuilder().append(total).append(':');
        boolean first = true;
        for (SquadRole r : SquadRole.values()) {
            if (n[r.ordinal()] == 0) continue;
            b.append(first ? " " : ", ").append(n[r.ordinal()]).append(' ').append(r.id());
            first = false;
        }
        return b.toString();
    }

    /** Removes the enrage modifiers of a mob (a self-test undoes its work; a real enraged mob keeps them for life). */
    static void calm(Mob m) {
        AttributeInstance speed = m.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(ENRAGE_SPEED);
        AttributeInstance dmg = m.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) dmg.removeModifier(ENRAGE_DAMAGE);
    }

    /** Is this healer allowed to heal this ally: alive, hurt, not a boss, not itself unless badly hurt? (BalanceLaw.isBoss is the mod's one test for a boss.) */
    static boolean healable(LivingEntity ally) { return ally.isAlive() && !BalanceLaw.isBoss(ally); }
}
