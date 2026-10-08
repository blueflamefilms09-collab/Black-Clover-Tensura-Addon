package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.NUGameRules;
import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.antimagic.NihilityZone;
import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.TimeStop;
import com.newuniverse.nusmp.blackclover.TimedModifiers;
import com.newuniverse.nusmp.book.AntiMagicBook;
import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 0.48 Genesis: Asta's Demon-Slayer Sword raised to a God-class existence (docs/demon_slayer_genesis_spec.md). It replaces the
 * 0.28 item at the same id ("demon_slayer_sword"), so swords already drawn keep working, and Black Divider stays on right-click.
 * <ul>
 *   <li><b>Conceptual Severance</b> (a full swing): EP-scaled true damage through the nusmp:conceptual_severance damage type
 *       (armour, enchantments, resistance and the hit cooldown are ignored) plus spiritual damage. A player loses at most the PvP
 *       hit cap of their max health per hit (40% by default), a boss 15%.</li>
 *   <li><b>Infinite durability</b> (unbreakable; old damage mends).</li>
 *   <li><b>Conceptual Nullification Field</b> (15 blocks, while held, checked every second): foes' Ultimate skills are interfered
 *       with (forced cooldown, toggles may misfire off), their magicules bleed away (1% of max a second, from the current pool),
 *       and stopped time can't hold the wielder or allies.</li>
 *   <li><b>Black Meteorite — Void Severance</b> (sneak + right-click): locks on to the strongest energy signature ahead (100
 *       blocks), crosses the gap as an anti-magic meteor cutting through whatever is on the way, severs the target twice and
 *       leaves a Nihility zone ({@link NihilityZone}) that shatters barriers. Costs 80% of max aura (stamina); 2 minutes at the
 *       default cooldown setting.</li>
 *   <li><b>Resonance:</b> the pommel's five-leaf clover blooms crimson near high-EP beings (stack data "Resonance" 0..4, read by
 *       the client renderer), and the blade breaks into shards while Black Meteorite runs ("MeteorUntil").</li>
 * </ul>
 * <b>Never a permanent player nerf:</b> Void Severance's max-EP cut on a player (or a player's pet or summon) is a transient
 * attribute modifier that ends after 60 s, on death and on relog. Only wild mobs lose max EP for good.
 */
public class DemonSlayerSwordItem extends MagicWeaponItem {
    public static final ResourceKey<DamageType> SEVERANCE =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("nusmp", "conceptual_severance"));
    static final ResourceLocation VOID_CUT = ResourceLocation.fromNamespaceAndPath("nusmp", "void_severance");
    static final ResourceLocation BLADE_GROWTH = ResourceLocation.fromNamespaceAndPath("nusmp", "demon_slayer_growth");
    public static final int CRIMSON = 0xFFC0102A, VOID = 0xFF0A0408;
    public static final double FIELD = 15, LOCK_RANGE = 100, ZONE_RADIUS = 12;
    private static final int GROWTH_TICKS = 160;
    static final int METEOR_COOLDOWN = 2400, ZONE_TICKS = 200, CUT_TICKS = 1200;
    static final String K_METEOR = "nusmp_black_meteorite_ready", K_JAM_TOLD = "nusmp_field_jam_told";

    /** How charged each player's last swing was (taken before vanilla resets it), for the severance. Server thread only. */
    private static final Map<UUID, Float> CHARGE = new HashMap<>();

    public DemonSlayerSwordItem() {
        super(Kind.DEMON_SLAYER, new Item.Properties().rarity(Rarity.EPIC).fireResistant().component(DataComponents.UNBREAKABLE, new Unbreakable(true)));
    }

    // ---------------------------------------------------------------- damage
    /** The severance damage type (plain indirect magic if the data pack entry is missing). */
    public static DamageSource severance(LivingEntity by) {
        return by.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(SEVERANCE)
                .<DamageSource>map(h -> new DamageSource(h, by))
                .orElseGet(() -> by.damageSources().indirectMagic(by, by));
    }

    /** How strongly the wielder's anti-magic answers the blade: 1.15 Anti-Magic, 1.4 Anti-Magic Lord, 1.75 in Black Form. */
    static double affinity(LivingEntity by) {
        if (!(by instanceof Player p)) return 1.15;
        var book = AntiMagic.book(p);
        if (book.isPresent()) {
            if (AntiMagicBook.inBlackForm(book.get())) return 1.75;
            return AntiMagicBook.isLord(book.get()) ? 1.4 : 1.15;
        }
        return AntiMagic.lord(p).isPresent() ? 1.4 : 1.0;
    }

    /**
     * Conceptual Severance damage before the caps: max(50, EP^0.6 / 2) x affinity x (1 + 0.25 x magic deficiency) x the global
     * spell damage factor. Deficiency is how empty the wielder's magicule pool is (anti-magic thrives where there is no magic).
     */
    public static float genesis(LivingEntity by, LivingEntity t) {
        double ep = Nullification.maxEP(by);
        double base = Math.max(50, Math.pow(Math.max(0, ep), 0.6) * 0.5);
        double deficiency = 0;
        try {
            double max = EnergyHelper.getMaxMagicule(by);
            var ex = TensuraStorages.getExistenceFrom(by);
            if (ex != null && max > 0) deficiency = 1 - Mth.clamp(ex.getMagicule() / max, 0, 1);
        } catch (Throwable ignored) {}
        return (float) (base * affinity(by) * (1 + 0.25 * deficiency) * NUGameRules.spellDamage(t.level()));
    }

    /** The balance caps: players the PvP hit cap, bosses 15% of max health per hit. */
    static float capped(LivingEntity t, float r) {
        float c = BalanceLaw.cap(t, r);
        return BalanceLaw.isBoss(t) && !(t instanceof Player) ? Math.min(c, t.getMaxHealth() * 0.15f) : c;
    }

    /** One Conceptual Severance of 'mult' strength: true damage and a share of it as spiritual damage. Returns the damage dealt. */
    public static float sever(LivingEntity by, LivingEntity t, float mult) {
        if (t == by || !t.isAlive() || t.isAlliedTo(by)) return 0;
        float dmg = capped(t, genesis(by, t) * mult);
        if (!t.hurt(severance(by), dmg)) return 0;
        EnergyBridge.spirit(t, dmg * 0.25);
        return dmg;
    }

    // ---------------------------------------------------------------- melee
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
        if (!player.level().isClientSide) CHARGE.put(player.getUUID(), player.getAttackStrengthScale(0.5f));
        return false;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean r = super.hurtEnemy(stack, target, attacker);
        if (attacker instanceof ServerPlayer p && usableBy(stack, p) && AntiMagic.isUser(p)) {
            Float charge = CHARGE.remove(p.getUUID());
            if (charge != null && charge >= 0.9f && sever(p, target, 1f) > 0) {
                Vec3 c = target.getBoundingBox().getCenter(), look = p.getViewVector(1f);
                VfxSpawn.send(p.serverLevel(), VfxShape.ANTI_MAGIC_SLASH, c.subtract(look.scale(1.2)), c.add(look.scale(1.2)), CRIMSON, 10, 1.0f);
            }
        }
        return r;
    }

    // ---------------------------------------------------------------- the field
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        super.inventoryTick(stack, level, holder, slot, selected);
        if (level.isClientSide || stack.isEmpty() || !(holder instanceof ServerPlayer p)) return;
        if ((p.tickCount + 7) % 20 != 0) return;
        if (stack.getDamageValue() > 0) stack.setDamageValue(0);                 // infinite durability: old wear mends
        boolean held = p.getMainHandItem() == stack || p.getOffhandItem() == stack;
        if (held && usableBy(stack, p)) {
            if (AntiMagic.isUser(p)) field(p, stack);
            else setResonance(stack, 0);
            updateBladeGrowth(p, stack);
        } else {
            setResonance(stack, 0);
            removeBladeGrowth(p);
        }
    }

    private static void growBlade(ServerPlayer p, ItemStack stack) {
        long until = p.level().getGameTime() + GROWTH_TICKS;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putLong("BladeGrowthUntil", until));
        updateBladeGrowth(p, stack);
        SpellRuntime.later(p.serverLevel(), GROWTH_TICKS, () -> {
            CustomData d = stack.get(DataComponents.CUSTOM_DATA);
            if (d == null || d.copyTag().getLong("BladeGrowthUntil") <= p.level().getGameTime()) removeBladeGrowth(p);
        });
    }

    private static void updateBladeGrowth(ServerPlayer p, ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        long until = d == null ? 0 : d.copyTag().getLong("BladeGrowthUntil");
        if (until <= p.level().getGameTime() || !AntiMagic.isUser(p)) { removeBladeGrowth(p); return; }
        var range = p.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (range != null) {
            range.removeModifier(BLADE_GROWTH);
            range.addTransientModifier(new AttributeModifier(BLADE_GROWTH, 1.5, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void removeBladeGrowth(ServerPlayer p) {
        var range = p.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (range != null) range.removeModifier(BLADE_GROWTH);
    }

    /** Conceptual Nullification Field, once a second while held. */
    static void field(ServerPlayer p, ItemStack stack) {
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        TimeStop.immune(p, 40);
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(4),
                x -> x.getOwner() != p && (x.getOwner() == null || !p.isAlliedTo(x.getOwner())))) {
            projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-1.1));
            projectile.setOwner(p);
            projectile.hurtMarked = true;
        }
        double strongest = 0;
        boolean jamTurn = (now / 20) % 2 == 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(FIELD),
                x -> x != p && x.isAlive() && !x.isSpectator() && x.distanceToSqr(p) <= FIELD * FIELD)) {
            strongest = Math.max(strongest, Nullification.maxEP(e));
            if (e.isAlliedTo(p)) { TimeStop.immune(e, 40); continue; }
            if (e instanceof Player o && o.isCreative()) continue;
            if (Nullification.bleed(e, 0.01) > 0) AntiMagic.addAmp(p, 1);
            if (jamTurn && Nullification.interfere(e, 3, 0.35f, 0.2f) > 0 && e instanceof ServerPlayer sp
                    && now >= sp.getPersistentData().getLong(K_JAM_TOLD)) {
                sp.getPersistentData().putLong(K_JAM_TOLD, now + 100);
                sp.displayClientMessage(Component.literal("The Demon-Slayer's field severs your Ultimate skills.").withStyle(ChatFormatting.DARK_RED), true);
            }
        }
        setResonance(stack, resonanceOf(strongest));
    }

    /** 0 below 10k EP, then one step per tenfold: 1 (10k), 2 (100k), 3 (1M), 4 (10M+). */
    static int resonanceOf(double ep) {
        if (ep < 10_000) return 0;
        return Mth.clamp((int) Math.floor(Math.log10(ep)) - 3, 0, 4);
    }

    static void setResonance(ItemStack stack, int res) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        int was = d == null ? 0 : d.copyTag().getInt("Resonance");
        if (was != res) CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putInt("Resonance", res));
    }

    /** Stack data changes (resonance, fracture) must not replay the hand's draw animation. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    // ---------------------------------------------------------------- techniques
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            InteractionResultHolder<ItemStack> result = super.use(level, player, hand); // Black Divider
            if (result.getResult().consumesAction() && player instanceof ServerPlayer p && AntiMagic.isUser(p))
                growBlade(p, stack);
            return result;
        }
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return InteractionResultHolder.pass(stack);
        if (!usableBy(stack, p)) { GrimoireBook.fail(p, "This sword answers only to its owner."); return InteractionResultHolder.fail(stack); }
        return blackMeteorite(p, stack) ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
    }

    /** Black Meteorite — Void Severance. */
    static boolean blackMeteorite(ServerPlayer p, ItemStack stack) {
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        long ready = p.getPersistentData().getLong(K_METEOR);
        if (now < ready) { GrimoireBook.fail(p, "Black Meteorite is gathering again (" + (ready - now + 19) / 20 + "s)."); return false; }
        LivingEntity target = lockOn(p, LOCK_RANGE);
        if (target == null) { GrimoireBook.fail(p, "No energy signature to lock on to."); return false; }
        Vec3 from = p.position(), dest = landing(p, target);
        if (dest == null) { GrimoireBook.fail(p, "No room to land beside " + target.getName().getString() + "."); return false; }
        if (!EnergyBridge.aura(p, 0.8, 100)) { GrimoireBook.fail(p, "Black Meteorite needs 80% of your stamina (aura)."); return false; }

        // the charge: the body coated in anti-magic, everything on the way cut out of the air
        eraseProjectiles(p, new AABB(from, dest).inflate(2));
        for (LivingEntity t : GrimoireBook.along(p, from.add(0, 1, 0), dest.add(0, 1, 0), 1.5)) {
            if (t == target) continue;
            stripOne(t);
            sever(p, t, 0.5f);
        }
        Vec3 face = target.getEyePosition().subtract(dest.add(0, p.getEyeHeight(), 0));
        float yaw = (float) (Math.atan2(face.z, face.x) * (180 / Math.PI)) - 90f;
        float pitch = (float) -(Math.atan2(face.y, Math.sqrt(face.x * face.x + face.z * face.z)) * (180 / Math.PI));
        p.teleportTo(level, dest.x, dest.y, dest.z, yaw, pitch);
        p.fallDistance = 0;
        VfxSpawn.send(level, VfxShape.DEMON_METEOR, from.add(0, 1, 0), dest.add(0, 1, 0), CRIMSON, 24, 1.6f);
        level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.6f, 0.6f);

        // the cut: twice, the soul struck, the existence severed
        sever(p, target, 1.5f);
        soulStrike(target);
        cutExistence(target);
        SpellRuntime.later(level, 6, () -> {
            if (!target.isAlive() || !p.isAlive()) return;
            sever(p, target, 1.5f);
            Vec3 mid = target.getBoundingBox().getCenter();
            VfxSpawn.send(level, VfxShape.ANTI_MAGIC_SLASH, mid.add(-1.6, 1.2, 0), mid.add(1.6, -1.2, 0), CRIMSON, 14, 2.2f);
            level.playSound(null, mid.x, mid.y, mid.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5f, 0.5f);
        });
        Vec3 c = target.getBoundingBox().getCenter();
        VfxSpawn.send(level, VfxShape.ANTI_MAGIC_SLASH, c.add(-1.6, -1.2, 0), c.add(1.6, 1.2, 0), CRIMSON, 14, 2.2f);

        // the Nihility zone, the blade's fracture, the cost
        Vec3 ground = target.position();
        Nullification.shatterBarriers(level, ground, ZONE_RADIUS, p);
        NihilityZone.start(level, p, ground, ZONE_RADIUS, ZONE_TICKS);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putLong("MeteorUntil", now + 30));
        AntiMagic.addAmp(p, 50);
        int cd = (int) Math.round(METEOR_COOLDOWN * NUGameRules.spellCooldown(level) / 0.6);   // 2 min at the default setting
        p.getPersistentData().putLong(K_METEOR, now + (p.isCreative() ? cd / 4 : cd));
        p.displayClientMessage(Component.literal("Black Meteorite — Void Severance!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
        return true;
    }

    /**
     * The target: what the crosshair is on (up to 'range'), else the strongest energy signature within a 30 degree cone ahead,
     * hostiles and players before anything else. Energy is sensed through walls.
     */
    static LivingEntity lockOn(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f), end = eye.add(look.scale(range));
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p, eye, end, p.getBoundingBox().expandTowards(look.scale(range)).inflate(1),
                e -> e instanceof LivingEntity l && l.isAlive() && e != p && !e.isSpectator() && !l.isAlliedTo(p), range * range);
        if (hit != null && hit.getEntity() instanceof LivingEntity l) return l;
        LivingEntity best = null;
        double bestScore = -1;
        for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(range),
                x -> x != p && x.isAlive() && !x.isSpectator() && !x.isAlliedTo(p))) {
            if (e instanceof Player o && o.isCreative()) continue;
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double d = to.length();
            if (d > range || d < 1e-3 || to.scale(1 / d).dot(look) < 0.866) continue;
            boolean hostile = e instanceof Enemy || e instanceof Player || (e instanceof Mob m && m.getTarget() == p);
            double score = Nullification.maxEP(e) + e.getMaxHealth() + (hostile ? 1e15 : 0);
            if (score > bestScore) { bestScore = score; best = e; }
        }
        return best;
    }

    /** Where Black Meteorite lands: just short of the target, on the side the wielder came from, in free space. */
    static Vec3 landing(ServerPlayer p, LivingEntity t) {
        Vec3 tp = t.position();
        Vec3 dir = new Vec3(p.getX() - tp.x, 0, p.getZ() - tp.z);
        if (dir.lengthSqr() < 1e-4) dir = p.getViewVector(1f).multiply(-1, 0, -1);
        if (dir.lengthSqr() < 1e-4) dir = new Vec3(1, 0, 0);
        dir = dir.normalize();
        double back = t.getBbWidth() / 2 + p.getBbWidth() / 2 + 0.6;
        for (double extra : new double[]{0, 0.8, 1.6, 2.4})
            for (double up : new double[]{0, 1, 2, -1}) {
                Vec3 c = tp.add(dir.scale(back + extra)).add(0, up, 0);
                if (p.level().noCollision(p, p.getBoundingBox().move(c.subtract(p.position())))) return c;
            }
        return null;
    }

    /** Massive soul damage: 30% of the target's max spiritual health (Tensura), never below 1. */
    static void soulStrike(LivingEntity t) {
        double max = attr("max_spiritual_health").map(h -> t.getAttribute(h) == null ? 0.0 : t.getAttribute(h).getValue()).orElse(0.0);
        EnergyBridge.spirit(t, max > 0 ? max * 0.3 : 20);
        EnergyBridge.effect(t, "fear", 60, 0);
    }

    /**
     * Void Severance: 10% of the target's max EP is cut away. Wild mobs lose it for good; players, their pets and summons only
     * for 60 s (a transient modifier on max magicules and max aura that also ends on death or relog).
     */
    static void cutExistence(LivingEntity t) {
        if (Nullification.playerSide(t)) {
            for (String a : new String[]{"max_magicule", "max_aura"})
                attr(a).ifPresent(h -> TimedModifiers.apply(t, h, VOID_CUT, -0.10, CUT_TICKS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            if (t instanceof ServerPlayer sp)
                sp.displayClientMessage(Component.literal("Void Severance: 10% of your max EP is cut away for 60 s.").withStyle(ChatFormatting.DARK_RED), true);
        } else {
            try { EnergyHelper.multiplyMaxEP(t, 0.9); } catch (Throwable ignored) {}
        }
    }

    static Optional<Holder.Reference<Attribute>> attr(String path) {
        return BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.fromNamespaceAndPath("tensura", path));
    }

    // ---------------------------------------------------------------- tooltip
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, tip, flag);
        tip.add(Component.literal("Sneak + right-click: Black Meteorite — Void Severance").withStyle(ChatFormatting.GOLD));
        tip.add(Component.literal("  Locks on to the strongest energy signature ahead (100 blocks), crosses the gap as an anti-magic meteor and severs it twice. Leaves a Nihility zone that shatters barriers and jams every skill inside.").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("  Costs 80% of your aura. Cooldown: " + Math.round(METEOR_COOLDOWN * NUGameRules.cooldownShown() / 0.6 / 20) + " s").withStyle(ChatFormatting.DARK_GRAY));
        tip.add(Component.literal("Conceptual Severance: full swings cut EP-scaled true damage through armour, enchantments and hit cooldowns").withStyle(ChatFormatting.DARK_RED));
        tip.add(Component.literal("Conceptual Nullification Field (15 blocks): Ultimate skills jam, magicules bleed away, stopped time can't hold you").withStyle(ChatFormatting.DARK_RED));
        tip.add(Component.literal("Black Divider grows the blade for 8 s (Anti-Magic users: +1.5 block reach)").withStyle(ChatFormatting.DARK_RED));
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        int res = d == null ? 0 : d.copyTag().getInt("Resonance");
        if (res > 0) tip.add(Component.literal("Resonance " + "●".repeat(res) + "○".repeat(4 - res) + " — a great existence is near").withStyle(ChatFormatting.RED));
    }
}
