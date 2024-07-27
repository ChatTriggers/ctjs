package com.chattriggers.ctjs.api.triggers

import com.chattriggers.ctjs.api.message.TextComponent
import org.mozilla.javascript.regexp.NativeRegExp

class ChatTrigger(method: Any, type: ITriggerType) : Trigger(method, type) {
    private var formatted: Boolean = false
    private lateinit var criteriaPattern: Regex
    private var usingCriteria = false
    private var global = false

    private var startsWith: Regex? = null
    private val contains = mutableSetOf<Regex>()
    private var endsWith: Regex? = null

    /**
     * Creates a regex string and flags according to the criteria
     */
    private fun createCriteria(criteria: Any): Pair<String, Set<RegexOption>> {
        var source = ".+"
        val flags = mutableSetOf<RegexOption>()

        when (criteria) {
            is CharSequence -> {
                val chatCriteria = criteria.toString()

                if ("\n" in chatCriteria)
                    flags.add(RegexOption.DOT_MATCHES_ALL)

                val replacedCriteria = chatCriteria.replace("\n", "\\n")
                    .replace(Regex("""\$\{[^*]+?}"""), "(.+?)")
                    .replace(Regex("""\$\{\*?}"""), "(?:.+?)")

                if ("" != chatCriteria)
                    source = replacedCriteria
            }
            is NativeRegExp -> {
                if (criteria["ignoreCase"] as Boolean)
                    flags.add(RegexOption.IGNORE_CASE)

                if (criteria["multiline"] as Boolean)
                    flags.add(RegexOption.MULTILINE)

                if (criteria["dotAll"] as Boolean)
                    flags.add(RegexOption.DOT_MATCHES_ALL)

                source = (criteria["source"] as String).let {
                    if ("" == it) ".+" else it
                }
            }
            else -> throw IllegalArgumentException("Expected String or Regexp Object")
        }

        formatted = formatted or (Regex("[&\u00a7]") in source)
        return source to flags
    }

    /**
     * Sets the chat criteria for this trigger.
     * Arguments for the trigger's method can be passed in using ${variable}.
     * Example: `setCriteria("<${name}> ${message}");`
     * Use ${*} to match a chat message but ignore the pass through.
     * @param criteria the chat criteria to set
     * @return the trigger object for method chaining
     */
    fun setCriteria(criteria: Any) = apply {
        check(startsWith == null && contains.isEmpty() && endsWith == null) {
            "Can not use setCriteria() with any of startsWith(), contains(), or endsWith()"
        }

        usingCriteria = true
        val (source, flags) = createCriteria(criteria)

        if (criteria is NativeRegExp && criteria["global"] as Boolean)
            global = true

        criteriaPattern = Regex("^${source}\$", flags)
    }

    /**
     * Sets the starting criteria for this trigger. In order for this trigger to run, the beginning of
     * the chat message must match [criteria].
     * Like [setCriteria], arguments can be passed in using ${variable}.
     * @return the trigger object for method chaining
     */
    fun startsWith(criteria: Any) = apply {
        check(!usingCriteria) { "Can not use both setCriteria() and startsWith()" }

        val (source, flags) = createCriteria(criteria)
        startsWith = Regex("^${source}", flags)
    }

    /**
     * Sets criteria this trigger must contain. In order for this trigger to run, the beginning of
     * the chat message must contain **all** [criteria].
     * Like [setCriteria], arguments can be passed in using ${variable}.
     * @return the trigger object for method chaining
     */
    fun contains(vararg criteria: Any) = apply {
        check(!usingCriteria) { "Can not use both setCriteria() and contains()" }

        for (criterion in criteria) {
            val (source, flags) = createCriteria(criterion)
            contains += Regex(source, flags)
        }
    }

    /**
     * Sets the ending criteria for this trigger. In order for this trigger to run, the end of
     * the chat message must match [criteria].
     * Like [setCriteria], arguments can be passed in using ${variable}.
     * @return the trigger object for method chaining
     */
    fun endsWith(criteria: Any) = apply {
        check(!usingCriteria) { "Can not use both setCriteria() and endsWith()" }

        val (source, flags) = createCriteria(criteria)
        endsWith = Regex("${source}\$", flags)
    }

    /**
     * Argument 0 (Event) The chat message event
     * @param args list of arguments as described
     */
    override fun trigger(args: Array<out Any?>) {
        require(args[0] is Event) {
            "Argument 1 must be a ChatTrigger.Event"
        }

        val chatEvent = args[0] as Event
        if (chatEvent.isCancelled()) return

        val chatMessage = getChatMessage(chatEvent.message)

        val variables = getVariables(chatMessage) ?: return
        callMethod((variables + chatEvent).toTypedArray())
    }

    // helper method to get the proper chat message based on the presence of color codes
    private fun getChatMessage(chatMessage: TextComponent) =
        if (formatted)
            chatMessage.formattedText.replace("\u00a7", "&")
        else chatMessage.unformattedText

    /**
     * A method to check whether a received chat message
     * matches this trigger's definition criteria.
     * Ex. "FalseHonesty joined Cops vs Crims" would match `${playername} joined ${gamejoined}`
     * @param chat the chat message to compare against
     * @return a list of the variables, in order or null if it doesn't match
     */
    private fun getVariables(chat: String): List<String>? {
        if (usingCriteria && ::criteriaPattern.isInitialized) {
            if (global)
                return criteriaPattern.findAll(chat).flatMap {
                    it.groupValues.drop(1)
                }.toList().ifEmpty { null }

            return criteriaPattern.find(chat)?.groupValues?.drop(1)
        }

        val variables = mutableListOf<String>()
        var start = 0
        var end = chat.length

        if (startsWith != null) {
            val matcher = startsWith!!.find(chat) ?: return null
            start = matcher.range.last + 1
            matcher.groupValues.drop(1).let { variables += it }
        }

        if (endsWith != null) {
            var subStart = chat.length - 1
            var matcher: MatchResult? = null

            while (subStart >= 0) {
                matcher = endsWith!!.find(chat.substring(subStart))
                if (matcher == null) {
                    subStart--
                } else {
                    break
                }
            }

            if (matcher == null) return null

            end = subStart
            matcher.groupValues.drop(1).let { variables += it }
        }

        for (contain in contains) {
            contain.find(chat.substring(start, end))?.groupValues?.drop(1)?.let {
                variables += it
            } ?: return null
        }

        return variables
    }

    class Event(@JvmField val message: TextComponent) : CancellableEvent()
}
