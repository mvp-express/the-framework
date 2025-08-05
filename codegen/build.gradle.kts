plugins {
    java
}

dependencies {
    implementation(project(":runtime"))
    // YAML parsing for .mvpe.yaml schema files
    implementation("org.yaml:snakeyaml:2.2")
    
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}