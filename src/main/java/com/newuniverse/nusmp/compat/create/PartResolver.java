package com.newuniverse.nusmp.compat.create;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: the only place that turns a logical {@link Part} into a block of the running game, and the reason the mod needs no compile-time dependency on Create: it looks the
 * block up in the block registry by id, with every candidate id OPTIONAL. The first candidate that exists wins; when none exists (Create is absent, or an id was renamed in a
 * version I could not check) the part falls back to the vanilla block named by the part, the miss is logged ONCE, and the trap still works, because everything that hurts is
 * done by the mod itself, never by a Create block. State properties are set by NAME through the block's own state definition (the way {@code StateCarry} does), so a property
 * a block does not have, or a value it does not accept, is skipped instead of throwing.
 */
public final class PartResolver {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();
    private static final Map<Part, Resolved> CACHE = new ConcurrentHashMap<>();

    /** The block a part resolved to, its id, and whether it is a Create block (false for a vanilla stand-in). */
    public record Resolved(Block block, String id, boolean create) {}

    private PartResolver() {}

    /** Is the Create mod loaded? Safe before mod loading finished. */
    public static boolean createLoaded() {
        ModList list = ModList.get();
        return list != null && list.isLoaded(CreateIds.MOD_ID);
    }

    /** Forgets what was resolved (a server start, or a test that changes the world's registries). */
    public static void clear() { CACHE.clear(); }

    /** Logs a line the first time a key is seen, and returns whether it did. */
    public static boolean logOnce(String key, String line) {
        if (!LOGGED.add(key)) return false;
        LOG.warn("[nusmp] Create compat: {}", line);
        return true;
    }

    public static Resolved resolve(Part part) {
        Resolved r = CACHE.get(part);
        if (r != null) return r;
        r = lookup(part);
        CACHE.put(part, r);
        return r;
    }

    private static Resolved lookup(Part part) {
        if (!part.vanilla() && createLoaded()) {
            for (String id : CreateIds.candidates(part)) {
                if (CreateIds.FORBIDDEN.contains(id)) continue;
                ResourceLocation rl = ResourceLocation.tryParse(id);
                if (rl == null) continue;
                Optional<Block> b = BuiltInRegistries.BLOCK.getOptional(rl);
                if (b.isPresent() && b.get() != Blocks.AIR) return new Resolved(b.get(), id, true);
            }
            logOnce("part:" + part, part + " was not found in the block registry under " + CreateIds.candidates(part) + "; the vanilla " + part.fallback() + " stands in for it");
        }
        ResourceLocation fb = ResourceLocation.parse(part.fallback());
        Block block = BuiltInRegistries.BLOCK.get(fb);
        return new Resolved(block, part.fallback(), false);
    }

    /** The block state of a placed part in the world frame: the default state with the facing, axis and variant of the drawing set by name. */
    public static BlockState state(Blueprint.Placed p, Frame frame) {
        Resolved r = resolve(p.part());
        BlockState s = r.block().defaultBlockState();
        Direction face = p.facing() == null ? null : frame.dir(p.facing());
        switch (p.part()) {
            case LEVER -> {
                s = set(s, "face", "wall");
                s = set(s, "powered", "false");
            }
            case SAW -> s = set(s, "face", "wall");                                  // the vanilla stand-in, a grindstone, is hung on the wall
            case BELT -> {
                s = set(s, "slope", "horizontal");
                if (p.variant() != null) s = set(s, "part", p.variant());
            }
            default -> { }
        }
        if (face != null) s = set(s, "facing", face.getName());
        if (p.axis() != null) {
            char rotation = frame.axis(p.axis());
            s = set(s, "axis", String.valueOf(rotation));
            if (face != null) {
                char facing = face.getAxis() == Direction.Axis.X ? 'x' : face.getAxis() == Direction.Axis.Y ? 'y' : 'z';
                String flag = String.valueOf(Dir6.alongFirst(facing, rotation));
                s = set(s, "axis_along_first", flag);                              // Create's constant is AXIS_ALONG_FIRST_COORDINATE; the property name is, I believe, axis_along_first
                s = set(s, "axis_along_first_coordinate", flag);
            }
        }
        return s;
    }

    /** The state of a power cell: the redstone block when {@code on}, the inert block when not. */
    public static BlockState power(boolean on) { return resolve(on ? Part.POWER : Part.HOLD).block().defaultBlockState(); }

    /** The block of the ram. */
    public static BlockState ram() { return resolve(Part.RAM).block().defaultBlockState(); }

    /** Sets a state property by name; does nothing when the block has no such property or does not accept the value. */
    public static BlockState set(BlockState s, String name, String value) {
        Property<?> p = s.getBlock().getStateDefinition().getProperty(name);
        return p == null ? s : setByName(s, p, value);
    }

    private static <T extends Comparable<T>> BlockState setByName(BlockState s, Property<T> p, String value) {
        Optional<T> v = p.getValue(value);
        return v.isPresent() ? s.setValue(p, v.get()) : s;
    }

    /** For the command and the self-test: every part with the id it resolved to and whether that is a Create block. */
    public static Map<Part, Resolved> report() {
        Map<Part, Resolved> out = new EnumMap<>(Part.class);
        for (Part p : Part.values()) out.put(p, resolve(p));
        return out;
    }
}
