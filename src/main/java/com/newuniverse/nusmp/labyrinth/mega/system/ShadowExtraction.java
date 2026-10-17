package com.newuniverse.nusmp.labyrinth.mega.system;

import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.quests.NusmpEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.item.ItemStack;

/**
 * Why: "extract the souls of defeated Holy enemies as summoned minions" is two moments with a short memory between them. WHEN a hostile creature dies to a
 * player on the Holy road, it is written into a small bounded {@link ExtractLedger} (what it was, how strong, where, who killed it, when) if the pure
 * {@link ExtractRules} allow it: never a boss, never somebody's servant, never a named or persistent creature, never one that cannot fight. WHEN the same player
 * then uses the Shadow Core within {@code extractWindowTicks} (100) and within {@code extractRadius} (12) blocks of where it fell, a charge is spent, the memory
 * is used up and {@link ShadowMinions#raise} makes the shadow. A held core also announces "EXTRACTION READY" once per death, so the player knows the window is
 * open. Only the killer can extract (a party member cannot take the kill), nothing is remembered outside the Mega-Dungeon, and the ledger is bounded and
 * cleared when the server stops. A core spent to its last charge turns into nothing with a message (no leftover item), and creative players spend no charge.
 */
public final class ShadowExtraction {
    private ShadowExtraction() {}

    /** Creatures a datapack forbids as shadows (data/nusmp/tags/entity_type/shadow_immune.json, beside corpse_immune and paladin_immune). */
    public static final TagKey<EntityType<?>> IMMUNE = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("nusmp", "shadow_immune"));

    /** How many deaths are remembered at once, whatever the dungeon does. */
    private static final int LEDGER_CAP = 64;
    static final ExtractLedger LEDGER = new ExtractLedger(LEDGER_CAP);

    /** What using the core did. */
    public enum Result { RAISED, NOTHING, DISABLED, FAILED }

    // ================================================================ remembering
    /**
     * A creature died: remembers it as extractable when a player struck the last blow on a road the System covers and the rules allow it. Returns whether it was
     * remembered. The killer is the true source of the damage, so an arrow or a spell cast by the player counts, a shadow's blow does not.
     */
    public static boolean record(LivingEntity dead, DamageSource source) {
        if (!SystemConfig.enabled() || !(dead.level() instanceof ServerLevel level) || !MegaDungeon.in(level)) return false;
        ServerPlayer killer = source != null && source.getEntity() instanceof ServerPlayer sp ? sp : null;
        if (killer == null) return false;
        if (ExtractRules.refusal(facts(dead, level, true)) != null) return false;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType());
        double attack = dead instanceof Mob m && m.getAttribute(Attributes.ATTACK_DAMAGE) != null ? m.getAttributeValue(Attributes.ATTACK_DAMAGE) : 0.0;
        LEDGER.record(new ExtractLedger.Fallen(id.toString(), dead.getName().getString(), dead.getMaxHealth(), attack, dead.getX(), dead.getY(), dead.getZ(),
                level.dimension().location().toString(), killer.getUUID(), level.getGameTime()));
        return true;
    }

    /** Reads every fact the rules need off the creature. {@code byPlayer} says whether a player struck the last blow (the caller knows). */
    public static ExtractRules.Facts facts(LivingEntity dead, ServerLevel level, boolean byPlayer) {
        Polarity pol = MegaDungeon.polarityAt(level, dead.blockPosition());
        boolean road = pol != null && SystemQuests.covers(pol);
        boolean mob = dead instanceof Mob;
        boolean armed = dead instanceof Mob m && (m.getAttribute(Attributes.ATTACK_DAMAGE) != null || m instanceof RangedAttackMob);
        boolean persistent = dead instanceof Mob m && m.isPersistenceRequired();
        return new ExtractRules.Facts(byPlayer, road, ShadowMinions.isHostile(dead), mob, armed, ShadowMinions.isBoss(dead), ShadowMinions.isServant(dead),
                dead.hasCustomName(), persistent, dead.getType().is(IMMUNE));
    }

    /** Why this creature could not be extracted (null if it could), for the command and the self-test. */
    public static String refusal(LivingEntity e, boolean byPlayer) {
        return e.level() instanceof ServerLevel level ? ExtractRules.refusal(facts(e, level, byPlayer)) : "not in a server world";
    }

    // ================================================================ announcing
    /** Every ten ticks per player: says "EXTRACTION READY" once for each death in range of a player who holds a core. */
    public static void notifyReady(ServerPlayer p) {
        if (LEDGER.isEmpty() || !(ShadowCoreItem.is(p.getMainHandItem()) || ShadowCoreItem.is(p.getOffhandItem()))) return;
        ExtractLedger.Fallen f = nearest(p);
        if (f != null && LEDGER.markNotified(f)) SystemSync.toast(p, ToastKind.SHADOW, SystemText.TOAST_EXTRACTION_READY);
    }

    /** Forgets what is older than the window (server tick, every second). */
    public static void expire(MinecraftServer server) {
        if (LEDGER.isEmpty()) return;
        LEDGER.expire(server.overworld().getGameTime(), SystemConfig.get(SystemConfig.EXTRACT_WINDOW_TICKS));
    }

    private static ExtractLedger.Fallen nearest(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        return LEDGER.nearest(p.getUUID(), level.dimension().location().toString(), p.getX(), p.getY(), p.getZ(), level.getGameTime(),
                SystemConfig.get(SystemConfig.EXTRACT_WINDOW_TICKS), SystemConfig.get(SystemConfig.EXTRACT_RADIUS));
    }

    // ================================================================ using the core
    /**
     * The player uses a core: raises the creature they slew nearby as a shadow, spends a charge and, on the last one, the core itself. The player is told what
     * happened. The core is not touched unless a shadow really rose.
     */
    public static Result use(ServerPlayer p, ItemStack core) {
        if (!SystemConfig.enabled() || SystemConfig.get(SystemConfig.MAX_MINIONS) <= 0) {
            p.displayClientMessage(SystemMsg.text(SystemText.MSG_SYSTEM_OFF).withStyle(ChatFormatting.RED), true);
            return Result.DISABLED;
        }
        ExtractLedger.Fallen f = nearest(p);
        if (f == null) {
            p.displayClientMessage(SystemMsg.text(SystemText.MSG_NOTHING_TO_EXTRACT, SystemConfig.get(SystemConfig.EXTRACT_WINDOW_TICKS) / 20).withStyle(ChatFormatting.GRAY), true);
            return Result.NOTHING;
        }
        Mob shadow = ShadowMinions.raise(p, f);
        if (shadow == null) {
            p.displayClientMessage(SystemMsg.text(SystemText.MSG_BAD_TARGET, f.name()).withStyle(ChatFormatting.RED), true);
            return Result.FAILED;
        }
        LEDGER.take(f);
        String typeId = f.type();
        SystemSync.toast(p, ToastKind.SHADOW, SystemText.TOAST_EXTRACTED, "@" + shadow.getType().getDescriptionId());
        NusmpEvents.fire(p, "mega.shadow", typeId);
        if (!p.isCreative()) {
            int left = CoreCharges.spend(ShadowCoreItem.charges(core));
            if (CoreCharges.spent(left)) {
                core.shrink(1);
                SystemSync.toast(p, ToastKind.INFO, SystemText.TOAST_CORE_SPENT);
            } else {
                ShadowCoreItem.setCharges(core, left);
                p.displayClientMessage(SystemMsg.text(SystemText.MSG_CHARGES_LEFT, left).withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
        }
        return Result.RAISED;
    }

    // ================================================================ housekeeping
    /** The ledger's size (the status command). */
    public static int remembered() { return LEDGER.size(); }

    /** The first fallen creature of this player in range (the status command and the self-test), or null. */
    public static ExtractLedger.Fallen pending(ServerPlayer p) { return nearest(p); }

    /** Server stop: forgets everything. */
    public static void clear() { LEDGER.clear(); }
}
