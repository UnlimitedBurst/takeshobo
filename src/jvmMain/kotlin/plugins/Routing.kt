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
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.html.HTML
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


const val website = "https://gammaplus.takeshobo.co.jp"

//漫画url
val urlChannel = Channel<String>()

//当前漫画解析任务
var currentJob: Job? = null

//合成图片打包数据
val imageDataChannel = Channel<ImageFileData>()

val webSocketChannel = Channel<TestData>()

//图片JSON解析
fun Application.parse() {
    launch {
        log.info("初始化解析任务")

        val uploadDir = environment.config.property("ktor.deployment.filePath").getString()
        val filePath = environment.classLoader.getResource(uploadDir)?.path.apply { log.info("图片存储目录：${this}") }
            ?: throw IllegalArgumentException("图片存储目录初始化失败")

        while (true) {
            val urlParam = urlChannel.receive()

//
//                currentJob?.let {
//                    if(it.isActive) this.cancel()
//                }

            launch {
                log.info("开始解析:${urlParam}")
                val client = HttpClient(CIO)
                val response: HttpResponse = client.get(urlParam)
                if (response.status == HttpStatusCode.OK) {
                    val resHtml = response.readText()
//                        log.info("resHtml:\n$resHtml")
                    val titlePrefix = "title>"
                    val title =
                        Regex("$titlePrefix[\\u4e00-\\u9fa5\\u0800-\\u4e00\\s\\d\\uff00-\\uffef]+").find(resHtml)?.value?.replace(
                            titlePrefix,
                            ""
                        ) ?: throw IllegalArgumentException("无法解析漫画标题")
                    val romajiPrefix = "/manga/"
                    val romajiTitle = Regex("$romajiPrefix\\w+").find(resHtml)?.value?.replace(romajiPrefix, "")
                        ?: throw IllegalArgumentException("无法解析漫画罗马音标题")
                    log.info("漫画标题title=$title,romajiTitle=$romajiTitle")
                    webSocketChannel.send(MangaInfo(href = urlParam, title = title, romajiTitle = romajiTitle))
                    val imageDir = File(filePath, romajiTitle).apply { if (!exists()) mkdir() }
                    log.info("漫画图片存储到:${imageDir.absolutePath}")

                    Regex("data/.*.json").findAll(resHtml)
                        .apply {
                            withIndex().forEach {
                                log.info("开始解析：${it.value.value}")
                                val urlPath = "${urlParam}/${it.value.value}"
                                val jsonRes: HttpResponse = client.get(urlPath)
                                if (jsonRes.status == HttpStatusCode.OK) {
                                    log.info("$urlPath 请求成功")
                                    val t: t = Json.decodeFromString(jsonRes.readText())
                                    val originImagePath = "/data/${t.resources.i.src}"
                                    val imageUrl = "${urlParam}${originImagePath}"
                                    val imgRes: HttpResponse = client.get(imageUrl)
                                    if (imgRes.status == HttpStatusCode.OK && imgRes.contentType() == ContentType.Image.JPEG) {
                                        log.info("$imageUrl 请求成功")
                                        val filename = "${it.index + 1}.jpg"
                                        val file = File(imageDir, filename).apply {
                                            writeBytes(imgRes.readBytes())
                                        }
                                        log.info("存储漫画图片：${file.absolutePath}")
                                        val serverImagePath = "/${uploadDir}/${romajiTitle}/${filename}"
                                        log.info("serverImagePath:${serverImagePath}")
                                        webSocketChannel.send(
                                            UrlResult(
                                                originImagePath = originImagePath,
                                                t = t,
                                                serverImagePath = serverImagePath,
                                                romajiTitle = romajiTitle,
                                                filename = filename,
                                                isLast = count() == it.index + 1
                                            )
                                        )
                                    } else {
                                        log.warn("image url:${imageUrl} 响应码${jsonRes.status} 响应类型${imgRes.contentType()}")
                                    }
                                } else {
                                    log.warn("json url:${urlPath} 响应码:${jsonRes.status}")
                                }
                                webSocketChannel.send(
                                    ParseTask(
                                        total = count(),
                                        finish = it.index + 1,
                                        percentage = if (count() == it.index + 1) 100F else String.format(
                                            "%.2f",
                                            (it.index + 1) * 100F / count()
                                        ).toFloat()
                                    )
                                )
                            }
                        }
                } else {
                    log.warn("http code:${response.status}")
                    webSocketChannel.send(StringResult(message = "${urlParam}解析失败"))
                }
                client.close()
            }.apply {
                currentJob = this
                invokeOnCompletion {
                    log.info("${urlParam}解析完成")
                }
            }

        }
    }
    launch {
        log.info("初始化合成图片任务")

        val zipDir = environment.config.property("ktor.deployment.zipPath").getString()
        val zipPath = environment.classLoader.getResource(zipDir)?.path.apply { log.info("图片压缩目录：${this}") }
            ?: throw IllegalArgumentException("图片压缩目录初始化失败")


        while (true) {
            val imageFileData = imageDataChannel.receive()
            File(zipPath, imageFileData.romajiTitle).apply {
                imageFileData.data.apply {
                    launch(Dispatchers.IO) {
                        File(zipPath, "${imageFileData.romajiTitle}.zip").apply {
                            ZipOutputStream(FileOutputStream(this)).use { out ->
                                forEach { file ->

                                    out.putNextEntry(
                                        ZipEntry(
                                            file.originalFileName ?: throw IllegalArgumentException("无法获取图片名字")
                                        )
                                    )
                                    out.write(file.streamProvider().readBytes())
                                    out.closeEntry()
                                }
                            }
                            webSocketChannel.send(
                                ZipResult(
                                    zipUrl = "/${zipDir}/${name}",
                                    name = name,
                                    size = String.format("%.2fMB", length() / 1024 / 1024F)
                                )
                            )
                            webSocketChannel.send(StringResult(message = "打包任务完成"))
                        }
                    }
                }
            }
        }
    }

}

fun createFrameText(message: String): Frame.Text {
    return Frame.Text(Json.encodeToString(WebSocketServer(body = message, dataType = WebSocketResType.Text.name)))
}

fun Application.configureRouting() {

    routing {
        val uploadDir = environment.config.property("ktor.deployment.filePath").getString()
        static(uploadDir) {
            resources(uploadDir)
        }
        val zipDir = environment.config.property("ktor.deployment.zipPath").getString()
        static(zipDir) {
            resources(zipDir)
        }

        static("/static") {
            resources()
        }

        webSocket(Api.websocketPath) { // websocketSession
            launch {
                while (true) {
                    when (val data = webSocketChannel.receive()) {
                        is StringResult -> outgoing.send(createFrameText(data.message))
                        is UrlResult -> outgoing.send(
                            Frame.Text(
                                Json.encodeToString(
                                    WebSocketServer(
                                        body = data,
                                        dataType = WebSocketResType.Image.name
                                    )
                                )
                            )
                        )
                        is MangaInfo -> outgoing.send(
                            Frame.Text(
                                Json.encodeToString(
                                    WebSocketServer(
                                        body = data,
                                        dataType = WebSocketResType.Manga.name
                                    )
                                )
                            )
                        )
                        is ZipResult -> outgoing.send(
                            Frame.Text(
                                Json.encodeToString(
                                    WebSocketServer(
                                        body = data,
                                        dataType = WebSocketResType.Zip.name
                                    )
                                )
                            )
                        )
                        is ParseTask -> outgoing.send(
                            Frame.Text(
                                Json.encodeToString(
                                    WebSocketServer(
                                        body = data,
                                        dataType = WebSocketResType.Task.name
                                    )
                                )
                            )
                        )
                    }
                }
            }

            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val input = frame.readText()
                        try {
                            val webSocketMessage: WebSocketClient = Json.decodeFromString(input)
                            when (webSocketMessage.command) {
                                WebSocketClientCommand.Heart.name -> {
                                    outgoing.send(
                                        Frame.Text(
                                            Json.encodeToString(
                                                WebSocketServer(dataType = WebSocketResType.Heart.name, body = "心跳返回")
                                            )
                                        )
                                    )
                                }
                                WebSocketClientCommand.Cancel.name -> {
                                    if (currentJob?.isActive == true) {
                                        currentJob?.cancel()
                                        outgoing.send(
                                            Frame.Text(
                                                Json.encodeToString(
                                                    WebSocketServer(
                                                        dataType = WebSocketResType.Cancel.name,
                                                        body = true
                                                    )
                                                )
                                            )
                                        )
                                    } else {
                                        outgoing.send(createFrameText(message = "当前服务器没有正在运行的解析任务"))
                                    }
                                }
                                else -> outgoing.send(createFrameText(message = "未知命令：${webSocketMessage.command}"))
                            }
                        } catch (e: Exception) {
                            log.error(e)
                            outgoing.send(Frame.Text("非法命令：${input}"))
                        }
                    }
                    is Frame.Close -> {
                        currentJob?.cancel()
                        log.info("取消解析任务")
                    }
                    else -> log.warn("无法处理${frame.frameType}类型消息")
                }
            }
        }



        get("/") {
            call.respondHtml(HttpStatusCode.OK, HTML::index)
        }

        //打包漫画图片
        post(Api.IMAGE_API) {
            val d = call.receiveMultipart()
            d.readAllParts().apply {
                var romajiTitle = ""
                filterIsInstance<PartData.FormItem>().forEach {
                    if (it.name == "romajiTitle") romajiTitle = it.value
                }
                val fileList = filterIsInstance<PartData.FileItem>()
                if (romajiTitle.isNotEmpty() && fileList.isNotEmpty()) {
                    launch {
                        imageDataChannel.send(ImageFileData(romajiTitle = romajiTitle, data = fileList))
                    }
                    call.respond(MessageResponse(message = "初始化打包任务。。。"))
                } else {
                    log.warn("无法打包漫画图片，参数不合法[romajiTitle:${romajiTitle},fileList=${fileList}]")
                    call.respond(MessageResponse(message = "无法打包漫画图片，请联系管理员"))
                }
            }
        }

        //解析漫画图片数据
        post(Api.JSON_API) {
            val formParameters = call.receiveParameters()
            val urlParam = formParameters["url"] ?: ""
            log.info("urlParam:${urlParam}")
            if (urlParam.startsWith(website)) {
                launch {
                    urlChannel.send(urlParam.let { if (it.endsWith("/")) it else "$it/" })
                }
                call.respond(MessageResponse(message = "初始化解析任务。。。"))
            } else {
                call.respond(MessageResponse(message = "${urlParam}:非法漫画地址"))
            }
        }
    }
}
