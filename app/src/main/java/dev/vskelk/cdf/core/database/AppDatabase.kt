package dev.vskelk.cdf.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.vskelk.cdf.core.database.dao.*
import dev.vskelk.cdf.core.database.entity.*

@Database(
    entities = [
        OntologyNodeEntity::class,
        OntologyRelationEntity::class,
        CargoEntity::class,
        OrganoEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        PendingSyncEntity::class,
        ReactivoEntity::class,
        ReactivoOptionEntity::class,
        ReactivoIntentoEntity::class,
        ReactivoFragmentCrossRef::class,
        QuarantineEntity::class,
        DiagnosisEntity::class,
        UserTopicMasteryEntity::class,
        UserGapLogEntity::class,
        StudySessionEntity::class,
        NormativeFragmentEntity::class,
        DocumentSourceEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studySessionDao(): StudySessionDao
    abstract fun userMasteryDao(): UserMasteryDao
    abstract fun reactivoDao(): ReactivoDao
    abstract fun normativeDao(): NormativeDao
    abstract fun ontologyDao(): OntologyDao
    abstract fun diagnosisDao(): DiagnosisDao
    abstract fun quarantineDao(): QuarantineDao
    abstract fun conversationDao(): ConversationDao

    companion object {
        const val DATABASE_NAME = "vespa_database"
        const val SCHEMA_LOCATION = "schemas"
    }
}
