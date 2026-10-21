package tech.astro.liftoff.storage

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

/** Objects no row points to anymore. Deleted only after commit, so a rollback never loses a live file. */
data class StorageObjectsOrphaned(val keys: List<String>)

@Component
class StorageCleanupListener(private val storage: ObjectProvider<ObjectStorage>) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(fallbackExecution = true)
    fun on(event: StorageObjectsOrphaned) {
        val store = storage.ifAvailable ?: return
        event.keys.forEach { key ->
            runCatching { store.delete(key) }.onFailure { log.warn("Could not delete orphaned object {}: {}", key, it.message) }
        }
    }
}
