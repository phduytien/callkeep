package io.wazo.callkeep

object CallContainer {
    private var uuid: String? = null
    fun saveUUid(uuid: String?) {
        this.uuid = uuid
    }

    fun getSavedUuid(): String? {
        return uuid
    }

    fun removeSavedUuid() {
        this.uuid = null
    }
}