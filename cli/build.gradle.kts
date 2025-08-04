plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("express.mvp.cli.MainKt")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}