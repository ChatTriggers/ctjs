package com.chattriggers.ctjs.typing.annotations

/**
 * Indicates a type that is exposed to the JS runtime
 *
 * @param name An override for the class name, defaults to the name of the
 *             annotated class
 * @param singleton An override for whether the class is a singleton, default
 *                  to true if the class is an object, false otherwise
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class CTApi(val name: String = "", val singleton: Boolean = false)

fun CTApi.useClassName() = name.isEmpty()
