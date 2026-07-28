package top.easelink.lcg.ui.main.source

interface FavoritesRemoteDataSource {

    fun addFavorites(threadId: String, formHash: String): Boolean

    /**
     * 远端取消收藏。Discuz 的 favid 与 threadId 并不直接相等，正规流程是先到收藏列表反查
     * favid 再删；这里走的是 spacecp 的 delete 入口，type=thread + id=threadId 让服务端反查。
     * 调用方应对 false 做降级（本地删除照常执行）。
     */
    fun removeFavorites(threadId: String, formHash: String): Boolean
}
