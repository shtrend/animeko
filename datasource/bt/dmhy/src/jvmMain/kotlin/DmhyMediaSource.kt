package me.him188.ani.datasources.dmhy

class DmhyMediaSource(
    private val client: ScopedHttpClient,
) : TopicMediaSource() {
    class Factory : MediaSourceFactory {
        override val factoryId: FactoryId = FactoryId(ID)
        override val info: MediaSourceInfo get() = INFO
        override fun create(
            mediaSourceId: String,
            config: MediaSourceConfig,
            client: ScopedHttpClient
        ): MediaSource = DmhyMediaSource(client)
    }

    companion object {
        const val ID = "dmhy"
        val INFO = MediaSourceInfo(
            displayName = "動漫花園",
            description = "动漫资源聚合网站",
            iconUrl = "https://dmhy.org/favicon.ico",
            iconResourceId = "dmhy.png",
        )
    }

    override val info: MediaSourceInfo get() = INFO
    private val network by lazy {
        Network(client)
    }

    override val mediaSourceId: String get() = ID

    override suspend fun checkConnection(): ConnectionStatus {
        return try {
            network.list()
            ConnectionStatus.SUCCESS
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Failed to check connection" }
            ConnectionStatus.FAILED
        }
    }

    override suspend fun startSearch(query: DownloadSearchQuery): PagedSource<Topic> {
        return DmhyPagedSourceImpl(query, network)
    }
}