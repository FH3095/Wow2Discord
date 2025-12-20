plugins {
    id("java")
}

group = "eu.4fh"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("eu.4fh:abstract-bnet-api") {
        version {
            branch = "main"
        }
    }
    implementation("com.google.guava:guava:33.5.0-jre")
    implementation("net.dv8tion:JDA:6.2.+") {
        exclude(module = "opus-java")
        exclude(module = "tink")
    }
    implementation("com.zaxxer:HikariCP:7.0.+")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.+")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
