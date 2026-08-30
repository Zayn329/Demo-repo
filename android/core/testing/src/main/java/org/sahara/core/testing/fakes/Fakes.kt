package org.sahara.core.testing.fakes

import org.sahara.core.data.db.AuditEventDao
import org.sahara.core.data.db.AuditEventEntity
import org.sahara.core.data.db.EvidenceDao
import org.sahara.core.data.db.EvidenceEntryEntity
import org.sahara.core.data.db.IncidentDao
import org.sahara.core.data.db.IncidentEntity
import org.sahara.core.data.db.NotifyContactDao
import org.sahara.core.data.db.NotifyContactEntity

class FakeIncidentDao : IncidentDao {
    private val map = mutableMapOf<String, IncidentEntity>()

    override suspend fun getIncidentById(id: String): IncidentEntity? = map[id]

    override fun getAllIncidents() = kotlinx.coroutines.flow.flowOf(map.values.toList())

    override suspend fun insertIncident(incident: IncidentEntity) {
        map[incident.incidentId] = incident
    }

    override suspend fun updateIncidentState(id: String, state: String, sealedAt: Long?, merkleRoot: String?) {
        map[id]?.let { existing ->
            map[id] = existing.copy(state = state, sealedAt = sealedAt, finalMerkleRoot = merkleRoot)
        }
    }
}

class FakeEvidenceDao : EvidenceDao {
    private val list = mutableListOf<EvidenceEntryEntity>()

    override fun getEvidenceForIncident(incidentId: String) = kotlinx.coroutines.flow.flowOf(list.filter { it.incidentId == incidentId })

    override suspend fun insertEvidence(evidence: EvidenceEntryEntity) {
        list.add(evidence)
    }
}

class FakeAuditEventDao : AuditEventDao {
    private val list = mutableListOf<AuditEventEntity>()

    override fun getAuditLogs() = kotlinx.coroutines.flow.flowOf(list.toList())

    override suspend fun insertAudit(audit: AuditEventEntity) {
        list.add(audit)
    }
}

class FakeNotifyContactDao : NotifyContactDao {
    private val map = mutableMapOf<String, NotifyContactEntity>()

    override fun getContacts() = kotlinx.coroutines.flow.flowOf(map.values.toList())

    override suspend fun insertContact(contact: NotifyContactEntity) {
        map[contact.contactId] = contact
    }

    override suspend fun deleteContact(id: String) {
        map.remove(id)
    }
}
