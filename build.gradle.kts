import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

group = "com.github.Tianea2160"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24))
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_24)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    implementation(kotlin("stdlib"))

    testImplementation(libs.bundles.test)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("State Machine")
                description.set("Type-safe declarative state machine library for Kotlin")
                url.set("https://github.com/Tianea2160/statemachine")

                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }

                developers {
                    developer {
                        id.set("Tianea2160")
                        name.set("Tianea")
                    }
                }

                scm {
                    url.set("https://github.com/Tianea2160/statemachine")
                    connection.set("scm:git:git://github.com/Tianea2160/statemachine.git")
                    developerConnection.set("scm:git:ssh://github.com/Tianea2160/statemachine.git")
                }
            }
        }
    }
}
