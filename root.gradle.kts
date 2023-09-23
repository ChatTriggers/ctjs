plugins {
    kotlin("jvm") version "1.8.21" apply false
    id("io.github.juuxel.loom-quiltflower") version "1.10.0" apply false
    id("gg.essential.multi-version.root")
    id("gg.essential.multi-version.api-validation")
}

preprocess {
    val fabric11904 = createNode("1.19.4-fabric", 11904, "yarn")
    val fabric12001 = createNode("1.20.1-fabric", 12001, "yarn")

    fabric11904.link(fabric12001, file("versions/fabric1.19.4-1.20.1.txt"))
}

apiValidation {
    ignoredPackages.add("com.chattriggers.ctjs.internal")
}
