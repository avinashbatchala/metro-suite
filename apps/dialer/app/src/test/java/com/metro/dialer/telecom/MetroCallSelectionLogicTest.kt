package com.metro.dialer.telecom

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetroCallSelectionLogicTest {

    @Test
    fun emptySession_hasNoSelection() {
        val selection = MetroCallSelectionLogic.select(emptyList())
        assertNull(selection.primaryCallId)
        assertNull(selection.ringingCallId)
        assertTrue(selection.heldCallIds.isEmpty())
        assertNull(selection.conferenceCallId)
    }

    @Test
    fun activeCallIsPrimary() {
        val selection = MetroCallSelectionLogic.select(
            listOf(call("a", MetroCallState.ACTIVE)),
        )
        assertEquals("a", selection.primaryCallId)
    }

    @Test
    fun ringingIncomingOutranksActive() {
        val selection = MetroCallSelectionLogic.select(
            listOf(
                call("active", MetroCallState.ACTIVE, direction = MetroCallDirection.OUTGOING),
                call("ringing", MetroCallState.RINGING, direction = MetroCallDirection.INCOMING),
            ),
        )
        assertEquals("ringing", selection.primaryCallId)
        assertEquals("ringing", selection.ringingCallId)
    }

    @Test
    fun heldCallIsListedButNotPrimary() {
        val selection = MetroCallSelectionLogic.select(
            listOf(
                call("active", MetroCallState.ACTIVE),
                call("held", MetroCallState.HOLDING),
            ),
        )
        assertEquals("active", selection.primaryCallId)
        assertEquals(listOf("held"), selection.heldCallIds)
    }

    @Test
    fun removingOneCallKeepsTheOther() {
        val two = listOf(
            call("a", MetroCallState.ACTIVE),
            call("b", MetroCallState.HOLDING),
        )
        assertEquals("a", MetroCallSelectionLogic.select(two).primaryCallId)

        val one = two.filterNot { it.id == "a" }
        assertEquals("b", MetroCallSelectionLogic.select(one).primaryCallId)
        assertTrue(MetroCallSelectionLogic.select(one).heldCallIds.contains("b"))
    }

    @Test
    fun conferenceHostAndChildrenSelected() {
        val selection = MetroCallSelectionLogic.select(
            listOf(
                call("host", MetroCallState.ACTIVE, isConference = true, childIds = listOf("c1", "c2")),
                call("c1", MetroCallState.ACTIVE, parentId = "host"),
                call("c2", MetroCallState.ACTIVE, parentId = "host"),
            ),
        )
        assertEquals("host", selection.conferenceCallId)
        assertEquals(listOf("c1", "c2"), selection.conferenceChildIds)
    }

    @Test
    fun disconnectedOnlySessionStillHasPrimary() {
        val selection = MetroCallSelectionLogic.select(
            listOf(call("x", MetroCallState.DISCONNECTED)),
        )
        assertEquals("x", selection.primaryCallId)
    }

    private fun call(
        id: String,
        state: MetroCallState,
        direction: MetroCallDirection = MetroCallDirection.OUTGOING,
        isConference: Boolean = false,
        parentId: String? = null,
        childIds: List<String> = emptyList(),
    ) = MetroTelecomCall(
        id = id,
        direction = direction,
        state = state,
        phoneNumber = "+1000$id",
        displayName = id,
        phoneLabel = null,
        photoUri = null,
        contactLookupKey = null,
        contactId = null,
        connectTimeMillis = null,
        carrierLabel = null,
        canHold = true,
        canMerge = true,
        isConference = isConference,
        parentId = parentId,
        childIds = childIds,
    )
}
