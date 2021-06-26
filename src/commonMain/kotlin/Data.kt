import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class i(val src: String, val width: Int, val height: Int)

@Serializable
data class resources(val i: i)

@Serializable
data class area(val href: String, val left: Int, val top: Int, val right: Int, val bottom: Int)

@Serializable
data class view(val width: Int, val height: Int, val coords: List<String>, val areas: List<area>? = null)

@Serializable
data class t(@SerialName("ptimg-version") val ptimg_version: Int,
             val resources: resources, val views: List<view>)

@Serializable
data class ApiResponse<T>(val message:String,@Contextual val body:T?=null)

@Serializable
data class MessageResponse(val message: String)

@Serializable
data class UrlResult(val originImagePath:String,val serverImagePath:String, val t:t)

@Serializable
data class ParseTask(val total:Int,val finish:Int,val percentage:Float)

data class UrlParam(val url:String,val html:String)

const val websocketPath="/webSocket"

const val websiteTitle="朴实无华的takeshobo漫画解析工具"