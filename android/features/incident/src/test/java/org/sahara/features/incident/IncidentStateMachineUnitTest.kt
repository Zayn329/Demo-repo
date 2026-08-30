package org.sahara.features.incident

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.sahara.core.data.repository.AuditRepositoryImpl
import org.sahara.core.data.repository.IncidentRepositoryImpl
import org.sahara.core.domain.models.IncidentState
import org.sahara.core.testing.fakes.FakeAuditEventDao
import org.sahara.core.testing.fakes.FakeIncidentDao
import org.sahara.features.incident.statemachine.IncidentStateMachine

class IncidentStateMachineUnitTest {

    private lateinit var incidentRepository: IncidentRepositoryImpl
    private lateinit var auditRepository: AuditRepositoryImpl
    private lateinit var stateMachine: IncidentStateMachine

    @Before
    fun setup() {
        val fakeIncidentDao = FakeIncidentDao()
        val fakeAuditDao = FakeAuditEventDao()
        incidentRepository = IncidentRepositoryImpl(fakeIncidentDao)
        auditRepository = AuditRepositoryImpl(fakeAuditDao)
        stateMachine = IncidentStateMachine(incidentRepository, auditRepository)
    }

    @Test
    fun testStartAndStopMonitoring() = runBlocking {
        assertEquals(IncidentState.IDLE, stateMachine.currentState.value)

        stateMachine.startMonitoring()
        assertEquals(IncidentState.MONITORING, stateMachine.currentState.value)

        stateMachine.stopMonitoring()
        assertEquals(IncidentState.IDLE, stateMachine.currentState.value)
    }

    @Test
    fun testFullIncidentLifecycle() = runBlocking {
        stateMachine.startMonitoring()

        stateMachine.onSuspiciousSignalDetected("KEYWORD")
        assertEquals(IncidentState.SUSPICIOUS_SIGNAL, stateMachine.currentState.value)
        assertNotNull(stateMachine.currentIncident.value)

        stateMachine.transitionToCandidate()
        assertEquals(IncidentState.CANDIDATE_INCIDENT, stateMachine.currentState.value)

        stateMachine.activateIncident("MULTI_SIGNAL_FUSION")
        assertEquals(IncidentState.ACTIVE_INCIDENT, stateMachine.currentState.value)

        stateMachine.sealIncident("merkle_root_hash_123")
        assertEquals(IncidentState.SEALED, stateMachine.currentState.value)
        assertEquals("merkle_root_hash_123", stateMachine.currentIncident.value?.finalMerkleRoot)

        val auditLogs = auditRepository.getAuditLogs().first()
        assertEquals(5, auditLogs.size)
    }

    @Test
    fun testCancelIncidentPreservesHistory() = runBlocking {
        stateMachine.startMonitoring()
        stateMachine.activateIncident("PANIC_BUTTON")
        assertEquals(IncidentState.ACTIVE_INCIDENT, stateMachine.currentState.value)

        stateMachine.cancelIncident()
        assertEquals(IncidentState.CANCELLED, stateMachine.currentState.value)

        val saved = incidentRepository.getIncidentById(stateMachine.currentIncident.value!!.incidentId)
        assertNotNull(saved)
        assertEquals(IncidentState.CANCELLED, saved?.state)
    }
}
