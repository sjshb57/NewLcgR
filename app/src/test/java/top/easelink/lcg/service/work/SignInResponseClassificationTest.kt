package top.easelink.lcg.service.work

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 纯 JVM 单测：用真实抓取的 52pojie 响应片段，锁住"这个响应到底算什么"的判定。
 *
 * fixture 来源（2026-07-27 实抓）：
 * - waf_challenge_task.html —— GET /home.php?mod=task&do=apply&id=2 的真实返回。
 *   HTTP 200，但 body 是知道创宇 WAF 的 JS 挑战页。这是最容易被误判成"签到成功"的响应。
 * - login_page_snippet.html —— GET /member.php?mod=logging&action=login 的表单片段。
 *   注意 id 是 loginform_LqnM1（带随机后缀），且页面自带 formhash。
 *
 * 论坛改版 / WAF 策略变化时，这几条断言会第一时间红，比等用户反馈"签到没上"快得多。
 */
class SignInResponseClassificationTest {

    private fun fixture(name: String) =
        Jsoup.parse(
            checkNotNull(javaClass.classLoader?.getResourceAsStream("fixtures/$name")) {
                "fixture not found: $name"
            },
            "UTF-8",
            "https://www.52pojie.cn/"
        )

    // ---------- WAF 挑战页 ----------

    @Test
    fun `WAF挑战页被识别为挑战`() {
        val doc = fixture("waf_challenge_task.html")
        assertTrue(
            "签到端点返回的 WAF 挑战页必须被识别出来，否则会被当成签到成功",
            SignInWorker.looksLikeWafChallenge(doc)
        )
    }

    @Test
    fun `WAF挑战页没有alert_info`() {
        val doc = fixture("waf_challenge_task.html")
        // 这正是旧实现出问题的地方：读不到 alert_info 就当成"还没申请过"，
        // 于是继续 draw 并无条件 markSignInDone()。
        assertNull(SignInWorker.readAlertInfo(doc))
    }

    @Test
    fun `WAF挑战页不应被误判为登录页`() {
        val doc = fixture("waf_challenge_task.html")
        assertFalse(
            "挑战页里既没有登录表单也没有 messagelogin，不能报成 LOGIN_REQUIRED",
            SignInWorker.looksLikeLoginPage(doc)
        )
    }

    // ---------- 登录页 ----------

    @Test
    fun `真实登录页被识别为登录页`() {
        val doc = fixture("login_page_snippet.html")
        assertTrue(
            "登录表单 id 带随机后缀(loginform_LqnM1)，选择器必须用 [id^=loginform] 或 form[name=login]",
            SignInWorker.looksLikeLoginPage(doc)
        )
    }

    @Test
    fun `登录页自带formhash所以不能用noFormHash做判据`() {
        val doc = fixture("login_page_snippet.html")
        // 旧实现是 hasLoginForm && noFormHash，而真实登录页 formhash 是存在的，
        // 那个组合在登录页上反而返回 false。这条断言把这个前提固定下来。
        assertEquals("1107bd69", doc.selectFirst("input[name=formhash]")?.attr("value"))
    }

    @Test
    fun `登录页不是WAF挑战`() {
        assertFalse(SignInWorker.looksLikeWafChallenge(fixture("login_page_snippet.html")))
    }

    // ---------- 正常的任务结果页 ----------

    @Test
    fun `已申请的任务页返回已申请提示`() {
        val doc = Jsoup.parse(
            """
            <html><body>
              <div class="alert_info"><p>您已申请了此任务，请完成任务后领取奖励。</p></div>
            </body></html>
            """.trimIndent()
        )
        assertFalse(SignInWorker.looksLikeWafChallenge(doc))
        assertFalse(SignInWorker.looksLikeLoginPage(doc))
        assertTrue(SignInWorker.readAlertInfo(doc)!!.contains("已申请"))
    }

    @Test
    fun `领取成功的任务页有alert_info`() {
        val doc = Jsoup.parse(
            """
            <html><body>
              <div class="alert_info"><p>恭喜您，任务已完成，获得奖励。</p></div>
            </body></html>
            """.trimIndent()
        )
        assertFalse(SignInWorker.looksLikeWafChallenge(doc))
        assertEquals("恭喜您，任务已完成，获得奖励。", SignInWorker.readAlertInfo(doc))
    }

    // ---------- 会话失效页 ----------

    @Test
    fun `messagelogin提示页被识别为需要登录`() {
        val doc = Jsoup.parse(
            """
            <html><body>
              <div id="messagelogin">
                <div id="messagetext"><p>您需要先登录才能继续本操作</p></div>
              </div>
            </body></html>
            """.trimIndent()
        )
        assertTrue(SignInWorker.looksLikeLoginPage(doc))
        assertFalse(SignInWorker.looksLikeWafChallenge(doc))
    }
}
