import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.html.HTML
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File


const val website = "https://gammaplus.takeshobo.co.jp"

var taskChannel = Channel<ParseTask>()
val urlResultChannel = Channel<UrlResult>()
val htmlChannel= Channel<UrlParam>()

fun Application.parse(){
    log.info("初始化解析任务")

    val uploadDir = environment.config.property("ktor.deployment.filePath").getString()
    val filePath = environment.classLoader.getResource(uploadDir)?.path.apply { log.info("图片存储目录：${this}") }?:throw IllegalArgumentException("图片存储目录初始化失败")

    launch {
        while (true){
            val resHtml= htmlChannel.receive()
            launch {
                log.info("开始解析:${resHtml.url}")
                val client = HttpClient(CIO)

                val titlePrefix="title>"
                val title=Regex("$titlePrefix[\\u4e00-\\u9fa5\\u0800-\\u4e00\\s\\d\\uff00-\\uffef]+").find(resHtml.html)?.value?.replace(titlePrefix,"")?:throw IllegalArgumentException("无法解析漫画标题")
                val romajiPrefix="/manga"
                val romajiTitle=Regex("$romajiPrefix/\\w+").find(resHtml.url)?.value?.replace(romajiPrefix,"")?:throw IllegalArgumentException("无法解析漫画罗马音标题")
                log.info("解析漫画标题")
                val imageDir= File(filePath,romajiTitle).apply { if(!exists()) mkdir() }
                log.info("漫画图片存储到:${imageDir.absolutePath}")

                Regex("data/.*.json").findAll(resHtml.html)
                    .apply {
                        withIndex()
//TODO                            .forEach {
                            .first().let {
                            log.info("开始解析：${it.value.value}")
                            val urlPath = "${resHtml.url}/${it.value.value}"
                            val jsonRes: HttpResponse = client.get(urlPath)
                            if (jsonRes.status == HttpStatusCode.OK) {
                                log.info("url:${urlPath} request OK")
                                val t:t=Json.decodeFromString(jsonRes.readText())
                                val originImagePath="/data/${t.resources.i.src}"
                                val imageUrl="${resHtml.url}${originImagePath}"
                                val imgRes:HttpResponse=client.get(imageUrl)
                                if(imgRes.status==HttpStatusCode.OK&&imgRes.contentType()==ContentType.Image.JPEG){
                                    val filename="${it.index}.jpg"
                                    val file=File(imageDir,filename).apply {
                                        writeBytes(imgRes.readBytes())
                                    }
                                    log.info("存储漫画图片：${file.absolutePath}")
                                    val serverImagePath="/${uploadDir}/${romajiTitle}/${filename}"
                                    log.info("serverImagePath:${serverImagePath}")
                                    urlResultChannel.send(UrlResult(originImagePath = originImagePath, t = t,
                                    serverImagePath = serverImagePath))
                                }else{
                                    log.warn("image url:${imageUrl} 响应码${jsonRes.status} 响应类型${imgRes.contentType()}")
                                }
                            } else {
                                log.warn("json url:${urlPath} 响应码:${jsonRes.status}")
                            }
                            taskChannel.send(ParseTask(total = count(), finish = it.index + 1,percentage = if(count()==it.index+1) 100F else String.format("%.2f",(it.index+1)*100F/count()).toFloat()))
                        }
                    }
                client.close()
            }.apply {
                invokeOnCompletion {
                    log.info("${resHtml.url}解析完成")
                }
            }
        }
    }

}

fun Application.configureRouting() {

    routing {
        val uploadDir=environment.config.property("ktor.deployment.filePath").getString()
        static(uploadDir) {
            resources(uploadDir)
        }

        static("/static") {
            resources()
        }

        webSocket(websocketPath) { // websocketSession
            launch {
                while (true) {
                    val urlResult = urlResultChannel.receive()
                    outgoing.send(Frame.Text(Json.encodeToString(urlResult)))
                }
            }
            launch {
                while (true){
                    val task=taskChannel.receive()
                    outgoing.send(Frame.Text(Json.encodeToString(task)))
                }
            }
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        outgoing.send(Frame.Text("YOU SAID: $text"))
                        if (text.equals("bye", ignoreCase = true)) {
                            close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                        }
                    }
                }
            }
        }

        get("/") {
            call.respondHtml(HttpStatusCode.OK, HTML::index)
        }

        post("/api/json") {
            val formParameters = call.receiveParameters()
            val urlParam = formParameters["url"] ?: ""
            log.info("urlParam:${urlParam}")
            if (urlParam.startsWith(website)) {
                val client = HttpClient(CIO)
                val response: HttpResponse = client.get(urlParam)
                if (response.status == HttpStatusCode.OK) {
                    val resHtml = response.readText()
                    htmlChannel.send(UrlParam(url = urlParam,html = resHtml))
                    call.respond(MessageResponse(message = "开始执行解析任务"))
                } else {
                    log.warn("http code:${response.status}")
                    call.respond(MessageResponse(message = "请求失败"))
                }
                client.close()
            } else {
                call.respond(MessageResponse(message = "${urlParam}:非法漫画地址"))
            }
        }
    }
}
