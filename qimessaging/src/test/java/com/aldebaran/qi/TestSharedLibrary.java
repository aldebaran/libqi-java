/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestSharedLibrary {

    @Test
    public void testGetNameLinux() {
        String osName = System.getProperty("os.name");
        if (!osName.startsWith("Linux")) {
            return;
        }

        String libname = SharedLibrary.getLibraryName("qi");
        assertEquals("libqi.so", libname);

    }

    @Test
    public void testGetNameWindows() {
        String osName = System.getProperty("os.name");
        if (!osName.startsWith("Windows")) {
            return;
        }

        String libname = SharedLibrary.getLibraryName("qi");
        assertEquals("qi.dll", libname);
    }

    @Test
    public void testLoadLibQi() {
        assertEquals(true, SharedLibrary.loadLib("qi"));
    }
}
