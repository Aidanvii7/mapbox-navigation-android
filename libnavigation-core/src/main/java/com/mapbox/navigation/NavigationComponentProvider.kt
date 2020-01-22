package com.mapbox.navigation

import android.content.Context
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.trip.TripNotification
import com.mapbox.navigation.directions.session.DirectionsSession
import com.mapbox.navigation.directions.session.MapboxDirectionsSession
import com.mapbox.navigation.trip.service.MapboxTripService
import com.mapbox.navigation.trip.service.TripService
import com.mapbox.navigation.trip.session.MapboxTripSession
import com.mapbox.navigation.trip.session.TripSession
import com.mapbox.navigation.utils.timer.MapboxTimer

internal object NavigationComponentProvider {
    fun createDirectionsSession(
        router: Router
    ): DirectionsSession =
        MapboxDirectionsSession(router)

    fun createTripService(
        applicationContext: Context,
        tripNotification: TripNotification
    ): TripService =
        MapboxTripService(applicationContext, tripNotification)

    fun createTripSession(
        tripService: TripService,
        locationEngine: LocationEngine,
        locationEngineRequest: LocationEngineRequest
    ): TripSession = MapboxTripSession(
        tripService,
        locationEngine,
        locationEngineRequest
    )

    fun createMapboxTimer(
        restartAfter: Long,
        delayLambda: () -> Unit
    ) = MapboxTimer(restartAfter, delayLambda)
}