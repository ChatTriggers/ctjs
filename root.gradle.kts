plugins {
    kotlin("jvm") version "1.8.21" apply false
    id("io.github.juuxel.loom-quiltflower") version "1.10.0" apply false
    id("gg.essential.multi-version.root")
    id("gg.essential.multi-version.api-validation")
}

preprocess {
    val fabric11904 = createNode("1.19.4-fabric", 11904, "yarn")
    val fabric12004 = createNode("1.20.4-fabric", 12004, "yarn")

    fabric11904.link(fabric12004, file("versions/fabric1.19.4-1.20.4.txt"))
}

apiValidation {
    ignoredPackages.add("com.chattriggers.ctjs.internal")
}

tasks.register("generateDokkaDocs") {
    group = "documentation"

    val mainProjectName = rootProject.file("versions/mainProject").readText().trim()
    val mainProject = subprojects.first { mainProjectName in it.name }
    dependsOn(mainProject.tasks["dokkaHtml"])

    doLast {
        val dest = rootProject.file("build/javadocs/")
        dest.deleteRecursively()
        mainProject.file("build/javadocs/").copyRecursively(dest)
    }
}
