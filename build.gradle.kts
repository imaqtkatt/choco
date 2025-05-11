plugins {
    id("java")
}

group = "com.github.imaqtkatt"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.ow2.asm:asm:9.8")
}

tasks.test {
    useJUnitPlatform()
}