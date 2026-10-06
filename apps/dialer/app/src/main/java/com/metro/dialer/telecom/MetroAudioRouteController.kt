package com.metro.dialer.telecom

import android.os.Build
import android.os.OutcomeReceiver
import android.telecom.CallEndpoint
import android.telecom.CallEndpointException
import android.telecom.InCallService
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

/**
 * API 34+ call-endpoint routing. The UI works with [MetroCallEndpoint] domain objects; this maps
 * back to the platform [CallEndpoint] advertised by Telecom (cached from
 * `onAvailableCallEndpointsChanged`) and requests the change.
 */
object MetroAudioRouteController {

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun requestEndpoint(
        service: InCallService,
        endpoint: MetroCallEndpoint,
        available: List<CallEndpoint>,
    ) {
        val target = available.firstOrNull {
            MetroCallAudioMapper.mapEndpoint(it).id == endpoint.id
        } ?: return
        runCatching {
            service.requestCallEndpointChange(
                target,
                ContextCompat.getMainExecutor(service),
                object : OutcomeReceiver<Void, CallEndpointException> {
                    override fun onResult(result: Void?) = Unit
                    override fun onError(error: CallEndpointException) = Unit
                },
            )
        }
    }
}
