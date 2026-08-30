package org.sahara.core.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.sahara.core.data.db.AuditEventEntity
import org.sahara.core.data.db.EvidenceEntryEntity
import org.sahara.core.data.db.IncidentEntity
import org.sahara.core.data.db.NotifyContactEntity
import org.sahara.core.domain.models.ContactType
import org.sahara.core.domain.models.EvidenceEntry
import org.sahara.core.domain.models.EvidenceType
import org.sahara.core.domain.models.Incident
import org.sahara.core.domain.models.IncidentState
import org.sahara.core.domain.models.NotifyContact
import java.util.UUID

class RepositoryMappingUnitTest {

    private lateinit var fakeIncidentDao: FakeIncidentDao
    private lateinit var fakeEvidenceDao: FakeEvidenceDao
    private lateinit var fakeAuditDao: FakeAuditEventDao
    private lateinit var fakeContactDao: FakeNotifyContactDao

    private lateinit var incidentRepository: IncidentRepositoryImpl
    private lateinit var evidenceRepository: EvidenceRepositoryImpl
    private lateinit var auditRepository: AuditRepositoryImpl
    private lateinit var contactRepository: ContactRepositoryImpl

    @Before
    fun setup() {
        fakeIncidentDao = FakeIncidentDao()
        fakeEvidenceDao = FakeEvidenceDao()
        fakeAuditDao = FakeAuditEventDao()
        fakeContactDao = FakeNotifyContactDao()

        incidentRepository = IncidentRepositoryImpl(fakeIncidentDao)
        evidenceRepository = EvidenceRepositoryImpl(fakeEvidenceDao)
        auditRepository = AuditRepositoryImpl(fakeAuditDao)
        contactRepository = ContactRepositoryImpl(fakeContactDao)
    }

    @Test
    fun testIncidentSaveAndGet() = runBlocking {
        val incident = Incident(
            state = IncidentState.MONITORING,
            triggerSources = listOf("KEYWORD")
        )
        incidentRepository.saveIncident(incident)

        val retrieved = incidentRepository.getIncidentById(incident.incidentId)
        assertNotNull(retrieved)
        assertEquals(incident.incidentId, retrieved?.incidentId)
        assertEquals(IncidentState.MONITORING, retrieved?.state)
        assertEquals(listOf("KEYWORD"), retrieved?.triggerSources)
    }

    @Test
    fun testIncidentUpdateState() = runBlocking {
        val incident = Incident(state = IncidentState.ACTIVE_INCIDENT)
        incidentRepository.saveIncident(incident)

        incidentRepository.updateState(incident.incidentId, IncidentState.SEALED, sealedAt = 1000L, merkleRoot = "root_hash")

        val retrieved = incidentRepository.getIncidentById(incident.incidentId)
        assertEquals(IncidentState.SEALED, retrieved?.state)
        assertEquals(1000L, retrieved?.sealedAt)
        assertEquals("root_hash", retrieved?.finalMerkleRoot)
    }

    @Test
    fun testEvidenceSaveAndList() = runBlocking {
        val incidentId = UUID.randomUUID()
        val evidence = EvidenceEntry(
            incidentId = incidentId,
            type = EvidenceType.AUDIO,
            encryptedPath = "/path/to/file",
            sha256 = "hash",
            signatureReference = "sig_ref"
        )
        evidenceRepository.saveEvidence(evidence)

        val list = evidenceRepository.getEvidenceForIncident(incidentId).first()
        assertEquals(1, list.size)
        assertEquals(evidence.evidenceId, list[0].evidenceId)
    }

    @Test
    fun testContactSaveAndList() = runBlocking {
        val contact = NotifyContact(
            displayName = "Bob",
            type = ContactType.SMS_ONLY,
            phoneNumber = "+9876543210"
        )
        contactRepository.saveContact(contact)

        val contacts = contactRepository.getContacts().first()
        assertEquals(1, contacts.size)
        assertEquals("Bob", contacts[0].displayName)

        contactRepository.deleteContact(contact.contactId)
        val afterDelete = contactRepository.getContacts().first()
        assertEquals(0, afterDelete.size)
    }
}

class FakeIncidentDao : org.sahara.core.data.db.IncidentDao {
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

class FakeEvidenceDao : org.sahara.core.data.db.EvidenceDao {
    private val list = mutableListOf<EvidenceEntryEntity>()

    override fun getEvidenceForIncident(incidentId: String) = kotlinx.coroutines.flow.flowOf(list.filter { it.incidentId == incidentId })

    override suspend fun insertEvidence(evidence: EvidenceEntryEntity) {
        list.add(evidence)
    }
}

class FakeAuditEventDao : org.sahara.core.data.db.AuditEventDao {
    private val list = mutableListOf<AuditEventEntity>()

    override fun getAuditLogs() = kotlinx.coroutines.flow.flowOf(list.toList())

    override suspend fun insertAudit(audit: AuditEventEntity) {
        list.add(audit)
    }
}

class FakeNotifyContactDao : org.sahara.core.data.db.NotifyContactDao {
    private val map = mutableMapOf<String, NotifyContactEntity>()

    override fun getContacts() = kotlinx.coroutines.flow.flowOf(map.values.toList())

    override suspend fun insertContact(contact: NotifyContactEntity) {
        map[contact.contactId] = contact
    }

    override suspend fun deleteContact(id: String) {
        map.remove(id)
    }
}
