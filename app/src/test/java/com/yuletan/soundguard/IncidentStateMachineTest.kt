package com.yuletan.soundguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class IncidentStateMachineTest {
    @Test
    fun highSeverityWaitsForBeneficiaryBeforeNotifyingCaregiver() {
        val machine = IncidentStateMachine()
        machine.setCaregiverCount(2)

        val incident = machine.detect("Smoke alarm", SoundSeverity.High, 0.9f, 1_000L)

        assertEquals(IncidentStatus.WaitingUser, incident?.status)
        assertEquals(121_000L, incident?.nextDeadlineAt)
        assertEquals(IncidentStatus.WaitingUser, machine.advance(120_999L)?.status)
        assertEquals(IncidentStatus.CaregiverNotified, machine.advance(121_000L)?.status)
        assertEquals(0, machine.activeIncident()?.caregiverIndex)
    }

    @Test
    fun beneficiarySafeResponseClosesIncidentAsFalseAlarm() {
        val machine = IncidentStateMachine()
        machine.detect("Glass break", SoundSeverity.High, 0.8f, 0L)

        val result = machine.respondAsBeneficiary(BeneficiaryResponse.Safe, 500L)

        assertEquals(IncidentStatus.FalseAlarm, result?.status)
        assertEquals(500L, result?.resolvedAt)
        assertEquals(1, machine.history().size)
    }

    @Test
    fun caregiverTimeoutEscalatesInOrderAndStopsAfterLastCaregiver() {
        val machine = IncidentStateMachine()
        machine.setCaregiverCount(2)
        machine.detect("Alarm", SoundSeverity.High, 0.95f, 0L)

        machine.advance(IncidentStateMachine.TWO_MINUTES_MS)
        assertEquals(IncidentStatus.CaregiverNotified, machine.activeIncident()?.status)
        machine.advance(IncidentStateMachine.TWO_MINUTES_MS * 2)
        assertEquals(IncidentStatus.Escalated, machine.activeIncident()?.status)
        assertEquals(1, machine.activeIncident()?.caregiverIndex)
        assertNotNull(machine.activeIncident()?.nextDeadlineAt)
        machine.advance(IncidentStateMachine.TWO_MINUTES_MS * 3)
        assertEquals(IncidentStatus.Escalated, machine.activeIncident()?.status)
        assertNull(machine.activeIncident()?.nextDeadlineAt)
    }

    @Test
    fun lowSeverityAlertDoesNotStartResponseWindow() {
        val machine = IncidentStateMachine()

        val incident = machine.detect("Door knock", SoundSeverity.Low, 0.6f, 10L)

        assertEquals(IncidentStatus.Detected, incident?.status)
        assertNull(incident?.nextDeadlineAt)
    }
}
