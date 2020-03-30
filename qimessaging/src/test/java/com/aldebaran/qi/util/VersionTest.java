package com.aldebaran.qi.util;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VersionTest {

    @Test
    public void testVersionReading() throws IOException {
        String propStr = "version=3.1.0";
        InputStream in = new ByteArrayInputStream(propStr.getBytes("UTF-8"));

        DefaultArtifactVersion version = Version.read(in);
        assertEquals(version.getMajorVersion(), 3);
        assertEquals(version.getMinorVersion(),  1);
        assertEquals(version.getIncrementalVersion(), 0);
    }

    @Test(expected = RuntimeException.class)
    public void propertyNotFound() throws IOException {
        String propStr = "toto";
        InputStream in = new ByteArrayInputStream(propStr.getBytes("UTF-8"));
        DefaultArtifactVersion res = Version.read(in);
    }

    @Test(expected = IOException.class)
    public void inputStreamIOFailure() throws IOException {
        InputStream in = mock(InputStream.class);
        when(in.read(any(byte[].class))).thenThrow(new IOException());
        DefaultArtifactVersion v = Version.read(in);
    }

    @Test(expected = NullPointerException.class)
    public void nullInputStream() throws IOException {
        Version.read(null);
    }
}
