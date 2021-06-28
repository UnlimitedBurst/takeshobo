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
data class t(
    @SerialName("ptimg-version") val ptimg_version: Int,
    val resources: resources, val views: List<view>
)

@Serializable
data class MessageResponse(val message: String)

sealed class TestData

//漫画图片块解析结果
@Serializable
data class UrlResult(
    val originImagePath: String,
    val serverImagePath: String,
    val t: t,
    val romajiTitle: String,
    val filename: String,
    val isLast: Boolean = false
) : TestData()

//漫画图片解析进度
@Serializable
data class ParseTask(val total: Int, val finish: Int, val percentage: Float) : TestData()

@Serializable
enum class WebSocketClientCommand {
    //取消解析任务
    Cancel,

    //心跳
    Heart
}

@Serializable
enum class WebSocketResType {
    //普通消息
    Text,

    //任务进度
    Task,

    //漫画数据
    Image,

    //漫画压缩包
    Zip,

    //漫画信息
    Manga,

    //取消任务
    Cancel,

    //心跳
    Heart
}

@Serializable
data class WebSocketClient(val command: String)

@Serializable
data class WebSocketServerType(var dataType: String)

@Serializable
class WebSocketServer<T>(var dataType: String, val body: T)

//漫画信息
@Serializable
data class MangaInfo(val title: String, val romajiTitle: String, val href: String) : TestData()

//漫画打包信息
@Serializable
data class ZipResult(val zipUrl: String, val name: String, val size: String) : TestData()

data class StringResult(val message: String) : TestData()

const val websiteTitle = "朴实无华的takeshobo漫画解析工具"

