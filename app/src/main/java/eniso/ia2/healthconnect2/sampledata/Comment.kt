package eniso.ia2.healthconnect2.sampledata

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Comment(
    val id: String,
    val text: String,
    var idUser:String,
    val Date:String= LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))

)
