package org.sahara.features.panic

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.sahara.core.data.repository.AuditRepositoryImpl
import org.sahara.core.data.repository.IncidentRepositoryImpl
import org.sahara.core.domain.models.IncidentState
import org.sahara.core.testing.fakes.FakeAuditEventDao
import org.sahara.core.testing.fakes.FakeIncidentDao
import org.sahara.features.incident.statemachine.IncidentStateMachine
import org.sahara.features.panic.controller.PanicController
import org.sahara.features.panic.controller.PanicState

@OptIn(ExperimentalCoroutinesApi::class)
class PanicControllerUnitTest {

    private lateinit var stateMachine: IncidentStateMachine
    private lateinit var panicController: PanicController
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        val fakeIncidentDao = FakeIncidentDao()
        val fakeAuditDao = FakeAuditEventDao()
        val incidentRepository = IncidentRepositoryImpl(fakeIncidentDao)
        val auditRepository = AuditRepositoryImpl(fakeAuditDao)
        stateMachine = IncidentStateMachine(incidentRepository, auditRepository)
        panicController = PanicController(stateMachine, cancellationWindowSeconds = 1, scope = TestScope(testDispatcher))
    }

    @Test
    fun testImmediatePanicActivation() = runTest(testDispatcher) {
        assertEquals(PanicState.Idle, panicController.panicState.value)

        panicController.triggerPanicImmediately("VOLUME_GESTURE")

        assertEquals(PanicState.Active, panicController.panicState.value)
        assertEquals(IncidentState.ACTIVE_INCIDENT, stateMachine.currentState.value)
        assertEquals("VOLUME_GESTURE", stateMachine.currentIncident.value?.triggerSources?.first())
    }

    @Test
    fun testCancelPanicBeforeActivation() = runTest(testDispatcher) {
        panicController.triggerPanic("IN_APP_BUTTON")
        panicController.cancelPanic()

        assertEquals(PanicState.Cancelled, panicController.panicState.value)
        assertEquals(IncidentState.IDLE, stateMachine.currentState.value)
    }
}
