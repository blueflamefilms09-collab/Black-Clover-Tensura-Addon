package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.Set;
import com.newuniverse.nusmp.particle.fx.FxRelic;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Wave 3, the two consumables: the Genie Gin (one wish of three, for a Minecraft day) and Mad Marbles (a pour of glass marbles that trips the
 * first creature to step on it and goes off in sparks).
 */
final class Wave3Goods {
    private Wave3Goods() {}

    // ================================================================ Genie Gin
    private static final String K_WISH = "w3_wish";
    private static final String[] WISHES = {"Power", "Fortune", "Fame"};
    private static final Set<String> TENSURA_HUMANOIDS = Set.of("goblin", "hob_goblin", "orc", "orc_lord", "lizardman", "war_gnome", "dwarf", "elf", "kobold");

    static int selected(ItemStack stack) {
        CompoundTag t = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Math.floorMod(t.getInt(K_WISH), WISHES.length);
    }

    /**
     * Make a Wish. Sneaking while you drink turns the bottle to the next wish (Power, Fortune, Fame) and gives the bottle back; otherwise the genie
     * grants the wish it is turned to, for a Minecraft day (twenty minutes):
     * <ul>
     *   <li><b>Power</b> (one ability score to 30): Strength II, Health Boost III and Resistance for the day, and three Tensura skills lent to you
     *       (Danger Sense, Physical Attack Resistance, Sage).</li>
     *   <li><b>Fortune</b> (the bag of 10,000 gp): a stash of Magic Stones, emeralds and gold, and Luck II.</li>
     *   <li><b>Fame</b> (charm every humanoid): you are the Hero of the Village, and humanoid creatures within 24 blocks that do not shrug it off (existence) stop
     *       hunting you until you strike one.</li>
     * </ul>
     */
    static void wish(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack stack = c.stack();
        int sel = selected(stack);
        if (c.sneaking()) {
            int next = (sel + 1) % WISHES.length;
            CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putInt(K_WISH, next));
            if (!p.isCreative()) stack.grow(1);                                         // the bottle item takes one after this act ran: turning it must not use it up
            Wave3Util.tell(p, "Genie Gin: the genie waits for a wish of " + WISHES[next] + ". Drink without sneaking to make it.", ChatFormatting.LIGHT_PURPLE);
            return;
        }
        ServerLevel sl = p.serverLevel();
        com.newuniverse.nusmp.particle.fx.Fx.spray(sl, FxThemes.RELIC_WAVE_3, FxTone.LIGHT, p.getX(), p.getY() + 1.4, p.getZ(), 30, 0.6, 0.6, 0.6, 0.08);
        com.newuniverse.nusmp.particle.fx.Fx.spray(sl, FxThemes.RELIC_WAVE_3, FxTone.LIGHT, p.getX(), p.getY() + 1.4, p.getZ(), 20, 0.7, 0.7, 0.7, 0.05);
        sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5f, 0.7f);
        switch (sel) {
            case 0 -> power(p);
            case 1 -> fortune(p);
            default -> fame(p);
        }
    }

    private static void power(ServerPlayer p) {
        int t = Wave3Rules.WISH_TICKS;
        Wave3Util.giveQuiet(p, t, 1, "minecraft:strength");
        Wave3Util.giveQuiet(p, t, 2, "minecraft:health_boost");
        Wave3Util.giveQuiet(p, t, 0, "minecraft:resistance");
        int skills = Fx.lendSkills(p, t, "tensura:danger_sense", "tensura:physical_attack_resistance", "tensura:sage");
        Wave3Util.say(p, "Genie Gin: \"Power it is!\" For a day your body is at its peak" + (skills > 0 ? " and " + skills + " Tensura skill" + (skills == 1 ? " is" : "s are") + " lent to you." : "."));
    }

    private static void fortune(ServerPlayer p) {
        gift(p, "tensura:magic_stone", 24, "minecraft:diamond", 6);
        gift(p, "minecraft:emerald", 32, null, 0);
        gift(p, "minecraft:gold_ingot", 32, null, 0);
        Wave3Util.giveQuiet(p, Wave3Rules.WISH_TICKS, 1, "minecraft:luck");
        Wave3Util.say(p, "Genie Gin: \"Fortune it is!\" A stash drops into your hands, and luck follows you for a day.");
    }

    private static void fame(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        Wave3Util.giveQuiet(p, Wave3Rules.WISH_TICKS, 0, "minecraft:hero_of_the_village");
        int n = 0;
        for (LivingEntity e : Wave3Util.near(sl, p.position(), 24.0, x -> humanoid(x) && Wave3Util.bends(p, x))) {
            if (n >= 12) break;
            Wave3Field.charm((Mob) e, p, Wave3Rules.WISH_TICKS, false);
            com.newuniverse.nusmp.particle.fx.Fx.spray(sl, FxThemes.RELIC_WAVE_3, FxTone.LIGHT, e.getX(), e.getEyeY() + 0.3, e.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
            n++;
        }
        Wave3Util.say(p, "Genie Gin: \"Fame it is!\" Villagers hail you as a hero" + (n > 0 ? ", and " + n + " humanoid creature" + (n == 1 ? "" : "s") + " now count you a close friend until you raise a hand against them." : "."));
    }

    private static boolean humanoid(LivingEntity e) {
        if (e instanceof AbstractIllager || e instanceof Witch || e instanceof Zombie || e instanceof AbstractSkeleton || e instanceof AbstractPiglin) return true;
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return key != null && "tensura".equals(key.getNamespace()) && TENSURA_HUMANOIDS.contains(key.getPath());
    }

    /** Puts {@code count} of an item in the player's hands (the stand-in if the first does not exist). */
    private static void gift(ServerPlayer p, String id, int count, String standIn, int standInCount) {
        Optional<Item> item = item(id);
        int n = count;
        if (item.isEmpty() && standIn != null) {
            item = item(standIn);
            n = standInCount;
        }
        if (item.isEmpty()) return;
        while (n > 0) {
            ItemStack s = new ItemStack(item.get(), Math.min(n, item.get().getDefaultMaxStackSize()));
            n -= s.getCount();
            if (!p.getInventory().add(s)) p.drop(s, false);
        }
    }

    private static Optional<Item> item(String id) {
        ResourceLocation rl = Wave3Util.rl(id);
        return rl == null ? Optional.empty() : BuiltInRegistries.ITEM.getOptional(rl);
    }

    // ================================================================ Mad Marbles
    /** Pour: a square of marbles spills onto the ground just in front of you (three by three, half a minute, three patches at most per player). */
    static void pour(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0e-3) flat = new Vec3(p.getDirection().getStepX(), 0, p.getDirection().getStepZ());
        flat = flat.normalize();
        Vec3 ground = new Vec3(p.getX() + flat.x * 1.8, p.getY(), p.getZ() + flat.z * 1.8);
        Wave3Field.pour(p, ground);
        for (int rgb : new int[]{0xE250D8, 0xFF9A2E, 0x2EE8D8, 0x58E848}) com.newuniverse.nusmp.particle.fx.Fx.spray(sl, FxThemes.RELIC_WAVE_3, FxRelic.tone(rgb), ground.x, ground.y + 0.2, ground.z, 6, 1.0, 0.1, 1.0, 0.02);
        com.newuniverse.nusmp.particle.fx.Fx.spray(sl, FxThemes.RELIC_WAVE_3, FxTone.LIGHT, ground.x, ground.y + 0.2, ground.z, 12, 1.0, 0.1, 1.0, 0.1);
        sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0f, 1.6f);
        Wave3Util.tell(p, "Mad Marbles: the marbles spill out. The first to step on them falls, and they go off.", ChatFormatting.LIGHT_PURPLE);
    }
}
