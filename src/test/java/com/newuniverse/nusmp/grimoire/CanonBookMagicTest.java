package com.newuniverse.nusmp.grimoire;

import com.newuniverse.nusmp.blackclover.MagicType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Every canon book names a magic this mod has. */
class CanonBookMagicTest {
    @Test
    void canonMagicsExist() {
        for (CanonBook b : CanonBook.values()) assertNotNull(MagicType.valueOf(b.magic), b.name());
    }
}
