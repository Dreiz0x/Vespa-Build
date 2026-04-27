package dev.vskelk.cdf.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.vskelk.cdf.core.database.dao.OntologyDao
import dev.vskelk.cdf.core.database.dao.StudySessionDao
import dev.vskelk.cdf.core.database.dao.UserMasteryDao
import dev.vskelk.cdf.core.database.dao.ReactivoDao
import dev.vskelk.cdf.core.database.dao.NormativeDao
import dev.vskelk.cdf.core.database.dao.DiagnosisDao
import dev.vskelk.cdf.core.database.dao.QuarantineDao
import dev.vskelk.cdf.core.database.dao.ConversationDao
import dev.vskelk.cdf.core.database.entity.OntologyNodeEntity
import dev.vskelk.cdf.core.database.entity.OntologyRelationEntity
import dev.vskelk.cdf.core.database.entity.CargoEntity
import dev.vskelk.cdf.core.database.entity.OrganoEntity
import dev.vskelk.cdf.core.database.entity.ConversationEntity
import dev.vskelk.cdf.core.database.entity.MessageEntity
import dev.vskelk.cdf.core.database.entity.PendingSyncEntity
import dev.vskelk.cdf.core.database.entity.ReactivoEntity
import dev.vskelk.cdf.core.database.entity.ReactivoOptionEntity
import dev.vskelk.cdf.core.database.entity.ReactivoIntentoEntity
import dev.vskelk.cdf.core.database.entity.ReactivoFragmentCrossRef
import dev.vskelk.cdf.core.database.entity.CuarentenaFragmentoEntity
import dev.vskelk.cdf.core.database.entity.DiagnosisEntity
import dev.vskelk.cdf.core.database.entity.UserTopicMasteryEntity
import dev.vskelk.cdf.core.database.entity.UserGapLogEntity
import dev.vskelk.cdf.core.database.entity.StudySessionEntity
import dev.vskelk.cdf.core.database.entity.NormativeFragmentEntity
import dev.vskelk.cdf.core.database.entity.DocumentSourceEntity

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
        CuarentenaFragmentoEntity::class,
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
    // No change needed here, already correct
    abstract fun diagnosisDao(): DiagnosisDao
    abstract fun cuarentenaDao(): QuarantineDao
    abstract fun conversationDao(): ConversationDao

    companion object {
        const val DATABASE_NAME = "vespa_database"
        const val SCHEMA_LOCATION = "schemas"
    }
}
