package com.chattriggers.ctjs.engine.langs

import kotlinx.serialization.Serializable
import org.fife.ui.rsyntaxtextarea.SyntaxConstants

@Serializable
enum class Lang(val langName: String, val extension: String, val syntaxStyle: String) {
    JS("js", "js", SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT)
}
