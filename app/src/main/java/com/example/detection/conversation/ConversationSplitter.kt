package com.example.detection.conversation

import com.example.domain.models.ChatSender
import com.example.domain.models.ConversationMessage
import java.util.UUID

object ConversationSplitter {

    // Matches "[10:32] Name: text" or "[10:32 AM, 12/11/2025] Name: text"
    private val bracketedRegex = Regex("""^\[([^\]]+)]\s*([^:]{1,40}):\s*(.+)$""")

    // Matches "Them: text", "Me: text", "Caller: text", "User: text", "Rajesh: text"
    private val namedPrefixRegex = Regex("""^([A-Za-z0-9_.+()\-\s]{1,30}):\s*(.+)$""")

    private val meLabels = setOf("me", "you", "user", "victim", "myself", "main", "मैं")

    fun split(rawInput: String): List<ConversationMessage> {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return emptyList()

        val lines = trimmed.lines().map { it.trim() }
        val nonEmptyLines = lines.filter { it.isNotEmpty() }

        // Check if structured prefixes are used on multiple lines
        val structuredCount = nonEmptyLines.count {
            bracketedRegex.matches(it) || (namedPrefixRegex.matches(it) && !it.startsWith("http", true))
        }

        if (structuredCount >= 2 || (structuredCount == 1 && nonEmptyLines.size == 1)) {
            val result = mutableListOf<ConversationMessage>()
            var lastMsg: ConversationMessage? = null
            for (line in nonEmptyLines) {
                val bMatch = bracketedRegex.matchEntire(line)
                if (bMatch != null) {
                    val ts = bMatch.groupValues[1].trim()
                    val senderRaw = bMatch.groupValues[2].trim()
                    val text = bMatch.groupValues[3].trim()
                    val sender = classifySender(senderRaw)
                    val msg = ConversationMessage(
                        id = UUID.randomUUID().toString(),
                        sender = sender,
                        senderLabel = senderRaw,
                        text = text,
                        timestampLabel = ts
                    )
                    result.add(msg)
                    lastMsg = msg
                    continue
                }

                val nMatch = if (!line.startsWith("http", true) && !line.startsWith("upi://", true)) {
                    namedPrefixRegex.matchEntire(line)
                } else null

                if (nMatch != null) {
                    val senderRaw = nMatch.groupValues[1].trim()
                    val text = nMatch.groupValues[2].trim()
                    val sender = classifySender(senderRaw)
                    val msg = ConversationMessage(
                        id = UUID.randomUUID().toString(),
                        sender = sender,
                        senderLabel = senderRaw,
                        text = text
                    )
                    result.add(msg)
                    lastMsg = msg
                } else if (lastMsg != null) {
                    val merged = lastMsg.copy(text = "${lastMsg.text}\n$line")
                    result[result.lastIndex] = merged
                    lastMsg = merged
                } else {
                    val msg = ConversationMessage(
                        id = UUID.randomUUID().toString(),
                        sender = ChatSender.THEM,
                        senderLabel = "Them",
                        text = line
                    )
                    result.add(msg)
                    lastMsg = msg
                }
            }
            return result
        }

        // Otherwise check blank-line separated blocks
        val blocks = trimmed.split(Regex("""\n\s*\n+"""))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (blocks.size >= 2) {
            return blocks.mapIndexed { idx, block ->
                ConversationMessage(
                    id = UUID.randomUUID().toString(),
                    sender = ChatSender.THEM,
                    senderLabel = "Message ${idx + 1}",
                    text = block
                )
            }
        }

        // Finally split individual lines if multiple lines exist
        return if (nonEmptyLines.size >= 2) {
            nonEmptyLines.mapIndexed { idx, line ->
                ConversationMessage(
                    id = UUID.randomUUID().toString(),
                    sender = ChatSender.THEM,
                    senderLabel = "Message ${idx + 1}",
                    text = line
                )
            }
        } else {
            listOf(
                ConversationMessage(
                    id = UUID.randomUUID().toString(),
                    sender = ChatSender.THEM,
                    senderLabel = "Them",
                    text = trimmed
                )
            )
        }
    }

    private fun classifySender(label: String): ChatSender {
        return if (label.lowercase() in meLabels) ChatSender.ME else ChatSender.THEM
    }
}
