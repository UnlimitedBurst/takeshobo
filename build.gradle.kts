import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlin_version="1.5.20"
    kotlin("multiplatform") version kotlin_version
    kotlin("plugin.serialization") version kotlin_version
    application
}

val serialization_version:String by project
val kotlin_version:String by project
val logback_version:String by project
val ktor_version:String by project
val `kotlin-react-version`:String by project
val `kotlin-styled-version`:String by project
group = "jp.co.takeshobo"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-js-wrappers") }
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") }
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
        withJava()
    }
    js(LEGACY) {
        binaries.executable()
        browser {
            commonWebpackConfig {
                cssSupport.enabled = true
            }
            testTask {
                useKarma {
                    useChrome()
                }
            }
        }
    }
    sourceSets {
        val commonMain by getting{
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization_version")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("io.ktor:ktor-server-netty:$ktor_version")
                implementation("io.ktor:ktor-client-core:$ktor_version")
                implementation("io.ktor:ktor-client-cio:$ktor_version")
                implementation("io.ktor:ktor-html-builder:$ktor_version")
                implementation("io.ktor:ktor-serialization:$ktor_version")
                implementation("io.ktor:ktor-websockets:$ktor_version")

                implementation("ch.qos.logback:logback-classic:$logback_version")

            }
        }
        val jvmTest by getting{
            dependencies {
                implementation("io.ktor:ktor-server-tests:$ktor_version")
            }

        }
        val jsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react:${`kotlin-react-version`}")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom:${`kotlin-react-version`}")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-styled:${`kotlin-styled-version`}")
            }
        }
        val jsTest by getting
    }
}

application{
    mainClass.set("ServerKt")
}

tasks {
    "run"(JavaExec::class) {
        environment("CHROME_BIN","F:\\ChromeUpdater\\chrome.exe")
    }
}

tasks.getByName<KotlinWebpack>("jsBrowserProductionWebpack") {
    outputFileName = "js.js"
}

tasks.getByName<Jar>("jvmJar") {
    dependsOn(tasks.getByName("jsBrowserProductionWebpack"))
    val jsBrowserProductionWebpack = tasks.getByName<KotlinWebpack>("jsBrowserProductionWebpack")
    from(File(jsBrowserProductionWebpack.destinationDirectory, jsBrowserProductionWebpack.outputFileName))
}

tasks.getByName<JavaExec>("run") {
    dependsOn(tasks.getByName<Jar>("jvmJar"))
    classpath(tasks.getByName<Jar>("jvmJar"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
    }
}

tasks.withType<ProcessResources>{
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}