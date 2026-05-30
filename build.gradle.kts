plugins {
    kotlin("jvm") version Kotlin.version
    id("org.jetbrains.dokka") version Dokka.version
    id("maven-publish")
}

group = "cz.creeperface.hytale.placeholderapi"
version = "2.0"

repositories {
    mavenCentral()
    mavenLocal()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("snapshot") {
                from(components["kotlin"])

                pom {
                    name.set("PlaceholderAPI")
                    description.set("Placeholder API for Hytale")
                    licenses {
                        license {
                            name.set("GNU GENERAL PUBLIC LICENSE v3.0")
                            url.set("https://www.gnu.org/licenses/gpl-3.0.en.html")
                        }
                    }
                }
            }
        }
        repositories {
            mavenLocal()
        }
    }
}

dependencies {
    compileOnly(kotlin("reflect", Kotlin.version))

    // Hytale server JAR (resolved per OS)
    val userHome = System.getProperty("user.home")
    val osName = System.getProperty("os.name").lowercase()
    val hytaleBase = when {
        osName.contains("mac") -> "$userHome/Library/Application Support/Hytale"
        osName.contains("win") -> "${System.getenv("APPDATA")}/Hytale"
        else -> "$userHome/.local/share/Hytale"
    }
    val hytaleServerJar = files("$hytaleBase/install/release/package/game/latest/Server/HytaleServer.jar")
    compileOnly(hytaleServerJar)
    testImplementation(hytaleServerJar)

    dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:${Dokka.version}")

    testImplementation("junit:junit:${JUnit.version}")
    testImplementation(kotlin("reflect", Kotlin.version))
}

tasks.test {
    // HytaleLogger refuses to initialize unless the JUL LogManager is its custom one.
    // Setting the system property before the test JVM starts ensures the right manager
    // is selected the first time java.util.logging is touched.
    systemProperty("java.util.logging.manager", "com.hypixel.hytale.logger.backend.HytaleLogManager")
}