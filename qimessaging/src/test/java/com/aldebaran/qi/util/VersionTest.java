package com.aldebaran.qi.util;

import com.aldebaran.qi.Result;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VersionTest {

    @Test
    public void testVersionReading() throws UnsupportedEncodingException {
        String propStr = "version=3.1.0";
        InputStream in = new ByteArrayInputStream(propStr.getBytes("UTF-8"));

        DefaultArtifactVersion version = Version.read(in).get();
        assertEquals(version.getMajorVersion(), 3);
        assertEquals(version.getMinorVersion(),  1);
        assertEquals(version.getIncrementalVersion(), 0);
    }

    @Test
    public void propertyNotFound() throws Throwable {
        String propStr = "toto";
        InputStream in = new ByteArrayInputStream(propStr.getBytes("UTF-8"));

        Result<DefaultArtifactVersion, Throwable> res =
                Version.read(in);
        assertFalse(res.isPresent());
        assertSame(RuntimeException.class, res.getErr().getClass());
    }

    @Test
    public void inputStreamIOFailure() throws IOException {
        InputStream in = mock(InputStream.class);
        when(in.read(any(byte[].class))).thenThrow(new IOException());
        Result<DefaultArtifactVersion, Throwable> res =
                Version.read(in);
        assertFalse(res.isPresent());
        assertSame(IOException.class, res.getErr().getClass());
    }

    @Test(expected = NullPointerException.class)
    public void nullInputStream() {
        Version.read(null);
    }
}
