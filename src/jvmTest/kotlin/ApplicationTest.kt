import cool.kirito.bili.live.server.plugins.configureSerialization
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import plugins.configureWebSockets
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals


class ApplicationTest {
    //@Test
    fun testRoot() {
        withTestApplication({
            configureWebSockets()
            configureRouting()
            configureSerialization()
        }) {
            handleRequest(HttpMethod.Post, "/").apply {
//                assertEquals(HttpStatusCode.OK, response.status())
                environment.log.info("返回！！！！！！！！！！！！！！！:${response.content}")
            }
        }
    }

    //@Test
    fun testParse(){
        withTestApplication({
            configureWebSockets()
            configureRouting()
            configureSerialization()
        }) {
            handleRequest(HttpMethod.Post,"/api/json"){
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody("url=https://gammaplus.takeshobo.co.jp/manga/kobito_recipe/_files/02_1/")
            }
                .apply {
                assertEquals(HttpStatusCode.OK,response.status())
                environment.log.info("json:${response.content}")
            }
        }

    }

    @Test
    fun testRegex(){
        val s="<div id=\"content\" class=\"pages ptbinb-container\" data-binbsp-direction=\"rtl\" data-binbsp-toc=\"toc\" data-binbsp-recommend=\"../recommend_01/index.html#more[next] ../recommend2/[next]\">\n" +
                "\t\t\t<div data-ptimg=\"data/0001.ptimg.json\" data-binbsp-spread=\"center\" data-binbsp-anchors=\"L_book_000\"></div>\n" +
                "\t\t\t<div data-ptimg=\"data/0002.ptimg.json\" data-binbsp-spread=\"right\"></div>\n" +
                "\t\t\t<div data-ptimg=\"data/0003.ptimg.json\" data-binbsp-spread=\"left\"></div>\n" +
                "\t\t\t<div data-ptimg=\"data/0004.ptimg.json\" data-binbsp-spread=\"right\"></div>\n" +
                "\t\t\t<div data-ptimg=\"data/0005.ptimg.json\" data-binbsp-spread=\"left\"></div>"

        Regex("data/.*.json").findAll(s).forEach {
            println(it.value)
        }
    }

    @Test
    fun testNumber(){
        println(String.format("%.2f",3*100/22F))
    }

    @Test
    fun testUrl() {
        println(
            Regex("/manga/\\w+")
                .find("https://gammaplus.takeshobo.co.jp/manga/kobito_recipe/_files/02_1/")?.value
        )
    }

    @Test
    fun testSlice() {
        val t = "sdfsdfsdfsd=="
        println(t.slice(IntRange(t.length - 2, t.length - 1)))
    }

    @Test
    fun testJSON() {
        val input = "{\"dataType\":\"Text\",\"body\":123}"
        val json = Json { ignoreUnknownKeys = true }
        val f: WebSocketServerType = json.decodeFromString(input)
        println("反序列化：${f}")
        val we: WebSocketServer<Int> = Json.decodeFromString(input)
        println("反序列化：${we}")

        val s = WebSocketServer(body = "fsdfsdf", dataType = WebSocketResType.Text.name)
        println("序列化：${json.encodeToString(s)}")

        Json.encodeToString(WebSocketServer(body = "666", dataType = WebSocketResType.Text.name)).apply {
            println("序列化：${this}")
        }

        Json.encodeToString(WebSocketServer(body = "666", dataType = WebSocketResType.Text.name)).apply {
            println(this)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    //@Test
    fun testHttpClient(): Unit = runBlockingTest {
        launch {
            delay(5000)
        }
        HttpClient(CIO).apply {
            val httpResponse: HttpResponse = get("https://gammaplus.takeshobo.co.jp/manga/kobito_recipe/_files/02_1")
            println(httpResponse.status)
        }

    }

    @Test
    fun testSize() {
        File("E:\\JetBrains\\IdeaProjects\\manga\\build\\processedResources\\jvm\\main\\static\\zip\\kobito_recipe.zip")
            .apply {
                println(String.format("%.2fMB", length() / 1024 / 1024F))
            }

    }

}
