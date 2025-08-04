plugins {
    java
    kotlin("jvm") version "1.9.0" apply false // Just for CLI/codegen
}

group = "express.mvp"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}


tasks.register("mvpegen") {
    group = "mvp.express"
    description = "Run the MVP.Express code generator on mvpe.schema.yaml"

    doLast {
        javaexec {
            mainClass.set("express.mvp.cli.MainKt")
            classpath = sourceSets["main"].runtimeClasspath
            args = listOf("generate", "mvpe.schema.yaml", "-o", "examples/account-service/src/main/java")
        }
    }
}