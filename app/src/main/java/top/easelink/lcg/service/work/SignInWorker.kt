package top.easelink.lcg.service.work

import android.content.Context
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

    enum class SignInResult { SUCCESS, ALREADY_DONE, LOGIN_REQUIRED }

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

                if (looksLikeLoginPage(applyResponse)) {
                    Timber.w("Sign in landed on login page, session expired")
                    return SignInResult.LOGIN_REQUIRED
                }

                val applyMessage = applyResponse.getElementsByClass("alert_info")
                    .first()
                    ?.selectFirst("p")
                    ?.text()
                    .orEmpty()

                if (applyMessage.contains(TASK_APPLIED)) {
                    Timber.d("Task already applied today, skipping draw")
                    SignInResult.ALREADY_DONE
                } else {
                    Timber.d("Apply result: %s, drawing reward", applyMessage)
                    val drawResponse = JsoupClient.sendGetRequestWithUrl(DRAW_TASK_URL)
                    val drawText = drawResponse.getElementsByClass("alert_info")
                        .first()
                        ?.selectFirst("p")
                        ?.text()
                    Timber.d("Draw result: %s", drawText)
                    markSignInDone()
                    SignInResult.SUCCESS
                }
            } catch (e: Exception) {
                Timber.e(e, "Sign in request failed")
                throw e
            }
        }

        /**
         * Discuz 把未登录用户重定向到登录页（或显示带 messagetext 的提示）。
         * 这两个特征足以让我们识别"签到时被踢"的状态。
         */
        private fun looksLikeLoginPage(doc: Document): Boolean {
            val hasLoginForm = doc.selectFirst("form[name=login], div#loginform, table#loginform") != null
            val noFormHash = doc.selectFirst("input[name=formhash]") == null
            val msg = doc.getElementById("messagetext")?.text().orEmpty()
            return (hasLoginForm && noFormHash) ||
                    msg.contains("您还未登录") ||
                    msg.contains("请先登录")
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
