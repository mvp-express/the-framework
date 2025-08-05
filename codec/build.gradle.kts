plugins {
    java
}

dependencies {
    // SBE (Simple Binary Encoding) for high-performance message serialization
    implementation("uk.co.real-logic:sbe-all:1.30.0")
    
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}