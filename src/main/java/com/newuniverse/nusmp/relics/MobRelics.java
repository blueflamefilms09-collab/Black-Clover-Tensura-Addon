package com.newuniverse.nusmp.relics;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.FxRelic;

/**
 * Why: relics were a player thing: the once-a-second pass that turns a worn relic's passives into attribute modifiers and effects only ran for
 * players, and a mob had no way to carry or attune one. The owner wants named mobs (a goblin given a name, a tamed monster) to attune to relics.
 * <p>
 * How: sneak + use a relic on an eligible mob ({@link #eligible}: a living non-player that has a name or an owner; every mob with
 * {@code relics_mobs.namedOnly=false}) and it takes the relic: weapons into an empty hand, helmets, chestplates and boots into the empty armour slot, and
 * everything else (rings, charms, cloaks, belts, gloves, necklaces) into a small pack kept in the mob's persistent data. A relic that needs attunement is bound to the
 * mob's own UUID at that moment (three at most, like a player), which is the attunement of a mob: there is no five-second ritual for it. The existing
 * {@link Relics} scan reads the pack too, so {@link Relics#loadout} already says what works for the mob, and this class applies it every second:
 * attribute passives as transient modifiers, effect passives as quiet ambient effects, and resistances through {@link RelicEngine}'s damage listener.
 * Sneak + empty hand takes the last relic back (only the player who gave it, or an operator). When the mob dies its pack drops and the bond is cut
 * (a dropped relic is free to attune to a player again).
 * <p>
 * Not done on purpose: acts (they are written for a ServerPlayer), skill leases and the grade's existence bonus. A mob wears the relic's numbers, it does not cast it.
 */
public final class MobRelics {
    private MobRelics() {}

    private static final String PACK = "nusmp_mob_relics", GIVER = "nusmp_mob_relic_giver";
    private static final String MOD_NS = "nusmp";
    /** Attribute modifiers this class put on a mob: mob uuid -> modifier id -> attribute, so a removed relic's numbers go away. */
    private static final Map<UUID, Map<ResourceLocation, Holder<Attribute>>> APPLIED = new HashMap<>();

    public static void register(IEventBus bus) {
        bus.addListener(MobRelics::onInteract);
        bus.addListener(MobRelics::onTick);
        bus.addListener(MobRelics::onDrops);
        bus.addListener(MobRelics::onLeave);
    }

    // ---------------------------------------------------------------- who may carry relics
    /** May this entity be given relics: a living non-player with a name or an owner (or any mob when namedOnly is off). */
    public static boolean eligible(LivingEntity e) {
        if (!MobRelicsConfig.enabled() || e == null || e instanceof Player || !(e instanceof Mob) || !e.isAlive()) return false;
        if (!MobRelicsConfig.namedOnly()) return true;
        return e.hasCustomName() || (e instanceof OwnableEntity o && o.getOwnerUUID() != null);
    }

    /** Does this mob carry anything the pass has to look after (a pack, or relic gear it was given)? */
    public static boolean carries(LivingEntity e) {
        return e != null && !(e instanceof Player) && e.getPersistentData().hasUUID(GIVER);
    }

    // ---------------------------------------------------------------- the pack
    /** Does the entity have a pack tag at all (written by this class, by the Avatar's trinket slots or by any code that puts relics in a mob's pack)? Cheap: no stack is parsed. 1.99.0: what {@link WornSync} looks for. */
    public static boolean hasPack(LivingEntity e) {
        return e != null && !(e instanceof Player) && e.getPersistentData().contains(PACK, Tag.TAG_LIST);
    }

    /** The relics in the mob's pack (copies; never null). */
    public static List<ItemStack> pack(LivingEntity e) {
        if (e == null || e instanceof Player) return List.of();
        CompoundTag data = e.getPersistentData();
        if (!data.contains(PACK, Tag.TAG_LIST)) return List.of();
        ListTag list = data.getList(PACK, Tag.TAG_COMPOUND);
        List<ItemStack> out = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            Optional<ItemStack> s = ItemStack.parse(e.registryAccess(), list.getCompound(i));
            s.filter(x -> !x.isEmpty()).ifPresent(out::add);
        }
        return out;
    }

    private static void savePack(LivingEntity e, List<ItemStack> stacks) {
        ListTag list = new ListTag();
        for (ItemStack s : stacks) if (!s.isEmpty()) list.add(s.save(e.registryAccess()));
        if (list.isEmpty()) e.getPersistentData().remove(PACK); else e.getPersistentData().put(PACK, list);
    }

    private static int attunedCount(LivingEntity e) {
        int n = 0;
        for (Relics.Worn w : Relics.wornList(e)) {
            RelicDef d = w.def();
            if (d.needsAttunement() && !d.isPairLeft() && e.getUUID().equals(Attune.owner(w.stack()))) n++;
        }
        return n;
    }

    // ---------------------------------------------------------------- giving and taking back
    private static void onInteract(PlayerInteractEvent.EntityInteract ev) {
        if (!(ev.getEntity() instanceof ServerPlayer p) || ev.getHand() != InteractionHand.MAIN_HAND || !p.isShiftKeyDown()) return;
        if (!(ev.getTarget() instanceof LivingEntity mob) || mob instanceof Player || !(eligible(mob) || carries(mob))) return;
        ItemStack held = p.getMainHandItem();
        RelicDef def = Relics.defOf(held);
        if (def != null && eligible(mob)) {
            if (give(p, mob, held, def)) { ev.setCanceled(true); ev.setCancellationResult(InteractionResult.SUCCESS); }
            else { ev.setCanceled(true); ev.setCancellationResult(InteractionResult.FAIL); }
        } else if (held.isEmpty() && carries(mob)) {
            if (takeBack(p, mob)) { ev.setCanceled(true); ev.setCancellationResult(InteractionResult.SUCCESS); }
        }
    }

    private static boolean give(ServerPlayer p, LivingEntity mob, ItemStack held, RelicDef def) {
        if (def.slot() == RelicSlot.CONSUMABLE) { tell(p, def.cardName() + " is used up, not worn: a mob cannot carry it.", ChatFormatting.GRAY); return false; }
        if (Relics.wornList(mob).size() >= MobRelicsConfig.maxCarried()) {                       // worn gear and the pack both count (the scan reads the pack)
            tell(p, mob.getDisplayName().getString() + " cannot carry more relics (" + MobRelicsConfig.maxCarried() + ").", ChatFormatting.RED);
            return false;
        }
        boolean attuneNeeded = def.needsAttunement() && !def.isPairLeft();
        if (attuneNeeded && attunedCount(mob) >= AttuneRules.CAP) {
            tell(p, mob.getDisplayName().getString() + " can keep only " + AttuneRules.CAP + " attuned relics.", ChatFormatting.RED);
            return false;
        }
        ItemStack one = held.copyWithCount(1);
        if (attuneNeeded) one.set(RelicComponents2.ATTUNED.get(), mob.getUUID());
        if (!equip(mob, one, def)) {
            List<ItemStack> pack = new ArrayList<>(pack(mob));
            pack.add(one);
            savePack(mob, pack);
        }
        if (!p.getAbilities().instabuild) held.shrink(1);
        mob.getPersistentData().putUUID(GIVER, p.getUUID());
        Relics.invalidate(mob);
        ServerLevel level = p.serverLevel();
        level.playSound(null, mob.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, 1.2f, 1.1f);
        FxRelic.ring(level, mob.position(), 1.0, def.paletteGlow());
        tell(p, def.cardName() + (attuneNeeded ? " attunes to " : " is carried by ") + mob.getDisplayName().getString() + ".", ChatFormatting.GOLD);
        return true;
    }

    /** Puts the relic into an empty hand or armour slot where it belongs; false when it has to go in the pack. */
    private static boolean equip(LivingEntity mob, ItemStack one, RelicDef def) {
        EquipmentSlot slot = switch (def.slot()) {
            case MAIN_HAND -> EquipmentSlot.MAINHAND;
            case HEAD -> EquipmentSlot.HEAD;
            case CHEST -> EquipmentSlot.CHEST;
            case FEET -> EquipmentSlot.FEET;
            default -> null;
        };
        if (slot == null || !mob.getItemBySlot(slot).isEmpty()) return false;
        if (slot != EquipmentSlot.MAINHAND && !(one.getItem() instanceof net.minecraft.world.item.ArmorItem)) return false;   // only real armour goes on an armour slot
        mob.setItemSlot(slot, one);
        mob.getPersistentData().putBoolean(GIVER + "_" + slot.getName(), true);                                  // remembered, so only what WE put here can be taken back
        if (mob instanceof Mob m) m.setDropChance(slot, 1.0f);                                                          // what it was given is not lost when it dies
        return true;
    }

    private static boolean takeBack(ServerPlayer p, LivingEntity mob) {
        UUID giver = mob.getPersistentData().hasUUID(GIVER) ? mob.getPersistentData().getUUID(GIVER) : null;
        if (giver != null && !giver.equals(p.getUUID()) && !p.hasPermissions(2)) { tell(p, "Only the one who gave the relic can take it back.", ChatFormatting.GRAY); return false; }
        ItemStack back = ItemStack.EMPTY;
        List<ItemStack> pack = new ArrayList<>(pack(mob));
        if (!pack.isEmpty()) {
            back = pack.remove(pack.size() - 1);
            savePack(mob, pack);
        } else {
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.FEET}) {
                String flag = GIVER + "_" + slot.getName();
                ItemStack s = mob.getItemBySlot(slot);
                if (mob.getPersistentData().getBoolean(flag) && Relics.defOf(s) != null) {
                    back = s;
                    mob.setItemSlot(slot, ItemStack.EMPTY);
                    mob.getPersistentData().remove(flag);
                    break;
                }
            }
        }
        if (back.isEmpty()) return false;
        back.remove(RelicComponents2.ATTUNED.get());                                                                    // the bond to the mob is cut
        if (!p.getInventory().add(back)) p.drop(back, false);
        Relics.invalidate(mob);
        if (Relics.wornList(mob).isEmpty()) mob.getPersistentData().remove(GIVER);
        tell(p, "You take " + back.getHoverName().getString() + " back from " + mob.getDisplayName().getString() + ".", ChatFormatting.GRAY);
        return true;
    }

    // ---------------------------------------------------------------- the pass: a mob wears its relics' numbers
    private static final com.newuniverse.nusmp.perf.PerfProbe.Section PROBE_S_RELIC_MOB_PACK = com.newuniverse.nusmp.perf.PerfProbe.section("s.relic.mob_pack");   // 1.99.0: performance probe (off by default: one flag read)

    private static void onTick(EntityTickEvent.Post ev) {
        long t0 = com.newuniverse.nusmp.perf.PerfProbe.begin();
        try {
            onTick0(ev);
        } finally {
            com.newuniverse.nusmp.perf.PerfProbe.end(PROBE_S_RELIC_MOB_PACK, t0);
        }
    }

    private static void onTick0(EntityTickEvent.Post ev) {
        if (!(ev.getEntity() instanceof LivingEntity e) || e instanceof Player || (e.tickCount + e.getId()) % 20 != 0 || e.level().isClientSide() || !MobRelicsConfig.enabled()) return;      // 1.99.0: for every living entity every tick; the modulo first, the config read only one tick in twenty
        if (!carries(e)) { if (APPLIED.containsKey(e.getUUID())) clear(e); return; }
        Relics.Loadout lo = Relics.loadoutFresh(e);
        Map<ResourceLocation, Holder<Attribute>> keep = new HashMap<>();
        for (Relics.Worn w : lo.active()) {
            int i = 0;
            for (Passive ps : w.def().passives()) {
                if (ps.kind() == Passive.Kind.ATTR) {
                    ResourceLocation rl = Fx.id(ps.key());
                    Holder<Attribute> h = rl == null ? null : BuiltInRegistries.ATTRIBUTE.getHolder(rl).map(x -> (Holder<Attribute>) x).orElse(null);
                    if (h != null && !(w.def().slot().isHand() && w.stack() == e.getOffhandItem() && w.stack() != e.getMainHandItem())) {
                        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MOD_NS, "mobrelic/" + w.def().id() + "/" + i);
                        AttributeInstance inst = e.getAttribute(h);
                        if (inst != null) {
                            AttributeModifier cur = inst.getModifier(id);
                            if (cur == null || cur.amount() != ps.amount() || cur.operation() != ps.operation()) {
                                if (cur != null) inst.removeModifier(id);
                                inst.addTransientModifier(new AttributeModifier(id, ps.amount(), ps.operation()));
                            }
                            keep.put(id, h);
                        }
                    }
                } else if (ps.kind() == Passive.Kind.EFFECT) {
                    Optional<? extends Holder<net.minecraft.world.effect.MobEffect>> h = Fx.effect(ps.key());
                    if (h.isPresent()) e.addEffect(new MobEffectInstance(h.get(), ps.key().endsWith("night_vision") ? 260 : 80, ps.amp(), true, false));
                }
                i++;
            }
        }
        Map<ResourceLocation, Holder<Attribute>> old = APPLIED.getOrDefault(e.getUUID(), Map.of());
        for (Map.Entry<ResourceLocation, Holder<Attribute>> en : old.entrySet()) {
            if (keep.containsKey(en.getKey())) continue;
            AttributeInstance inst = e.getAttribute(en.getValue());
            if (inst != null) inst.removeModifier(en.getKey());
        }
        if (keep.isEmpty()) APPLIED.remove(e.getUUID()); else APPLIED.put(e.getUUID(), keep);
    }

    private static void clear(LivingEntity e) {
        Map<ResourceLocation, Holder<Attribute>> old = APPLIED.remove(e.getUUID());
        if (old == null) return;
        for (Map.Entry<ResourceLocation, Holder<Attribute>> en : old.entrySet()) {
            AttributeInstance inst = e.getAttribute(en.getValue());
            if (inst != null) inst.removeModifier(en.getKey());
        }
    }

    private static void onLeave(EntityLeaveLevelEvent ev) { APPLIED.remove(ev.getEntity().getUUID()); }

    // ---------------------------------------------------------------- death: the pack drops, the bond is cut
    private static void onDrops(LivingDropsEvent ev) {
        LivingEntity e = ev.getEntity();
        if (e instanceof Player || e.level().isClientSide()) return;
        for (ItemStack s : pack(e)) {
            s.remove(RelicComponents2.ATTUNED.get());
            ev.getDrops().add(new ItemEntity(e.level(), e.getX(), e.getY() + 0.5, e.getZ(), s));
        }
        e.getPersistentData().remove(PACK);
        for (ItemEntity it : ev.getDrops()) {
            ItemStack s = it.getItem();
            if (Relics.defOf(s) != null && e.getUUID().equals(Attune.owner(s))) s.remove(RelicComponents2.ATTUNED.get());
        }
        clear(e);
    }

    private static void tell(ServerPlayer p, String text, ChatFormatting colour) { p.displayClientMessage(Component.literal(text).withStyle(colour), true); }
}
