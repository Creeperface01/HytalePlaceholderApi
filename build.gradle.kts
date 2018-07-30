plugins {
    kotlin("jvm") version Kotlin.version
    id("org.jetbrains.dokka") version Dokka.version
    id("maven-publish")
}

group = "com.creeperface.hytale.placeholderapi"
version = "1.0"

repositories {
    mavenCentral()
    mavenLocal()
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
    compileOnly(fileTree("libs").include("*.jar"))
    compileOnly(kotlin("reflect", Kotlin.version))

    dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:${Dokka.version}")

    testImplementation("junit:junit:${JUnit.version}")
}