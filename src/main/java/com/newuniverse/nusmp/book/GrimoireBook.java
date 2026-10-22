package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.GrimoireCover;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.blackclover.TimeStop;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.magic.Element;
import io.github.manasmods.tensura.ability.magic.spiritual.SpiritualMagic;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * A grimoire = a Tensura UNIQUE skill (gold name, Unique list). The book item is only the key.
 * Select a page and release the Tensura skill key to cast immediately. Cost goes
 * through getMagiculeCost + EnergyHelper.isOutOfEnergy, damage through createSource with Tensura
 * damage types, cooldowns through instance.setCoolDown(cooldown, mode) only after a successful cast.
 *
 * Modes: the family's pages (mode 0 unlocked, the rest open by mastery: multiverse.MasteryPages), then Spirit Dive
 * (element books, Spirit Lord gate) and Devil Union (five-leaf / triple-spade covers only).
 */
public abstract class GrimoireBook extends Skill {
    public static final int CAST_TICKS = 20, CAST_TICKS_MASTERED = 8;
    public final MagicType magic;
    public final int color;
    private final ResourceLocation icon;
    private List<BookPage> all;
    private int builtVersion;

    protected GrimoireBook(MagicType magic, int color) {
        this(magic, color, ResourceLocation.fromNamespaceAndPath("nusmp", "textures/skill/grimoire/" + magic.name().toLowerCase() + ".png"));
    }

    protected GrimoireBook(MagicType magic, int color, ResourceLocation icon) {
        super(Skill.SkillType.UNIQUE);
        this.magic = magic;
        this.color = color;
        this.icon = icon;
    }

    /** 0.47: a book of another Tensura skill class (Kotodama is God-class: ULTIMATE). */
    protected GrimoireBook(MagicType magic, int color, Skill.SkillType type) {
        super(type);
        this.magic = magic;
        this.color = color;
        this.icon = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/skill/grimoire/" + magic.name().toLowerCase() + ".png");
    }

    // ---------------------------------------------------------------- 1.27.0: what a book calls itself (a merged book answers dynamically)
    /** The magic this book casts RIGHT NOW: its fixed magic (the retired merged-magic skills answer a placeholder). */
    public MagicType magicNow() { return magic; }

    /** The id the fault guard and the profile registry know this book by: "FLAME", or "merged:flame+water" for a merged book. */
    public String magicId() { return magic.name(); }

    /** How the book names its magic in messages and chants. */
    public String magicLabel() { return magic.displayName; }

    /** The glow colour of this book's chant circle (a merged book blends its parents'). */
    public int colorNow() { return color; }

    /** 1.27.0: changes whenever the book's family pages change (a merged book whose definition changed); fixed for every other book, so their page list is built once. */
    protected int pagesVersion() { return 0; }

    /** 1.27.0: does this book get the universal pages (Devil Union, Summon, Store / Draw Weapon, Mana Strike / Zone, the wizard's spells)? Not the Forbidden book, not a merged book. */
    protected boolean universalPages() { return !(this instanceof ForbiddenBook); }

    /** 1.27.0: whose floating grimoire flips its pages when the player switches spell (null = any). */
    protected MagicType summonMagic() { return this instanceof ForbiddenBook ? null : magic; }

    /** Does learning this book hand over a physical grimoire item? (Not for a merged book: its parents' grimoires stay the keys.) */
    protected boolean givesGrimoireItem() { return !(this instanceof ForbiddenBook); }

    /** The family's own pages, mode order (mode 0 = starter). */
    protected abstract List<BookPage> familyPages();

    /** Tensura damage type for this book. */
    public abstract ResourceKey<DamageType> damageType();

    /** Element for Spirit Dive, or null if this book has no spirit. */
    public Element spiritElement() { return null; }
    public String spiritName() { return null; }

    // ---------------------------------------------------------------- pages / modes
    public final List<BookPage> pages() {
        int version = pagesVersion();
        if (all == null || version != builtVersion) {
            builtVersion = version;
            List<BookPage> l = new ArrayList<>(familyPages());
            if (spiritElement() != null) {
                l.add(BookPage.signature("spirit_dive", "Spirit Dive", GrimoireBook::spiritDive).withCooldown(0));
                l.add(BookPage.signature("spirit_channeling", "Spirit Burst", GrimoireBook::channel));
            }
            if (universalPages()) l.add(BookPage.signature("devil_union", "Devil Union", GrimoireBook::devilUnion));
            // base ability, last so every existing page keeps its mode number: free, instant, no chant
            if (universalPages()) l.add(new BookPage(SUMMON_ID, "Summon Grimoire", "Summon Grimoire", 0, 0, 0, (b, i, p, m) -> true));
            if (universalPages()) {                                                 // 0.54: as in the show, a weapon goes back into the book and comes out again
                l.add(new BookPage(STORE_ID, "Store Weapon", "Store Weapon", 0, 0, 0, (b, i, p, m) -> true));
                l.add(new BookPage(DRAW_ID, "Draw Weapon", "Draw Weapon", 0, 0, 0, (b, i, p, m) -> true));
                // 1.5.0: every magic is a combat magic: raw mana as a bolt, and the Mana Zone (appended last, so no mode number moves)
                l.add(new BookPage(STRIKE_ID, "Mana Strike", "Mana Strike", 5, 100, 40, (b, i, p, m) -> com.newuniverse.nusmp.mana.ManaLaw.manaStrike(b, p)));
                l.add(new BookPage(ZONE_ID, "Mana Zone", "Mana Zone", 0, 0, 0, (b, i, p, m) -> true));
                l.addAll(SignatureSpell.pages());                                       // 1.16.0: the wizard's own spells, appended last
                l.add(com.newuniverse.nusmp.mana.zone.ManaZoneActions.volleyPage());     // 1.99.0: the Mana Zone's offence and its pressurised space, appended after everything (late-game gate: see usable)
                l.add(com.newuniverse.nusmp.mana.zone.ManaZoneActions.spacePage());
                l.add(com.newuniverse.nusmp.mana.zone.TrueMagic.page());                 // 1.99.0: True Magic (Mana Method): the real element from a rune array, natural attributes only, same late-game gate
            }
            all = l;
        }
        return all;
    }

    public int familyCount() { return familyPages().size(); }

    /**
     * Anything besides mastery, rank and race that a page waits for before it can open (0.34): character spells, Charmy's
     * Cotton / Food half. Null = nothing; otherwise who or what it waits for, shown to the player once.
     */
    public String pageBlock(ServerPlayer p, ManasSkillInstance inst, int mode) {
        return CharacterSpells.block(this, page(mode), p);
    }
    public BookPage page(int mode) { return mode >= 0 && mode < pages().size() ? pages().get(mode) : null; }
    private boolean isDive(int mode) { return page(mode) != null && (page(mode).id().equals("spirit_dive") || page(mode).id().equals("spirit_channeling")); }
    private boolean isUnion(int mode) { return page(mode) != null && page(mode).id().equals("devil_union"); }
    public static final String SUMMON_ID = "summon_grimoire";
    private boolean isSummon(int mode) { return page(mode) != null && page(mode).id().equals(SUMMON_ID); }
    /** 0.54: the last two pages of every grimoire: put the weapon in your hand back into the book, and draw a stored one (anim.WeaponStore). */
    public static final String STORE_ID = "store_weapon", DRAW_ID = "draw_weapon";
    private boolean isStore(int mode) { return page(mode) != null && page(mode).id().equals(STORE_ID); }
    /** 1.5.0: the universal pages (mana.ManaLaw). */
    public static final String STRIKE_ID = "mana_strike", ZONE_ID = "mana_zone";
    private boolean isManaZone(int mode) { return page(mode) != null && page(mode).id().equals(ZONE_ID); }
    /** 1.99.0: the Mana Zone page and the two pages that need it are LATE-GAME: not selectable, listed or castable until the gate (mana.zone.ManaZoneGate) is open. */
    private boolean isZoneGated(int mode) {
        BookPage pg = page(mode);
        return pg != null && (pg.id().equals(ZONE_ID) || pg.id().equals(com.newuniverse.nusmp.mana.zone.ManaZoneActions.VOLLEY_ID) || pg.id().equals(com.newuniverse.nusmp.mana.zone.ManaZoneActions.SPACE_ID)
                || pg.id().equals(com.newuniverse.nusmp.mana.zone.TrueMagic.ID));
    }
    private boolean isDrawWeapon(int mode) { return page(mode) != null && page(mode).id().equals(DRAW_ID); }
    /** Pages that act on the key press itself without magicule cost or a cooldown. */
    private boolean instant(int mode) {
        return isSummon(mode) || isStore(mode) || isDrawWeapon(mode) || isManaZone(mode)
                || this instanceof LegionBook && page(mode) != null && page(mode).id().equals(LegionBook.CHESSBOARD_ID);
    }

    public static boolean isUnlocked(ManasSkillInstance i, int mode) {
        return (mode == 0 || (i.getOrCreateTag().getInt("Unlocked") & (1 << mode)) != 0) && !com.newuniverse.nusmp.charset.CharacterSets.hidden(i, mode);      // character spell sets: a page the bound character never used (or a signature spell, in the basic book) is not in the grimoire
    }

    public static void unlock(ManasSkillInstance i, int mode) {
        i.getOrCreateTag().putInt("Unlocked", i.getOrCreateTag().getInt("Unlocked") | (1 << mode));
        i.markDirty();
    }

    /** Can this mode be selected/cast right now? */
    public boolean usable(ManasSkillInstance i, LivingEntity e, int mode) {
        if (mode < familyCount()) return isUnlocked(i, mode);
        if (isDive(mode)) return e instanceof ServerPlayer p ? spiritGateOpen(p) : true;
        if (isUnion(mode)) return cover(i).isForbidden();
        if (isZoneGated(mode)) return com.newuniverse.nusmp.mana.zone.ManaZoneManager.gateOpenFor(e);      // 1.99.0: before the instant-page rule below, which used to make the Mana Zone open to everyone
        if (instant(mode)) return true;
        if (page(mode) != null && page(mode).id().equals(STRIKE_ID)) return true;      // 1.5.0: Mana Strike, in every grimoire
        if (page(mode) != null && SignatureSpell.isSig(page(mode).id()))                // 1.16.0: the Signature Spell for every wizard, the born spells for a fused book
            return page(mode).id().equals(SignatureSpell.SIG_ID) || SignatureSpell.fused(i);
        return false;
    }

    @Override public ResourceLocation getSkillIcon() { return icon; }
    @Override public int getModes(ManasSkillInstance instance) { return pages().size(); }

    /** The instance names itself with the bound character in front of the magic ("Rades Spirito's Soul Corpse Magic"): see {@link GrimoireSkillInstance}. */
    @Override public io.github.manasmods.tensura.ability.TensuraSkillInstance createDefaultInstance() { return new GrimoireSkillInstance(this); }

    @Override
    public int nextMode(LivingEntity entity, ManasSkillInstance instance, int mode, boolean reverse) {
        int n = pages().size();
        int next = 0;
        for (int step = 1; step <= n; step++) {
            int m = Math.floorMod(mode + (reverse ? -step : step), n);
            if (usable(instance, entity, m)) { next = m; break; }
        }
        // switching spells while the grimoire is summoned flips its pages (server tells everyone watching)
        if (next != mode && entity instanceof ServerPlayer sp) GrimoireSummon.onSpellSwitch(sp, summonMagic(), reverse);
        return next;
    }

    @Override public String getModeId(ManasSkillInstance instance, int mode) { BookPage p = page(mode); return p == null ? "none" : p.id(); }

    @Override
    public Component getModeName(ManasSkillInstance instance, int mode) {
        BookPage p = page(mode);
        if (p == null) return Component.literal("?");
        if (com.newuniverse.nusmp.charset.CharacterSets.hidden(instance, mode)) return Component.literal("-").withStyle(ChatFormatting.DARK_GRAY);      // character spell sets: not a page of this grimoire at all
        if (mode < familyCount() && !isUnlocked(instance, mode)) return Component.literal("Sealed Page").withStyle(ChatFormatting.DARK_GRAY);
        if (mode >= familyCount() && !usable(instance, null, mode)) return Component.literal(com.newuniverse.nusmp.core.discovery.DiscoveryRules.QUESTION).withStyle(ChatFormatting.DARK_GRAY);   // 1.27.0: a base page that cannot be used yet stays hidden
        int cd = instance.getCoolDown(mode);   // ManasCore cooldowns are in seconds
        String shown = SignatureSpell.pageName(instance, p);                           // 1.16.0: a signature page shows the wizard's own spell name
        return Component.literal(cd > 0 ? shown + " (" + cd + "s)" : shown);
    }

    // ---------------------------------------------------------------- cost & cast loop
    @Override
    public double getMagiculeCost(LivingEntity entity, ManasSkillInstance instance, int mode) {
        BookPage p = page(mode);
        if (p == null) return 0;
        double c = BalanceLaw.cost(entity, p.costPercent(), p.costFloor()) * cover(instance).cost;
        if (p.costPercent() <= 0 && p.costFloor() <= 0) return 0;                       // e.g. Anti-Magic costs nothing
        if (cover(instance).kingdom == com.newuniverse.nusmp.blackclover.Kingdom.HEART && p.costPercent() >= 20) c *= 0.85; // Mana Method
        if (entity instanceof ServerPlayer sp) c *= com.newuniverse.nusmp.item.MagicGear.costMult(sp) * (1 + ForbiddenMagic.manaTax(sp)) * com.newuniverse.nusmp.mana.ManaLaw.costMult(sp);   // 1.5.0
        return c;
    }

    @Override public int getMaxHeldTime(ManasSkillInstance instance, LivingEntity entity) { return 200; }

    public int castTicks(ManasSkillInstance i, LivingEntity e) { return i.isMastered(e) ? CAST_TICKS_MASTERED : CAST_TICKS; }

    /** Utility pages act on press; spell pages wait for release so the cast fires only after the key is let go. */
    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int keyNumber, int mode) {
        if (!(entity instanceof ServerPlayer p)) return;
        if (!instant(mode)) {
            instance.getOrCreateTag().putInt("ChargeMode", mode);
            instance.getOrCreateTag().putInt("ChargeTicks", 0);
            instance.markDirty();
            if (CastHoldFx.pressed(instance, p.level().getGameTime()) && com.newuniverse.nusmp.firecast.FireCasting.chants(this, p, mode)) com.newuniverse.nusmp.anim.CastAnim.play(p, com.newuniverse.nusmp.anim.CastAnim.CHANT);   // 1.27.0: a repeated press does not replay the chant (the fire styles: Mereoleona does not chant)
        }
        if (isSummon(mode)) { GrimoireSummon.toggle(p, this); return; }
        if (isStore(mode)) { com.newuniverse.nusmp.anim.WeaponStore.store(p, this, instance); return; }
        if (isDrawWeapon(mode)) { com.newuniverse.nusmp.anim.WeaponStore.draw(p, this, instance); return; }
        if (isManaZone(mode)) {
            boolean wasOn = com.newuniverse.nusmp.mana.ManaLaw.zoneActive(p);
            if (com.newuniverse.nusmp.mana.ManaLaw.toggleZone(p) && !wasOn) com.newuniverse.nusmp.entity.zogratis.LuciusCapabilities.onManaZone(p);      // 1.45.0: switching the zone ON is a massive channel to the god-tier Lucius
            return;
        }
        if (instant(mode)) {
            if (this instanceof LegionBook && page(mode) != null && page(mode).id().equals(LegionBook.CHESSBOARD_ID))
                LegionArts.toggleChessboard(p);
            return;
        }
    }

    @Override
    public boolean onHeld(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int mode) {
        if (!(entity instanceof ServerPlayer p) || instant(mode)) return true;
        int charge = Math.min(castTicks(instance, p), Math.max(0, heldTicks));
        instance.getOrCreateTag().putInt("ChargeMode", mode);
        instance.getOrCreateTag().putInt("ChargeTicks", charge);
        instance.markDirty();
        // 1.27.0: one continuous hold circle (start once, grow with the charge, keep-alive) instead of a new circle every 4 ticks; none for a page that cannot be cast
        CastHoldFx.held(instance, p, colorNow(), charge, castTicks(instance, p), castReady(instance, p, mode));
        if (magic == MagicType.SCALE) com.newuniverse.nusmp.scalecast.ScaleCastKit.onHeld(p);      // Scale casting: the Scale stays out while a page is held
        if (com.newuniverse.nusmp.entity.zogratis.LuciusCapabilities.fightOn())          // 1.45.0: a heavy page held long enough is a massive channel to the god-tier Lucius
            com.newuniverse.nusmp.entity.zogratis.LuciusCapabilities.onGrimoireHold(p, page(mode), charge, castTicks(instance, p), castReady(instance, p, mode));
        com.newuniverse.nusmp.firecast.FireCasting.held(this, instance, p, heldTicks, mode);      // fire casting: Leopold's spiral winds while the key is held (nothing for every other caster)
        return true;
    }

    @Override
    public void onRelease(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int keyNumber, int mode) {
        if (!(entity instanceof ServerPlayer player) || instant(mode)) return;
        if (com.newuniverse.nusmp.firecast.FireCasting.release(this, instance, player, heldTicks, mode)) return;      // fire casting: Mereoleona readies the page (the fists release it); Leopold fixes his spiral level
        long now = player.level().getGameTime();
        long pendingUntil = instance.getOrCreateTag().getLong("PendingCastUntil");
        int charge = Math.min(castTicks(instance, player), Math.max(0, heldTicks));
        float chargePower = charge / (float) castTicks(instance, player);
        int windup = instance.isMastered(player) ? 4 : 7;
        // 1.27.0: the hold circle ends (or a tap gets its one release circle) even when an earlier cast is still winding up; none for a page that cannot be cast
        CastHoldFx.released(instance, player, colorNow(), pendingUntil <= now && castReady(instance, player, mode), windup, chargePower);
        if (com.newuniverse.nusmp.entity.zogratis.LuciusCapabilities.fightOn())          // 1.45.0: releasing a heavy page that was held long enough is a launched devastating attack
            com.newuniverse.nusmp.entity.zogratis.LuciusCapabilities.onGrimoireRelease(player, page(mode), charge, castTicks(instance, player), castReady(instance, player, mode));
        if (pendingUntil > now) return;
        long castAt = now + windup;
        instance.getOrCreateTag().putLong("PendingCastUntil", castAt);
        instance.getOrCreateTag().putFloat("MagicBuildUp", chargePower);
        instance.getOrCreateTag().putLong("MagicBuildUpUntil", castAt + 60);
        instance.getOrCreateTag().putInt("ChargeTicks", 0);
        instance.markDirty();
        com.newuniverse.nusmp.anim.CastAnim.play(player, com.newuniverse.nusmp.anim.CastAnim.CHANT);        // (the client ignores it while the chant already plays)
        SpellRuntime.later(player.serverLevel(), windup, () -> {
            if (instance.getOrCreateTag().getLong("PendingCastUntil") != castAt) return;
            instance.getOrCreateTag().putLong("PendingCastUntil", 0);
            instance.markDirty();
            com.newuniverse.nusmp.anim.CastAnim.stop(player);
            if (player.isAlive()) castPage(instance, player, mode);
        });
    }

    /** 1.27.0: whether castPage would get past its checks right now (no side effects): the circles of a held key are only shown for a page that can be cast. */
    private boolean castReady(ManasSkillInstance instance, ServerPlayer player, int mode) {
        return castReadyBase(instance, player, mode) && com.newuniverse.nusmp.firecast.FireCasting.ready(this, player, mode);      // fire casting: no hold circle for a page the style would refuse (Magna without the bat, Mereoleona's fists)
    }

    private boolean castReadyBase(ManasSkillInstance instance, ServerPlayer player, int mode) {
        BookPage p = page(mode);
        if (p == null) return false;
        if (com.newuniverse.nusmp.entity.zogratis.SpellSeal.sealed(player) || !usable(instance, player, mode) || !holdingBook(player)) return false;
        if (p.weaponTag() != null && !com.newuniverse.nusmp.item.WeaponMagicHelper.holdsAny(player, p.weaponTag())) return false;
        if (player.getPersistentData().getLong("nusmp_sealed_until") > player.level().getGameTime()) return false;
        return !instance.onCoolDown(mode);
    }

    /** Fire casting: whether the page could be cast right now (the hold's check), for the styles in {@code firecast}. */
    public boolean chargeReady(ManasSkillInstance instance, ServerPlayer player, int mode) { return castReadyBase(instance, player, mode); }

    /** Fire casting: casts the page the way a key release does (cost, cooldown, mastery, animation), for a style that releases it itself (Mereoleona's finisher punch). */
    public void castFromStyle(ManasSkillInstance instance, ServerPlayer player, int mode) { castPage(instance, player, mode); }

    private void castPage(ManasSkillInstance instance, ServerPlayer player, int mode) {
        BookPage p = page(mode);
        if (p == null) return;
        if (com.newuniverse.nusmp.entity.zogratis.SpellSeal.sealed(player)) { fail(player, com.newuniverse.nusmp.entity.zogratis.SpellSeal.MESSAGE); return; }   // 1.20.0: Spatial Mana Domination
        if (!usable(instance, player, mode)) { fail(player, "That page is still sealed."); return; }
        if (!holdingBook(player)) { fail(player, "Your pages are sealed shut. Summon your " + magicLabel() + " grimoire first (Summon Grimoire)."); return; }
        if (p.weaponTag() != null && !com.newuniverse.nusmp.item.WeaponMagicHelper.holdsAny(player, p.weaponTag())) {
            fail(player, "You must hold the required weapon to cast " + p.name() + ".");
            return;
        }
        if (player.getPersistentData().getLong("nusmp_sealed_until") > player.level().getGameTime()) { fail(player, "Your grimoire has been sealed!"); return; }
        String styleRefusal = com.newuniverse.nusmp.firecast.FireCasting.refuse(this, player, mode);      // fire casting: Magna needs his bat (nothing for every other caster)
        if (styleRefusal != null) { fail(player, styleRefusal); return; }
        if (ArmourCast.beforeCast(player, this, instance, p.id())) return;      // 1.99.0: the armour page of a worn armour only takes it off (free, no cooldown, no spell)
        if (instance.onCoolDown(mode)) { fail(player, p.name() + " is recharging (" + instance.getCoolDown(mode) + "s)."); return; }
        // 1.27.0: a magic whose code faulted is on a short recovery cooldown (checked BEFORE any mana is spent), and its cast runs
        // inside SafeCast: an exception in one magic's spell or effect code can not crash the server tick (core.magic.profile.SafeCast)
        long gameTime = player.level().getGameTime();
        if (com.newuniverse.nusmp.core.magic.profile.SafeCast.blocked(magicId(), gameTime)) {
            fail(player, magicLabel() + " is recovering (" + (com.newuniverse.nusmp.core.magic.profile.SafeCast.ticksLeft(magicId(), gameTime) + 19) / 20 + "s).");
            return;
        }
        instance.getOrCreateTag().putBoolean("ManaZone", false);
        if (EnergyHelper.isOutOfEnergy(player, instance, mode)) return;
        com.newuniverse.nusmp.labyrinth.mega.flow.FlowCasts.beforeCast(player, magicId(), p.id());      // 1.55.0: the Rukh flow: this tick's damage comes from this page
        if (!com.newuniverse.nusmp.core.magic.profile.SafeCast.run(magicId(), gameTime, player, () -> com.newuniverse.nusmp.sylph.SylphCasting.run(this, p, player, mode, () -> com.newuniverse.nusmp.undine.UndineCasting.run(this, p, instance, player, mode)))) { com.newuniverse.nusmp.anim.CastAnim.play(player, com.newuniverse.nusmp.anim.CastAnim.FAIL); return; }
        ArmourCast.afterCast(player, this, instance, p.id(), mode);      // 1.99.0: only an armour page equips; every other page leaves the armour layer alone (book.ArmourPolicy)
        MagicType castMagic = magicNow();
        if (castMagic == MagicType.SCALE) com.newuniverse.nusmp.scalecast.ScaleCastKit.onCast(player);      // Scale casting (docs/magic/scale_casting.md): the floating Scale weighs the foe, the weighing animation starts
        String clip = castMagic == MagicType.KEY ? com.newuniverse.nusmp.anim.CastAnim.KEY_TURN
                : castMagic == MagicType.DICE || castMagic == MagicType.GAME ? com.newuniverse.nusmp.anim.CastAnim.DICE_TOSS
                : com.newuniverse.nusmp.anim.CastAnim.releaseFor(p);
        String styledClip = com.newuniverse.nusmp.firecast.FireCasting.releaseClip(this, player, mode, clip);      // fire casting: the style's own release clip (the bat swing started at the toss)
        if (styledClip != null) com.newuniverse.nusmp.anim.CastAnim.play(player, styledClip);
        shout(player, p.incantation());
        if (p.cooldown() > 0) {
            int ticks = (int) (p.cooldown() * com.newuniverse.nusmp.mana.ManaLaw.cooldownMult(player) * com.newuniverse.nusmp.item.MagicGear.cooldownMult(player) * com.newuniverse.nusmp.NUGameRules.spellCooldown(player.level()));   // 0.48: 40% shorter by default
            instance.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(ticks), mode);
        }
        addMasteryPoint(instance, player);
        com.newuniverse.nusmp.charset.CharacterSets.onCast(instance, p.id());      // character spell sets: the cast counts toward the rules that name this page (Spirit of Zephyr after the Spirit Dive)
        com.newuniverse.nusmp.quests.QuestHooks.onCast(player, String.valueOf(castMagic), p.id(), p.costPercent());
        com.newuniverse.nusmp.labyrinth.mega.flow.FlowCasts.onCast(player, magicId(), p.id(), p.costPercent());      // 1.55.0: the Rukh flow: a finished cast is Light, Dark or neutral; an opposing one is rejected in a Holy place
        instance.getOrCreateTag().putInt("LastMode", mode);
        com.newuniverse.nusmp.item.MagicGear.onCast(player);
        com.newuniverse.nusmp.relics.wave.Wave6Events.onSpellCast(player);
        com.newuniverse.nusmp.spiritdark.SpiritDark.onCast(player, castMagic);      // Spirit of Darkness: the companion reacts to a Dark or Shadow cast (nothing for any other caster)
        com.newuniverse.nusmp.relics.wave.Wave16.onSpellCast(player, p.costPercent());      // 1.25.0: Brooch of Po'eur's free cast      // 1.24.0: Neket's robe stores a Mage Armor per cast
        DreamWorld.onCast(player, castMagic);                                     // 0.41: Spatial / Time / Anti-Magic / Dream tear a dream open
        MirrorWorks.echo(player);                                                 // 0.42: Real Doubles cast with you
        com.newuniverse.nusmp.mana.ManaLaw.onCast(player);                          // 1.5.0: negative mana corrupts
        com.newuniverse.nusmp.franchise.FranchiseEvents.onSpellCast(player);        // 1.111.0: franchise engravings of the held book (mana back, spell power)
        exhaustion(player);
        instance.markDirty();
    }

    // ---------------------------------------------------------------- shared spell helpers
    public static GrimoireCover cover(ManasSkillInstance i) { return GrimoirePages.coverOf(i); }
    public double masteryFrac(ManasSkillInstance i) { return Math.min(1.0, i.getMastery() / (double) Math.max(1, getMaxMastery())); }

    /** Spirit Dive makes this book's spells 20% bigger. */
    public static float size(ManasSkillInstance i, LivingEntity e) {
        float s = e.level().getGameTime() < i.getOrCreateTag().getLong("DiveUntil") ? 1.2f : 1f;
        return s * buildUpMultiplier(i, e) * (float) com.newuniverse.nusmp.firecast.FireCasting.sizeMult(e);      // fire casting: Leopold's charged spiral (1 for every other caster)
    }

    private static float buildUpMultiplier(ManasSkillInstance instance, LivingEntity entity) {
        if (entity.level().getGameTime() > instance.getOrCreateTag().getLong("MagicBuildUpUntil")) return 1f;
        return 1f + Math.max(0f, Math.min(1f, instance.getOrCreateTag().getFloat("MagicBuildUp"))) * 0.2f;
    }
    public static boolean inUnion(ManasSkillInstance i, LivingEntity e) { return inDevilState(e); }

    /** Devil Union (element books) or Devil Dive (Forbidden book) is active on this entity. */
    public static boolean inDevilState(LivingEntity e) { return e.level().getGameTime() < e.getPersistentData().getLong("nusmp_devil_until"); }

    public static void startDevilState(ServerPlayer p, int ticks) {
        p.getPersistentData().putLong("nusmp_devil_until", p.level().getGameTime() + ticks);
        p.getPersistentData().putLong("nusmp_devil_paid", p.level().getGameTime());
    }

    /** Balance-law damage through a Tensura damage source (+10% pierce during Devil Union). */
    public void hurt(ManasSkillInstance i, ServerPlayer caster, LivingEntity target, int mode, float raw) {
        hurtAs(i, caster, target, mode, raw, damageType());
    }

    /** 0.45: {@link #hurt} through a chosen Tensura damage type (Dice elements, Light), same multipliers. */
    public void hurtAs(ManasSkillInstance i, ServerPlayer caster, LivingEntity target, int mode, float raw, ResourceKey<DamageType> type) {
        if (target == caster || target.isAlliedTo(caster)) return;
        float r = raw * (float) cover(i).damage * (inUnion(i, caster) ? 1.1f : 1f)
                * buildUpMultiplier(i, caster) * (float) com.newuniverse.nusmp.firecast.FireCasting.damageMult(caster)      // fire casting: Leopold's charged spiral (1 for every other caster)
                * ArmourState.spellBoost(caster, magic)                                       // 1.99.0: a worn armour of this magic lends the spell half of its attack bonus (it never equips)
                * (float) com.newuniverse.nusmp.item.MagicGear.damageMult(caster, magic);
        // Dark Magic is Arcane Stage: strong against devils and devil-tainted mages.
        if ((magic == MagicType.DARK || magic == MagicType.SHADOW) && (inDevilState(target)
                || (target instanceof ServerPlayer tp && GrimoirePages.grimoireOf(tp).map(g -> cover(g).isForbidden()).orElse(false)))) r *= 1.25f;
        com.newuniverse.nusmp.blackclover.CopyMemory.remember(target, this, mode);
        target.hurt(createSource(i, caster, type, mode), BalanceLaw.damage(caster, target, r, masteryFrac(i)));
    }

    public static LivingEntity target(ServerPlayer p, double range) {
        Vec3 start = p.getEyePosition(), end = start.add(p.getViewVector(1f).scale(range));
        HitResult wall = p.level().clip(new net.minecraft.world.level.ClipContext(start, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, p));
        if (wall.getType() != HitResult.Type.MISS) end = wall.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p.level(), p, start, end,
                p.getBoundingBox().expandTowards(end.subtract(start)).inflate(1), e -> e instanceof LivingEntity && e.isAlive() && e != p);
        return hit == null ? null : (LivingEntity) hit.getEntity();
    }

    /** Point under the crosshair (block hit or max range). */
    public static Vec3 aim(ServerPlayer p, double range) { return p.pick(range, 1f, false).getLocation(); }

    public static List<LivingEntity> around(ServerPlayer p, Vec3 c, double r) {
        return p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r),
                e -> e != p && e.isAlive() && !e.isSpectator() && !e.isAlliedTo(p) && e.distanceToSqr(c) <= r * r);
    }

    /** Entities within 'width' of the segment a->b. */
    public static List<LivingEntity> along(ServerPlayer p, Vec3 a, Vec3 b, double width) {
        List<LivingEntity> out = new ArrayList<>();
        Vec3 ab = b.subtract(a);
        double len2 = Math.max(1e-6, ab.lengthSqr());
        for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(a, b).inflate(width + 1),
                e -> e != p && e.isAlive() && !e.isAlliedTo(p))) {
            Vec3 c = e.getBoundingBox().getCenter();
            double t = Math.max(0, Math.min(1, c.subtract(a).dot(ab) / len2));
            if (a.add(ab.scale(t)).distanceTo(c) <= width + e.getBbWidth() / 2) out.add(e);
        }
        return out;
    }

    /** Hard control gate (shared 12 s lock). */
    public static boolean control(ServerPlayer p) {
        if (BalanceLaw.beginControl(p)) return true;
        fail(p, "Your control magic needs " + (BalanceLaw.controlCooldownLeft(p) + 19) / 20 + "s.");
        return false;
    }

    // ---------------------------------------------------------------- VFX helpers
    public void vfx(ServerPlayer p, VfxShape shape, Vec3 from, Vec3 to, int ticks, float power) {
        VfxSpawn.send(p.serverLevel(), shape, com.newuniverse.nusmp.sylph.SylphCasting.origin(p, com.newuniverse.nusmp.undine.UndineCasting.origin(p, com.newuniverse.nusmp.firecast.FireCasting.origin(p, from))), to, color, ticks, power);      // fire casting: a spell's own start moves to the emitter (the ball, the fist, the Salamander's mouth)
    }

    /** Spinning magic circle under the caster in this book's color (every cast). */
    public void castCircle(ServerPlayer p, float power) {
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.MAGIC_CIRCLE, p, p.position().add(0, 1, 0), color, 24, power);
    }

    /** Circle burst on impact. */
    public void impact(ServerPlayer p, Vec3 at, float power) {
        VfxSpawn.send(p.serverLevel(), VfxShape.MAGIC_CIRCLE_EXPLOSION, at, at.add(0, 1, 0), color, 20, power);
    }

    /** Element index for the spirit effects: 0 fire, 1 water, 2 wind, 3 earth. */
    public int spiritVariant() {
        Element e = spiritElement();
        return e == Element.WATER ? 1 : e == Element.WIND ? 2 : e == Element.EARTH ? 3 : 0;
    }

    /** Sends a spec-driven spirit effect (the server reports where; the client builds the look). */
    public void spiritFx(ServerPlayer p, VfxShape shape, net.minecraft.world.phys.Vec3 from, net.minecraft.world.phys.Vec3 to, float power, boolean follow) {
        VfxSpawn.send(p.serverLevel(), new com.newuniverse.nusmp.vfx.VfxPayload(shape.ordinal(), from, to, spiritVariant(), 0, power,
                follow ? p.getId() : -1, p.getRandom().nextLong()));
    }

    // ---------------------------------------------------------------- Spirit Dive / Devil Union
    public boolean spiritGateOpen(ServerPlayer p) {
        Element el = spiritElement();
        if (el == null) return false;
        var spirit = TensuraStorages.getSpiritFrom(p);
        if (spirit == null || spirit.getSpiritLevel(el) != SpiritualMagic.SpiritLevel.LORD) return false;
        var owner = SpiritSlots.get(p.getServer()).owner(spiritName());
        return owner == null || owner.equals(p.getUUID());
    }

    /** 0.38: the transformation armour this book's Spirit Dive puts on (null = none): Sylph's wind, Salamander's fire. */
    public com.newuniverse.nusmp.blackclover.ModeArmor.Mode diveArmor() {
        if (SpiritDivePieces.LOOK) return null;                    // 1.99.0: the four dives draw Aura.SPIRIT_DIVE (client.aura.SpiritDiveAura); the older overlays below stay registered
        if (this instanceof WindBook) return com.newuniverse.nusmp.blackclover.ModeArmor.Mode.WIND_SPIRIT_DIVE;
        if (this instanceof FireBook) return com.newuniverse.nusmp.blackclover.ModeArmor.Mode.FIRE_SPIRIT_DIVE;
        if (this instanceof WaterBook) return com.newuniverse.nusmp.blackclover.ModeArmor.Mode.VALKYRIE;       // 1.99.0: Noelle's Valkyrie plates and avian water wings are Undine's dive
        return null;
    }

    /** 1.99.0: the mode number of the Spirit Dive page of this book, or -1. */
    private int diveIndex() {
        List<BookPage> l = pages();
        for (int k = 0; k < l.size(); k++) if (SpiritDiveForm.PAGE.equals(l.get(k).id())) return k;
        return -1;
    }

    private static boolean spiritDive(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        long now = p.level().getGameTime();
        if (now < i.getOrCreateTag().getLong("DiveUntil")) {       // recast ends it early, lock still applies
            i.getOrCreateTag().putLong("DiveUntil", now);
            i.setCoolDown(60, mode);                                   // seconds
            if (b.diveArmor() != null) com.newuniverse.nusmp.blackclover.ModeArmor.stop(p, b.diveArmor());
            SpiritDiveForm.end(p, b);                                  // 1.99.0: the form's stats and its look go with the dive
            return false;
        }
        if (!SpiritBond.willAnswer(p)) { fail(p, b.spiritName() + " will not answer you. Earn back its trust."); return false; }
        SpiritSlots slots = SpiritSlots.get(p.getServer());
        if (!p.getUUID().equals(slots.owner(b.spiritName())) && com.newuniverse.nusmp.relics.djinn.Bonds.refuse(p, com.newuniverse.nusmp.relics.djinn.Bonds.Kind.SPIRIT, "a spirit bond")) return false;   // 1.24.0: one bond
        if (slots.owner(b.spiritName()) == null && !com.newuniverse.nusmp.item.MagicGear.consumeSpiritCharm(p)) {
            fail(p, "You need a Spirit Charm to reach " + b.spiritName() + " the first time.");
            return false;
        }
        if (!slots.claim(b.spiritName(), p.getUUID())) {
            fail(p, b.spiritName() + " has already chosen a mage.");
            return false;
        }
        SpiritBond.bond(p, b.spiritName());
        SpiritBond.addTrust(p, 2, "spirit_dive");
        SpiritBond.heavyUse(p);
        i.getOrCreateTag().putLong("DiveUntil", now + 160);
        if (b.diveArmor() != null) com.newuniverse.nusmp.blackclover.ModeArmor.start(p, b.diveArmor(), 160);   // 0.38: the Spirit Dive armour overlay
        SpiritDiveForm.begin(p, b, i);                                // 1.99.0: the dive is a FORM (stats, ambient intake, held while the upkeep is paid; book.ArmourPolicy)
        i.setCoolDown(3, mode);                                       // seconds: long enough not to end the dive by accident; the 60 s spirit lock starts when the dive ends (recast or upkeep failure)
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.SPIRIT_AURA, p, p.position().add(0, 1, 0), b.color, 160, 1.0F);
        p.getServer().getPlayerList().broadcastSystemMessage(Component.literal(p.getName().getString() + " dives with " + b.spiritName() + "!")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.MAGIC_CIRCLE_EXPLOSION, p, p.position().add(0, 1, 0), b.color, 30, 2.0F);
        return true;
    }

    /** Spirit Channeling release: 25%+ Overdrive dash, 50%+ Spirit Nova, 100% Spirit Sovereign's Cataclysm. */
    private static boolean channel(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!SpiritBond.willAnswer(p)) { fail(p, b.spiritName() + " will not answer you."); return false; }
        int gauge = 50;
        SpiritBond.heavyUse(p);
        net.minecraft.world.phys.Vec3 eye = p.getEyePosition(), from = p.position();
        if (gauge < 50) {          // Elemental Overdrive: a dash cloaked in the element
            net.minecraft.world.phys.Vec3 dir = p.getViewVector(1f).multiply(1, 0, 1).normalize(), dest = from;
            for (double d = 8; d >= 1; d -= 0.5) {
                net.minecraft.world.phys.Vec3 c = from.add(dir.scale(d));
                if (p.level().noCollision(p, p.getBoundingBox().move(c.subtract(from)))) { dest = c; break; }
            }
            for (LivingEntity t : along(p, from.add(0, 1, 0), dest.add(0, 1, 0), 1.5)) b.hurt(i, p, t, mode, 10f);
            p.teleportTo(dest.x, dest.y, dest.z);
            b.spiritFx(p, VfxShape.FX_SPIRIT_OVERDRIVE, from, dest, 1f, false);
        } else if (gauge < 100) {  // Spirit Nova: an 8-block burst that destroys projectiles
            for (var pr : p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class, p.getBoundingBox().inflate(8)))
                if (pr.getOwner() != p) pr.discard();
            for (LivingEntity t : around(p, from, 8)) {
                b.hurt(i, p, t, mode, 14f);
                net.minecraft.world.phys.Vec3 away = t.position().subtract(from).normalize();
                t.knockback(1.8, -away.x, -away.z);
            }
            b.spiritFx(p, VfxShape.FX_SPIRIT_NOVA, from, from, 1.2f, false);
        } else {                   // Cataclysm: 6 s as a living conduit of the element
            i.getOrCreateTag().putLong("CataclysmUntil", p.level().getGameTime() + 120);
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 120, 2));
            com.newuniverse.nusmp.blackclover.TimedModifiers.apply(p, net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE,
                    ResourceLocation.fromNamespaceAndPath("nusmp", "cataclysm_kb"), 1.0, 120);
            b.spiritFx(p, VfxShape.FX_SPIRIT_CATACLYSM, from, from, 1.2f, true);
            SpellRuntime.zone(p.serverLevel(), 120, 10, age -> {
                for (LivingEntity t : around(p, p.position(), 4 * size(i, p))) b.hurt(i, p, t, mode, 6f);
            });
            p.getServer().getPlayerList().broadcastSystemMessage(Component.literal(p.getName().getString() + " becomes a living conduit of " + b.spiritName() + "!")
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
        }
        return true;
    }

    private static boolean devilUnion(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (p.hasEffect(com.newuniverse.nusmp.entity.zogratis.ZogratisEffects.PURIFIED)) { fail(p, "Lucius's Soul Purge has stripped your devil's power for now."); return false; }   // 1.20.0
        startDevilState(p, 160);
        p.getPersistentData().putString("nusmp_devil_src", "union");
        com.newuniverse.nusmp.quests.NusmpEvents.fire(p, "devil.union");
        p.getServer().getPlayerList().broadcastSystemMessage(Component.literal(p.getName().getString() + " has entered Devil Union.")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.DEVIL_CIRCLE, p, p.position().add(0, 1, 0), 0xFFB01010, 40, 2.0F);
        VfxSpawn.send(p.serverLevel(), VfxShape.ANTI_MAGIC_SLASH, p.getEyePosition(), p.getEyePosition().add(p.getViewVector(1f).scale(4)), 0xFF2A0A18, 18, 2.0F);
        return true;
    }

    @Override public boolean canTick(ManasSkillInstance instance, LivingEntity entity) { return true; }

    @Override
    public void onTick(ManasSkillInstance i, LivingEntity e) {
        if (e instanceof ServerPlayer p) second(i, p);
    }

    /**
     * 0.48: the book's once-a-second upkeep. ManasCore only ticks the skills it treats as active (a grimoire sitting in the skill list
     * may never tick, which left Cotton / Food grimoires unrolled), so our own player tick ({@link #onPlayerTick}) drives it too;
     * whichever comes first in a second runs it, the other skips.
     */
    public void second(ManasSkillInstance i, ServerPlayer p) {
        long now = p.level().getGameTime();
        var stamp = i.getOrCreateTag();
        long last = stamp.getLong("SecondAt");
        if (last <= now && now - last < 20) return;
        stamp.putLong("SecondAt", now);
        if (this instanceof LegionBook) LegionArts.updateChessboard(i, p);
        // Spirit Dive upkeep: magicule each second; ends if the Lord contract is lost.
        if (now < i.getOrCreateTag().getLong("DiveUntil") && !com.newuniverse.nusmp.relics.Severance.isSevered(p)) {      // 1.45.0: severed, the upkeep waits (nothing is drained, nothing ends)
            if (!spiritGateOpen(p) || !drain(p, EnergyHelper.getMaxMagicule(p) * 0.02)) {
                i.getOrCreateTag().putLong("DiveUntil", now);
                if (diveArmor() != null) com.newuniverse.nusmp.blackclover.ModeArmor.stop(p, diveArmor());
                SpiritDiveForm.end(p, this);
                if (diveIndex() >= 0) i.setCoolDown(60, diveIndex());     // 1.99.0: the spirit lock starts when the dive ends
                fail(p, "The spirit slips away.");
            } else {
                SpiritDiveForm.second(p, this, i, now);                    // 1.99.0: the ambient intake, and the dive held while the upkeep is paid
            }
        }
        // Devil Union: HP drain; at the price floor it ends and you're stunned 2 s.
        if (inDevilState(p) && !(this instanceof ForbiddenBook) && "union".equals(p.getPersistentData().getString("nusmp_devil_src"))) {
            if (p.getHealth() <= 6f) {
                p.getPersistentData().putLong("nusmp_devil_until", now);
                TimeStop.freeze(p, 40);
                fail(p, "The devil lets go. You collapse.");
            } else {
                p.hurt(p.damageSources().magic(), (float) com.newuniverse.nusmp.item.MagicGear.selfDamageMult(p));
            }
        }
        manaSense(p);
        tickBook(i, p);
    }

    /** Per-second hook for subclasses. */
    protected void tickBook(ManasSkillInstance i, ServerPlayer p) {}

    /** Every second: each grimoire book the player has gets its upkeep (see {@link #second}). */
    private static final com.newuniverse.nusmp.perf.PerfProbe.Section PROBE_S_BOOK_GRIMOIRE = com.newuniverse.nusmp.perf.PerfProbe.section("s.book.grimoire");   // 1.99.0: performance probe (off by default: one flag read)

    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) {
        long t0 = com.newuniverse.nusmp.perf.PerfProbe.begin();
        try {
            onPlayerTick0(e);
        } finally {
            com.newuniverse.nusmp.perf.PerfProbe.end(PROBE_S_BOOK_GRIMOIRE, t0);
        }
    }

    private static void onPlayerTick0(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        var skills = io.github.manasmods.manascore.skill.api.SkillAPI.getSkillsFrom(p);
        if (p.tickCount % 10 == 3) {
            skills.getSkill(com.newuniverse.nusmp.skill.NUSkills.BOOK_LEGION.getId())
                    .ifPresent(i -> LegionArts.updateChessboard(i, p));
        }
        if (p.tickCount % 20 != 3) return;
        for (var h : com.newuniverse.nusmp.skill.NUSkills.BOOKS)
            skills.getSkill(h.getId()).ifPresent(i -> { if (i.getSkill() instanceof GrimoireBook b) b.second(i, p); });
    }

    private static boolean drain(ServerPlayer p, double amount) {
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null || ex.getMagicule() < amount) return false;
        ex.setMagicule(ex.getMagicule() - amount);
        ex.markDirty();
        return true;
    }

    // ---------------------------------------------------------------- mana (Black Clover rules on Tensura magicule)
    /** Heart Kingdom Magic Stages: Stage 9 (new) to Stage 0 (mastered). Anti-Magic and devils are Arcane Stage. */
    public String stage(ManasSkillInstance i) {
        if (magic == MagicType.ANTI_MAGIC || this instanceof ForbiddenBook) return "Arcane Stage";
        return "Stage " + (9 - (int) Math.round(masteryFrac(i) * 9));
    }

    /** Spending your mana to nearly nothing exhausts you (Black Clover: empty mana = collapse). */
    private static void exhaustion(ServerPlayer p) { com.newuniverse.nusmp.mana.ManaLaw.exhaust(p); }   // 1.5.0: mana is life force (mana.ManaLaw)

    /** Mana Skin: while you hold at least half your magicule, incoming damage is reduced 10%. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance i, LivingEntity owner, net.minecraft.world.damagesource.DamageSource source,
                                 io.github.manasmods.manascore.network.api.util.Changeable<Float> amount) {
        var ex = TensuraStorages.getExistenceFrom(owner);
        if (ex != null && ex.getMagicule() >= EnergyHelper.getMaxMagicule(owner) * 0.5) amount.set(amount.get() * 0.9f);
        if (owner.getPersistentData().getLong("nusmp_misfortune_until") > owner.level().getGameTime()) amount.set(amount.get() * 1.1f);
        if (owner.level().getGameTime() < i.getOrCreateTag().getLong("CataclysmUntil")) {   // living conduit: a spiritual body
            if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL) || source.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING)
                    || source.is(net.minecraft.tags.DamageTypeTags.IS_FREEZING) || source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) amount.set(0f);
            else if (!com.newuniverse.nusmp.antimagic.AntiMagic.isMagic(source)) amount.set(amount.get() * 0.7f);
        }
        return true;
    }

    /** Five-sided diamond: a fortune ward negates one lethal blow per day. */
    @Override
    public boolean onDeath(ManasSkillInstance i, LivingEntity owner, net.minecraft.world.damagesource.DamageSource source) {
        if (!(owner instanceof ServerPlayer p) || cover(i) != GrimoireCover.FIVE_SIDED) return true;
        if (source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)) return true;
        long now = p.level().getGameTime();
        if (now < p.getPersistentData().getLong("nusmp_fortune_ready")) return true;
        p.getPersistentData().putLong("nusmp_fortune_ready", now + 24000);
        p.setHealth(8f);
        p.displayClientMessage(Component.literal("Good Fortune turns the blow aside.").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
        impact(p, p.position().add(0, 1, 0), 1f);
        return false;
    }

    /** Wind-derived mages (wind, lightning, storm, mist) sense mana: hostiles hunting you glow. */
    protected void manaSense(ServerPlayer p) {
        if (p.tickCount % 100 != 0) return;
        if (magic != MagicType.WIND && magic != MagicType.LIGHTNING && magic != MagicType.STORM && magic != MagicType.MIST) return;
        for (net.minecraft.world.entity.Mob m : p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, p.getBoundingBox().inflate(16))) {
            if (m.getTarget() == p) m.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, 40, 0, true, false));
        }
    }

    /** Granting this skill (acceptance, admin or /tensura) always hands over the matching grimoire. */
    @Override
    public void onLearnSkill(ManasSkillInstance instance, LivingEntity entity) {
        super.onLearnSkill(instance, entity);
        if (entity instanceof ServerPlayer p && givesGrimoireItem()) GrimoireKeeper.ensureGrimoire(p, this, instance);
    }

    // ---------------------------------------------------------------- misc
    /**
     * Casting needs your grimoire of this book's magic summoned (0.24; the Forbidden book accepts any of yours). Holding it, or having
     * it on the hotbar, is not enough. The Anti-Magic exception: the Anti-Magic Lord's Black Form toggle and the demon swords work
     * without a summoned book (they are the swords drawn from it), but the Anti-Magic book's pages still need it out.
     */
    protected boolean holdingBook(ServerPlayer p) {
        return GrimoireSummon.isFloating(p, this instanceof ForbiddenBook ? null : magic);
    }

    public static void fail(ServerPlayer p, String msg) {
        p.displayClientMessage(Component.literal(msg).withStyle(ChatFormatting.RED), true);
        com.newuniverse.nusmp.anim.CastAnim.play(p, com.newuniverse.nusmp.anim.CastAnim.FAIL);                 // 0.54: the fizzle flinch
    }

    private void shout(ServerPlayer p, String incantation) {
        String label = com.newuniverse.nusmp.grimoire.CanonNaming.named(CanonSkillSync.canonOf(p, magic.name()), magicLabel());     // the character's name in front of the magic
        Component line = Component.literal(label + ": " + incantation + "!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        for (ServerPlayer other : p.serverLevel().players()) if (other.distanceToSqr(p) < 32 * 32) other.displayClientMessage(line, true);
    }

    public static ServerLevel level(ServerPlayer p) { return p.serverLevel(); }
}
