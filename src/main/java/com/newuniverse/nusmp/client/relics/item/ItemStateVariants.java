package com.newuniverse.nusmp.client.relics.item;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.HeroWeaponItem;
import com.newuniverse.nusmp.item.MagicWeaponItem;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.RelicBearer;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicRangedSupport;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.socket.SocketCatalog;
import com.newuniverse.nusmp.relics.socket.SocketComponents;
import com.newuniverse.nusmp.relics.socket.SocketDef;
import com.newuniverse.nusmp.relics.socket.SocketGrades;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Decides, for the stack WeaponRenderer is about to draw and the entity holding it, which mesh to use and which glow variant to add (the client side
 * of docs/asset_pipeline.md "Bows and crossbows" and "NBT state layers"). Only mod items reach here (WeaponRenderer draws nothing else), and nothing
 * is ever changed on a stack or an entity: it is a read-only decision per draw, cached by (mesh id, stage, variants) until the next resource reload.
 * <ol>
 *   <li><b>stage</b> ({@link RangedStage}): a relic bow or crossbow by its draw and load; the two self-loading crossbows (Bolt Thrower, Crossbow of
 *       Swift Death) are CHARGED when ready and SPENT while their use cooldown runs;</li>
 *   <li><b>variants</b> ({@link VariantRules}): GENESIS (the stack's effective tier from Legend up, or the holder's Genesis body flag) and FIVE LEAF (the
 *       holder's grimoire, for Black Clover weapons);</li>
 *   <li><b>socket</b> (1.29.0, relics/socket): a stack with a bound Spirit or Devil carries its colour in {@link Result#socketRgb}, which the renderer
 *       draws as an extra emissive pass; the ambient particles are {@link SocketFx}'s (a client tick, not a draw call);</li>
 *   <li><b>mesh</b>: the first of the candidates the mesh set really has; if the variant has no mesh of its own the glow is intensified or tinted.</li>
 * </ol>
 */
public final class ItemStateVariants {
    private ItemStateVariants() {}

    /** The persistent-data key of a player whose body is Genesis (server side; mirrored to clients by relics/RelicHolderSync). */
    public static final String GENESIS_KEY = "TensuraGenesisBody";

    /**
     * The answer for one draw. {@code intensify}: pulse harder and add an emissive pass; {@code tint}: violet-black glow; {@code socketRgb}: the 0xRRGGBB
     * of the bound socket's element for an extra emissive pass in that colour (0 = no socket).
     */
    public record Result(String meshId, boolean intensify, boolean tint, int socketRgb) {
        public Result(String meshId, boolean intensify, boolean tint) { this(meshId, intensify, tint, 0); }
    }

    private record Key(String base, int stage, boolean genesis, boolean fiveLeaf, int socketRgb) {}

    private static final Map<Key, Result> CACHE = new HashMap<>();

    /** A resource reload may add or drop meshes: forget the answers. */
    public static void clearCache() { CACHE.clear(); }

    /** {@code baseId}: the mesh id the other state logic (the lantern, Marid's Valor) already chose; {@code exists}: does this mesh id load. */
    public static Result resolve(ItemStack stack, String baseId, ItemDisplayContext ctx, Predicate<String> exists) {
        LivingEntity holder = HolderTracker.holderFor(ctx);
        int stage = stageOf(stack, holder);
        VariantRules.Decision d = decision(stack, holder);
        int socketRgb = socketRgb(stack);
        Key key = new Key(baseId, stage, d.genesis(), d.fiveLeaf(), socketRgb);
        Result r = CACHE.get(key);
        if (r == null) {
            r = pick(baseId, stage, d, exists);
            if (socketRgb != 0) r = new Result(r.meshId(), true, r.tint(), socketRgb);     // a socket always gets the stronger glow; the renderer tints the extra pass
            if (CACHE.size() > 512) CACHE.clear();
            CACHE.put(key, r);
        }
        return r;
    }

    /** The colour of the stack's first bound socket, or 0. Reads the synced component only; no allocation. */
    static int socketRgb(ItemStack stack) {
        String id = SocketComponents.firstId(stack);
        if (id.isEmpty()) return 0;
        SocketDef d = SocketCatalog.byId(id);
        return d == null ? 0 : d.rgb() & 0xFFFFFF;                                  // no catalog colour is black, so 0 can mean "no socket"
    }

    static Result pick(String baseId, int stage, VariantRules.Decision d, Predicate<String> exists) {
        for (VariantRules.Candidate c : VariantRules.candidates(RangedStage.chain(baseId, stage), d)) {
            if (exists.test(c.meshId())) return new Result(c.meshId(), VariantRules.intensify(d, c.tag()), VariantRules.tint(d, c.tag()));
        }
        return new Result(baseId, false, false);
    }

    // ---------------------------------------------------------------- the stage
    static int stageOf(ItemStack stack, LivingEntity holder) {
        Item item = stack.getItem();
        if (item instanceof RelicBearer bearer && bearer.relic().isRanged()) {
            RangedStage.Kind kind = bearer.relic().rangedKind() == RelicDef.RangedKind.CROSSBOW ? RangedStage.Kind.CROSSBOW : RangedStage.Kind.BOW;
            boolean charged = kind == RangedStage.Kind.CROSSBOW && CrossbowItem.isCharged(stack);
            boolean using = holder != null && holder.isUsingItem() && holder.getUseItem() == stack && !RelicRangedSupport.actMode(holder);
            float pull = 0f;
            if (using) {
                int duration = stack.getUseDuration(holder);
                int remaining = holder.getUseItemRemainingTicks();
                pull = kind == RangedStage.Kind.BOW ? RangedStage.bowPull(duration, remaining)
                        : RangedStage.crossbowPull(charged, duration, remaining, CrossbowItem.getChargeDuration(stack, holder));
            }
            return RangedStage.stage(kind, using, charged, pull);
        }
        if (item instanceof MagicWeaponItem w && (w.kind == MagicWeaponItem.Kind.BOLT_THROWER || w.kind == MagicWeaponItem.Kind.CROSSBOW_OF_SWIFT_DEATH)) {
            if (holder == null) return RangedStage.REST;
            boolean cooling = holder instanceof Player p && p.getCooldowns().isOnCooldown(item);
            return cooling ? RangedStage.SPENT : RangedStage.CHARGED;
        }
        // 1.62.0: the Legendary Bow draws like a relic bow, so its pull meshes (legendary_bow_pull1..3, legendary_bow_curse_pull1..3) are chosen by the same stages
        if (item instanceof HeroWeaponItem hero && hero.kind() == HeroKind.BOW) {
            boolean using = holder != null && holder.isUsingItem() && holder.getUseItem() == stack;
            float pull = using ? RangedStage.bowPull(stack.getUseDuration(holder), holder.getUseItemRemainingTicks()) : 0f;
            return RangedStage.stage(RangedStage.Kind.BOW, using, false, pull);
        }
        return RangedStage.REST;
    }

    // ---------------------------------------------------------------- the variants
    static VariantRules.Decision decision(ItemStack stack, LivingEntity holder) {
        RelicDef def = Relics.defOf(stack);
        // 1.29.0: from Legend up (the effective tier: a raised tier counts) a relic wears the "_genesis" mesh if it ships one, else the stronger glow
        boolean stackGenesis = def != null && (def.grade() == Grade.GENESIS || VariantRules.elaborate(SocketGrades.tier(stack)));
        if (!stackGenesis && com.newuniverse.nusmp.item.ElsdociaRules.isLegacyException(stack)) stackGenesis = true;     // 1.97.0: Elsdocia is Genesis grade
        boolean bc = stack.getItem() instanceof MagicWeaponItem;
        int packed = holder == null ? 0 : HolderFlags.get(holder.getId());
        boolean holderGenesis = HolderFlags.genesis(packed) || (holder != null && genesisBody(holder));
        return VariantRules.decide(new VariantRules.Inputs(stackGenesis, holderGenesis, HolderFlags.leaves(packed), bc, holder != null));
    }

    /** The flag as the entity itself holds it (the integrated server's own player, or a mod that mirrors it): top level or inside "PlayerPersisted". */
    static boolean genesisBody(LivingEntity e) {
        CompoundTag t = e.getPersistentData();
        return t.getBoolean(GENESIS_KEY) || t.getCompound("PlayerPersisted").getBoolean(GENESIS_KEY);
    }
}
