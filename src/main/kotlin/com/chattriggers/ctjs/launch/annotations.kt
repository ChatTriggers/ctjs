
package com.chattriggers.ctjs.launch

/*
 * Annotation classes present some difficulties, especially when trying to
 * create them from an embedded engine context. They've been more-or-less
 * recreated here as normal classes for the loaders to use
 */

data class Mixin(
    val target: String,
    val priority: Int?,
    val remap: Boolean?,
)

data class At(
    val value: String,
    val id: String?,
    val slice: String?,
    val shift: Shift?,
    val by: Int?,
    val args: List<String>?,
    val target: String?,
    val ordinal: Int?,
    val opcode: Int?,
    val remap: Boolean?,
) {
    enum class Shift {
        NONE,
        BEFORE,
        AFTER,
        BY,
    }
}

data class Slice(
    val id: String?,
    val from: At?,
    val to: At?,
)

// There are three ways to capture a local:
//   - By name, only if the local is a parameter: Local(parameterName = "foo")
//   - By absolute local index: Local(index = 4)
//   - By ordinal and type: Local(type = "F", ordinal = 1)
data class Local(
    val print: Boolean?,
    val parameterName: String?,
    val index: Int?,
    val ordinal: Int?,
    val type: String?,
    val mutable: Boolean?,
)

sealed interface IInjector

data class Inject(
    val method: String,
    val id: String?,
    val slice: List<Slice>?,
    val at: List<At>?,
    val cancellable: Boolean?,
    val locals: List<Local>?,
    val remap: Boolean?,
    val require: Int?,
    val expect: Int?,
    val allow: Int?,
    val constraints: String?,
) : IInjector

data class Redirect(
    val method: String,
    val slice: Slice?,
    val at: At,
    val locals: List<Local>?,
    val remap: Boolean?,
    val require: Int?,
    val expect: Int?,
    val allow: Int?,
    val constraints: String?,
) : IInjector

data class ModifyArg(
    val method: String,
    val slice: Slice?,
    val at: At,
    val index: Int,
    val captureAllParams: Boolean?,
    val locals: List<Local>?,
    val remap: Boolean?,
    val require: Int?,
    val expect: Int?,
    val allow: Int?,
    val constraints: String?,
) : IInjector

data class ModifyArgs(
    val method: String,
    val slice: Slice?,
    val at: At,
    val locals: List<Local>?,
    val remap: Boolean?,
    val require: Int?,
    val expect: Int?,
    val allow: Int?,
    val constraints: String?,
) : IInjector

data class ModifyExpressionValue(
    val method: String,
    val at: At,
    val slice: List<Slice>?,
    val locals: List<Local>?,
    val remap: Boolean?,
    val require: Int?,
    val expect: Int?,
    val allow: Int?,
) : IInjector
