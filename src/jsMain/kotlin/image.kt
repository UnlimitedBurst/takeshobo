import kotlinext.js.getOwnPropertyNames
import kotlinx.browser.document
import kotlinx.browser.window
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.DataView
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.set
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.Image
import org.w3c.dom.url.URL
import org.w3c.fetch.RequestInit
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.xhr.FormData
import kotlin.js.Promise
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

data class un(val width: Int, val height: Int)


data class coord(
    val resid: String,
    val xsrc: Int,
    val ysrc: Int,
    val width: Int,
    val height: Int,
    val xdest: Int,
    val ydest: Int
)

data class formatview(val width: Int, val height: Int, val coords: List<coord>, val areas: List<area>? = null)

data class transfer(val index:Int,val coords: List<coord>)

data class n(val width: Int,val height: Int,val transfers:List<transfer>)

data class N(
    val left: Int, val top: Int, val width: Int, val height: Int,
    val bottom: Int = top + height, val right: Int = left + width
)

object p {
    //speedbinb.js?dmy=016301:formatted:3998
    fun Rectangle(t: Int, i: Int, n: Int, r: Int): N {
        return N(left = t, top = i, width = n, height = r)
    }

    //speedbinb.js?dmy=016301:formatted:3966
    fun intersect(t:N,i:N):N?{
        val n = t.left
        val r = t.left + t.width
        val e = t.top
        val s = t.top + t.height
        val h = i.left
        val u = i.left + i.width
        val o = i.top
        val a = i.top + i.height
        if (n < u && h < r && e < a && o < s) {
            val f = max(n, h)
            val c = max(e, o)
            return N(f,c,min(r, u) - f,min(s, a) - c)
        }
        return null
    }
}

class e(val Xs: Int = 3, private val Ws: Int = 8, private val Ys: Int = 4, val un: un) {

    //speedbinb.js?dmy=016301:formatted:8848
    fun Us(t: Int): N {
        val i = this.un.height
        val n = ceil( (i + this.Ys * (this.Xs - 1)) / this.Ws.toDouble()).toInt()
        val r = ceil(t * n / this.Xs.toDouble()).toInt() * this.Ws
        val e = ceil((t + 1) * n / this.Xs.toDouble()).toInt() * this.Ws
        val s = n * this.Ws
        val h = r * i / s
        val u = e * i / s
        val o = e - r
        val a = if (1 == this.Xs) 0 else round(h + (u - h - o) * t / (this.Xs - 1).toDouble()).toInt()
        return p.Rectangle(t = 0, i = a, n = this.un.width, r = o)
    }
}

object d {

    data class e(val resources: resources,val views: List<formatview>)

    lateinit var urlResult: UrlResult

    //speedbinb.js?dmy=016301:formatted:8699
    private fun RS(t: resources, i: String): coord {
        val n =   (Regex("^([^:]+):(\\d+),(\\d+)\\+(\\d+),(\\d+)>(\\d+),(\\d+)\$").matchEntire(i)
            ?: throw IllegalArgumentException("Invalid format for Image Transfer : $i")).groupValues
        val r = n[1]
        if ("_$r" !in t.getOwnPropertyNames())
            throw IllegalArgumentException("resid $r not found.")
        return coord(
            resid = r, xsrc = n[2].toInt(10), ysrc = n[3].toInt(10), width = n[4].toInt(10),
            height = n[5].toInt(10), xdest = n[6].toInt(10), ydest = n[7].toInt(10)
        )
    }

    //speedbinb.js?dmy=016301:formatted:8632
    private fun FS():e {
        val t=urlResult.t
        val n =
            t.resources.copy(i = t.resources.i.copy(src =urlResult.originImagePath))
        return e(resources = n,views = t.views.map { it ->
            formatview(width = it.width, height = it.height, coords = it.coords.map {
                RS(t = n, i = it)
            }, areas = it.areas)
        })
    }

    //speedbinb.js?dmy=016301:formatted:8604
    fun Gs(): n {
        val e = FS()
        val u = e.views[0]
        return n(width = u.width, height = u.height, transfers = listOf(transfer(index = 0, coords = u.coords)))
    }

}

object ImageDataManager {
    private val data = mutableListOf<ImageData>()

    fun append(d: ImageData) {
        data.add(d)
    }

    fun requestZip(urlResult: UrlResult) {
        val form = FormData()
        form.apply {
            append(name = "romajiTitle", value = urlResult.romajiTitle)
            data.forEach {
                append(name = it.fileName, value = it.blob, filename = it.fileName)
            }
        }
        window.fetch(Api.IMAGE_API, RequestInit(method = "post", body = form))
        data.clear()
    }
}

data class ImageData(val blob: Blob, val fileName: String)

class ImageLoader(val urlResult: UrlResult) {

    private val canvasHtml = createCanvas()

    private var imageHeight = 0.0

    //图片解析
    private val tasks = mutableListOf<Promise<Image>>()

    //speedbinb.js?dmy=016301:formatted:8766
    private fun callback(): List<n> {
        d.urlResult = urlResult
        val n = d.Gs()
//        console.info("Gs:")
//        console.info(n)
        val u = e(un = un(width = n.width, height = n.height))
        val s = n.transfers[0].coords
        val h = mutableListOf<n>()
        repeat(u.Xs) {
            val r = u.Us(t = it)
//            console.info("r:")
//            console.info(r)
            val e = mutableListOf<coord>()
            s.forEach { t ->
//                console.info("-------------------")
//                console.info("t:")
//                console.info(t)
                val _i = p.Rectangle(t = t.xdest, i = t.ydest, n = t.width, r = t.height)
//                console.info("p.Rectangle(i):")
//                console.info(_i)
                p.intersect(t=r,i=_i)?.let {
                    n->
//                    console.info("p.Rectangle.intersect(n):")
//                    console.info(n)
//                    console.info("-------------------")
                    e.add(coord(resid = t.resid,
                    xsrc = t.xsrc+(n.left-t.xdest),
                    ysrc= t.ysrc + (n.top - t.ydest),
                    width= n.width,
                    height= n.height,
                    xdest= n.left - r.left,
                    ydest = n.top - r.top))

                }

            }
//            console.info("e.size=${e.size}")
            h.add(n(width = r.width,height = r.height,transfers = listOf(transfer(index = 0,coords = e))))
        }
//        console.info(h)
        return h.toList()
    }

    private fun createCanvas(): HTMLCanvasElement {
        return  document.createElement("canvas") as HTMLCanvasElement
    }

    //speedbinb.js?dmy=016301:formatted:7976
    private fun us(t:n,image:Image){
//        console.info("开始绘制漫画页")
        val canvas:HTMLCanvasElement= createCanvas()
        canvas.apply {
            width=t.width
            height=t.height
            val ctx:CanvasRenderingContext2D= getContext("2d") as CanvasRenderingContext2D
            ctx.clearRect(0.0, 0.0, width.toDouble(), height.toDouble())
            t.transfers.forEach {
                i->
                i.coords.forEach {
                    t->
                    ctx.drawImage(image = image,sx=t.xsrc.toDouble(),sy=t.ysrc.toDouble(),sw=t.width.toDouble(),sh=t.height.toDouble(),
                        dx=t.xdest.toDouble(),dy=t.ydest.toDouble(),dw=t.width.toDouble(),dh=t.height.toDouble())
                }
            }

        }

        Promise<Image>(executor = { resolve, _ ->
            canvasToBlob(t=canvas,{blob: Blob ->
//                console.info("blob.size:${blob.size}")
                val i=URL.createObjectURL(blob=blob)
                console.info("加载图片dataUrl:\n${i}")
                Image().apply {
                    onload = {
                        imageHeight += this.naturalHeight
                        resolve(this)
                    }
                    src = i
                }
            })
        }).apply {
            tasks.add(this)
        }
    }

    fun dataURItoBlob(dataURI: String): Blob {
        // convert base64 to raw binary data held in a string
        val byteString = window.atob(dataURI.split(',')[1])

        // separate out the mime component
        val mimeString = dataURI.split(',')[0].split(':')[1].split(';')[0]

        // write the bytes of the string to an ArrayBuffer
        val arrayBuffer = ArrayBuffer(byteString.length)
        val _ia = Uint8Array(arrayBuffer)

        byteString.withIndex().forEach {
            _ia[it.index] = it.value.code.toByte()
        }

        val dataView = DataView(arrayBuffer)
        return Blob(arrayOf(dataView), options = BlobPropertyBag(type = mimeString))
    }


    private fun create(resolve: (value: Boolean) -> Unit) {
        Promise.all(promise = tasks.toTypedArray()).then {
            canvasHtml.let { canvas ->
                canvas.width = it.first().naturalWidth
                canvas.height = imageHeight.toInt()
                val ctx: CanvasRenderingContext2D = canvas.getContext("2d") as CanvasRenderingContext2D
                ctx.clearRect(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble() - 4 * 2)
                var dy = 0.0
                it.forEach {
                    ctx.drawImage(
                        image = it, dx = 0.0, dy = dy,
                        dw = it.naturalWidth.toDouble(), dh = it.naturalHeight.toDouble().apply {
                            dy += this.toInt() - 4
                        })
                }
                Image().apply {
                    onload = {
                        console.info("拼接图片大小naturalWidth:${naturalWidth},naturalHeight:${naturalHeight}")
                    }
                    src = canvas.toDataURL().let {
                        val blob = dataURItoBlob(it)
                        val name = "${urlResult.romajiTitle}_${urlResult.filename}"
                        ImageDataManager.append(ImageData(blob = blob, fileName = name))
                        resolve(true)
                        URL.createObjectURL(blob)
                    }.apply {
                        console.info("拼接图片url：\n$this")
                    }
                        }
                    }
        }


    }

    //speedbinb.js?dmy=016301:formatted:3798
    private fun canvasToBlob(
        t:HTMLCanvasElement, callback:(t:Blob)->Unit, n:String="image/jpeg", r: Double =.9){
        val i=t.toDataURL(type=n,quality = r).split(",")[1]
//        console.info("url length:${i.length}")
        val blobTransfer=w(t=i)
//        console.info("w[i]:")
//        console.info(blobTransfer)
        val blob= Blob(blobParts=arrayOf(blobTransfer),options = BlobPropertyBag(type = n))
//        console.info("blob size:${blob.size}")
        callback(blob)
    }

    //speedbinb.js?dmy=016301:formatted:3602
    fun _m(t: String): Array<Int> {
        val i= arrayOf<Int>()
        repeat(t.length){
            i[t[it].code]=it
        }
        return i
    }

    val m=_m("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/")

    //speedbinb.js?dmy=016301:formatted:3608
    fun w(t:String): Uint8Array {
        val e=t.length
        val s = t.slice(IntRange(e-2,e-1)).split("=").count() - 1
        val  h = 3 * ((e + 3) / 4) - s
        val u = Uint8Array(h)
        var i=0
        var n=i
        while (n<e){
            val r=m[t[n].code] shl 18 or (m[t[n + 1].code] shl 12) or (m[t[n + 2].code] shl 6) or m[t[n + 3].code]
            u[i]=(r shr  16 and  255).toByte()
            u[i+1]=(r shr 8 and 255).toByte()
            u[i+2]=(255 and r).toByte()
            i+=3
            n+=4
        }
        return u
    }

    //speedbinb.js?dmy=016301:formatted:8020
    fun rebuild(): Promise<Boolean> {
        return Promise(executor = { resolve, _ ->
            hs().then {
                console.info("image(naturalWidth:${it.naturalWidth},naturalHeight=${it.naturalHeight})")
                callback().map { t ->
                    //                val n=t(width = t.width,height = t.height)
                    us(t = t, image = it)
                }
                create(resolve = resolve)
            }
        })
    }

    //speedbinb.js?dmy=016301:formatted:7949
    private fun hs(): Promise<Image> {
        return Promise(executor = { resolve, reject ->
            val e=Image()
//            e.crossOrigin="anonymous"
            val t=urlResult.serverImagePath
            e.onload={
                resolve(e)
            }
            e.onerror = { _: dynamic, _: String, _: Int, _: Int, _: Any? ->
                reject(Error("Failed to load image. : $t"))
            }
            e.onabort = {
                reject(Error("Failed to load image. : $t"))
            }
            e.src=t.apply { console.info("img path:${this}") }
        })
    }
}


