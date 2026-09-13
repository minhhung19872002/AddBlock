package com.hung.adskipper

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Bộ nhớ đệm vòng cho nhật ký hoạt động + kênh phát sự kiện sang Flutter.
 * Accessibility service và Activity nằm chung một tiến trình nên object tĩnh
 * là đủ, không cần Broadcast.
 */
object SkipLog {

    const val KIND_SKIP = "skip"
    const val KIND_CLOSE = "close"
    const val KIND_SERVICE_ON = "service_on"
    const val KIND_SERVICE_OFF = "service_off"

    private const val MAX_ENTRIES = 120

    data class Entry(
        val time: Long,
        val kind: String,
        val label: String,
        val packageName: String,
    ) {
        fun toMap(): Map<String, Any?> = mapOf(
            "time" to time,
            "kind" to kind,
            "label" to label,
            "package" to packageName,
        )
    }

    private val entries = ArrayDeque<Entry>()
    private val listeners = CopyOnWriteArrayList<(Entry) -> Unit>()

    fun add(kind: String, label: String, packageName: String) {
        val entry = Entry(System.currentTimeMillis(), kind, label, packageName)
        synchronized(entries) {
            entries.addFirst(entry)
            while (entries.size > MAX_ENTRIES) entries.removeLast()
        }
        listeners.forEach { runCatching { it(entry) } }
    }

    fun snapshot(): List<Map<String, Any?>> = synchronized(entries) {
        entries.map { it.toMap() }
    }

    fun clear() = synchronized(entries) { entries.clear() }

    fun addListener(listener: (Entry) -> Unit) { listeners.add(listener) }

    fun removeListener(listener: (Entry) -> Unit) { listeners.remove(listener) }
}
