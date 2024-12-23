package us.huseli.fistopy

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import us.huseli.fistopy.interfaces.ILogger

data class CachedValue<V>(val value: V?, val timestamp: Long = System.currentTimeMillis())

open class MutexCache<I, K, V>(
    private val itemToKey: (I) -> K,
    private val fetchMethod: suspend MutexCache<I, K, V>.(I) -> V?,
    private val debugLabel: String? = null,
    private val retentionMs: Long = 20_000L,
) : ILogger {
    private val cache = mutableMapOf<K, CachedValue<V>>()
    private val mutexes = mutableMapOf<K, Mutex>()
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val logPrefix: String
        get() = debugLabel?.let { "[$it] " } ?: ""

    init {
        scope.launch {
            while (true) {
                val oldKeys = cache.filterValues { it.timestamp < System.currentTimeMillis() - retentionMs }.keys
                if (oldKeys.isNotEmpty()) {
                    log("${logPrefix}Throwing ${oldKeys.size} items")
                    cache.minusAssign(oldKeys)
                }
                delay(retentionMs)
            }
        }
    }

    fun clear() {
        log("${logPrefix}Clearing ${cache.size} items")
        cache.clear()
    }

    suspend fun get(item: I, forceReload: Boolean = false, retryOnNull: Boolean = false): V {
        return getValueSync(item = item, forceReload = forceReload, retryOnNull = retryOnNull)
            ?: throw Exception("value is null for item=$item")
    }

    suspend fun getOrNull(item: I, forceReload: Boolean = false, retryOnNull: Boolean = false): V? = try {
        getValueSync(item = item, forceReload = forceReload, retryOnNull = retryOnNull)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logError("item=$item, error=$e", e)
        null
    }

    fun update(key: K, value: V?) {
        cache[key] = CachedValue(value)
    }

    private suspend fun getValueSync(
        item: I,
        forceReload: Boolean = false,
        retryOnNull: Boolean = false,
    ): V? {
        val key = itemToKey(item)
        val mutex = mutexes.getOrPut(key) { Mutex() }

        return mutex.withLock {
            val cachedValue = cache[key]
            val shouldRun = cachedValue == null || forceReload || (cachedValue.value == null && retryOnNull)

            if (shouldRun) fetchMethod(item).also { cache[key] = CachedValue(it) }
            else cachedValue?.value
        }
    }
}


fun <I, V> getMutexCache(debugLabel: String? = null, fetchMethod: suspend MutexCache<I, I, V>.(I) -> V?) =
    MutexCache(itemToKey = { it }, fetchMethod = fetchMethod, debugLabel = debugLabel)
