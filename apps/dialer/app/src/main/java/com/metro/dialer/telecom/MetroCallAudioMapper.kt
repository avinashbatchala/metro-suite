package com.metro.dialer.telecom

import android.os.Build
import android.telecom.CallAudioState
import androidx.annotation.RequiresApi

/**
 * Maps platform audio-route state into the Metro [MetroCallAudioState] domain.
 *
 * API 34+ uses [android.telecom.CallEndpoint]; older platforms use the legacy [CallAudioState]
 * route bitmask. The UI only ever sees [MetroCallAudioState].
 */
object MetroCallAudioMapper {

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun mapEndpoint(endpoint: android.telecom.CallEndpoint): MetroCallEndpoint {
        val type = when (endpoint.endpointType) {
            android.telecom.CallEndpoint.TYPE_EARPIECE -> MetroCallEndpointType.EARPIECE
            android.telecom.CallEndpoint.TYPE_SPEAKER -> MetroCallEndpointType.SPEAKER
            android.telecom.CallEndpoint.TYPE_BLUETOOTH -> MetroCallEndpointType.BLUETOOTH
            android.telecom.CallEndpoint.TYPE_WIRED_HEADSET -> MetroCallEndpointType.WIRED_HEADSET
            android.telecom.CallEndpoint.TYPE_STREAMING -> MetroCallEndpointType.STREAMING
            else -> MetroCallEndpointType.UNKNOWN
        }
        val name = endpoint.endpointName?.toString()?.takeIf { it.isNotBlank() }
            ?: defaultEndpointLabel(type)
        return MetroCallEndpoint(id = endpoint.identifier.toString(), type = type, label = name)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun mapEndpoints(
        current: android.telecom.CallEndpoint?,
        available: List<android.telecom.CallEndpoint>,
        muted: Boolean,
    ): MetroCallAudioState = MetroCallAudioState(
        muted = muted,
        currentEndpoint = current?.let { mapEndpoint(it) },
        availableEndpoints = available.map(::mapEndpoint),
    )

    fun mapLegacy(
        route: Int,
        supportedRouteMask: Int,
        muted: Boolean,
        bluetoothName: String? = null,
    ): MetroCallAudioState {
        val endpoints = buildList {
            if (supportedRouteMask and CallAudioState.ROUTE_EARPIECE != 0) {
                add(MetroCallEndpoint("earpiece", MetroCallEndpointType.EARPIECE, defaultEndpointLabel(MetroCallEndpointType.EARPIECE)))
            }
            if (supportedRouteMask and CallAudioState.ROUTE_SPEAKER != 0) {
                add(MetroCallEndpoint("speaker", MetroCallEndpointType.SPEAKER, defaultEndpointLabel(MetroCallEndpointType.SPEAKER)))
            }
            if (supportedRouteMask and CallAudioState.ROUTE_BLUETOOTH != 0) {
                add(
                    MetroCallEndpoint(
                        "bluetooth",
                        MetroCallEndpointType.BLUETOOTH,
                        bluetoothName?.takeIf { it.isNotBlank() }
                            ?: defaultEndpointLabel(MetroCallEndpointType.BLUETOOTH),
                    ),
                )
            }
            if (supportedRouteMask and CallAudioState.ROUTE_WIRED_HEADSET != 0) {
                add(
                    MetroCallEndpoint(
                        "wired",
                        MetroCallEndpointType.WIRED_HEADSET,
                        defaultEndpointLabel(MetroCallEndpointType.WIRED_HEADSET),
                    ),
                )
            }
        }
        val currentType = when (route) {
            CallAudioState.ROUTE_SPEAKER -> MetroCallEndpointType.SPEAKER
            CallAudioState.ROUTE_BLUETOOTH -> MetroCallEndpointType.BLUETOOTH
            CallAudioState.ROUTE_WIRED_HEADSET -> MetroCallEndpointType.WIRED_HEADSET
            else -> MetroCallEndpointType.EARPIECE
        }
        return MetroCallAudioState(
            muted = muted,
            currentEndpoint = endpoints.firstOrNull { it.type == currentType }
                ?: MetroCallEndpoint(currentType.name.lowercase(), currentType, defaultEndpointLabel(currentType)),
            availableEndpoints = endpoints,
        )
    }

    fun defaultEndpointLabel(type: MetroCallEndpointType): String = when (type) {
        MetroCallEndpointType.EARPIECE -> "phone"
        MetroCallEndpointType.SPEAKER -> "speaker"
        MetroCallEndpointType.BLUETOOTH -> "Bluetooth"
        MetroCallEndpointType.WIRED_HEADSET -> "headset"
        MetroCallEndpointType.HEARING_AID -> "hearing aid"
        MetroCallEndpointType.STREAMING -> "streaming"
        MetroCallEndpointType.UNKNOWN -> "audio"
    }
}
