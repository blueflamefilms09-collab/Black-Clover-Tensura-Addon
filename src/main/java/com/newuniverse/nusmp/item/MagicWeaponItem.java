package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Black Clover swords. Right-click uses the sword's technique (vanilla item cooldown).
 * Demon swords are bound to the Anti-Magic user who drew them from their grimoire.
 */
public class MagicWeaponItem extends SwordItem {
    public enum Kind {
        // 0.28: abilities renamed and rebuilt after the Black Clover wiki (ids kept so old worlds load)
        DEMON_SLASHER_KATANA(Tiers.DIAMOND, 3, -2.2f, "Infinite Slash", "A long flying anti-magic slash from overhead: cuts every spell in a 20-block line and strips a buff from all it hits.", 160, true),
        MIASMA_KATANA(Tiers.DIAMOND, 3, -2.3f, "Dark Cloaked Dimension Slash", "A 16-block cut of darkness that cuts spells and space first. Hits are left in darkness.", 200, false),
        SPELL_FORGED_RAPIER(Tiers.DIAMOND, 1, -1.8f, "Spatial Edge", "Blink 6 blocks forward, piercing everything on the way.", 120, false),
        SEVERING_GREATSWORD(Tiers.NETHERITE, 5, -3.2f, "Sword Rain", "Eight blades fall on the spot you aim at.", 300, false),
        DEMON_SLAYER(Tiers.NETHERITE, 6, -3.0f, "Black Divider", "Bats spells back with the flat of the blade, then a huge anti-magic sweep that strips magic from everything hit.", 160, true),
        DEMON_DWELLER(Tiers.NETHERITE, 3, -2.2f, "Black Slash", "A flying anti-magic slash that cuts spells out of its path and knocks its target back.", 120, true),
        DEMON_DESTROYER(Tiers.NETHERITE, 4, -2.6f, "Causality Break", "Undoes spell effects: cleanses you and allies nearby, erases spells around you and strips every buff from foes close by.", 300, true),
        LICHT_DWELLER(Tiers.NETHERITE, 4, -2.4f, "Conquering Eon", "Licht's white Demon-Dweller: a 20-block slash that grows with every ally near you, and heals you.", 240, false),
        LICHT_DESTROYER(Tiers.NETHERITE, 5, -2.6f, "Causality Break", "Licht's white Demon-Destroyer: strips every buff from foes in front of you and erases their spells.", 240, false),
        // 0.32: a cryo-lattice mana runeblade (docs/runeblade_spec.md)
        RIMEHEART(Tiers.NETHERITE, 5, -2.4f, "Glacial Matrix Burst", "Vents the blade's stored mana as a frost shockwave: erases spells within 5 blocks, freezes and slows everything caught. Critical hits shatter a smaller burst around the target; paired with a second magic sword the burst recharges 40% faster.", 200, false),
        // 0.52: Zagred's quill-blade (docs/zagred_boss_gdd.md 3.1)
        LAST_WORD(Tiers.NETHERITE, 5, -1.8f, "Sentence", "Writes a short sentence of glyph letters along your line of sight that detonates in reading order. Every hit leaves a struck-out mark: three marks within 5 s strip a buff and one layer of a barrier (20 s cooldown).", 400, false),
        ELSDOCIA(Tiers.NETHERITE, 6, -2.8f, "Legacy Release", "Absorbs magic from struck foes, stores the charge, and releases it as a broad arcane rift. The stored souls lend their strength; Key Magic opens the rift farther.", 360, false),
        // 0.100: the weapons of the "Custom weapon i want added" reference sheets (logic in ReferenceWeapons)
        QUICKLINGS_IVY(Tiers.DIAMOND, 2, -1.6f, "Ensnaring Ivy", "Ivy lashes out 12 blocks: everything it catches is rooted and poisoned. Critical hits ensnare for free.", 160, false),
        MAGEBANE_LONGSWORD(Tiers.NETHERITE, 4, -2.4f, "Dispel Surge", "A turquoise surge strips a buff from foes ahead, jams their casting, erases spells around you and hardens you for 5 s.", 200, false),
        PRISM_BLADE(Tiers.NETHERITE, 4, -2.4f, "Blinding Flash", "The blade flares through the whole spectrum: every foe within 10 blocks is burned, blinded and lit up.", 240, false),
        THUNDERCLEAVE(Tiers.NETHERITE, 7, -3.2f, "Thunder Arc", "A thunderous arc 8 blocks wide hurls foes back, slows and deafens them. Every hit arcs to a second foe.", 260, false),
        BLOODBOUND_KATANA(Tiers.NETHERITE, 5, -2.1f, "Bloodflare", "Hits store Blood Charges (max 10) that bite on every blow. Bloodflare spends them all in a 14-block crimson slash and heals you.", 100, false),
        PERMAFROST_SABRE(Tiers.NETHERITE, 4, -2.0f, "Whirlwind Frost", "A freezing spin hits everything within 5 blocks. Held with the Cyclone Rapier in the other hand: Boreal Fusion, 8 blocks and lifting.", 180, false),
        CYCLONE_RAPIER(Tiers.DIAMOND, 2, -1.6f, "Cyclone Thrust", "Spin through up to 5 blocks, flinging foes into the air. With the Permafrost Sabre in the other hand: Boreal Fusion.", 160, false),
        // the two crossbows of the "Magic Item Compendium I" sheet (logic in BoltArts, reached through ReferenceWeapons)
        BOLT_THROWER(Tiers.DIAMOND, 1, -2.0f, "Force Bolt", "Looses a bolt of pure force up to 48 blocks: no ammunition, and armour does not turn it. Every bolt leaves a Thrower's Mark on the foe.", 24, false),
        CROSSBOW_OF_SWIFT_DEATH(Tiers.DIAMOND, 2, -2.2f, "Notched Bolt", "Looses a bolt that may crit, likelier the more wounded the foe. A crit is Swift Death: it executes a foe under 15% health and frightens the rest.", 40, false),
        // the Club of Rot and the Doom Hammer of the same sheet (logic in RotDoomArts, reached through ReferenceWeapons)
        CLUB_OF_ROT(Tiers.DIAMOND, 6, -3.0f, "Putrid Slam", "A green shockwave rots everything within 5 blocks (two Rot stacks) and withers the plants and crops under it. Every hit inflicts Rot: Poison, Weakness and a rotting maximum health, spreading to nearby foes.", 180, false),
        DOOM_HAMMER(Tiers.NETHERITE, 8, -3.3f, "Doom Slam", "Leap and slam down: a ring of cracks and a shockwave 6 blocks wide throws foes up and stuns them briefly (bosses are only staggered). Used in mid-air it dives instead, and the longer the fall the harder it lands.", 240, false),
        // the Magebane Shield and the Weapon of Resurgence (logic in MagebaneShieldArts / ResurgenceArts, reached through ReferenceWeapons)
        MAGEBANE_SHIELD(Tiers.IRON, 0, -2.4f, "Spellbreak Parry", "Raises the shield for 2 s: projectiles are turned back at their caster, spell bolts are erased and returned, and magic or missile damage is cancelled and thrown back. Held in either hand, magic damage you take is cut by a third.", 360, false),
        WEAPON_OF_RESURGENCE(Tiers.DIAMOND, 3, -2.4f, "Resurgent Smite", "A radiant burst 7 blocks wide mends you and your allies (more the fuller your Lay on Hands pool) and cleanses an affliction; undead and demons are burned, everyone else is dazzled. Every hit mends you and a wounded ally.", 220, false),
        // the Brooch Pin and the Hand of Providence (logic in BroochPinArts / HandOfProvidenceArts, reached through ReferenceWeapons; the first stopgap, PinProvidenceArts, is kept but no longer wired)
        BROOCH_PIN(Tiers.DIAMOND, 0, -0.8f, "Piercing Pin", "A quick lunge of up to 7 blocks: every foe on the line is run through (armour does not stop the pin) and slowed, and the first is struck from behind as you land past it.", 100, false),
        HAND_OF_PROVIDENCE(Tiers.NETHERITE, 2, -1.8f, "Laying On of Hands", "Lays the gauntlet on the ally you are looking at within 6 blocks, or on yourself: a big mend, Regeneration, and every curse and poison cleansed.", 200, false);

        final Tier tier; final int damage; final float speed; final String ability, desc; final int cooldown; final boolean demon;
        Kind(Tier t, int d, float s, String a, String desc, int cd, boolean demon) {
            tier = t; damage = d; speed = s; ability = a; this.desc = desc; cooldown = cd; this.demon = demon;
        }
    }

    public final Kind kind;

    public MagicWeaponItem(Kind kind) {
        super(kind.tier, new Item.Properties().attributes(attributes(kind))
                .rarity(kind.demon || kind == Kind.RIMEHEART || kind == Kind.ELSDOCIA || kind == Kind.PRISM_BLADE || kind == Kind.THUNDERCLEAVE || kind == Kind.BLOODBOUND_KATANA ? Rarity.EPIC : Rarity.RARE).fireResistant());
        this.kind = kind;
    }

    /** 0.48: for a sword with its own item properties (the Genesis Demon-Slayer is unbreakable); the kind's attributes are added. */
    protected MagicWeaponItem(Kind kind, Item.Properties properties) {
        super(kind.tier, properties.attributes(attributes(kind)));
        this.kind = kind;
    }

    private static final net.minecraft.resources.ResourceLocation RIMEHEART_STANCE =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("nusmp", "rimeheart_stance");

    /** Sword attributes; the Rimeheart also roots its wielder a little (knockback resistance while held). */
    private static net.minecraft.world.item.component.ItemAttributeModifiers attributes(Kind kind) {
        var a = SwordItem.createAttributes(kind.tier, kind.damage, kind.speed);
        if (kind == Kind.RIMEHEART)
            a = a.withModifierAdded(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(RIMEHEART_STANCE, 0.25,
                            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND);
        return a;
    }

    /** Dual-wield synergy: another magic sword in the off hand. */
    static boolean pairedBlade(Player p) { return p.getOffhandItem().getItem() instanceof MagicWeaponItem; }

    int cooldownFor(Player p) {
        int base = kind == Kind.RIMEHEART && pairedBlade(p) ? Math.round(kind.cooldown * 0.6f) : kind.cooldown;
        return Math.max(1, (int) Math.round(base * com.newuniverse.nusmp.NUGameRules.spellCooldown(p.level())));   // 0.48: 40% shorter by default
    }

    /** A burst of frost mana: freezes, slows and hurts everything within r of c (the wielder and allies excepted). */
    private void frostBurst(ServerPlayer p, Vec3 c, double r, float dmg, LivingEntity skip) {
        for (LivingEntity t : GrimoireBook.around(p, c, r)) {
            if (t == skip) continue;
            hit(p, t, dmg);
            t.setTicksFrozen(Math.min(t.getTicksRequiredToFreeze() + 100, t.getTicksFrozen() + 120));
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
        }
    }

    // ---------------------------------------------------------------- binding (demon swords)
    public static ItemStack bound(Item item, Player owner) {
        ItemStack s = new ItemStack(item);
        CompoundTag t = new CompoundTag();
        t.putUUID("Owner", owner.getUUID());
        t.putString("OwnerName", owner.getName().getString());
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        return s;
    }

    /** A sword drawn from a grimoire (Sword Magic): bound to its caster and gone after {@code ticks}. */
    public static ItemStack summoned(Item item, Player owner, int ticks) {
        ItemStack s = bound(item, owner);
        CompoundTag t = s.get(DataComponents.CUSTOM_DATA).copyTag();
        t.putLong("ExpiresAt", owner.level().getGameTime() + ticks);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        return s;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity holder, int slot, boolean selected) {
        if (level.isClientSide || level.getGameTime() % 20 != 0) return;
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        if (d == null) return;
        CompoundTag t = d.copyTag();
        if (t.contains("ExpiresAt") && level.getGameTime() >= t.getLong("ExpiresAt")) stack.setCount(0);   // the summoned sword fades
    }

    public static boolean usableBy(ItemStack s, Player p) {
        if (!(s.getItem() instanceof MagicWeaponItem w)) return true;
        CustomData d0 = s.get(DataComponents.CUSTOM_DATA);
        if (!w.kind.demon && (d0 == null || !d0.copyTag().contains("ExpiresAt"))) return true;
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        if (d == null || !d.copyTag().hasUUID("Owner")) return true;
        UUID o = d.copyTag().getUUID("Owner");
        return o.equals(p.getUUID());
    }

    // ---------------------------------------------------------------- technique
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return InteractionResultHolder.pass(stack);
        if (!usableBy(stack, p)) { GrimoireBook.fail(p, "This sword answers only to its owner."); return InteractionResultHolder.fail(stack); }
        if (p.isShiftKeyDown() && WeaponArts.alt(kind) != null) {                    // 0.52: the second technique (sneak + right-click)
            WeaponArts.tryAlt(p, kind);
            return InteractionResultHolder.success(stack);
        }
        if (!technique(p, stack)) return InteractionResultHolder.fail(stack);
        p.getCooldowns().addCooldown(this, cooldownFor(p));
        p.displayClientMessage(Component.literal(kind.ability + "!").withStyle(kind.demon ? ChatFormatting.DARK_GRAY : ChatFormatting.AQUA, ChatFormatting.BOLD), true);
        return InteractionResultHolder.success(stack);
    }

    protected void hit(ServerPlayer p, LivingEntity t, float raw) {
        if (t == p || t.isAlliedTo(p)) return;
        t.hurt(p.damageSources().playerAttack(p), BalanceLaw.damage(p, t, raw, 0.6));
    }

    protected static void stripOne(LivingEntity t) {
        for (MobEffectInstance e : new ArrayList<>(t.getActiveEffects())) if (e.getEffect().value().isBeneficial()) { t.removeEffect(e.getEffect()); return; }
    }

    protected static void eraseProjectiles(ServerPlayer p, AABB box) {
        for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, box)) if (pr.getOwner() != p) pr.discard();
    }

    private boolean technique(ServerPlayer p, ItemStack stack) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        switch (kind) {
            case DEMON_SLASHER_KATANA -> {
                Vec3 end = eye.add(look.scale(20));
                eraseProjectiles(p, new AABB(eye, end).inflate(1.6));
                for (LivingEntity t : GrimoireBook.along(p, eye, end, 1.6)) {
                    hit(p, t, 11);
                    stripOne(t);
                    var ex = TensuraStorages.getExistenceFrom(t);
                    if (ex != null) { ex.setMagicule(Math.max(0, ex.getMagicule() - EnergyHelper.getMaxMagicule(t) * 0.05)); ex.markDirty(); }
                }
                VfxSpawn.send(p.serverLevel(), VfxShape.ANTI_MAGIC_SLASH, eye, end, 0xFF2A0A30, 16, 1.6f);
            }
            case MIASMA_KATANA -> {
                Vec3 end = eye.add(look.scale(16));
                eraseProjectiles(p, new AABB(eye, end).inflate(1.2));
                for (LivingEntity t : GrimoireBook.along(p, eye, end, 1.2)) { hit(p, t, 12); t.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0)); }
                VfxSpawn.send(p.serverLevel(), VfxShape.WIND_SLASH, eye, end, 0xFF1A0F2A, 16, 1.6f);
                VfxSpawn.send(p.serverLevel(), VfxShape.SPATIAL_RIFT, end, end.add(0, 1, 0), 0xFF5B3A8A, 16, 0.8f);
            }
            case SPELL_FORGED_RAPIER -> {
                Vec3 from = p.position(), dir = look.multiply(1, 0, 1).normalize(), dest = null;
                for (double d = 6; d >= 1; d -= 0.5) {
                    Vec3 c = from.add(dir.scale(d));
                    if (p.level().noCollision(p, p.getBoundingBox().move(c.subtract(from)))) { dest = c; break; }
                }
                if (dest == null) { GrimoireBook.fail(p, "No room to move."); return false; }
                for (LivingEntity t : GrimoireBook.along(p, from.add(0, 1, 0), dest.add(0, 1, 0), 1.2)) hit(p, t, 8);
                p.teleportTo(dest.x, dest.y, dest.z);
                VfxSpawn.send(p.serverLevel(), VfxShape.SPATIAL_RIFT, from, from.add(0, 1, 0), 0xFFB088FF, 12, 0.6f);
                VfxSpawn.send(p.serverLevel(), VfxShape.THREAD_LINE, from.add(0, 1, 0), dest.add(0, 1, 0), 0xFFE0E8FF, 10, 1f);
            }
            case SEVERING_GREATSWORD -> {
                Vec3 c = p.pick(20, 1f, false).getLocation();
                var rnd = p.getRandom();
                for (int k = 0; k < 8; k++) {
                    Vec3 at = c.add(rnd.nextGaussian() * 1.6, 0, rnd.nextGaussian() * 1.6);
                    SpellRuntime.later(p.serverLevel(), 4 + k * 3, () -> {
                        VfxSpawn.send(p.serverLevel(), VfxShape.LIGHTNING_SPEAR, at.add(0, 10, 0), at, 0xFFD8E2FF, 8, 0.8f);
                        for (LivingEntity t : GrimoireBook.around(p, at, 1.5)) hit(p, t, 5);
                    });
                }
                VfxSpawn.send(p.serverLevel(), VfxShape.WEAPON_CONSTRUCTS, c, c.add(0, 1, 0), 0xFFD8E2FF, 40, 1.2f);
            }
            case DEMON_SLAYER -> {
                for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(6))) {
                    if (pr.getOwner() == p) continue;
                    pr.setDeltaMovement(pr.getDeltaMovement().scale(-1.2));   // the flat of the blade bats spells back
                    pr.setOwner(p);
                    pr.hurtMarked = true;
                }
                for (LivingEntity t : GrimoireBook.around(p, p.position(), 6)) {
                    if (t.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) < 0.3) continue;
                    hit(p, t, 13);
                    stripOne(t);
                }
                VfxSpawn.send(p.serverLevel(), VfxShape.ANTI_MAGIC_SLASH, eye, eye.add(look.scale(6)), 0xFF2A0A30, 18, 2.0f);
            }
            case DEMON_DWELLER -> {
                Vec3 end = eye.add(look.scale(16));
                eraseProjectiles(p, new AABB(eye, end).inflate(1.2));
                CustomData storedData = stack.get(DataComponents.CUSTOM_DATA);
                int stored = storedData == null ? 0 : Math.min(5, storedData.copyTag().getInt("DwellerCharge"));
                if (stored > 0) CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putInt("DwellerCharge", 0));
                SpellRuntime.bolt(p, eye, look.scale(1.8), 1.0, 9, true, null, (b, t) -> {
                    hit(p, t, 10 + stored * 2);
                    stripOne(t);
                    t.knockback(1.0 + stored * 0.1, p.getX() - t.getX(), p.getZ() - t.getZ());
                }, null);
                VfxSpawn.send(p.serverLevel(), VfxShape.ANTI_MAGIC_SLASH, eye, end, 0xFF2A0A30, 16, 1.4f);
            }
            case DEMON_DESTROYER -> {
                List<LivingEntity> cleanse = new ArrayList<>(p.serverLevel().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(4), x -> x == p || x.isAlliedTo(p)));
                for (LivingEntity t : cleanse) {
                    for (MobEffectInstance e : new ArrayList<>(t.getActiveEffects())) if (!e.getEffect().value().isBeneficial()) t.removeEffect(e.getEffect());
                    t.getPersistentData().putLong("nusmp_misfortune_until", 0);
                    t.getPersistentData().putLong("nusmp_sealed_until", 0);
                }
                eraseProjectiles(p, p.getBoundingBox().inflate(5));
                for (LivingEntity t : GrimoireBook.around(p, p.position(), 3.5)) { for (int k = 0; k < 4; k++) stripOne(t); hit(p, t, 8); }
                VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.ANTI_MAGIC_SLASH, p, p.position().add(0, 1, 0).add(look), 0xFF2A0A30, 18, 1.2f);
            }
            case LICHT_DWELLER -> {
                int allies = 0;
                for (Player o : p.serverLevel().players()) if (o != p && o.distanceToSqr(p) < 16 * 16 && o.isAlliedTo(p)) allies++;
                Vec3 end = eye.add(look.scale(20));
                eraseProjectiles(p, new AABB(eye, end).inflate(2));
                for (LivingEntity t : GrimoireBook.along(p, eye, end, 2)) hit(p, t, 12 + 3 * Math.min(5, allies));
                BalanceLaw.heal(p, 4 + Math.min(5, allies));
                VfxSpawn.send(p.serverLevel(), VfxShape.WIND_SLASH, eye, end, 0xFFF4F8FF, 18, 2.2f);
            }
            case RIMEHEART -> {
                eraseProjectiles(p, p.getBoundingBox().inflate(5));
                frostBurst(p, p.position(), 5, 10, null);
                for (LivingEntity t : GrimoireBook.around(p, p.position(), 5)) {
                    Vec3 away = t.position().subtract(p.position()).normalize();
                    t.knockback(0.9, -away.x, -away.z);
                }
                VfxSpawn.send(p.serverLevel(), VfxShape.WATER_BURST, p.position().add(0, 0.6, 0), p.position(), 0xFF8FE9FF, 22, 1.7f);
                VfxSpawn.send(p.serverLevel(), VfxShape.WATER_RING, p.position().add(0, 0.1, 0), p.position().add(0, 1, 0), 0xFF3FE9FF, 20, 1.6f);
            }
            case LAST_WORD -> {
                java.util.List<Vec3> pts = com.newuniverse.nusmp.entity.ZagredAttacks.plan(p, 4);
                com.newuniverse.nusmp.entity.ZagredAttacks.telegraph(p, pts);
                com.newuniverse.nusmp.entity.ZagredAttacks.redact(p, 1.9f, pts, 12);
            }
            case ELSDOCIA -> {
                CustomData data = stack.get(DataComponents.CUSTOM_DATA);
                int charge = data == null ? 0 : Mth.clamp(data.copyTag().getInt("ElsdociaCharge"), 0, 1000);
                if (charge <= 0) {
                    GrimoireBook.fail(p, "Elsdocia has not absorbed enough magic to release.");
                    return false;
                }
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("ElsdociaCharge", 0));
                boolean keyed = com.newuniverse.nusmp.book.GrimoireSummon.isFloating(p, com.newuniverse.nusmp.blackclover.MagicType.KEY);
                Vec3 end = eye.add(look.scale(keyed ? 28 : 22));
                eraseProjectiles(p, new AABB(eye, end).inflate(1.5 + charge / 700f));
                float raw = 8f + charge * 0.024f;
                for (LivingEntity target : GrimoireBook.along(p, eye, end, 1.25 + charge / 800f)) {
                    hit(p, target, raw * (keyed ? 1.35f : 1f));
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0));
                }
                BalanceLaw.heal(p, 2f + charge * 0.006f);
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0));
                VfxSpawn.send(p.serverLevel(), VfxShape.SPATIAL_RIFT, eye, end, 0xFF82DFFF, 20, 1.1f + charge / 550f);
                if (keyed) VfxSpawn.send(p.serverLevel(), VfxShape.SPATIAL_RIFT, end, end.add(0, 1.2, 0), 0xFFFFD66E, 26, 1.35f);
            }
            case LICHT_DESTROYER -> {
                eraseProjectiles(p, p.getBoundingBox().inflate(5));
                for (LivingEntity t : GrimoireBook.around(p, p.position(), 5)) {
                    if (t.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) < 0.4) continue;
                    hit(p, t, 10);
                    for (MobEffectInstance e : new ArrayList<>(t.getActiveEffects())) if (e.getEffect().value().isBeneficial()) t.removeEffect(e.getEffect());
                }
                VfxSpawn.send(p.serverLevel(), VfxShape.WIND_SLASH, eye, eye.add(look.scale(5)), 0xFFF4F8FF, 16, 1.6f);
            }
            default -> { if (!ReferenceWeapons.technique(p, kind, stack)) return false; }          // 0.100
        }
        return true;
    }

    // ---------------------------------------------------------------- Last Word's marks
    /** A struck-out mark on the target; the third within 5 s strips a buff and one barrier layer (20 s cooldown). */
    private static void redactMark(ServerPlayer p, LivingEntity target) {
        long now = p.level().getGameTime();
        long[] old = target.getPersistentData().getLongArray("nusmp_redact_marks");
        long[] marks = new long[old.length + 1];
        int n = 0;
        for (long m : old) if (now - m <= 100) marks[n++] = m;
        marks[n++] = now;
        VfxSpawn.send(p.serverLevel(), VfxShape.KOTO_SHATTER, target.getBoundingBox().getCenter(), target.position(), 0xFFD02040, 10, 0.5f);
        if (n >= 3 && now >= p.getPersistentData().getLong("nusmp_redact_cd")) {
            p.getPersistentData().putLong("nusmp_redact_cd", now + 400);
            stripOne(target);
            if (target instanceof com.newuniverse.nusmp.entity.ZagredBossEntity z) z.breakBarrierLayer();
            VfxSpawn.send(p.serverLevel(), VfxShape.KOTO_SHATTER, target.getBoundingBox().getCenter(), target.position(), 0xFFFFFFFF, 20, 1.6f);
            p.displayClientMessage(Component.literal("Struck out!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
            n = 0;
        }
        target.getPersistentData().putLongArray("nusmp_redact_marks", java.util.Arrays.copyOf(marks, n));
    }

    // ---------------------------------------------------------------- on hit
    /** 0.52: remembers how charged the swing was, for the full-swing strike (see {@link WeaponArts}). */
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, net.minecraft.world.entity.Entity target) {
        WeaponArts.swing(player);
        return false;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean r = super.hurtEnemy(stack, target, attacker);
        if (attacker instanceof ServerPlayer p && usableBy(stack, p)) {
            switch (kind) {
                case MIASMA_KATANA -> target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
                case DEMON_SLAYER, DEMON_DWELLER, DEMON_SLASHER_KATANA -> { if (AntiMagic.isUser(p)) stripOne(target); }
                case DEMON_DESTROYER -> { if (AntiMagic.isUser(p)) for (int k = 0; k < 3; k++) stripOne(target); }
                case LICHT_DESTROYER -> { for (int k = 0; k < 3; k++) stripOne(target); }
                case RIMEHEART -> {
                    target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 100, target.getTicksFrozen() + (pairedBlade(p) ? 80 : 50)));
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
                    if (p.fallDistance > 0 && !p.onGround() && !p.isInWater()) {        // a critical hit shatters a mana burst
                        frostBurst(p, target.position(), 3, 5, target);
                        VfxSpawn.send(p.serverLevel(), VfxShape.WATER_BURST, target.position().add(0, target.getBbHeight() / 2, 0), target.position(), 0xFF8FE9FF, 16, 0.9f);
                    }
                }
                case LAST_WORD -> redactMark(p, target);
                default -> {}
            }
            if (ReferenceWeapons.isReference(kind)) ReferenceWeapons.onHit(p, stack, target, kind);   // 0.100
            if (kind == Kind.ELSDOCIA) absorbElsdocia(stack, target);
            if (kind == Kind.DEMON_DWELLER && AntiMagic.isUser(p)) absorbDweller(stack, target, p);
            WeaponArts.fullSwing(p, target, kind);                                 // 0.52: the weapon's signature strike on a full swing
            if (kind.demon && AntiMagic.isUser(p)) AntiMagic.addAmp(p, 10);
        }
        return r;
    }

    private static void absorbDweller(ItemStack stack, LivingEntity target, ServerPlayer wielder) {
        CustomData currentData = stack.get(DataComponents.CUSTOM_DATA);
        if (currentData != null && currentData.copyTag().getInt("DwellerCharge") >= 5) return;
        var existence = TensuraStorages.getExistenceFrom(target);
        if (existence == null) return;
        double max = EnergyHelper.getMaxMagicule(target), current = existence.getMagicule();
        if (max <= 0 || current <= 0) return;
        double absorbed = Math.min(current, Math.max(1, max * 0.015));
        existence.setMagicule(current - absorbed);
        existence.markDirty();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putInt("DwellerCharge", Math.min(5, t.getInt("DwellerCharge") + 1)));
        AntiMagic.addAmp(wielder, Math.max(1, (int) (absorbed / Math.max(1, max) * 100)));
    }

    private static void absorbElsdocia(ItemStack stack, LivingEntity target) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.copyTag().getInt("ElsdociaCharge") >= 1000) return;
        var existence = TensuraStorages.getExistenceFrom(target);
        if (existence == null) return;
        double max = EnergyHelper.getMaxMagicule(target), current = existence.getMagicule();
        if (max <= 0 || current <= 0) return;
        double absorbed = Math.min(current, Math.max(1, max * 0.02));
        existence.setMagicule(current - absorbed);
        existence.markDirty();
        int gained = Math.max(1, (int) Math.ceil(absorbed / max * 1000));
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putInt("ElsdociaCharge", Math.min(1000, tag.getInt("ElsdociaCharge") + gained)));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        int firstLine = tip.size();
        tip.add(Component.literal("Right-click: " + kind.ability).withStyle(ChatFormatting.GOLD));
        tip.add(Component.literal("  " + kind.desc).withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("  Cooldown: " + Math.round(kind.cooldown * com.newuniverse.nusmp.NUGameRules.cooldownShown() / 20) + " s").withStyle(ChatFormatting.DARK_GRAY));
        WeaponArts.Alt alt = WeaponArts.alt(kind);
        if (alt != null) {                                                          // 0.52
            tip.add(Component.literal("Sneak + right-click: " + alt.name()).withStyle(ChatFormatting.GOLD));
            tip.add(Component.literal("  " + alt.desc()).withStyle(ChatFormatting.GRAY));
            tip.add(Component.literal("  Cooldown: " + Math.round(alt.cooldown() * com.newuniverse.nusmp.NUGameRules.cooldownShown() / 20) + " s").withStyle(ChatFormatting.DARK_GRAY));
            tip.add(Component.literal("Full swing: the weapon's signature strike lands too").withStyle(ChatFormatting.DARK_AQUA));
        }
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        if (kind == Kind.BLOODBOUND_KATANA) tip.add(Component.literal("  Blood Charges: " + ReferenceWeapons.blood(stack) + "/" + ReferenceWeapons.BLOOD_MAX).withStyle(ChatFormatting.DARK_RED));
        if (kind == Kind.PERMAFROST_SABRE || kind == Kind.CYCLONE_RAPIER) tip.add(Component.literal("  The Boreal Blades: hold both, one in each hand, for Boreal Fusion").withStyle(ChatFormatting.AQUA));
        if (BoltArts.isBolt(kind)) BoltArts.tooltip(stack, kind, tip);                  // the two crossbows
        if (RotDoomArts.isMine(kind)) RotDoomArts.tooltip(stack, kind, tip);            // the Club of Rot and the Doom Hammer
        if (kind == Kind.MAGEBANE_SHIELD) MagebaneShieldArts.tooltip(tip);               // the Magebane Shield
        if (kind == Kind.WEAPON_OF_RESURGENCE) ResurgenceArts.tooltip(stack, ctx, tip);  // the Weapon of Resurgence
        if (BroochPinArts.isMine(kind)) BroochPinArts.tooltip(stack, tip);               // the Brooch Pin
        if (HandOfProvidenceArts.isMine(kind)) HandOfProvidenceArts.tooltip(stack, tip); // the Hand of Providence
        if (kind == Kind.RIMEHEART) tip.add(Component.literal("  Off-hand magic sword: burst recharges 40% faster, hits freeze longer").withStyle(ChatFormatting.AQUA));
        if (kind == Kind.DEMON_DWELLER) {
            int charge = d == null ? 0 : Math.min(5, d.copyTag().getInt("DwellerCharge"));
            tip.add(Component.literal("Anti-Magic users absorb magic on hit; Black Slash releases up to 5 charges (" + charge + "/5)").withStyle(ChatFormatting.DARK_RED));
        }
        if (kind == Kind.ELSDOCIA) {
            int charge = d == null ? 0 : Mth.clamp(d.copyTag().getInt("ElsdociaCharge"), 0, 1000);
            tip.add(Component.literal("Legacy engraving: Legacy of the Wizard Kings").withStyle(ChatFormatting.LIGHT_PURPLE));
            tip.add(Component.literal("Stored magic: " + charge + "/1000").withStyle(ChatFormatting.AQUA));
            tip.add(Component.literal("Key Magic grimoire summoned: extended rift").withStyle(ChatFormatting.GOLD));
            tip.add(Component.literal("The first Wizard King's legacy: a broad crystal blade with a clover emblem, crafted by Lemiel Silvamillion Clover. It holds a piece of the past Wizard Kings' souls and was used by Asta to defeat Conrad.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        if (d != null && d.copyTag().contains("ExpiresAt"))
            tip.add(Component.literal("Drawn from a grimoire: fades after a minute").withStyle(ChatFormatting.AQUA));
        if (kind.demon && d != null && d.copyTag().contains("OwnerName"))
            tip.add(Component.literal("Bound to " + d.copyTag().getString("OwnerName")).withStyle(ChatFormatting.DARK_PURPLE));
        wrapTooltip(tip, firstLine);
    }

    /** 1.2.0: tooltip lines do not wrap by themselves: long lines (Elsdocia's description ran off the screen) are split at word breaks, continuation lines indented. */
    static void wrapTooltip(List<Component> tip, int from) {
        final int width = 44;
        for (int i = from; i < tip.size(); i++) {
            Component c = tip.get(i);
            String text = c.getString();
            if (text.length() <= width) continue;
            String indent = text.startsWith("  ") ? "  " : "";
            List<Component> lines = new java.util.ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String word : text.trim().split(" +")) {
                if (line.length() > 0 && line.length() + 1 + word.length() > width) { lines.add(Component.literal((lines.isEmpty() ? indent : indent + "  ") + line).withStyle(c.getStyle())); line.setLength(0); }
                if (line.length() > 0) line.append(' ');
                line.append(word);
            }
            if (line.length() > 0) lines.add(Component.literal((lines.isEmpty() ? indent : indent + "  ") + line).withStyle(c.getStyle()));
            tip.remove(i);
            tip.addAll(i, lines);
            i += lines.size() - 1;
        }
    }
}
