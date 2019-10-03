package com.aldebaran.qi;

class SystemUtil {

    enum OSType {
        Linux,
        Mac,
        Windows,
        unknown
    }

    private static final String OS_NAME = System.getProperty("os.name");

    static final boolean IS_OS_MAC     = getOsMatchesName("Mac");
    static final boolean IS_OS_WINDOWS = getOsMatchesName("Windows");
    static final boolean IS_OS_LINUX   = getOsMatchesName("Linux")
                                      || getOsMatchesName("LINUX");

    static final OSType osType = findOS();

    private static OSType findOS() {
        if (IS_OS_LINUX)
            return OSType.Linux;
        else if (IS_OS_MAC)
            return OSType.Mac;
        else if (IS_OS_WINDOWS)
            return OSType.Windows;
        else
            return OSType.unknown;
    }

    private static boolean isOSNameMatch(final Optional<String> osName, final String osNamePrefix)  {
        return osName.isPresent() && osName.get().startsWith(osNamePrefix);
    }

    private static boolean getOsMatchesName(final String osNamePrefix) {
        return isOSNameMatch(Optional.ofNullable(OS_NAME), osNamePrefix);
    }
}