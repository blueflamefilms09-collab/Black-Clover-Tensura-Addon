package com.newuniverse.nusmp.blackclover;

import com.newuniverse.nusmp.book.GrimoireBook;
import net.minecraft.world.entity.LivingEntity;

/** Remembers the last grimoire page that hit an entity, for Copy Magic. */
public final class CopyMemory {
    private CopyMemory() {}

    public static void remember(LivingEntity target, GrimoireBook book, int mode) {
        if (book.magic == MagicType.COPY || mode >= book.familyCount()) return;   // never copy Spirit Dive / Devil Union / Copy itself
        target.getPersistentData().putString("nusmp_copy_book", book.getRegistryName().toString());
        target.getPersistentData().putInt("nusmp_copy_mode", mode);
        target.getPersistentData().putLong("nusmp_copy_at", target.level().getGameTime());
    }
}
