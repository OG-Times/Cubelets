plugins {
    id("java")
}

group = "me.joseph.cubelets"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven {
        name = "refinePublicRepo"
        url = uri("https://maven.refinedev.org/public-repo")
    }

    maven {
        name = "codemc-repo"
        url = uri("https://repo.codemc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("org.paperspigot:PaperSpigot:1.8.8")
    compileOnly("me.filoghost.holographicdisplays:holographicdisplays-api:3.0.0")
    compileOnly("org.projectlombok:lombok:1.18.42")

    annotationProcessor("org.projectlombok:lombok:1.18.42")
}