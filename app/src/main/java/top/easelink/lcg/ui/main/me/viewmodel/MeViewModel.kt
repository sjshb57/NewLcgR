package top.easelink.lcg.ui.main.me.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import timber.log.Timber
import top.easelink.framework.threadpool.IOPool
import top.easelink.lcg.R
import top.easelink.lcg.account.UserDataRepo
import top.easelink.lcg.account.UserInfo
import top.easelink.lcg.ui.main.me.source.UserInfoRepo
import top.easelink.lcg.ui.main.model.AntiScrapingException
import top.easelink.lcg.utils.showMessage
import java.net.SocketTimeoutException

class MeViewModel : ViewModel() {

    @Suppress("BlockingMethodInNonBlockingContext")
    fun fetchUserInfoDirect() {
        if (UserDataRepo.isLoggedIn) {
            UserDataRepo.updateUserInfo(
                UserInfo(
                    userName = UserDataRepo.username,
                    avatarUrl = UserDataRepo.avatar,
                    wuaiCoin = UserDataRepo.coin,
                    credit = UserDataRepo.credit,
                    groupInfo = UserDataRepo.group,
                    enthusiasticValue = UserDataRepo.enthusiasticValue,
                    answerRate = UserDataRepo.answerRate,
                    signInStateUrl = null
                )
            )
        }
        viewModelScope.launch(IOPool) {
            try {
                val userInfo = UserInfoRepo.requestUserInfo()
                when {
                    // 服务端明确返回"未登录"占位（用户名 = "登录或注册"），才认为会话失效
                    userInfo != null && userInfo == UserInfo.getDefaultUserInfo() -> {
                        UserDataRepo.clearAll()
                    }
                    // 拿到正常 UserInfo → 同步本地
                    userInfo != null -> {
                        UserDataRepo.isLoggedIn = true
                        UserDataRepo.updateUserInfo(userInfo)
                    }
                    // userInfo == null（网络异常 / 解析错误 / 反爬等）→ 保留现状，不动登录态
                    // 这条路径之前会 clearAll()，是"每次进 app 掉登录"的根因
                    else -> Timber.w("requestUserInfo returned null, keeping current login state")
                }
            } catch (e: Exception) {
                Timber.e(e)
                when (e) {
                    is SocketTimeoutException -> R.string.network_error // 网络错误，不认为是登陆异常
                    is AntiScrapingException -> R.string.anti_scraping_error // 针对触发反爬虫机制的处理
                    else -> R.string.general_error
                }.let {
                    showMessage(it)
                }
            }
        }
    }
}
