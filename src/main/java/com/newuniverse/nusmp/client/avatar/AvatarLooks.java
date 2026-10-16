package com.newuniverse.nusmp.client.avatar;

import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.appearance.AppearanceData;
import net.minecraft.nbt.CompoundTag;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Why: the renderer asks for an Avatar's look several times a frame (skin, size, layers), and decoding the synced tag each time would allocate every
 * call. The synced tag object only changes when the server sends a new one, so the decoded look is kept per entity and reused while the same tag
 * instance is still there. Weakly keyed: a removed Avatar frees its entry.
 */
final class AvatarLooks {
    private AvatarLooks() {}

    private record Entry(CompoundTag tag, AppearanceData look) {}

    private static final Map<AvatarEntity, Entry> CACHE = new WeakHashMap<>();

    static AppearanceData of(AvatarEntity e) {
        CompoundTag tag = e.getEntityData().get(AvatarEntity.APPEARANCE);
        Entry c = CACHE.get(e);
        if (c != null && c.tag == tag) return c.look;
        AppearanceData look = AppearanceData.fromTag(tag);
        CACHE.put(e, new Entry(tag, look));
        return look;
    }

    static void clear() { CACHE.clear(); }
}
