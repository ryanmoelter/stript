import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    application
}

group = "com.ryanmoelter"
version = "0.2.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
    }
}

application {
    mainClass.set("MainKt")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")

    // Pins and wiring
    implementation("com.diozero:diozero-core:1.3.1")

    // Server
    val ktorVersion = "2.0.0-beta-1"
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-cio:$ktorVersion")
    implementation("io.ktor:ktor-network:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:1.2.10")

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("io.strikt:strikt-core:0.33.0")
    testImplementation("io.mockk:mockk:1.12.2")
}

//task("createProperties") {
//    dependsOn("processResources")
//    doLast {
//        mkdir("$buildDir/resources/main")
//        val file = File("$buildDir/resources/main/version.properties")
//        file.createNewFile()
//        file.writer().use { w ->
//            val p = java.util.Properties()
//            p["version"] = project.version.toString()
//            p.store(w, null)
//        }
//    }
//}

//classes {
//    dependsOn createProperties
//}
