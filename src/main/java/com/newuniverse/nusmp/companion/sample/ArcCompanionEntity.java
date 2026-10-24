package com.newuniverse.nusmp.companion.sample;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcNpcEntity;
import com.newuniverse.nusmp.companion.CompanionConfig;
import com.newuniverse.nusmp.companion.CompanionRules;
import com.newuniverse.nusmp.companion.bond.Bond;
import com.newuniverse.nusmp.companion.bond.Companion;
import com.newuniverse.nusmp.companion.bond.CompanionData;
import com.newuniverse.nusmp.companion.bond.PersonState;
import com.newuniverse.nusmp.companion.bond.Stance;
import com.newuniverse.nusmp.companion.follow.CompanionTravel;
import com.newuniverse.nusmp.companion.follow.PortalFollower;
import com.newuniverse.nusmp.companion.follow.TravelRules;
import com.newuniverse.nusmp.companion.talk.Bubble;
import com.newuniverse.nusmp.companion.talk.Talk;
import com.newuniverse.nusmp.companion.talk.TalkView;
import com.newuniverse.nusmp.companion.talk.Talkable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * Why: the ONE sample companion that proves the whole system end to end, and it is an existing canon character, not a new one: Charmy Pappitson of the arcs' roster (her model, name, colours and talk clip), as a
 * subclass of the arcs' friendly character so nothing of the thirty shared characters changes. A right click opens the TALK SCREEN (four tabs); gifts, the high five, stances and the pouch are the shared bond system;
 * a friend can be asked to walk with the player (she follows, comes through dimension changes, fights by her stance with {@code Bond.wants}) and to wait again; she says things in speech bubbles. It is
 * creative / spawn egg only. Everything is decided on the server; the client only draws the screen. With {@code companions.enabled} off, a right click does the old thing (the arcs' chat talk).
 */
public class ArcCompanionEntity extends ArcNpcEntity implements Talkable, Companion, PortalFollower {
    private static final EntityDataAccessor<Byte> SOCIAL = SynchedEntityData.defineId(ArcCompanionEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> SOCIAL_AT = SynchedEntityData.defineId(ArcCompanionEntity.class, EntityDataSerializers.INT);

    private final PersonState bond = new PersonState();
    private UUID owner;
    private int socialUntil;
    private Player followedCache;
    private int followedAt = -100;

    public ArcCompanionEntity(EntityType<? extends ArcCompanionEntity> type, Level level) {
        super(type, level);
        setCharacter(CompanionProfiles.CHARMY.characterId());
    }

    public static AttributeSupplier.Builder attributes() {
        return ArcNpcEntity.attributes().add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.MAX_HEALTH, 60.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(SOCIAL, (byte) 0);
        b.define(SOCIAL_AT, 0);
    }

    private CompanionProfiles.Profile profile() { return CompanionProfiles.of(characterId()); }

    // ================================================================ the right click
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!CompanionConfig.enabled()) return super.mobInteract(player, hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) Talk.open(sp, this);
        return InteractionResult.CONSUME;
    }

    // ================================================================ talking
    @Override
    public TalkView talkOpen(ServerPlayer p) {
        beginTalk(p);
        Component greet = Component.translatable(profile().langPrefix() + "greet", p.getName());
        Bubble.say(this, greet);
        return view(p, greet);
    }

    @Override
    public TalkView talkChoose(ServerPlayer p, String id) {
        Component shared = Bond.choose(this, p, id);                       // every k: id is the bond system's: gifts, the high five, stances, the pouch
        if (shared != null) {
            beginTalk(p);
            return view(p, shared);
        }
        String lp = profile().langPrefix();
        switch (id) {
            case "t:self" -> {
                return view(p, Component.translatable(lp + "a.self"));
            }
            case "t:food" -> {
                return view(p, Component.translatable(lp + "a.food"));
            }
            case "t:magic" -> {
                return view(p, Component.translatable(lp + "a.magic"));
            }
            case "t:join" -> {
                return view(p, join(p));
            }
            case "t:wait" -> {
                return view(p, wait(p));
            }
            default -> {
                return view(p, Component.translatable(lp + "greet", p.getName()));
            }
        }
    }

    private TalkView view(ServerPlayer p, Component text) {
        String lp = profile().langPrefix();
        ArcCharacter c = character();
        TalkView.Builder b = TalkView.builder(getName(), Component.translatable(lp + "sub"), c == null ? profile().fallbackColor() : c.color()).text(text);
        b.add("t:self", Component.translatable(lp + "q.self"), TalkView.TALK);
        b.add("t:food", Component.translatable(lp + "q.food"), TalkView.TALK);
        b.add("t:magic", Component.translatable(lp + "q.magic"), TalkView.TALK);
        if (owner != null && owner.equals(p.getUUID())) {
            b.add("t:wait", Component.translatable(lp + "q.wait"), TalkView.INTERACT);
        } else if (owner != null) {
            b.addBlocked("t:join", Component.translatable(lp + "q.join"), TalkView.INTERACT, Component.translatable(lp + "join.taken"));
        } else if (bondTier(p) < CompanionRules.ACQUAINTANCE) {
            b.addBlocked("t:join", Component.translatable(lp + "q.join"), TalkView.INTERACT, Component.translatable(lp + "join.need"));
        } else if (CompanionTravel.followersOf(p, 64.0).size() >= CompanionConfig.maxFollowers()) {
            b.addBlocked("t:join", Component.translatable(lp + "q.join"), TalkView.INTERACT, Component.translatable("nusmp.companion.join.full"));
        } else {
            b.add("t:join", Component.translatable(lp + "q.join"), TalkView.INTERACT);
        }
        Bond.options(b, this, p);                                          // adds the shared choices to the tabs
        return b.build();
    }

    private Component join(ServerPlayer p) {
        String lp = profile().langPrefix();
        if (owner != null && !owner.equals(p.getUUID())) return Component.translatable(lp + "join.taken");
        if (owner == null) {
            if (bondTier(p) < CompanionRules.ACQUAINTANCE) return Component.translatable(lp + "join.need");
            if (CompanionTravel.followersOf(p, 64.0).size() >= CompanionConfig.maxFollowers()) return Component.translatable("nusmp.companion.join.full");
            owner = p.getUUID();
            followedCache = null;
        }
        Component line = Component.translatable(lp + "a.join");
        Bubble.say(this, line);
        return line;
    }

    private Component wait(ServerPlayer p) {
        if (owner == null || !owner.equals(p.getUUID())) return Component.translatable(profile().langPrefix() + "a.wait");
        owner = null;
        followedCache = null;
        getNavigation().stop();
        setHome(blockPosition());                                          // she stays around here now
        Component line = Component.translatable(profile().langPrefix() + "a.wait");
        Bubble.say(this, line);
        return line;
    }

    // ================================================================ the bond system
    @Override public String bondKey() { return profile().bondKey(); }

    @Override public String langPrefix() { return profile().langPrefix(); }

    @Override public PersonState bond() { return bond; }

    @Override public int bondRapport(ServerPlayer p) { return CompanionData.rapport(p, bondKey()); }

    @Override public void bondAddRapport(ServerPlayer p, int n) { CompanionData.addRapport(p, bondKey(), n); }

    @Override public UUID bondOwner() { return owner; }

    @Override public boolean fighter() { return true; }

    @Override public void bondSpeak(Component line) { Bubble.say(this, line); }

    @Override
    public void bondFeel(boolean eating, int ticks) {
        if (level() instanceof ServerLevel sl) sl.sendParticles(eating ? Fx.options(FxThemes.WORLD, FxOld.HAPPY_VILLAGER) : ParticleTypes.ANGRY_VILLAGER, getX(), getY() + 2.0, getZ(), 4, 0.3, 0.2, 0.3, 0.0);
    }

    @Override public int socialKind() { return CompanionRules.cleanKind(entityData.get(SOCIAL)); }

    @Override
    public float socialAge(float pt) { return socialKind() == NONE ? -1f : level().getGameTime() + pt - entityData.get(SOCIAL_AT); }

    @Override
    public void social(int kind, int ticks) {
        entityData.set(SOCIAL, (byte) CompanionRules.cleanKind(kind));
        entityData.set(SOCIAL_AT, (int) level().getGameTime());
        socialUntil = ticks < 0 ? Integer.MAX_VALUE : tickCount + ticks;
        if (kind != NONE && level() instanceof ServerLevel sl) Fx.spray(sl, FxThemes.WORLD, FxOld.HAPPY_VILLAGER, getX(), getY() + 1.6, getZ(), 5, 0.4, 0.3, 0.4, 0.0);
    }

    @Override public UUID followedPlayer() { return owner; }

    /** The player she walks with, if online in this level (looked up at most twice a second). */
    Player followed() {
        if (owner == null) return null;
        if (tickCount - followedAt >= 10 || (followedCache != null && !followedCache.isAlive())) {
            followedAt = tickCount;
            followedCache = level().getPlayerByUUID(owner);
        }
        return followedCache != null && followedCache.isAlive() && !followedCache.isSpectator() ? followedCache : null;
    }

    // ================================================================ every tick
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        Bond.tick(this);                                                    // the pouch looking after the player
        if (socialKind() != NONE && tickCount >= socialUntil) social(NONE, 0);          // the high five ends
    }

    /** While she walks with someone she is not held to the post she was placed at. */
    @Override
    public boolean isWithinRestriction() { return owner != null || super.isWithinRestriction(); }

    // ================================================================ goals
    @Override
    protected void registerGoals() {
        super.registerGoals();                                              // the arcs' float / face-the-talker / post / stroll / look goals
        goalSelector.addGoal(1, new StanceMelee(this));
        goalSelector.addGoal(2, new FollowPlayerGoal(this));
        targetSelector.addGoal(1, new StanceTargets(this));
    }

    /** Walks to the player she walks with when she is more than six blocks behind; puts herself beside them when she is more than 24 blocks behind. */
    static final class FollowPlayerGoal extends Goal {
        private final ArcCompanionEntity me;
        private int recalc, teleportCooldown;

        FollowPlayerGoal(ArcCompanionEntity me) {
            this.me = me;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player o = me.followed();
            return o != null && !me.talking() && me.distanceToSqr(o) > TravelRules.START_FOLLOW * TravelRules.START_FOLLOW;
        }

        @Override
        public boolean canContinueToUse() {
            Player o = me.followed();
            return o != null && !me.talking() && me.distanceToSqr(o) > TravelRules.KEEP_DISTANCE * TravelRules.KEEP_DISTANCE;
        }

        @Override public void start() { recalc = 0; }

        @Override public void stop() { me.getNavigation().stop(); }

        @Override
        public void tick() {
            Player o = me.followed();
            if (o == null) return;
            me.getLookControl().setLookAt(o, 10.0f, me.getMaxHeadXRot());
            if (teleportCooldown > 0) teleportCooldown--;
            if (--recalc > 0) return;
            recalc = 10;
            if (TravelRules.tooFarBehind(me.distanceToSqr(o)) && teleportCooldown == 0 && me.level() instanceof ServerLevel sl) {
                Vec3 spot = CompanionTravel.findSpot(sl, o.position(), me);
                me.getNavigation().stop();
                me.teleportTo(spot.x, spot.y, spot.z);
                teleportCooldown = 40;
            } else {
                me.getNavigation().moveTo(o, 1.15);
            }
        }
    }

    /** Fights only in a stance that fights. */
    static final class StanceMelee extends MeleeAttackGoal {
        private final ArcCompanionEntity me;

        StanceMelee(ArcCompanionEntity me) {
            super(me, 1.2, true);
            this.me = me;
        }

        @Override
        public boolean canUse() { return me.bond != null && me.bond.stance != Stance.PASSIVE && super.canUse(); }
    }

    /**
     * Picks a target by her stance: one box query around her every 10 ticks while she walks with someone (every 40 when she walks with nobody), none at all when PASSIVE; candidates go through
     * {@code Bond.wants}, which refuses players, people, companions, pets and allies.
     */
    static final class StanceTargets extends TargetGoal {
        private final ArcCompanionEntity me;
        private LivingEntity pick;
        private int cooldown;

        StanceTargets(ArcCompanionEntity me) {
            super(me, false);
            this.me = me;
            setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (me.bond == null || me.bond.stance == Stance.PASSIVE || me.talking()) return false;
            if (--cooldown > 0) return false;
            cooldown = me.owner != null ? 10 : 40;
            Player o = me.followed();
            AABB box = me.getBoundingBox().inflate(16.0, 6.0, 16.0);
            List<LivingEntity> found = me.level().getEntitiesOfClass(LivingEntity.class, box, c -> Bond.wants(me, me, o, c));
            if (found.isEmpty()) return false;
            found.sort(Comparator.comparingDouble(c -> c.distanceToSqr(me)));
            pick = found.get(0);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity t = me.getTarget();
            return t != null && t.isAlive() && me.bond.stance != Stance.PASSIVE && me.distanceToSqr(t) < 32.0 * 32.0 && Bond.foe(me, me.followed(), t);
        }

        @Override
        public void start() {
            me.setTarget(pick);
            super.start();
        }

        @Override
        public void stop() {
            pick = null;
            me.setTarget(null);
        }
    }

    // ================================================================ save data
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        bond.save(tag, registryAccess());
        if (owner != null) tag.putUUID("CompanionOwner", owner);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        bond.load(tag, registryAccess());
        owner = tag.hasUUID("CompanionOwner") ? tag.getUUID("CompanionOwner") : null;
        followedCache = null;
    }
}
