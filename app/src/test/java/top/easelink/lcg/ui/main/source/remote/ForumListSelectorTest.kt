package top.easelink.lcg.ui.main.source.remote

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 列表解析的选择器回归测试。
 *
 * 论坛模板一改版，App 就是静默少内容 —— 不崩不报错。这里用真实抓取的 fixture
 * 把生产代码用到的选择器钉住。挂了先去网页版 F12 看结构，再改
 * [ArticlesRemoteDataSource]，最后重抓 fixture。
 */
class ForumListSelectorTest {

    /** 与 ArticlesRemoteDataSource.TH_TITLE 保持一致；改那边记得改这里。 */
    private val thTitle = "th"

    private fun load(name: String): Document {
        val stream = javaClass.classLoader!!.getResourceAsStream("fixtures/$name")
            ?: error("fixture 不存在: $name")
        return Jsoup.parse(stream, "UTF-8", "https://www.52pojie.cn/")
    }

    private fun rows(name: String) = load(name).select("tbody[id^=normal]")

    // ==================== th.common / th.new 的分布 ====================

    @Test
    fun `guide 页匿名访问时全部是 th_common`() {
        val r = rows("guide_hot_rows.html")
        assertTrue("fixture 应至少有 1 行", r.isNotEmpty())
        assertEquals("guide 页匿名时应全是 th.common", r.size, r.count { it.selectFirst("th.common") != null })
    }

    @Test
    fun `板块页匿名访问时全部是 th_new`() {
        val r = rows("forum_16_rows.html")
        assertTrue("fixture 应至少有 1 行", r.isNotEmpty())
        assertEquals("板块页匿名时应全是 th.new", r.size, r.count { it.selectFirst("th.new") != null })
    }

    @Test
    fun `写死 th_common 会漏掉板块页的每一行——这就是原来的 bug`() {
        val r = rows("forum_16_rows.html")
        assertEquals(
            "如果这里不再是 0，说明论坛把板块页也改成 th.common 了，可以简化 TH_ANY",
            0,
            r.count { it.selectFirst("th.common > .xst") != null }
        )
    }

    @Test
    fun `逗号写法是错的 别再改回去`() {
        // CSS 逗号在顶层拆分，在 guide 页会命中 <th> 本身而不是标题
        val row = rows("guide_hot_rows.html").first()
        val wrong = row.select("th.common, th.new > .xst").first()!!
        val right = row.select("$thTitle > .xst").first()!!
        assertTrue("错误写法会命中 th 本身", wrong.tagName() == "th")
        assertTrue("正确写法命中 a.xst", right.tagName() == "a")
    }

    @Test
    fun `每个主题行恰好只有一个 th 所以直接用 th 是安全的`() {
        listOf("guide_hot_rows.html", "guide_newthread_rows.html", "forum_16_rows.html").forEach { f ->
            rows(f).forEach { row ->
                assertEquals("$f 每行应恰好一个 <th>", 1, row.select("th").size)
            }
        }
    }

    @Test
    fun `th 在两种页面上都能取到标题`() {
        listOf("guide_hot_rows.html", "guide_newthread_rows.html", "forum_16_rows.html").forEach { f ->
            val r = rows(f)
            val hit = r.count { !it.selectFirst("$thTitle > .xst")?.text().isNullOrBlank() }
            assertEquals("$f 的标题应全部解析成功", r.size, hit)
        }
    }

    @Test
    fun `th 在两种页面上都能取到帖子链接`() {
        listOf("guide_hot_rows.html", "forum_16_rows.html").forEach { f ->
            rows(f).forEach { row ->
                val href = row.selectFirst("$thTitle a.xst")?.attr("href").orEmpty()
                assertTrue("$f 应解析出 thread 链接，实际=$href", href.contains("thread-"))
            }
        }
    }

    // ==================== 其余字段 ====================

    @Test
    fun `回复数与查看数`() {
        listOf("guide_hot_rows.html", "forum_16_rows.html").forEach { f ->
            rows(f).forEach { row ->
                val reply = row.selectFirst("td.num a.xi2")?.text()
                val view = row.selectFirst("td.num em")?.text()
                assertTrue("$f 回复数应为纯数字，实际=$reply", reply?.toIntOrNull() != null)
                assertTrue("$f 查看数应为纯数字，实际=$view", view?.toIntOrNull() != null)
            }
        }
    }

    @Test
    fun `作者取 td_by 下带 uid 的链接`() {
        listOf("guide_hot_rows.html", "forum_16_rows.html").forEach { f ->
            rows(f).forEach { row ->
                val author = row.select("td.by").select("a[href*=uid]").firstOrNull()?.text()
                assertTrue("$f 作者不应为空", !author.isNullOrBlank())
            }
        }
    }

    @Test
    fun `guide 页能取到板块名 而板块页本身没有`() {
        rows("guide_hot_rows.html").forEach { row ->
            val origin = row.selectFirst("td.by > a[target]")?.text().orEmpty()
            assertTrue("guide 页应带板块名，实际=$origin", origin.contains("『"))
        }
        // 板块页里你已经在那个板块了，Discuz 不再输出板块列 —— origin 为空是正确行为
        val originInForum = rows("forum_16_rows.html").count { it.selectFirst("td.by > a[target]") != null }
        assertEquals("板块页不应有板块名列", 0, originInForum)
    }

    @Test
    fun `悬赏金额来自 span_xi1 里的 span_xw1`() {
        val bounty = rows("guide_newthread_rows.html")
            .mapNotNull { it.selectFirst("$thTitle > span.xi1 > span.xw1")?.text() }
        assertTrue("新鲜出炉 fixture 里应至少有一个悬赏帖", bounty.isNotEmpty())
        bounty.forEach {
            assertTrue("悬赏金额应为纯数字，实际=$it", it.toIntOrNull() != null)
        }
    }

    @Test
    fun `推荐标记来自 th 下 img 的 title 属性`() {
        val titles = rows("guide_hot_rows.html")
            .flatMap { row ->
                row.selectFirst(thTitle)?.getElementsByTag("img")?.map { it.attr("title") } ?: emptyList()
            }
        assertTrue(
            "应能读到「评价指数 N」这类标记，实际=$titles",
            titles.any { it.contains("评价指数") || it.contains("热度") }
        )
    }
}
