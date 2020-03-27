package com.aldebaran.qi.util;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Maven-based version utilities.
 * more details at https://docs.oracle.com/middleware/1212/core/MAVEN/maven_version.htm#MAVEN400
 */
public class Version {
    /**
     * Reads a version from an InputStream.
     * @param stream InputStream that contains a property named "version" which contains a formatted version.
     * @return The maven version, or a throwable if an error occured.
     */
    static DefaultArtifactVersion read(InputStream stream) throws IOException
    {
        Properties property = new Properties();
        String prop = null;

        property.load(stream);
        prop = property.getProperty("version");

        if(prop == null)
            throw new RuntimeException("The \"version\" property could not be found in the project's resources.");

        return new DefaultArtifactVersion(prop);
    }

    /**
     * Gets the version of libqi-java from the Maven properties.
     */
    public static DefaultArtifactVersion current() throws IOException
    {
        InputStream resourcesAsStream = Version.class.getResourceAsStream("/project.properties");
        return read(resourcesAsStream);
    }
}
