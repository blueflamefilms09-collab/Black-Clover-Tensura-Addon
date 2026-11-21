package com.newuniverse.nusmp.sinweapons;

import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.perf.PerfProbe;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Why: the server half of the five Berserk weapon skills (Black Axe of Sloth, Black Gunblade of Envy, Black Spear of Vanity, Black Chakrams, Seifort Holy Sword), set up like the Black Sword Greed's skill: the weapon is
 * built with a skill. Everything is decided here, on the server thread, keyed by the player's UUID, and nothing is kept in a static map: the owner is a UUID text in the stack's custom data ({@link SinSkillRules#OWNER_TAG}),
 * the forge clock is one number in the player's persistent data. Three jobs: the once-a-second look at the inventory that binds an unbound stack to its carrier and gives that weapon's skill once (it is permanent, so the
 * weapon can be forged again); the gate of the right-click technique ({@link #mayUse}, setting {@code soulBound}); and the two modes of the skill, Technique (the same technique as the right click, with its own cooldown)
 * and Manifest / Store (the bound weapon in the main hand goes back into the skill, else a fresh bound one is forged). Cost: per player one int compare per tick, and every 20 ticks at most {@link SinSkillRules#MAX_SLOTS}
 * slots read twice (an item-class test each); no world scan, no packets of its own (the skill system syncs the learned skill, a message goes to the one player).
 */
public final class SinSkillService {
    private SinSkillService() {}

    private static final PerfProbe.Section PROBE_SCAN = PerfProbe.section("s.sinskill.scan");
    private static final String FORGE_KEY = "nusmp_sin_forge_";

    public static void register(IEventBus bus) { bus.addListener(SinSkillService::onPlayerTick); }

    // ---------------------------------------------------------------- the stack's owner
    /** The owner text of a stack of one of the weapons, or null. */
    public static String ownerText(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d != null && d.contains(SinSkillRules.OWNER_TAG) ? d.copyTag().getString(SinSkillRules.OWNER_TAG) : null;
    }

    public static void bindStack(ItemStack s, ServerPlayer p) {
        String id = p.getUUID().toString();
        CustomData.update(DataComponents.CUSTOM_DATA, s, t -> t.putString(SinSkillRules.OWNER_TAG, id));
    }

    public static SinSkillRules.Owner ownerOf(ItemStack s, ServerPlayer p) { return SinSkillRules.ownerOf(ownerText(s), p.getUUID().toString()); }

    /** True when the Lords system (setting lordsEnabled) owns this weapon: one in the world, soul-bound to one wielder; the old carrier binding stays off for it. */
    private static boolean lordManaged(SinRules.Kind k) {
        com.newuniverse.nusmp.lords.LordRules.Sin s = com.newuniverse.nusmp.lords.LordItems.fromKind(k);
        return s != null && com.newuniverse.nusmp.lords.LordService.managed(s);
    }

    private static SinRules.Kind kindOf(ItemStack s) { return s.getItem() instanceof SinWeaponItem w ? w.kind() : null; }

    private static boolean isMine(ItemStack s, ServerPlayer p, SinRules.Kind k) { return kindOf(s) == k && ownerOf(s, p) == SinSkillRules.Owner.MINE; }

    /** The right click's check: may this player use the stack's technique? (the setting soulBound off lets anyone) */
    public static boolean mayUse(ServerPlayer p, ItemStack stack) {
        com.newuniverse.nusmp.lords.LordRules.Sin lord = com.newuniverse.nusmp.lords.LordItems.sinOf(stack);
        if (lord != null && com.newuniverse.nusmp.lords.LordService.managed(lord)) return com.newuniverse.nusmp.lords.LordService.mayUse(p, stack);      // 1-of-1 Lord weapons: the world's token and wielder decide
        return SinSkillRules.mayUseTechnique(SinWeaponConfig.soulBound(), ownerOf(stack, p));
    }

    /** A refused click: one message, and a second of the item's own cooldown so a held button does not repeat it. */
    public static void refuse(ServerPlayer p, Item item) {
        say(p, "sinskill.msg.not_yours", "The weapon does not answer you: it is bound to another.", ChatFormatting.DARK_RED);
        p.getCooldowns().addCooldown(item, 20);
    }

    // ---------------------------------------------------------------- the skill of each weapon
    private static DeferredHolder<ManasSkill, ? extends ManasSkill> holder(SinRules.Kind k) {
        return switch (k) {
            case SLOTH -> NUSkills.LORD_OF_SLOTHFUL_WEIGHT;
            case ENVY -> NUSkills.LORD_OF_GREEN_EYED_ENVY;
            case VANITY -> NUSkills.LORD_OF_EMPTY_VANITY;
            case CHAKRAMS -> NUSkills.LORD_OF_THE_TWIN_RINGS;
            case HOLY -> NUSkills.SEIFORT_HOLY_JUDGEMENT;
        };
    }

    private static Item item(SinRules.Kind k) {
        return switch (k) {
            case SLOTH -> SinWeaponBoot.SLOTH.get();
            case ENVY -> SinWeaponBoot.ENVY.get();
            case VANITY -> SinWeaponBoot.VANITY.get();
            case CHAKRAMS -> SinWeaponBoot.CHAKRAMS.get();
            case HOLY -> SinWeaponBoot.HOLY.get();
        };
    }

    private static Skills skillsOf(ServerPlayer p) {
        try {
            return SkillAPI.getSkillsFrom(p);
        } catch (RuntimeException noSkills) {
            return null;
        }
    }

    public static boolean has(ServerPlayer p, SinRules.Kind k) {
        Skills s = skillsOf(p);
        return s != null && s.getSkill(holder(k).getId()).isPresent();
    }

    /** Gives the weapon's skill once (it stays: the weapon can be forged again). Returns true when it was given now. */
    static boolean grant(ServerPlayer p, SinRules.Kind k) {
        Skills skills = skillsOf(p);
        if (skills == null || skills.getSkill(holder(k).getId()).isPresent()) return false;
        ManasSkill skill = holder(k).get();
        skills.learnSkill(skill.createDefaultInstance(), Component.translatableWithFallback("sinskill.msg.granted", "%s answers: its skill is yours.", skill.getName()).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        return true;
    }

    // ---------------------------------------------------------------- the once-a-second look
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !SinSkillRules.scanTurn(p.tickCount, p.getId()) || p.isSpectator()) return;
        long t0 = PerfProbe.begin();
        try {
            SafeCast.tick("sinskill.scan", () -> scan(p));
        } finally {
            PerfProbe.end(PROBE_SCAN, t0);
        }
    }

    /** Binds every unbound stack of the weapons the player carries (inventory only, at most one per weapon, only if they have no bound one of it) and gives that weapon's skill. Other players' stacks are never touched. */
    public static void scan(ServerPlayer p) {
        Inventory inv = p.getInventory();
        int n = SinSkillRules.slotsScanned(inv.getContainerSize());
        int mine = 0;                                                                         // one bit per weapon kind: a bound stack of the player's is carried
        for (int i = 0; i < n; i++) {
            ItemStack s = inv.getItem(i);
            SinRules.Kind k = kindOf(s);
            if (k != null && ownerOf(s, p) == SinSkillRules.Owner.MINE) mine |= 1 << k.ordinal();
        }
        for (int i = 0; i < n; i++) {
            ItemStack s = inv.getItem(i);
            SinRules.Kind k = kindOf(s);
            if (k == null || lordManaged(k) || isOff(p, k) || !SinSkillRules.shouldBind(ownerOf(s, p), (mine & (1 << k.ordinal())) != 0)) continue;      // 4.30.1: a skill the player removed is not given again
            bindStack(s, p);
            mine |= 1 << k.ordinal();
            grant(p, k);
        }
    }

    // ---------------------------------------------------------------- the skill's modes
    /** Technique: the weapon of this kind in the main hand fires its technique exactly as the right click does (same cooldown, same caps). */
    public static void technique(ServerPlayer p, SinRules.Kind k) {
        ItemStack s = p.getMainHandItem();
        if (!(s.getItem() instanceof SinWeaponItem w) || w.kind() != k) {
            say(p, "sinskill.msg.hold", "Hold %s in your main hand.", ChatFormatting.GRAY, Component.translatableWithFallback("item.anime_multiverse." + k.id, k.english));
            return;
        }
        if (!mayUse(p, s)) {
            refuse(p, w);
            return;
        }
        w.fire(p);
    }

    /** The extra techniques (modes 3 and 4, slot 1 and 2): the weapon of this kind in the main hand, the owner check, then its own technique with its own cooldown ({@link SinWeaponItem#ability}). */
    public static void ability(ServerPlayer p, SinRules.Kind k, int slot) {
        ItemStack s = p.getMainHandItem();
        if (!(s.getItem() instanceof SinWeaponItem w) || w.kind() != k) {
            say(p, "sinskill.msg.hold", "Hold %s in your main hand.", ChatFormatting.GRAY, Component.translatableWithFallback("item.anime_multiverse." + k.id, k.english));
            return;
        }
        if (!SinWeaponConfig.abilities()) {
            say(p, "sinskill.msg.abilities_off", "The extra techniques are switched off on this server.", ChatFormatting.GRAY);
            return;
        }
        if (!mayUse(p, s)) {
            refuse(p, w);
            return;
        }
        w.ability(p, slot);
    }

    /** Manifest / Store: see {@link SinSkillRules#manifest}. Binds a stack the player holds first, so a /give'd weapon in the hand is theirs before it is stored. */
    public static void manifest(ServerPlayer p, SinRules.Kind k) {
        if (lordManaged(k)) {                                                                  // one of each in the world: the wielder only calls their own back
            com.newuniverse.nusmp.lords.LordService.manifest(p, com.newuniverse.nusmp.lords.LordItems.fromKind(k));
            return;
        }
        scan(p);
        Inventory inv = p.getInventory();
        boolean holds = isMine(p.getMainHandItem(), p, k), carries = holds;
        int n = SinSkillRules.slotsScanned(inv.getContainerSize());
        for (int i = 0; i < n && !carries; i++) carries = isMine(inv.getItem(i), p, k);
        long now = p.serverLevel().getGameTime();
        String key = FORGE_KEY + k.id;
        Component name = Component.translatableWithFallback("item.anime_multiverse." + k.id, k.english);
        switch (SinSkillRules.manifest(holds, carries, now, p.getPersistentData().getLong(key))) {
            case STORE -> {
                p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                p.getPersistentData().putLong(key, SinSkillRules.nextForgeAt(now));
                say(p, "sinskill.msg.stored", "%s returns into the skill.", ChatFormatting.GRAY, name);
            }
            case ALREADY_CARRIED -> say(p, "sinskill.msg.carried", "%s is already with you.", ChatFormatting.GRAY, name);
            case WAIT -> say(p, "sinskill.msg.wait", "The skill is still gathering itself.", ChatFormatting.GRAY);
            case FORGE -> {
                ItemStack fresh = new ItemStack(item(k));
                bindStack(fresh, p);
                if (p.getMainHandItem().isEmpty()) p.setItemInHand(InteractionHand.MAIN_HAND, fresh);
                else if (!inv.add(fresh)) {
                    say(p, "sinskill.msg.full", "There is no room to forge the weapon.", ChatFormatting.GRAY);
                    return;
                }
                p.getPersistentData().putLong(key, SinSkillRules.nextForgeAt(now));
                say(p, "sinskill.msg.forged", "%s is forged from the skill.", ChatFormatting.DARK_PURPLE, name);
            }
        }
    }

    // ---------------------------------------------------------------- removing a skill (4.30.1)
    private static final String OFF_KEY = "nusmp_sin_off_";

    /** True when the player removed this weapon's skill (one byte in their persistent data): the scan then neither binds nor grants it. */
    public static boolean isOff(ServerPlayer p, SinRules.Kind k) { return p.getPersistentData().getBoolean(OFF_KEY + k.id); }

    /** The same by weapon id (the Lords of the sins use the same ids); false for an id that is no weapon. */
    public static boolean isOffById(ServerPlayer p, String id) { return p.getPersistentData().getBoolean(OFF_KEY + id); }

    /** Removes the weapon's skill from the player and keeps it from coming back by itself; the carried stacks they own are set free (unbound), nothing is taken. Returns true when anything changed. */
    public static boolean remove(ServerPlayer p, SinRules.Kind k) {
        boolean changed = !isOff(p, k);
        p.getPersistentData().putBoolean(OFF_KEY + k.id, true);
        for (com.newuniverse.nusmp.lords.LordRules.Sin sin : com.newuniverse.nusmp.lords.LordRules.Sin.values())
            if (sin.id.equals(k.id)) com.newuniverse.nusmp.lords.LordService.hud(p, sin, true);      // the side bar goes at once (4.64.1)
        Skills skills = skillsOf(p);
        if (skills != null && skills.getSkill(holder(k).getId()).isPresent()) {
            skills.forgetSkill(holder(k).getId(), Component.translatableWithFallback("sinskill.msg.removed", "%s fades from you.", holder(k).get().getName()).withStyle(ChatFormatting.GRAY));
            changed = true;
        }
        Inventory inv = p.getInventory();
        int n = SinSkillRules.slotsScanned(inv.getContainerSize());
        for (int i = 0; i < n; i++) {
            ItemStack s = inv.getItem(i);
            if (isMine(s, p, k)) CustomData.update(DataComponents.CUSTOM_DATA, s, t -> t.remove(SinSkillRules.OWNER_TAG));
        }
        return changed;
    }

    /** Why (4.77.0): {@code /multiverse sin purge}: {@link #remove} plus the weapon itself. Deletes every stack of that Berserk weapon from the player's main inventory, armour and off hand; returns how many stacks went. */
    public static int purgeItems(ServerPlayer p, SinRules.Kind k) {
        Inventory inv = p.getInventory();
        int gone = 0;
        int n = SinSkillRules.slotsScanned(inv.getContainerSize());
        for (int i = 0; i < n; i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof SinWeaponItem w && w.kind() == k) { inv.setItem(i, ItemStack.EMPTY); gone++; }
        }
        return gone;
    }

    /** Undoes {@link #remove}: the next look at the inventory binds a carried unbound weapon again and gives its skill. Returns true when the player had removed it. */
    public static boolean restore(ServerPlayer p, SinRules.Kind k) {
        boolean was = isOff(p, k);
        p.getPersistentData().remove(OFF_KEY + k.id);
        for (com.newuniverse.nusmp.lords.LordRules.Sin sin : com.newuniverse.nusmp.lords.LordRules.Sin.values())
            if (sin.id.equals(k.id)) com.newuniverse.nusmp.lords.LordService.hud(p, sin, true);
        return was;
    }

    /** The persistent-data key of one weapon's forge clock (the self-test resets it). */
    static String forgeKey(SinRules.Kind k) { return FORGE_KEY + k.id; }

    private static void say(ServerPlayer p, String key, String fallback, ChatFormatting color, Object... args) {
        p.displayClientMessage(Component.translatableWithFallback(key, fallback, args).withStyle(color), true);
    }
}
