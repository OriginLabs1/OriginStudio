Minimum Requirements and Compatibility
Here’s a quick breakdown of what CoGo needs to run smoothly.

Basic information
Minimum operating system: Android 9
Minimum supported Android Gradle Plugin (AGP): 7.3.0
Maximum supported Android Gradle Plugin (AGP): 9.3.1
Supported CPU architecture: 32-bit ARM (armeabi-v7a), 64-bit ARM (arm64-v8a)
Approximate system needs
Minimum storage: Approximately 3.5GB (assets installation requires around 2GB, while builds could require an additional 1GB)
Minimum free RAM: Approximately 1.5GB of free RAM, but 4GB recommended (free RAM is the amount available for applications to use, not the total device RAM)
Java considerations
Code on the Go ships with JDK 21 , which is sufficient for nearly all use cases; official support is not provided for other JDK versions.
For any project, the Java source version depends on the AGP version it is configured to use.
Projects with AGP 9.3.1 (shipped by default) can be configured to use up to Java 17 syntax.
If the project is configured with any other AGP version, then the Java source version may differ, in which case you may need to verify whether it is possible to use a newer or older Java source version.
Kotlin considerations
CoGo ships with Kotlin 2.3.21
As with Java, this also depends on the AGP version, but we recommend using Kotlin 2.3.21. It may be possible to use a newer Kotlin version, but support is not tested or guaranteed.