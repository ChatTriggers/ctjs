package com.chattriggers.ctjs.internal.engine.module

internal object ModuleDependencyResolver {
    fun sort(modules: Collection<Module>): List<Module> {
        val byName = modules.associateBy { it.name.lowercase() }
        require(byName.size == modules.size) { "Duplicate module names are not supported" }

        val result = mutableListOf<Module>()
        val permanent = mutableSetOf<String>()
        val path = mutableListOf<String>()

        fun visit(module: Module) {
            val key = module.name.lowercase()
            if (key in permanent)
                return
            val cycleStart = path.indexOf(key)
            if (cycleStart >= 0) {
                val cycle = (path.drop(cycleStart) + key).joinToString(" -> ") { byName.getValue(it).name }
                error("Detected a module dependency cycle: $cycle")
            }

            path += key
            module.metadata.requires.orEmpty().forEach { dependencyName ->
                val dependency = byName[dependencyName.lowercase()]
                    ?: error("Module ${module.name} requires missing module $dependencyName")
                visit(dependency)
            }
            path.removeAt(path.lastIndex)
            permanent += key
            result += module
        }

        modules.forEach(::visit)
        return result
    }
}
