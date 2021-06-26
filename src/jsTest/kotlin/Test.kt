import kotlinx.css.p
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.set

import kotlin.test.Test


class JsTest {

    @Serializable
    data class FF(@SerialName("ptimg-version") val c:String)

    @Test
    fun testParse(){
        val c=Json.decodeFromString<t>("{\"ptimg-version\":1,\"resources\":{\"i\":{\"src\":\"0012.jpg\",\"width\":908,\"height\":1264}},\"views\":[{\"width\":844,\"height\":1200,\"coords\":[\"i:4,4+106,150>420,1050\",\"i:118,4+106,150>530,450\",\"i:232,4+106,150>738,0\",\"i:346,4+106,150>526,750\",\"i:460,4+106,150>420,750\",\"i:574,4+106,150>106,300\",\"i:688,4+106,150>318,600\",\"i:802,4+102,150>742,600\",\"i:4,162+106,150>106,450\",\"i:118,162+106,150>526,1050\",\"i:232,162+106,150>0,900\",\"i:346,162+106,150>636,150\",\"i:460,162+102,150>424,900\",\"i:570,162+106,150>0,1050\",\"i:684,162+106,150>106,1050\",\"i:798,162+106,150>0,600\",\"i:4,320+106,150>738,1050\",\"i:118,320+106,150>106,150\",\"i:232,320+106,150>424,600\",\"i:346,320+106,150>420,300\",\"i:460,320+106,150>530,150\",\"i:574,320+106,150>424,0\",\"i:688,320+102,150>742,150\",\"i:798,320+106,150>0,450\",\"i:4,478+106,150>738,750\",\"i:118,478+106,150>632,1050\",\"i:232,478+106,150>106,900\",\"i:346,478+102,150>106,750\",\"i:456,478+106,150>106,600\",\"i:570,478+106,150>424,450\",\"i:684,478+106,150>212,900\",\"i:798,478+106,150>318,450\",\"i:4,636+106,150>632,300\",\"i:118,636+106,150>314,1050\",\"i:232,636+106,150>106,0\",\"i:346,636+106,150>318,0\",\"i:460,636+106,150>314,750\",\"i:574,636+102,150>636,0\",\"i:684,636+106,150>632,900\",\"i:798,636+106,150>738,900\",\"i:4,794+106,150>318,150\",\"i:118,794+106,150>530,600\",\"i:232,794+106,150>212,0\",\"i:346,794+106,150>212,450\",\"i:460,794+106,150>0,150\",\"i:574,794+102,150>636,450\",\"i:684,794+106,150>738,300\",\"i:798,794+106,150>0,750\",\"i:4,952+106,150>208,750\",\"i:118,952+106,150>212,150\",\"i:232,952+106,150>636,600\",\"i:346,952+106,150>212,300\",\"i:460,952+106,150>0,300\",\"i:574,952+102,150>212,1050\",\"i:684,952+106,150>526,900\",\"i:798,952+106,150>424,150\",\"i:4,1110+106,150>530,0\",\"i:118,1110+106,150>526,300\",\"i:232,1110+106,150>632,750\",\"i:346,1110+106,150>738,450\",\"i:460,1110+106,150>212,600\",\"i:574,1110+106,150>0,0\",\"i:688,1110+102,150>318,300\",\"i:798,1110+106,150>318,900\"]}]}")
        println(c.ptimg_version)
        println(c.resources.i)
        println(c.views)
    }


    @Test
    fun testRS(){

        val f=Regex("^([^:]+):(\\d+),(\\d+)\\+(\\d+),(\\d+)>(\\d+),(\\d+)\$").matchEntire("i:574,4+106,150>106,600")
        println(f?.groupValues)
    }

    @Test
    fun testUnitArray(){
        ImageLoader(UrlResult(originImagePath = "",serverImagePath = "",
            t=t(ptimg_version = 0,resources = resources(i = i(src = "",width = 0,height = 0)),views = listOf())))
            .apply {
                m.forEach {
                    println("m;${it}")
                }
            }
    }

}
