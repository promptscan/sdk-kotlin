val javaVersion: String = System.getProperty("java.version")
if (javaVersion.substringBefore(".").toInt() < 11) {
    throw GradleException("Java 11 or higher is required to build this project. You are using Java $javaVersion.")
}

rootProject.name = "promptscan-sdk"