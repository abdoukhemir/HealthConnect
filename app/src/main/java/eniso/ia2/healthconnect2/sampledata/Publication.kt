package eniso.ia2.healthconnect2.sampledata

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Publication(
    val id: String,
    val name: String,
    val content: String,
    val visible: Boolean = true,
    val Date: String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")),
    val signals: MutableList<Signal> = mutableListOf() // Add signals list
)