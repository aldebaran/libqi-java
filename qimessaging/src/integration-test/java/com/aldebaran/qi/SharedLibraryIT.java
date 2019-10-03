package com.aldebaran.qi;

import org.junit.Test;

import static org.junit.Assert.fail;

public class SharedLibraryIT {

    @Test
    public void testLoadEmbeddedLibraries() {
        try {
            EmbeddedTools.loadEmbeddedLibraries();
        } catch (RuntimeException ignored) {
            fail("loadEmbeddedLibraries should not throw any exceptions.");
        }
    }
}
