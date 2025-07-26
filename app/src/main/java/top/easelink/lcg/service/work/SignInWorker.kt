package top.easelink.lcg.service.work

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import top.easelink.lcg.BuildConfig
import top.easelink.lcg.network.JsoupClient
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class SignInWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Timber.d("Sign In Job Start")
            withContext(Dispatchers.IO) {
                sendSignInRequest()
            }
            Result.success()
        } catch (e: SocketTimeoutException) {
            Timber.e(e, "Sign in timeout, will retry")
            Result.retry()
        } catch (e: Exception) {
            Timber.e(e, "Sign in failed")
            Result.failure()
        }
    }

    companion object {
        private val WORK_INTERVAL: Long = if (BuildConfig.DEBUG) 15L else 8L
        private val DEFAULT_TIME_UNIT = if (BuildConfig.DEBUG) TimeUnit.SECONDS else TimeUnit.HOURS

        private const val APPLY_TASK_URL = "https://www.52pojie.cn/home.php?mod=task&do=apply&id=2"
        private const val DRAW_TASK_URL = "https://www.52pojie.cn/home.php?mod=task&do=draw&id=2"
        private const val TASK_APPLIED = "已申请"
        const val TAG = "SignInWorker"

        @Throws(SocketTimeoutException::class)
        fun sendSignInRequest() {
            try {
                val applyResponse = JsoupClient.sendGetRequestWithUrl(APPLY_TASK_URL)
                applyResponse.getElementsByClass("alert_info")
                    .first()
                    ?.selectFirst("p")
                    ?.text()
                    ?.takeIf { it.contains(TASK_APPLIED) }
                    ?.let {
                        Timber.d("Task applied: $it")
                        JsoupClient.sendGetRequestWithUrl(DRAW_TASK_URL)
                            .getElementsByClass("alert_info")
                            .first()
                            ?.selectFirst("p")
                            ?.let { drawText ->
                                Timber.d("Draw result: $drawText")
                            }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Sign in request failed")
                throw e
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