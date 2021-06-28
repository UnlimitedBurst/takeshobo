
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import kotlin.test.Ignore
import kotlin.test.Test


class JsTest {

    @Ignore
    fun testRS() {
        val f = Regex("^([^:]+):(\\d+),(\\d+)\\+(\\d+),(\\d+)>(\\d+),(\\d+)\$").matchEntire("i:574,4+106,150>106,600")
        println(f?.groupValues)
    }

    private fun createUrlResult(): UrlResult {
        return UrlResult(
            originImagePath = "", serverImagePath = "",
            t = t(
                ptimg_version = 6666,
                resources = resources(i = i(src = "", width = 0, height = 0)),
                views = listOf()
            ),
            romajiTitle = "",
            filename = ""
        )
    }

    @Ignore
    fun testUnitArray() {
        ImageLoader(createUrlResult())
            .apply {
                m.forEach {
                    println("m;${it}")
                }
            }
    }

    @Test
    fun testJSON() {

        val urlResult = createUrlResult()

        globalJson.encodeToString(WebSocketServer(dataType = WebSocketResType.Image.name, body = urlResult)).apply {
            println("序列化：$this")
            globalJson.decodeFromString<WebSocketServer<UrlResult>>(this).apply {
                println("反序列化：${this.body}")
            }
            globalJson.parseToJsonElement(this).apply {
                println("type:${jsonObject.getValue("dataType")}")
                globalJson.decodeFromJsonElement(WebSocketServer.serializer(UrlResult.serializer()), this).apply {
                    println("反序列化：${this.body}")
                }
            }
        }
    }


}
