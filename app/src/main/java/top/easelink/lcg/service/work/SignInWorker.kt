package top.easelink.lcg.service.work

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.Operation
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.nodes.Document
import timber.log.Timber
import top.easelink.lcg.BuildConfig
import top.easelink.lcg.R
import top.easelink.lcg.account.UserDataRepo
import top.easelink.lcg.network.JsoupClient
import top.easelink.lcg.utils.SharedPreferencesHelper
import top.easelink.lcg.utils.showMessage
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class SignInWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (!UserDataRepo.isLoggedIn) {
            Timber.d("Sign in skipped: not logged in")
            return Result.success()
        }
        return try {
            withContext(Dispatchers.IO) {
                when (sendSignInRequest()) {
                    SignInResult.SUCCESS, SignInResult.ALREADY_DONE -> Result.success()
                    // retry 而非 success/failure：success 会掩盖问题，failure 会让周期任务不再重排
                    SignInResult.BLOCKED_BY_WAF -> Result.retry()
                    SignInResult.LOGIN_REQUIRED -> {
                        showMessage(R.string.auto_sign_in_login_required)
                        Result.failure()
                    }
                }
            }
        } catch (e: SocketTimeoutException) {
            Timber.e(e, "Sign in timeout, will retry")
            Result.retry()
        } catch (e: Exception) {
            Timber.e(e, "Sign in failed")
            Result.failure()
        }
    }

    enum class SignInResult { SUCCESS, ALREADY_DONE, LOGIN_REQUIRED, BLOCKED_BY_WAF }

    companion object {
        private val WORK_INTERVAL: Long = if (BuildConfig.DEBUG) 15L else 8L
        private val DEFAULT_TIME_UNIT = if (BuildConfig.DEBUG) TimeUnit.SECONDS else TimeUnit.HOURS

        private const val APPLY_TASK_URL = "https://www.52pojie.cn/home.php?mod=task&do=apply&id=2"
        private const val DRAW_TASK_URL = "https://www.52pojie.cn/home.php?mod=task&do=draw&id=2"
        private const val TASK_APPLIED = "已申请"
        const val TAG = "SignInWorker"

        // 与 LCGApp.trySignIn 共用的去重时间戳；7 小时窗口，避免与 8 小时周期重复触发。
        private const val SP_KEY_LAST_SIGN_IN_AT = "sp_key_last_sign_in_at"
        private const val DEDUP_WINDOW_MS = 7L * 60 * 60 * 1000

        @Throws(SocketTimeoutException::class)
        fun sendSignInRequest(): SignInResult {
            return try {
                val applyResponse = JsoupClient.sendGetRequestWithUrl(APPLY_TASK_URL)

                if (looksLikeWafChallenge(applyResponse)) {
                    return SignInResult.BLOCKED_BY_WAF
                }

                if (looksLikeLoginPage(applyResponse)) {
                    Timber.w("Sign in landed on login page, session expired")
                    return SignInResult.LOGIN_REQUIRED
                }

                // 拿不到 alert_info = 不是 Discuz 任务结果页，宁可判未知也不要假成功
                val applyMessage = readAlertInfo(applyResponse)
                if (applyMessage == null) {
                    Timber.w("Apply page has no alert_info, unrecognized response")
                    return SignInResult.BLOCKED_BY_WAF
                }

                if (applyMessage.contains(TASK_APPLIED)) {
                    Timber.d("Task already applied today, skipping draw")
                    // 已申请也要刷新去重时间戳，否则每次冷启动都会重打一次 apply
                    markSignInDone()
                    return SignInResult.ALREADY_DONE
                }

                Timber.d("Apply result: %s, drawing reward", applyMessage)
                val drawResponse = JsoupClient.sendGetRequestWithUrl(DRAW_TASK_URL)

                if (looksLikeWafChallenge(drawResponse)) {
                    return SignInResult.BLOCKED_BY_WAF
                }

                val drawText = readAlertInfo(drawResponse)
                if (drawText == null) {
                    Timber.w("Draw page has no alert_info, unrecognized response")
                    return SignInResult.BLOCKED_BY_WAF
                }

                Timber.d("Draw result: %s", drawText)
                markSignInDone()
                SignInResult.SUCCESS
            } catch (e: Exception) {
                Timber.e(e, "Sign in request failed")
                throw e
            }
        }

        /** 读取 Discuz 任务页的结果提示；页面结构不对时返回 null（区别于"提示为空串"）。 */
        @VisibleForTesting
        internal fun readAlertInfo(doc: Document): String? =
            doc.getElementsByClass("alert_info")
                .first()
                ?.selectFirst("p")
                ?.text()

        /**
         * 识别知道创宇 WAF 的 JS 挑战页：返回 HTTP 200 但 body 引用 /wzws-waf-cgi/ 脚本。
         * Jsoup 不执行 JS 过不了挑战，必须如实上报而不是当成签到成功。
         * WAF 策略与来源 IP 强相关（国内移动网络实测不触发），这里只做防御性识别。
         */
        @VisibleForTesting
        internal fun looksLikeWafChallenge(doc: Document): Boolean {
            val hasWafScript = doc.selectFirst("script[src*=wzws-waf-cgi]") != null
            if (hasWafScript) {
                Timber.w("WAF JS challenge detected on sign-in endpoint")
            }
            return hasWafScript
        }

        /**
         * 识别 Discuz 的"需要登录"提示页。
         * 选择器按 52pojie 真实 HTML 校正：登录表单 id 带随机后缀(loginform_XXXXX)，
         * 且登录页自带 formhash —— 所以不能用 "有表单且无 formhash" 判定。
         */
        @VisibleForTesting
        internal fun looksLikeLoginPage(doc: Document): Boolean {
            if (doc.getElementById("messagelogin") != null) return true
            val msg = doc.getElementById("messagetext")?.text().orEmpty()
            if (msg.contains("您需要先登录") || msg.contains("您还未登录") || msg.contains("请先登录")) {
                return true
            }
            // 兜底：页面主体就是登录表单，且没有任何任务结果提示。
            val hasLoginForm = doc.selectFirst("form[name=login], form[id^=loginform]") != null
            return hasLoginForm && doc.getElementsByClass("alert_info").isEmpty()
        }

        /** 是否在去重窗口内已经成功签过 —— 供 LCGApp 冷启动判断要不要再跑一次。 */
        fun isRecentlyExecuted(): Boolean {
            val sp = SharedPreferencesHelper.getUserSp()
            val last = sp.getLong(SP_KEY_LAST_SIGN_IN_AT, 0L)
            return System.currentTimeMillis() - last < DEDUP_WINDOW_MS
        }

        private fun markSignInDone() {
            SharedPreferencesHelper.getUserSp().edit {
                putLong(SP_KEY_LAST_SIGN_IN_AT, System.currentTimeMillis())
            }
        }

        fun startSignInWork(context: Context): Operation {
            val constraints = Constraints.Builder()
                .setRequiresCharging(false)
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(false)
                .build()

            val request = PeriodicWorkRequest.Builder(
                SignInWorker::class.java,
                WORK_INTERVAL,
                DEFAULT_TIME_UNIT
            )
                .setConstraints(constraints)
                .addTag(TAG)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 15L, TimeUnit.MINUTES)
                .build()

            return WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
