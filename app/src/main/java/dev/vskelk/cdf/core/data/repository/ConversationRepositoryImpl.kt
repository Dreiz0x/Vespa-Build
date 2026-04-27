package dev.vskelk.cdf.core.data.repository

import dev.vskelk.cdf.core.database.dao.NormativeDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val normativeDao: NormativeDao
) {
    suspend fun getEnrichedPrompt(userQuery: String): String {
        val localContext = normativeDao.searchByKeyword("%$userQuery%")
            .joinToString("\n---\n") { it.content }

        return if (localContext.isNotEmpty()) {
            "[CORPUS LOCAL]\n$localContext\n\nPregunta del usuario: $userQuery"
        } else {
            userQuery
        }
    }
}
