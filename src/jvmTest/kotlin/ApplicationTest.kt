import cool.kirito.bili.live.server.plugins.configureSerialization
import io.ktor.http.*

import io.ktor.config.*
import kotlin.test.*
import io.ktor.server.testing.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import plugins.configureWebSockets


class ApplicationTest {
    @Test
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

    @Test
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
    fun testUrl(){
        println(Regex("/manga/\\w+")
            .find("https://gammaplus.takeshobo.co.jp/manga/kobito_recipe/_files/02_1/")?.value)
    }

    @Test
    fun testSlice(){
        val t="sdfsdfsdfsd=="
        println(t.slice(IntRange(t.length-2,t.length-1)))
    }
}