import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    application
}

group = "com.ryanmoelter"
version = "0.1-SNAPSHOT"

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
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClass.set("MainKt")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.31")
    testImplementation(group = "junit", name = "junit", version = "4.12")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")

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
