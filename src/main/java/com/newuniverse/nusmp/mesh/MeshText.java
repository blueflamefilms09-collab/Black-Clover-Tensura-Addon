package com.newuniverse.nusmp.mesh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import net.minecraft.server.packs.resources.Resource;

/**
 * The one open call for mesh files (3.32.0): hands back a reader over the text of a mesh whether the file in the jar is plain text or packed
 * ({@link MeshPack}). Loaders use it where they used to call {@code resource.openAsReader()}.
 */
public final class MeshText {
    private MeshText() {}

    public static BufferedReader reader(Resource resource) throws IOException {
        try (InputStream in = resource.open()) {
            return reader(in);
        }
    }

    public static BufferedReader reader(InputStream in) throws IOException {
        return new BufferedReader(new StringReader(MeshPack.text(in.readAllBytes())));
    }

    public static String text(InputStream in) throws IOException {
        return MeshPack.text(in.readAllBytes());
    }
}
