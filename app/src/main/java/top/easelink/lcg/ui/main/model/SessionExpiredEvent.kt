package top.easelink.lcg.ui.main.model

/**
 * 服务端已不再认账（cookie 失效 / 被踢下线）。
 * 由 JsoupClient 在解析响应时探测到登录页时发出，UI 层订阅后弹提示。
 */
class SessionExpiredEvent
