package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.particle.rukh.RukhFx;

import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Wave 19, the scrolls: the Scroll of Unreadable Spells (wild magic, seven letters a day), the Scroll of Absorption (takes one spell aimed only at
 * you and gives it back) and the six Bargain Bin Scrolls (a misspelt Charm Person, Fireball, Speak with Stones, Summon Greater Crab, Mage Hand and
 * Facsimile). A scroll that could not do its work is handed back (nothing is wasted on an empty room).
 */
final class Wave19Scrolls {
    private Wave19Scrolls() {}

    static final String K_ABS = "w19_abs", K_ABS_TYPE = "w19_abs_type", K_FACE_GEN = "w19_face_gen", K_FACE_UNTIL = "w19_face_until";
    static final String ABSORPTION_ID = "scroll_of_absorption";

    /** The consumable is used up after the act; this gives it back (the act refused or found nothing to work on). */
    static void keep(ActCtx c) {
        if (!c.player().isCreative()) c.stack().grow(1);
    }

    // ================================================================ Scroll of Unreadable Spells
    /** One letter read aloud: one roll on the wild magic table. Seven a day (the act's own counter); the scroll is kept. */
    static void unreadable(ActCtx c) {
        keep(c);
        ServerPlayer p = c.player();
        Wave19Rules.Surge s = Wave19Surge.roll(p);
        Wave19Util.say(p, "Unreadable Spells: " + Wave19Surge.line(s));
    }

    // ================================================================ Scroll of Absorption
    private static CompoundTag tag(ItemStack s) { return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }

    static boolean isAbsorption(ItemStack s) {
        return !s.isEmpty() && Relics.of(s).map(d -> ABSORPTION_ID.equals(d.id())).orElse(false);
    }

    static boolean charged(ItemStack s) { return tag(s).contains(K_ABS); }

    /**
     * Reading it. Empty, it only reminds you how it works and is kept: held in either hand it takes the next spell aimed only at you. Charged with
     * an absorbed spell, reading it lets that spell go at the foe you are looking at (or the nearest within 24 blocks) and uses the scroll up.
     */
    static void release(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag t = tag(c.stack());
        if (!t.contains(K_ABS)) {
            keep(c);
            Wave19Util.tell(p, "Scroll of Absorption: it is empty. Hold it in a hand: the next spell aimed only at you is drawn into it, and you may read it to cast it back.", ChatFormatting.LIGHT_PURPLE);
            return;
        }
        LivingEntity target = aimed(p, Wave19Rules.ABSORB_RANGE);
        if (target == null) {
            keep(c);
            Wave19Util.tell(p, "Scroll of Absorption: the spell has no one to go to. Face a foe.", ChatFormatting.GRAY);
            return;
        }
        float dmg = Wave19Rules.released(t.getFloat(K_ABS));
        String type = t.getString(K_ABS_TYPE);
        ServerLevel sl = p.serverLevel();
        beam(sl, p.getEyePosition().add(0, -0.3, 0), target.getBoundingBox().getCenter(), 0xB48CFF);
        Fx.damageHeavy(p, target, dmg, type.isBlank() ? "minecraft:magic" : type);
        Fx.burst(sl, target.position(), 0xB48CFF);
        sl.playSound(null, target.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.5f, 0.6f);
        Wave19Util.say(p, "Scroll of Absorption: the spell it swallowed goes back out.");
    }

    /** The reaction: called by the damage handler when a spell aimed only at the holder arrives. Charges one held scroll; true if it did. */
    static boolean absorb(ServerPlayer p, float amount, String damageTypeId) {
        ItemStack scroll = null;
        for (InteractionHand h : InteractionHand.values()) {
            ItemStack s = p.getItemInHand(h);
            if (isAbsorption(s) && !charged(s)) { scroll = s; break; }
        }
        if (scroll == null) return false;
        ItemStack target = scroll;
        if (scroll.getCount() > 1) {                                                    // one scroll out of the stack takes the spell
            target = scroll.split(1);
        }
        float held = Math.min(Wave19Rules.ABSORB_CAP, Math.max(0f, amount));
        String type = damageTypeId == null ? "minecraft:magic" : damageTypeId;
        CustomData.update(DataComponents.CUSTOM_DATA, target, (CompoundTag t) -> {
            t.putFloat(K_ABS, held);
            t.putString(K_ABS_TYPE, type);
        });
        if (target != scroll && !p.getInventory().add(target)) p.drop(target, false);
        ServerLevel sl = p.serverLevel();
        Fx.burst(sl, p.position(), 0xB48CFF);
        RukhFx.spray(sl, RukhFx.Alignment.LIGHT, p.getX(), p.getY() + 1.2, p.getZ(), 40, 0.5, 0.6, 0.5, 0.8);
        sl.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 1.8f);
        Wave19Util.say(p, "Scroll of Absorption: the spell is drawn into the scroll. Read it to send it back.");
        return true;
    }

    // ================================================================ Bargain Bin #1: Charm Parson
    private static final Set<String> HOLY_KNIGHTS = Set.of("falmuth_knight", "hinata_sakaguchi");

    private static boolean holyGear(ItemStack s) {
        if (s.isEmpty()) return false;
        ItemEnchantments en = s.getEnchantments();
        if (en.isEmpty()) return false;
        for (Holder<Enchantment> h : en.keySet()) {
            if (h.is(ResourceLocation.fromNamespaceAndPath("tensura", "holy_weapon")) || h.is(ResourceLocation.fromNamespaceAndPath("tensura", "holy_coat"))) return true;
        }
        return false;
    }

    /** Clergy: cleric villagers, Tensura's holy knights, and any creature in hand-held holy-engraved gear. */
    static boolean clergy(LivingEntity e) {
        if (e instanceof Villager v) return v.getVillagerData().getProfession() == VillagerProfession.CLERIC;
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        if (key != null && "tensura".equals(key.getNamespace()) && HOLY_KNIGHTS.contains(key.getPath())) return true;
        return holyGear(e.getMainHandItem()) || holyGear(e.getOffhandItem());
    }

    /**
     * Scroll of Charm Parson: the spell that was meant to charm anyone now charms only clergy. Cleric villagers, holy knights and creatures in
     * holy-engraved gear within sixteen blocks (twelve at most) are charmed for five minutes unless they are great beasts or bosses (a hundred health or
     * more shrug it off); a charmed cleric villager leaves you Hero of the Village for twenty minutes. If there is no clergy the scroll is kept.
     */
    static void charmParson(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        List<LivingEntity> near = sl.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(16.0), e -> e != p && e.isAlive() && !(e instanceof ServerPlayer) && clergy(e));
        near.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        int charmed = 0, villagers = 0, resisted = 0;
        for (LivingEntity e : near) {
            if (charmed >= 12) break;
            if (!(e instanceof Mob m)) continue;
            if (!Wave19Rules.charmTakes(e.getMaxHealth())) { resisted++; continue; }
            Wave19Field.charm(m, p, Wave19Rules.CHARM_TICKS, false);
            RukhFx.spray(sl, RukhFx.Alignment.LIGHT, e.getX(), e.getEyeY() + 0.3, e.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
            charmed++;
            if (e instanceof Villager) villagers++;
        }
        if (charmed == 0 && resisted == 0) {
            keep(c);
            Wave19Util.tell(p, "Scroll of Charm Parson: no priest, cleric or holy knight within sixteen blocks. The scroll is kept.", ChatFormatting.GRAY);
            return;
        }
        if (villagers > 0) Wave19Util.give(p, "minecraft:hero_of_the_village", Wave19Rules.HOUR, 0);
        Fx.ring(sl, p.position(), 5.0, 0xFF9AD0);
        Wave19Util.say(p, "Scroll of Charm Parson: " + charmed + " of the clergy are charmed" + (resisted > 0 ? " (" + resisted + " too mighty to be moved)" : "")
                + (villagers > 0 ? "; the village thinks well of you." : "."));
    }

    // ================================================================ Bargain Bin #2: Firball
    /**
     * Scroll of Firball: a swirling ball of pine needles flies up to 28 blocks along your look line and bursts at the first creature or block: 12
     * piercing damage to every foe within 3.5 blocks, half to one that dodges (three in ten do). No fire, no block damage.
     */
    static void firball(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        Vec3 end = eye.add(look.scale(Wave19Rules.FIRBALL_RANGE));
        HitResult blocks = sl.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 stop = blocks.getType() == HitResult.Type.MISS ? end : blocks.getLocation();
        EntityHitResult ent = ProjectileUtil.getEntityHitResult(sl, p, eye, stop, new AABB(eye, stop).inflate(1.0), e -> e instanceof LivingEntity l && l != p && l.isAlive() && !l.isSpectator() && e.isPickable());
        if (ent != null) stop = ent.getLocation();
        beam(sl, eye.add(0, -0.3, 0), stop, 0x2E7D32);
        RukhFx.spray(sl, RukhFx.Alignment.LIGHT, stop.x, stop.y, stop.z, 60, 1.6, 1.0, 1.6, 0.2);
        RukhFx.spray(sl, RukhFx.Alignment.LIGHT, stop.x, stop.y, stop.z, 1, 0, 0, 0, 0);
        sl.playSound(null, BlockPos.containing(stop), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9f, 1.5f);
        int hit = 0;
        for (LivingEntity t : Wave19Util.foesAt(p, stop, Wave19Rules.FIRBALL_RADIUS)) {
            boolean dodged = Wave19Rules.saves(Wave19Rules.d20(p.getRandom()::nextInt), 0, 15);
            Fx.damage(p, t, Wave19Rules.firball(dodged), "minecraft:cactus");
            hit++;
        }
        Wave19Util.say(p, "Scroll of Firball: the ball of pine needles bursts" + (hit > 0 ? " on " + hit + " foe" + (hit == 1 ? "." : "s.") : " on nothing."));
    }

    // ================================================================ Bargain Bin #3: Speak with Rocks
    /**
     * Scroll of Speak with Rocks: the stones of the cave answer, and none of them has anything interesting to say, but they do know where the
     * ore is: the nearest ores within ten blocks (24 at most) glimmer for a moment and the five nearest are named in chat with their distance.
     */
    static void rocks(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        BlockPos o = p.blockPosition();
        int r = Wave19Rules.ROCKS_RADIUS;
        List<BlockPos> ores = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(o.offset(-r, -r, -r), o.offset(r, r, r))) {
            if (!sl.isLoaded(pos)) continue;
            if (sl.getBlockState(pos).is(Tags.Blocks.ORES)) ores.add(pos.immutable());
        }
        ores.sort(Comparator.comparingDouble(b -> b.distSqr(o)));
        if (ores.size() > Wave19Rules.ROCKS_MAX) ores = new ArrayList<>(ores.subList(0, Wave19Rules.ROCKS_MAX));
        for (BlockPos b : ores) {
            RukhFx.spray(sl, RukhFx.Alignment.LIGHT, b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5, 4, 0.4, 0.4, 0.4, 0.02);
            RukhFx.spray(sl, RukhFx.Alignment.LIGHT, b.getX() + 0.5, b.getY() + 1.1, b.getZ() + 0.5, 1, 0, 0.1, 0, 0.02);
        }
        sl.playSound(null, o, SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.0f, 0.5f);
        Wave19Util.tell(p, "The rocks say: \"" + Wave19Rules.rockQuip(p.getRandom().nextInt(1000)) + "\"", ChatFormatting.GRAY);
        int named = 0;
        for (BlockPos b : ores) {
            if (named++ >= 5) break;
            BlockState st = sl.getBlockState(b);
            Wave19Util.tell(p, "  ...though " + st.getBlock().getName().getString() + " lies " + Wave19Rules.offsetText(b.getX() - o.getX(), b.getY() - o.getY(), b.getZ() - o.getZ()) + ".", ChatFormatting.DARK_GRAY);
        }
        if (ores.isEmpty()) Wave19Util.tell(p, "  ...they know of no ore within ten blocks. It is all rock.", ChatFormatting.DARK_GRAY);
    }

    // ================================================================ Bargain Bin #4: Summon Greater Crab
    /**
     * Scroll of Summon Greater Crab: a huge crab claws up from a ring marked on the ground six blocks ahead, two and a half seconds after you read
     * it, and it is hostile to everyone, you included. It lasts twenty seconds and then dissolves. At most two per caster. (The armoured beast of
     * Tensura stands in for the crab, scaled up; plain Minecraft gets the ravager.)
     */
    static void crab(ActCtx c) {
        ServerPlayer p = c.player();
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0e-3 ? new Vec3(p.getDirection().getStepX(), 0, p.getDirection().getStepZ()) : flat.normalize();
        Vec3 at = new Vec3(p.getX() + flat.x * 6.0, p.getY(), p.getZ() + flat.z * 6.0);
        if (!Wave19Field.summonCrab(p, at)) {
            keep(c);
            Wave19Util.tell(p, "Scroll of Summon Greater Crab: there are already crabs about. The scroll is kept.", ChatFormatting.GRAY);
            return;
        }
        Wave19Util.tell(p, "Scroll of Summon Greater Crab: a crab is coming up in the red ring, and it is angry with everyone. Get clear!", ChatFormatting.RED);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.TURTLE_LAY_EGG, SoundSource.PLAYERS, 1.4f, 0.5f);
    }

    // ================================================================ Bargain Bin #5: Mace Hand
    /** Scroll of Mace Hand: for twenty minutes your bare fist is iron: +5 damage on every bare-fist blow (added by the damage handler). */
    static void mace(ActCtx c) {
        ServerPlayer p = c.player();
        Wave19Util.data(p).putLong(Wave19Events.K_MACE, p.level().getGameTime() + Wave19Rules.MACE_TICKS);
        ServerLevel sl = p.serverLevel();
        RukhFx.spray(sl, RukhFx.Alignment.LIGHT, p.getX(), p.getY() + 1.0, p.getZ(), 20, 0.4, 0.4, 0.4, 0.2);
        sl.playSound(null, p.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8f, 1.4f);
        Wave19Util.say(p, "Scroll of Mace Hand: your fist swells and hardens into iron. Keep your main hand empty to strike with it.");
    }

    // ================================================================ Bargain Bin #6: Facsimile Copy
    /** What the scroll will copy: things that are used, not spent as material (tools, weapons, armour, food, potions). */
    static boolean copyable(ItemStack s) {
        if (s.isEmpty() || Relics.of(s).isPresent()) return false;
        if (s.has(DataComponents.CONTAINER) || s.has(DataComponents.BUNDLE_CONTENTS)) return false;
        return s.isDamageableItem() || s.has(DataComponents.FOOD) || s.has(DataComponents.POTION_CONTENTS);
    }

    /**
     * Scroll of Facsimile Copy: read it with the thing to copy in your other hand and a poorer copy appears in your pack: one item, its enchantments
     * gone, a damageable one with only half the durability the original had left. The copy crumbles after ten minutes, a copy of a copy after five, a third
     * generation after two and a half, and a fourth cannot be made.
     */
    static void facsimile(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack scrollHand = p.getMainHandItem() == c.stack() ? p.getMainHandItem() : p.getOffhandItem();
        ItemStack src = scrollHand == p.getMainHandItem() ? p.getOffhandItem() : p.getMainHandItem();
        if (!copyable(src)) {
            keep(c);
            Wave19Util.tell(p, "Scroll of Facsimile Copy: hold the thing to copy in your other hand: a tool, weapon, armour piece, food or potion (no relics, no containers).", ChatFormatting.GRAY);
            return;
        }
        int gen = Wave19Rules.nextGeneration(tag(src).getInt(K_FACE_GEN));
        if (gen < 0) {
            keep(c);
            Wave19Util.tell(p, "Scroll of Facsimile Copy: that copy is already too faded to copy again.", ChatFormatting.GRAY);
            return;
        }
        ItemStack copy = src.copyWithCount(1);
        copy.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (copy.isDamageableItem()) copy.setDamageValue(Wave19Rules.facsimileDamage(copy.getMaxDamage(), src.getDamageValue(), gen));
        long until = p.level().getGameTime() + Wave19Rules.facsimileLife(gen);
        CustomData.update(DataComponents.CUSTOM_DATA, copy, (CompoundTag t) -> {
            t.putInt(K_FACE_GEN, gen);
            t.putLong(K_FACE_UNTIL, until);
        });
        if (!p.getInventory().add(copy)) p.drop(copy, false);
        ServerLevel sl = p.serverLevel();
        RukhFx.spray(sl, RukhFx.Alignment.LIGHT, p.getX(), p.getY() + 1.3, p.getZ(), 30, 0.4, 0.4, 0.4, 0.6);
        sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.2f);
        Wave19Util.say(p, "Scroll of Facsimile Copy: a poorer copy (generation " + gen + ") appears; it will crumble in " + (Wave19Rules.facsimileLife(gen) / 1200) + " minutes or so.");
    }

    // ================================================================ shared
    /** The creature in the look line within {@code range} blocks, else the nearest foe within that range; null if none. */
    static LivingEntity aimed(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition(), end = eye.add(p.getLookAngle().scale(range));
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p.serverLevel(), p, eye, end, new AABB(eye, end).inflate(1.0),
                e -> e instanceof LivingEntity l && l != p && Wave19Util.foe(p, l) && e.isPickable());
        if (hit != null && hit.getEntity() instanceof LivingEntity l) return l;
        List<LivingEntity> foes = Wave19Util.foesAt(p, p.position(), range);
        return foes.isEmpty() ? null : foes.get(0);
    }

    /** A line of coloured dust from one point to another. */
    static void beam(ServerLevel sl, Vec3 from, Vec3 to, int rgb) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 0.1) return;
        int n = (int) Math.min(48, Math.max(6, len * 2));
        for (int i = 0; i <= n; i++) {
            Vec3 at = from.add(d.scale(i / (double) n));
            RukhFx.spray(sl, Wave19Util.align(rgb), at.x, at.y, at.z, 1, 0.03, 0.03, 0.03, 0);
        }
    }
}
