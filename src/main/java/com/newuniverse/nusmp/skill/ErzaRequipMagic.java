package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.requip.Armour;
import com.newuniverse.nusmp.requip.ArmouryRules;
import com.newuniverse.nusmp.requip.BarrageMath;
import com.newuniverse.nusmp.requip.LoadoutRules;
import com.newuniverse.nusmp.requip.PartialArmourMask;
import com.newuniverse.nusmp.requip.RequipArmoury;
import com.newuniverse.nusmp.requip.RequipLoadouts;
import com.newuniverse.nusmp.requip.RequipMagicConfig;
import com.newuniverse.nusmp.requip.RequipMagicFx;
import com.newuniverse.nusmp.requip.RequipMagicRules;
import com.newuniverse.nusmp.requip.RequipMagicRules.Mode;
import com.newuniverse.nusmp.requip.RequipRules;
import com.newuniverse.nusmp.requip.RequipWeapons;
import com.newuniverse.nusmp.skill.RequipScripts.Script;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Optional;

/**
 * Why: Requip: The Knight gave Erza Scarlet's armours; this is the rest of the Equip Magic as Fairy Tail shows it, appended to the same skill as modes 15 and up (the 14 armours and Dismiss keep their
 * places): the pocket space (<b>Armoury</b>), the instant swap (<b>Loadouts</b>), the weapon on call (<b>Requip: Blade</b>, <b>Barrage</b>, <b>Strike</b>), the armour worn piece by piece
 * (<b>Partial Armour</b>) and the named finishers. {@link #press} is the one entry the skill's key handler calls for those modes; the rules are {@link RequipMagicRules}, the cost goes through
 * {@link EnergyBridge}, the damage through the balance law ({@link RequipScripts.Script#hit}). The four passives (Instant-Change, Pocket Capacity, Weapon Affinity, Requip Reflex) are rules of mastery
 * read here and by the skill. No hunger or exhaustion is touched anywhere.
 */
public final class ErzaRequipMagic {
    private ErzaRequipMagic() {}

    private static boolean started;

    /** Called once from {@code requip.RequipSystem.init}: the script clock, the safety net for a thrown weapon, and the links the armoury window uses. */
    public static void init(IEventBus modBus) {
        if (started) return;
        started = true;
        NeoForge.EVENT_BUS.addListener(RequipScripts::onServerTick);
        NeoForge.EVENT_BUS.addListener(ErzaRequipMagic::onServerTick);
        RequipArmoury.SkillLink.set(new RequipArmoury.SkillLink.Hooks() {
            @Override public int capacity(ServerPlayer p) { return ErzaRequipMagic.capacity(p); }
            @Override public boolean mayOpen(ServerPlayer p) { return mayUse(p, Mode.ARMOURY.id()); }
            @Override public boolean mayUse(ServerPlayer p, String modeId) { return ErzaRequipMagic.mayUse(p, modeId); }
            @Override public void equipLoadout(ServerPlayer p, int n) { ErzaRequipMagic.equipLoadoutByButton(p, n); }
            @Override public void applyPartial(ServerPlayer p) { ErzaRequipMagic.applyPartialByButton(p); }
        });
    }

    // ---------------------------------------------------------------- the skill instance
    static Optional<ManasSkillInstance> instanceOf(ServerPlayer p) {
        try {
            return SkillAPI.getSkillsFrom(p).getSkill(NUSkills.ERZA_REQUIP.getId());
        } catch (Throwable noSkills) {
            return Optional.empty();
        }
    }

    static boolean privileged(ServerPlayer p) { return p.isCreative() || p.hasPermissions(2); }

    /** Mastery of the skill (0..1) of a player; 0 without the skill. */
    public static double mastery(ServerPlayer p) {
        return instanceOf(p).map(NUSkill::masteryFrac).orElse(0.0);
    }

    /** The armoury's slots for this player: mastery plus Pocket Capacity, held to max EP. */
    public static int capacity(ServerPlayer p) {
        return ArmouryRules.capacity(mastery(p), Nullification.maxEP(p), privileged(p));
    }

    /** True when the player knows Requip and the mode is open to them. */
    public static boolean mayUse(ServerPlayer p, String modeId) {
        if (instanceOf(p).isEmpty()) return false;
        Mode m = RequipMagicRules.byId(modeId);
        return m == null || RequipMagicRules.unlocked(m, mastery(p), Nullification.maxEP(p), privileged(p));
    }

    // ---------------------------------------------------------------- partial armour (read by the skill)
    /** The mask of the armour the player puts on now: the whole set unless Partial Armour is on. */
    static int maskOf(ServerPlayer p) {
        if (!p.hasData(RequipArmoury.DATA)) return PartialArmourMask.FULL;
        RequipArmoury.Data d = RequipArmoury.data(p);
        return d.partialOn ? PartialArmourMask.normalize(d.mask) : PartialArmourMask.FULL;
    }

    static double fractionOf(ServerPlayer p, Armour a) { return PartialArmourMask.fraction(a, maskOf(p)); }

    // ---------------------------------------------------------------- cooldowns of the modes
    private static String cdKey(Mode m) { return "MCd." + m.id(); }

    private static long cooldownLeft(ManasSkillInstance inst, ServerPlayer p, Mode m) {
        return Math.max(0L, inst.getOrCreateTag().getLong(cdKey(m)) - p.level().getGameTime());
    }

    private static boolean ready(ManasSkillInstance inst, ServerPlayer p, Mode m) {
        long left = cooldownLeft(inst, p, m);
        if (left > 0) { SkillUtil.fail(p, SkillMsg.t("erza.cooldown", "%s is recharging (%s s).", m.english(), (left + 19) / 20)); return false; }
        return true;
    }

    private static void rest(ManasSkillInstance inst, ServerPlayer p, Mode m, int ticks) {
        inst.getOrCreateTag().putLong(cdKey(m), p.level().getGameTime() + Math.max(0, ticks));
    }

    private static boolean pay(ServerPlayer p, Mode m, double floor) {
        if (m.cost() <= 0 || EnergyBridge.aura(p, m.cost() / 100.0, floor)) return true;
        SkillUtil.fail(p, SkillMsg.t("erza.no_power", "Not enough magic power for %s.", m.english()));
        return false;
    }

    // ---------------------------------------------------------------- the key
    /** The skill key in one of the appended modes. */
    static void press(ServerPlayer p, ManasSkillInstance inst, Mode m) {
        double mastery = NUSkill.masteryFrac(inst);
        boolean priv = privileged(p);
        double ep = Nullification.maxEP(p);
        if (!RequipMagicRules.unlocked(m, mastery, ep, priv)) {
            SkillUtil.fail(p, SkillMsg.t("erza.magic_locked", "%s is not yet yours: master Requip to %s percent and reach %s max EP.", m.english(), (int) Math.round(m.unlock() * 100), (long) m.minEp()));
            return;
        }
        switch (m) {
            case ARMOURY -> openArmoury(p);
            case LOADOUTS -> loadouts(p, inst, mastery, priv);
            case BLADE -> blade(p, inst, m, mastery);
            case BARRAGE -> barrage(p, inst, m, mastery);
            case STRIKE -> strike(p, inst, m, mastery);
            case PARTIAL -> partial(p, inst, m);
            default -> finisher(p, inst, m, mastery);
        }
        inst.markDirty();
    }

    // ---------------------------------------------------------------- Armoury
    private static void openArmoury(ServerPlayer p) {
        RequipMagicFx.pocket(p.serverLevel(), p);
        RequipArmoury.open(p);
    }

    // ---------------------------------------------------------------- Loadouts
    private static void loadouts(ServerPlayer p, ManasSkillInstance inst, double mastery, boolean priv) {
        RequipArmoury.Data d = RequipArmoury.data(p);
        int target = LoadoutRules.cycle(d.savedFlags(), d.active, p.isShiftKeyDown());
        if (target < 0) { SkillUtil.fail(p, SkillMsg.t("erza.no_loadout", "No loadout is saved: open the Armoury, tab Loadouts, and save what you wear.")); return; }
        equipLoadout(p, inst, target, mastery, priv);
    }

    private static void equipLoadoutByButton(ServerPlayer p, int n) {
        Optional<ManasSkillInstance> inst = instanceOf(p);
        if (inst.isEmpty() || !mayUse(p, Mode.LOADOUTS.id())) return;
        equipLoadout(p, inst.get(), n, NUSkill.masteryFrac(inst.get()), privileged(p));
        inst.get().markDirty();
    }

    /** The whole swap: the cooldown, a dry run of the plan, the cost, the swap, the circle. Nothing is spent unless the swap is going to happen. */
    static boolean equipLoadout(ServerPlayer p, ManasSkillInstance inst, int n, double mastery, boolean priv) {
        Mode m = Mode.LOADOUTS;
        if (!ready(inst, p, m)) return false;
        int cap = ArmouryRules.capacity(mastery, Nullification.maxEP(p), priv);
        RequipLoadouts.Outcome o = RequipLoadouts.equip(p, n, cap, false);
        switch (o.status()) {
            case ARMOURY_FULL -> { SkillUtil.fail(p, SkillMsg.t("erza.armoury_full", "The Armoury is full: the old set has nowhere to go. Nothing was changed.")); return false; }
            case NOTHING_FOUND -> { SkillUtil.fail(p, SkillMsg.t("erza.loadout_gone", "Nothing of loadout %s is left in the Armoury or on you.", n + 1)); return false; }
            case UNCHANGED -> { SkillUtil.actionbar(p, SkillMsg.t("erza.loadout_on", "Loadout %s is already on.", n + 1)); RequipArmoury.data(p).active = n; return false; }
            case EMPTY_LOADOUT -> { SkillUtil.fail(p, SkillMsg.t("erza.loadout_empty", "Loadout %s is empty.", n + 1)); return false; }
            case FAILED -> { SkillUtil.fail(p, SkillMsg.t("erza.loadout_failed", "The swap would not balance. Nothing was changed.")); return false; }
            default -> { }
        }
        if (!pay(p, m, RequipRules.EQUIP_FLOOR)) return false;
        RequipLoadouts.Outcome done = RequipLoadouts.equip(p, n, cap, true);
        if (!done.done()) return false;
        RequipMagicFx.swapCircle(p.serverLevel(), p, themeRgb(inst));
        flourish(p);
        SkillFx.sound(p.serverLevel(), p.position(), net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 1.0F, 1.2F);
        SkillUtil.actionbar(p, done.missing() > 0
                ? SkillMsg.t("erza.loadout_partial", "Loadout %s: %s pieces changed, %s missing.", n + 1, done.moved(), done.missing())
                : SkillMsg.t("erza.loadout_done", "Loadout %s: %s pieces changed.", n + 1, done.moved()));
        rest(inst, p, m, LoadoutRules.cooldown(mastery, priv));
        grow(inst, p, 0.5);
        return true;
    }

    /**
     * The look of an instant swap in the Djinn's equip animation: a few pieces of the Heart Kreuz armour forge in round the user and shatter out again (the forge takes {@link RequipRules#FORGE_TICKS},
     * the shatter {@link RequipRules#SHATTER_WINDOW}). It is the same render aura as the armours, so it is drawn only when no Erza armour is on (an armour's own lease is never overwritten).
     */
    static void flourish(ServerPlayer p) {
        if (!RequipMagicConfig.flourish() || com.newuniverse.nusmp.aura.PlayerAuras.active(p, com.newuniverse.nusmp.aura.Aura.ERZA)) return;
        int mask = PartialArmourMask.HELM | PartialArmourMask.PAULDRONS | PartialArmourMask.GAUNTLETS | PartialArmourMask.GREAVES;
        ErzaRequipFx.setAura(p, Armour.HEART_KREUZ, RequipRules.FORGE_TICKS + RequipRules.SHATTER_WINDOW, mask);
    }

    private static int themeRgb(ManasSkillInstance inst) {
        Armour a = ErzaRequipSkill.worn(inst);
        return a == null ? 0xE8C468 : a.glow();
    }

    private static void grow(ManasSkillInstance inst, ServerPlayer p, double amount) {
        if (amount > 0) inst.addMasteryPoint(p, amount);
    }

    // ---------------------------------------------------------------- Requip: Blade
    private static void blade(ServerPlayer p, ManasSkillInstance inst, Mode m, double mastery) {
        RequipArmoury.Data d = RequipArmoury.data(p);
        int cap = capacity(p);
        if (p.isShiftKeyDown()) {                                                                   // sneak: choose the next weapon of the Armoury
            int cell = RequipWeapons.cycleSelection(p);
            if (cell < 0) { SkillUtil.fail(p, SkillMsg.t("erza.no_weapon", "The Armoury holds no weapon.")); return; }
            SkillUtil.actionbar(p, SkillMsg.t("erza.blade_chosen", "Selected: %s", d.cells.get(cell).getHoverName()));
            return;
        }
        if (RequipWeapons.bladeOut(p)) {
            RequipWeapons.Result r = RequipWeapons.putAway(p, cap);
            if (r == RequipWeapons.Result.OK) { RequipMagicFx.pocket(p.serverLevel(), p); SkillUtil.actionbar(p, SkillMsg.t("erza.blade_away", "The weapon returns to the pocket.")); }
            else SkillUtil.fail(p, SkillMsg.t("erza.armoury_full", "The Armoury is full: the old set has nowhere to go. Nothing was changed."));
            return;
        }
        if (!ready(inst, p, m)) return;
        if (RequipWeapons.selectedCell(d) < 0) { SkillUtil.fail(p, SkillMsg.t("erza.no_weapon", "The Armoury holds no weapon.")); return; }
        if (!pay(p, m, 6)) return;
        RequipWeapons.Result r = RequipWeapons.summon(p);
        if (r == RequipWeapons.Result.OK) {
            RequipMagicFx.pocket(p.serverLevel(), p);
            SkillUtil.actionbar(p, SkillMsg.t("erza.blade_out", "Requip: %s", p.getMainHandItem().getHoverName()));
            rest(inst, p, m, m.cooldown());
            grow(inst, p, 0.5);
        } else if (r == RequipWeapons.Result.HAND_FLAGGED) {
            SkillUtil.fail(p, SkillMsg.t("erza.blade_flagged", "The armour's own weapon is in your hand: dismiss the armour first."));
        } else {
            SkillUtil.fail(p, SkillMsg.t("erza.no_weapon", "The Armoury holds no weapon."));
        }
    }

    // ---------------------------------------------------------------- Requip: Barrage
    private static void barrage(ServerPlayer p, ManasSkillInstance inst, Mode m, double mastery) {
        ItemStack w = RequipWeapons.aimedWeapon(p);
        if (w.isEmpty()) { SkillUtil.fail(p, SkillMsg.t("erza.barrage_none", "Hold a weapon, or keep one selected in the Armoury.")); return; }
        if (!ready(inst, p, m) || !pay(p, m, RequipRules.ATTACK_FLOOR)) return;
        int count = RequipBarrage.swordsFor(p, mastery);
        Armour a = ErzaRequipSkill.worn(inst);
        Script s = RequipBarrage.barrage(p, w, mastery, count, a == null ? RequipBarrage.GOLD : 0xFF000000 | a.glow());
        if (!RequipScripts.start(p.server, s)) { SkillUtil.fail(p, SkillMsg.t("erza.busy", "Too much magic is in the air already.")); return; }
        SkillUtil.actionbar(p, SkillMsg.t("erza.barrage_cast", "%s swords of %s", count, w.getHoverName()));
        rest(inst, p, m, DMUtil.cooldown(m.cooldown()));
        grow(inst, p, 1.0);
    }

    // ---------------------------------------------------------------- Requip: Strike
    private static void strike(ServerPlayer p, ManasSkillInstance inst, Mode m, double mastery) {
        if (RequipWeapons.thrown(p)) {                                                              // pressed again: call it home now
            RequipScripts.cancel(p, "strike");
            if (RequipWeapons.recall(p, capacity(p))) SkillUtil.actionbar(p, SkillMsg.t("erza.strike_back", "The weapon returns."));
            else SkillUtil.fail(p, SkillMsg.t("erza.strike_wait", "The weapon waits in the pocket for room."));
            return;
        }
        ItemStack hand = p.getMainHandItem();
        if (!RequipWeapons.isWeapon(hand) || com.newuniverse.nusmp.requip.RequipWeaponItem.flagged(hand)) { SkillUtil.fail(p, SkillMsg.t("erza.strike_none", "Hold a weapon to throw.")); return; }
        if (!ready(inst, p, m) || !pay(p, m, RequipRules.ATTACK_FLOOR)) return;
        ItemStack thrown = RequipWeapons.throwOut(p);
        if (thrown.isEmpty()) return;
        Armour a = ErzaRequipSkill.worn(inst);
        Script s = RequipBarrage.strike(p, thrown, mastery, capacity(p), a == null ? RequipBarrage.GOLD : 0xFF000000 | a.glow());
        if (!RequipScripts.start(p.server, s)) RequipWeapons.recall(p, capacity(p));
        rest(inst, p, m, DMUtil.cooldown(m.cooldown()));
        grow(inst, p, 1.0);
    }

    // ---------------------------------------------------------------- finishers
    private static void finisher(ServerPlayer p, ManasSkillInstance inst, Mode m, double mastery) {
        if (!ready(inst, p, m) || !pay(p, m, RequipRules.ATTACK_FLOOR)) return;
        Script s = RequipFinishers.build(p, m, mastery);
        if (s == null || !RequipScripts.start(p.server, s)) { SkillUtil.fail(p, SkillMsg.t("erza.busy", "Too much magic is in the air already.")); return; }
        SkillUtil.actionbar(p, Component.literal(m.english()).withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(RequipFinishers.colour(m) & 0xFFFFFF)));
        rest(inst, p, m, DMUtil.cooldown(m.cooldown()));
        grow(inst, p, 1.5);
    }

    // ---------------------------------------------------------------- Partial Armour
    private static void partial(ServerPlayer p, ManasSkillInstance inst, Mode m) {
        RequipArmoury.Data d = RequipArmoury.data(p);
        if (p.isShiftKeyDown()) {                                                                   // sneak: the next preset
            d.mask = PartialArmourMask.preset(PartialArmourMask.presetIndexOf(d.mask) + 1).mask();
            d.partialOn = d.mask != PartialArmourMask.FULL;
        } else if (wornMaskDiffers(p, inst)) {
            // the key applies the setting to the armour that is on
        } else {
            d.partialOn = !d.partialOn;
        }
        applyPartial(p, inst, m);
    }

    private static boolean wornMaskDiffers(ServerPlayer p, ManasSkillInstance inst) {
        return ErzaRequipSkill.worn(inst) != null && ErzaRequipSkill.wornMask(inst) != maskOf(p);
    }

    private static void applyPartialByButton(ServerPlayer p) {
        Optional<ManasSkillInstance> inst = instanceOf(p);
        if (inst.isEmpty() || !mayUse(p, Mode.PARTIAL.id())) return;
        applyPartial(p, inst.get(), Mode.PARTIAL);
        inst.get().markDirty();
    }

    /** Makes the worn armour match the setting (shatter, forge again with the chosen pieces), or just reports it when no armour is on. */
    static void applyPartial(ServerPlayer p, ManasSkillInstance inst, Mode m) {
        int mask = maskOf(p);
        int idx = PartialArmourMask.presetIndexOf(mask);
        Component what = idx >= 0 ? Component.translatableWithFallback("nusmp.requip.preset." + PartialArmourMask.preset(idx).id(), PartialArmourMask.preset(idx).english())
                : Component.translatableWithFallback("nusmp.requip.preset.custom", "Custom pieces");
        Armour now = ErzaRequipSkill.worn(inst);
        if (now == null || ErzaRequipSkill.pending(inst) != null) {
            SkillUtil.actionbar(p, SkillMsg.t("erza.partial_set", "Partial Armour: %s", what));
            return;
        }
        if (ErzaRequipSkill.wornMask(inst) == mask) { SkillUtil.actionbar(p, SkillMsg.t("erza.partial_set", "Partial Armour: %s", what)); return; }
        if (!ready(inst, p, m)) return;
        if (!EnergyBridge.aura(p, RequipRules.equipShare(now, true) * PartialArmourMask.fraction(now, mask), RequipRules.EQUIP_FLOOR)) {
            SkillUtil.fail(p, SkillMsg.t("erza.no_power", "Not enough magic power for %s.", m.english()));
            return;
        }
        ErzaRequipSkill.startSwap(p, inst, now, now);
        rest(inst, p, m, m.cooldown());
        SkillUtil.actionbar(p, SkillMsg.t("erza.partial_on", "Partial Armour: %s (%s percent of the armor)", what, (int) Math.round(PartialArmourMask.fraction(now, mask) * 100)));
        grow(inst, p, 0.5);
    }

    // ---------------------------------------------------------------- Requip Reflex
    /** A hit was taken: if a loadout is marked as the reflex loadout (else the Adamantine Armor is open to the user) swap to it, once per pause. */
    static void reflex(ServerPlayer p, ManasSkillInstance inst) {
        if (!p.hasData(RequipArmoury.DATA)) return;
        RequipArmoury.Data d = RequipArmoury.data(p);
        long now = p.level().getGameTime();
        double mastery = NUSkill.masteryFrac(inst);
        if (!RequipMagicRules.reflexReady(RequipMagicConfig.reflexEnabled(), mastery, now, d.reflexAt, RequipMagicConfig.reflexTicks())) return;
        if (ErzaRequipSkill.pending(inst) != null) return;
        boolean acted = false;
        int n = d.reflex;
        if (n >= 0 && n < d.loadouts.size() && d.loadouts.get(n).saved() && d.active != n) {
            int cap = ArmouryRules.capacity(mastery, Nullification.maxEP(p), privileged(p));
            RequipLoadouts.Outcome o = RequipLoadouts.equip(p, n, cap, true);
            if (o.done()) {
                acted = true;
                RequipMagicFx.swapCircle(p.serverLevel(), p, themeRgb(inst));
                SkillUtil.actionbar(p, SkillMsg.t("erza.reflex_loadout", "Requip Reflex: loadout %s", n + 1));
            }
        } else if (n < 0 && ErzaRequipSkill.worn(inst) != Armour.ADAMANTINE && RequipRules.unlocked(Armour.ADAMANTINE, mastery, privileged(p))
                && EnergyBridge.aura(p, RequipRules.equipShare(Armour.ADAMANTINE, ErzaRequipSkill.worn(inst) != null) * fractionOf(p, Armour.ADAMANTINE), RequipRules.EQUIP_FLOOR)) {
            ErzaRequipSkill.startSwap(p, inst, ErzaRequipSkill.worn(inst), Armour.ADAMANTINE);
            acted = true;
            SkillUtil.actionbar(p, SkillMsg.t("erza.reflex_adamantine", "Requip Reflex: Adamantine Armor"));
        }
        if (acted) d.reflexAt = now;
    }

    // ---------------------------------------------------------------- Weapon Affinity
    /** The damage factor of the melee hits of the weapon that Requip: Blade summoned: up to +40 percent with mastery. */
    static float affinityFactor(ServerPlayer p, double mastery) {
        if (RequipScripts.hitting || !p.hasData(RequipArmoury.DATA) || !RequipWeapons.bladeOut(p)) return 1.0f;
        return (float) BarrageMath.affinity(mastery);
    }

    // ---------------------------------------------------------------- the safety net
    /** Every second: a weapon waiting in the limbo with no throw running (the caster logged out, died or the server restarted mid-throw) is brought home. */
    static void safetyNet(ServerPlayer p) {
        if (!p.hasData(RequipArmoury.DATA) || p.isRemoved() || !p.isAlive()) return;
        RequipArmoury.Data d = RequipArmoury.data(p);
        if (d.limbo.isEmpty() || RequipScripts.running(p, "strike")) return;
        RequipWeapons.recall(p, capacity(p));
    }

    private static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % 20 != 0) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) safetyNet(p);
    }
}
