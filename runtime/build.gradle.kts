plugins {
    java
}

dependencies {
    implementation(project(":transport"))
    implementation(project(":codec"))
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}