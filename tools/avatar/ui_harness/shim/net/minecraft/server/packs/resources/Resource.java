package net.minecraft.server.packs.resources;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Why: harness shim: a file of the repo's assets. */
public class Resource {
    private final Path file;
    public Resource(Path file) { this.file = file; }
    public InputStream open() throws IOException { return Files.newInputStream(file); }
}
