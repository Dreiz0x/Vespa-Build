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
        NormativeFragmentEntity::class,
        DocumentSourceEntity::class,
        ReactivoFragmentCrossRef::class,
        ReactivoEntity::class,
        ReactivoOptionEntity::class,
        ReactivoIntentoEntity::class,
        UserTopicMasteryEntity::class,
        UserGapLogEntity::class,
        StudySessionEntity::class,
        QuarantineEntity::class,
        DiagnosisEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        PendingSyncEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ontologyDao(): OntologyDao
    abstract fun cargoDao(): CargoDao
    abstract fun organoDao(): OrganoDao
    abstract fun normativeDao(): NormativeDao
    abstract fun documentSourceDao(): DocumentSourceDao
    abstract fun reactivoDao(): ReactivoDao
    abstract fun userMasteryDao(): UserMasteryDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun quarantineDao(): QuarantineDao
    abstract fun diagnosisDao(): DiagnosisDao
    abstract fun conversationDao(): ConversationDao

    companion object {
        const val DATABASE_NAME = "vespa_database"
        const val SCHEMA_LOCATION = "schemas"
    }
}
