package com.yuletan.soundguard

import java.util.UUID

enum class IncidentStatus {
    Detected,
    WaitingUser,
    CaregiverNotified,
    CaregiverAcknowledged,
    Resolved,
    FalseAlarm,
    Escalated,
}

enum class BeneficiaryResponse {
    Safe,
    NeedHelp,
}

data class IncidentRecord(
    val id: String,
    val soundLabel: String,
    val severity: SoundSeverity,
    val confidence: Float,
    val status: IncidentStatus,
    val startedAt: Long,
    val userResponse: BeneficiaryResponse? = null,
    val caregiverIndex: Int? = null,
    val nextDeadlineAt: Long? = null,
    val resolvedAt: Long? = null,
)

/**
 * Local incident contract shared by audio monitoring and the eventual backend
 * notification worker. Time is supplied by callers so transitions are testable.
 */
class IncidentStateMachine(
    private val beneficiaryWindowMs: Long = TWO_MINUTES_MS,
    private val caregiverWindowMs: Long = TWO_MINUTES_MS,
    private val maxHistory: Int = 50,
) {
    companion object {
        const val TWO_MINUTES_MS = 120_000L
        private val TERMINAL_STATUSES = setOf(
            IncidentStatus.CaregiverAcknowledged,
            IncidentStatus.Resolved,
            IncidentStatus.FalseAlarm,
        )
    }

    private var active: IncidentRecord? = null
    private val history = ArrayDeque<IncidentRecord>()
    private var caregiverCount = 0

    fun setCaregiverCount(count: Int) {
        caregiverCount = count.coerceAtLeast(0)
    }

    fun activeIncident(): IncidentRecord? = active

    fun history(): List<IncidentRecord> = history.toList()

    fun reset() {
        active = null
        history.clear()
    }

    fun detect(
        soundLabel: String,
        severity: SoundSeverity,
        confidence: Float,
        now: Long,
    ): IncidentRecord? {
        if (severity == SoundSeverity.None) return null
        if (active != null && active!!.status !in TERMINAL_STATUSES) return active

        val incident = IncidentRecord(
            id = UUID.randomUUID().toString(),
            soundLabel = soundLabel,
            severity = severity,
            confidence = confidence.coerceIn(0f, 1f),
            status = if (severity == SoundSeverity.High) IncidentStatus.WaitingUser else IncidentStatus.Detected,
            startedAt = now,
            nextDeadlineAt = if (severity == SoundSeverity.High) now + beneficiaryWindowMs else null,
        )
        if (severity == SoundSeverity.Low) {
            history.addLast(incident)
            while (history.size > maxHistory) history.removeFirst()
            active = null
            return incident
        }
        active = incident
        return incident
    }

    fun respondAsBeneficiary(response: BeneficiaryResponse, now: Long): IncidentRecord? {
        val incident = active ?: return null
        if (incident.status != IncidentStatus.WaitingUser) return incident
        return update(
            incident.copy(
                status = if (response == BeneficiaryResponse.Safe) IncidentStatus.FalseAlarm else IncidentStatus.CaregiverNotified,
                userResponse = response,
                caregiverIndex = if (response == BeneficiaryResponse.NeedHelp) firstCaregiverOrNull() else null,
                nextDeadlineAt = if (response == BeneficiaryResponse.NeedHelp) caregiverDeadline(now) else null,
                resolvedAt = if (response == BeneficiaryResponse.Safe) now else null,
            ),
            now,
        )
    }

    fun acknowledgeCaregiver(now: Long): IncidentRecord? {
        val incident = active ?: return null
        if (incident.status !in setOf(IncidentStatus.CaregiverNotified, IncidentStatus.Escalated)) return incident
        return update(incident.copy(status = IncidentStatus.CaregiverAcknowledged, nextDeadlineAt = null, resolvedAt = now), now)
    }

    /** Advances expired response windows and returns the current incident. */
    fun advance(now: Long): IncidentRecord? {
        val incident = active ?: return null
        if (incident.nextDeadlineAt == null || now < incident.nextDeadlineAt) return incident

        return when (incident.status) {
            IncidentStatus.WaitingUser -> {
                if (caregiverCount == 0) {
                    update(incident.copy(status = IncidentStatus.Escalated, nextDeadlineAt = null), now)
                } else {
                    update(
                        incident.copy(
                            status = IncidentStatus.CaregiverNotified,
                            caregiverIndex = 0,
                            nextDeadlineAt = now + caregiverWindowMs,
                        ),
                        now,
                    )
                }
            }
            IncidentStatus.CaregiverNotified,
            IncidentStatus.Escalated -> {
                val next = (incident.caregiverIndex ?: -1) + 1
                if (next < caregiverCount) {
                    update(incident.copy(status = IncidentStatus.Escalated, caregiverIndex = next, nextDeadlineAt = now + caregiverWindowMs), now)
                } else {
                    update(incident.copy(status = IncidentStatus.Escalated, nextDeadlineAt = null), now)
                }
            }
            else -> incident
        }
    }

    private fun firstCaregiverOrNull(): Int? = if (caregiverCount > 0) 0 else null

    private fun caregiverDeadline(now: Long): Long? = if (caregiverCount > 0) now + caregiverWindowMs else null

    private fun update(next: IncidentRecord, now: Long): IncidentRecord {
        active = next
        if (next.status in TERMINAL_STATUSES) {
            history.addLast(next)
            while (history.size > maxHistory) history.removeFirst()
            if (next.status != IncidentStatus.CaregiverAcknowledged) active = next
        }
        return next
    }

}
