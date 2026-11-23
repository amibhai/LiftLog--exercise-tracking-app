package io.github.amibhai.liftlog.core.domain.id

import java.util.UUID

/** Creates primary keys for new rows (SPEC §9: UUIDs, so sync can be added later). */
fun interface IdGenerator {
    fun newId(): String
}

class RandomUuidGenerator : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}
