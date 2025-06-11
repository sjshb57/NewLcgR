package top.easelink.framework.guard

import top.easelink.framework.utils.debugDo

object ExceptionHandler {
    private const val DEFAULT_ERROR_TYPE = "GENERAL_ERROR"

    /**
     * Throw Exception directly if in Debug Mode, do nothing in Release mode
     * @param t Throwable
     * @param type defined by user, help to identify the error
     */
    fun safeLogException(t: Throwable, type: String = DEFAULT_ERROR_TYPE) {
        debugDo { throw t }
        // 预留位置：可在此处集成其他崩溃统计服务（如Firebase Crashlytics）
    }
}