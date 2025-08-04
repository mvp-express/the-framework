plugins {
    java
    application
}

application {
    mainClass.set("express.mvp.example.ServerMain")
}

dependencies {
    implementation(project(":runtime"))
    implementation(project(":transport"))
    implementation(project(":codec"))
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}