package com.newuniverse.nusmp.avatar.cast;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.avatar.AvatarAiConfig;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.AvatarPersonalityTraits;
import com.newuniverse.nusmp.avatar.ai.HasDndStats;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.entity.TensuraCaster;
import com.newuniverse.nusmp.entity.zogratis.SpellSeal;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: grimoire pages are written for a {@code ServerPlayer} (they read the view vector, the eye position, persistent data, Tensura storage,
 * the held weapon). An Avatar is a mob. So each Avatar gets ONE lazily created FakePlayer "body double", named after it and carrying the AVATAR'S
 * OWN UUID, never added to the level's entity or player list (so nobody sees it, it is on no tab list, earns no advancements, saves nothing).
 * Before every cast {@link #sync} copies the avatar's position, aim at the target, held items, health fraction, dimension and its Tensura
 * strength onto the double; after the cast the effects the spell had on its caster (movement, effects) are copied back.
 *
 * <p>What it does NOT do: it never calls {@code GrimoireBook.castPage} (that needs a real player's key press, charge and mana). It runs the page's
 * own {@code Cast} and pays the cost itself, from the AVATAR'S pool: the cost is computed with the book's real formula against the double
 * (whose max magicule mirrors the avatar's pool), then taken from {@link AvatarManaPool} stored in the avatar's persistent data. A frenzied
 * Yandere skips the cost. Cooldowns are kept in the avatar's persistent data, scaled by INT/WIS, personality and frenzy
 * ({@link AvatarCastMath}). Only pages {@link AvatarSkillList} allows are ever cast.
 *
 * <p>Damage: the spells hurt with the double as the attacker. {@code AvatarCasterEvents} turns that into the AVATAR being the attacker
 * (the double's UUID is the avatar's, so even a projectile that forgets its cached owner resolves to the avatar) and drops hits on the creator,
 * the creator's team, pets, villagers and other Avatars, because area pages hit "everyone but the caster".
 */
public final class SimulatedPlayerCaster extends FakePlayer {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<UUID, SimulatedPlayerCaster> CACHE = new HashMap<>();
    private static final Map<UUID, GrimoireCopy> BOOKS = new HashMap<>();

    static final String TAG_COOLDOWNS = "nusmp_av_cd", TAG_MANA = "nusmp_av_mana", TAG_MANA_AT = "nusmp_av_mana_at", TAG_GAP = "nusmp_av_cast_gap";
    private static final int BOOK_REFRESH_TICKS = 600;

    private record GrimoireCopy(GrimoireBook book, ManasSkillInstance instance, long at) {}

    /** Why a cast did or did not happen (for the goal's decision and the tests of the glue). */
    public enum Result { OK, DISABLED, NO_GRIMOIRE, NOT_ALLOWED, SEALED, COOLDOWN, NO_MANA, FAILED }

    private final UUID avatarId;
    private double mirroredMagicule = -1, mirroredEp = -1;

    private SimulatedPlayerCaster(ServerLevel level, AvatarEntity avatar) {
        super(level, new GameProfile(avatar.getUUID(), profileName(avatar.avatarName())));
        this.avatarId = avatar.getUUID();
    }

    private static String profileName(String name) {
        StringBuilder sb = new StringBuilder();
        if (name != null) for (char c : name.toCharArray()) if (sb.length() < 16 && (Character.isLetterOrDigit(c) && c < 128 || c == '_')) sb.append(c);
        return sb.length() == 0 ? "Avatar" : sb.toString();
    }

    /** The Avatar's body double, created on first use; null on the client. Recreated when the Avatar changed dimension. */
    public static SimulatedPlayerCaster of(AvatarEntity avatar) {
        if (avatar == null || !(avatar.level() instanceof ServerLevel sl)) return null;
        SimulatedPlayerCaster c = CACHE.get(avatar.getUUID());
        if (c == null || c.level() != sl) {
            c = new SimulatedPlayerCaster(sl, avatar);
            CACHE.put(avatar.getUUID(), c);
        }
        return c;
    }

    /** Forgets an Avatar's double and book copy (it left the world). */
    public static void forget(UUID avatarId) {
        CACHE.remove(avatarId);
        BOOKS.remove(avatarId);
        KITS.remove(avatarId);
    }

    public static void forgetAll() {
        CACHE.clear();
        BOOKS.clear();
        KITS.clear();
    }

    public UUID avatarId() { return avatarId; }

    // ---------------------------------------------------------------- the double is not a person
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public boolean isInvulnerableTo(DamageSource source) { return true; }
    @Override public boolean isSpectator() { return false; }
    @Override public boolean isCreative() { return false; }

    // ---------------------------------------------------------------- grimoire resolution
    /** The grimoire (book + a private COPY of its instance) this Avatar casts from, or null. Refreshed every 30 s while its source is available. */
    private GrimoireCopy grimoire(AvatarEntity avatar) {
        GrimoireCopy have = BOOKS.get(avatarId);
        long now = avatar.level().getGameTime();
        if (have != null && now - have.at < BOOK_REFRESH_TICKS) return have;
        ManasSkillInstance src = null;
        try {
            if (avatar instanceof HasGrimoire hg) src = hg.avatarGrimoire();
            if (src == null) {
                ServerPlayer creator = avatar.creatorPlayer();
                if (creator != null) src = GrimoirePages.grimoireOf(creator).orElse(null);
            }
        } catch (RuntimeException | LinkageError soft) {
            LOG.debug("[nusmp] Avatar grimoire lookup failed", soft);
        }
        if (src == null || !(src.getSkill() instanceof GrimoireBook book)) return have;       // keep the old copy while the creator is away
        GrimoireCopy fresh = new GrimoireCopy(book, src.copy(), now);
        BOOKS.put(avatarId, fresh);
        return fresh;
    }

    public boolean hasGrimoire(AvatarEntity avatar) { return grimoire(avatar) != null; }

    /** The castable attack pages right now (allowed, unlocked by the creator, off cooldown, affordable) for the planner. */
    public List<AvatarCastPlanner.Candidate> candidates(AvatarEntity avatar) {
        List<AvatarCastPlanner.Candidate> out = new ArrayList<>();
        GrimoireCopy g = grimoire(avatar);
        if (g == null || !AvatarAiConfig.castEnabled() || SpellSeal.sealed(avatar)) return out;
        long now = avatar.level().getGameTime();
        double pool = poolNow(avatar);
        sync(avatar, null);
        List<BookPage> pages = g.book.pages();
        for (int mode = 0; mode < pages.size(); mode++) {
            BookPage p = pages.get(mode);
            AvatarSkillList.Kind kind = AvatarSkillList.kind(p.id(), p.costPercent());
            if (kind != AvatarSkillList.Kind.ATTACK || p.weaponTag() != null) continue;
            if (!g.book.usable(g.instance, avatar, mode) || cooldownLeft(avatar, p.id(), now) > 0) continue;
            if (!avatar.frenzied() && !AvatarManaPool.canPay(pool, estimateCost(avatar, g, mode))) continue;
            out.add(new AvatarCastPlanner.Candidate(mode, p.id(), p.costPercent(), AvatarCastPlanner.looksMelee(p.id()), kind));
        }
        return out;
    }


    /** The display name of a page (for the shout). Null when the mode does not exist. */
    public String pageName(AvatarEntity avatar, int mode) {
        GrimoireCopy g = grimoire(avatar);
        BookPage p = g == null ? null : g.book.page(mode);
        return p == null ? null : p.name();
    }

    // ---------------------------------------------------------------- the cast
    /** Copies the avatar's state onto the double and aims it at {@code aimAt} (a point, usually the target's body). */
    public void sync(AvatarEntity avatar, Vec3 aimAt) {
        moveTo(avatar.getX(), avatar.getY(), avatar.getZ(), avatar.getYRot(), avatar.getXRot());
        setDeltaMovement(Vec3.ZERO);
        tickCount = avatar.tickCount;
        setItemSlot(EquipmentSlot.MAINHAND, avatar.getMainHandItem());
        setItemSlot(EquipmentSlot.OFFHAND, avatar.getOffhandItem());
        var maxHp = getAttribute(Attributes.MAX_HEALTH);
        if (maxHp != null && maxHp.getBaseValue() != avatar.getMaxHealth()) maxHp.setBaseValue(avatar.getMaxHealth());
        setHealth(Math.max(1f, getMaxHealth() * (avatar.getHealth() / Math.max(1f, avatar.getMaxHealth()))));
        double max = poolMax(avatar);
        if (max != mirroredMagicule) { EnergyHelper.setMaxMagicule(this, max); mirroredMagicule = max; }
        double ep = EnergyHelper.getMaxEP(avatar);
        if (ep != mirroredEp && ep > 0) { EnergyHelper.setBaseMaxEP(this, ep); mirroredEp = ep; }
        if (aimAt != null) {
            lookAt(EntityAnchorArgument.Anchor.EYES, aimAt);
            avatar.lookAt(EntityAnchorArgument.Anchor.EYES, aimAt);
            setYHeadRot(getYRot());
            yBodyRot = getYRot();
        }
    }

    /**
     * Casts the page {@code mode} of the Avatar's grimoire at {@code target}. All checks, the cost, the cooldown and the frenzy burn happen here.
     * Safe: an exception inside a spell is swallowed by {@link SafeCast} (the book is put on its recovery cooldown like for a player).
     */
    public Result castPage(AvatarEntity avatar, int mode, LivingEntity target) {
        if (!AvatarAiConfig.castEnabled()) return Result.DISABLED;
        GrimoireCopy g = grimoire(avatar);
        if (g == null) return Result.NO_GRIMOIRE;
        BookPage p = g.book.page(mode);
        if (p == null || p.weaponTag() != null || !AvatarSkillList.allowedPage(p.id(), p.costPercent()) || !g.book.usable(g.instance, avatar, mode)) return Result.NOT_ALLOWED;
        if (SpellSeal.sealed(avatar)) return Result.SEALED;
        long now = avatar.level().getGameTime();
        if (cooldownLeft(avatar, p.id(), now) > 0) return Result.COOLDOWN;
        if (SafeCast.blocked(g.book.magicId(), now)) return Result.FAILED;
        sync(avatar, target == null ? null : target.getBoundingBox().getCenter());
        boolean free = avatar.frenzied();
        double cost = free ? 0 : estimateCost(avatar, g, mode);
        double pool = poolNow(avatar);
        if (!free && !AvatarManaPool.canPay(pool, cost)) return Result.NO_MANA;
        final int m = mode;
        boolean ok = SafeCast.run(g.book.magicId(), now, this, () -> p.cast().cast(g.book, g.instance, this, m));
        syncBack(avatar);
        if (!ok) return Result.FAILED;
        if (!free) setPool(avatar, AvatarManaPool.pay(pool, cost));
        startCooldown(avatar, p.id(), p.cooldown(), now);
        if (free) com.newuniverse.nusmp.avatar.yandere.YandereFrenzyHandler.burnForCast(avatar);
        return Result.OK;
    }

    /**
     * Casts one Tensura attack magic of the {@link AvatarSkillList} allowlist with the AVATAR itself as the caster, the way the mod's bosses and
     * summons do ({@code entity.TensuraCaster}: learned through ManasCore, aimed, pressed / held / released). No double is needed because these skills
     * take a LivingEntity. The cost is a flat share of the Avatar's pool (a frenzied Avatar pays none and burns health instead). Returns the skill
     * id cast, or null when nothing was ready, affordable or allowed.
     */
    public static ResourceLocation castTensura(AvatarEntity avatar, LivingEntity target) {
        if (!AvatarAiConfig.castEnabled() || !(avatar.level() instanceof ServerLevel) || SpellSeal.sealed(avatar)) return null;
        long now = avatar.level().getGameTime();
        List<ResourceLocation> kit = KITS.computeIfAbsent(avatar.getUUID(), k -> TensuraCaster.learn(avatar, 3, AvatarSkillList.tensuraKeywords()));
        kit = kit.stream().filter(id -> AvatarSkillList.allowedTensuraSkill(id.getPath())).toList();
        if (kit.isEmpty() || cooldownLeft(avatar, "tensura_cast", now) > 0) return null;
        double pool = poolNow(avatar), cost = avatar.frenzied() ? 0 : poolMax(avatar) * TENSURA_COST_SHARE;
        if (!AvatarManaPool.canPay(pool, cost)) return null;
        ResourceLocation cast = TensuraCaster.cast(avatar, target, kit, 6);
        if (cast == null) return null;
        if (cost > 0) setPool(avatar, AvatarManaPool.pay(pool, cost));
        startCooldown(avatar, "tensura_cast", 100, now);
        if (avatar.frenzied()) com.newuniverse.nusmp.avatar.yandere.YandereFrenzyHandler.burnForCast(avatar);
        return cast;
    }

    /** Share of the pool one Tensura magic cast costs. */
    static final double TENSURA_COST_SHARE = 0.06;
    private static final Map<UUID, List<ResourceLocation>> KITS = new HashMap<>();

    /** After a spell: what it did to its caster is done to the avatar (a dash moves it, a buff buffs it), then the double is reset. */
    private void syncBack(AvatarEntity avatar) {
        try {
            Vec3 d = getDeltaMovement();
            if (d.lengthSqr() > 1.0e-4) avatar.setDeltaMovement(avatar.getDeltaMovement().add(d));
            double moved = position().distanceToSqr(avatar.position());
            if (moved > 0.01 && moved < 24 * 24 && avatar.level().noCollision(avatar, avatar.getBoundingBox().move(position().subtract(avatar.position()))))
                avatar.teleportTo(getX(), getY(), getZ());
            for (MobEffectInstance e : new ArrayList<>(getActiveEffects())) {
                avatar.addEffect(new MobEffectInstance(e));
                removeEffect(e.getEffect());
            }
        } catch (RuntimeException soft) {
            LOG.debug("[nusmp] Avatar caster sync-back failed", soft);
        }
    }

    // ---------------------------------------------------------------- cost and pool
    private double estimateCost(AvatarEntity avatar, GrimoireCopy g, int mode) {
        BookPage p = g.book.page(mode);
        if (p == null) return Double.MAX_VALUE;
        try {
            return Math.max(0, g.book.getMagiculeCost(this, g.instance, mode));
        } catch (RuntimeException soft) {
            return BalanceLaw.cost(this, p.costPercent(), p.costFloor());
        }
    }

    static int intelligence(AvatarEntity a) { return a instanceof HasDndStats s ? s.intelligence() : 10; }

    static int wisdom(AvatarEntity a) { return a instanceof HasDndStats s ? s.wisdom() : 10; }

    /** The pool maximum: Tensura's magicule maximum for this avatar, or the floor that grows with INT. */
    public static double poolMax(AvatarEntity avatar) {
        double t = 0;
        try { t = EnergyHelper.getMaxMagicule(avatar); } catch (RuntimeException | LinkageError soft) { /* no Tensura data: use the floor */ }
        return AvatarManaPool.max(t, intelligence(avatar));
    }

    /** Current magicules of the avatar (regenerated lazily from the last time it was looked at, so it needs no per-tick work). */
    public static double poolNow(AvatarEntity avatar) {
        CompoundTag d = avatar.getPersistentData();
        long now = avatar.level().getGameTime();
        double max = poolMax(avatar);
        if (!d.contains(TAG_MANA, Tag.TAG_DOUBLE)) { d.putDouble(TAG_MANA, max); d.putLong(TAG_MANA_AT, now); return max; }
        long at = d.getLong(TAG_MANA_AT);
        double cur = AvatarManaPool.regen(d.getDouble(TAG_MANA), max, (int) Math.max(0, Math.min(now - at, 1_000_000)));
        d.putDouble(TAG_MANA, cur);
        d.putLong(TAG_MANA_AT, now);
        return cur;
    }

    static void setPool(AvatarEntity avatar, double value) {
        avatar.getPersistentData().putDouble(TAG_MANA, value);
        avatar.getPersistentData().putLong(TAG_MANA_AT, avatar.level().getGameTime());
    }

    // ---------------------------------------------------------------- cooldowns
    static int cooldownLeft(AvatarEntity avatar, String id, long now) {
        CompoundTag cds = avatar.getPersistentData().getCompound(TAG_COOLDOWNS);
        return (int) Math.max(0, cds.getLong(id) - now);
    }

    private static void startCooldown(AvatarEntity avatar, String id, int pageTicks, long now) {
        var traits = AvatarPersonalityTraits.combine(avatar.primary(), avatar.secondary());
        int ticks = AvatarCastMath.pageCooldownTicks(pageTicks, AvatarPersonalityTraits.mentalCooldownMult(intelligence(avatar), wisdom(avatar)),
                traits.cooldownMult(), avatar.cooldownMultiplier(), avatar.frenzied());
        CompoundTag root = avatar.getPersistentData();
        CompoundTag cds = root.getCompound(TAG_COOLDOWNS);
        cds.putLong(id, now + ticks);
        if (cds.size() > 64) cds.getAllKeys().removeIf(k -> cds.getLong(k) <= now);         // stays small over a long life
        root.put(TAG_COOLDOWNS, cds);
        int gap = AvatarCastMath.gapTicks(AvatarAiConfig.castGapTicks(),
                AvatarPersonalityTraits.mentalCooldownMult(intelligence(avatar), wisdom(avatar)), traits.cooldownMult(), avatar.cooldownMultiplier(), avatar.frenzied());
        root.putLong(TAG_GAP, now + gap);
    }

    /** Is the Avatar past its global gap between two casts? */
    public static boolean gapOver(AvatarEntity avatar) { return avatar.level().getGameTime() >= avatar.getPersistentData().getLong(TAG_GAP); }
}
