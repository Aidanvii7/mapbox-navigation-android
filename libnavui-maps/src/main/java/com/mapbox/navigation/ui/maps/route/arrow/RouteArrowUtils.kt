package com.mapbox.navigation.ui.maps.route.arrow

import androidx.core.graphics.drawable.DrawableCompat
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayerAbove
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconRotationAlignment
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.base.internal.model.route.RouteConstants
import com.mapbox.navigation.ui.base.model.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.utils.internal.extensions.getBitmap
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMisc

internal object RouteArrowUtils {
    fun obtainArrowPointsFrom(routeProgress: RouteProgress): List<Point> {
        val reversedCurrent: List<Point> =
            routeProgress.currentLegProgress?.currentStepProgress?.stepPoints?.reversed()
                ?: listOf()

        val arrowLineCurrent = LineString.fromLngLats(reversedCurrent)
        val arrowLineUpcoming = LineString.fromLngLats(routeProgress.upcomingStepPoints!!)

        if (arrowLineCurrent.coordinates().size < 2) {
            return listOf()
        }

        if (arrowLineUpcoming.coordinates().size < 2) {
            return listOf()
        }

        val arrowCurrentSliced = TurfMisc.lineSliceAlong(
            arrowLineCurrent,
            0.0,
            RouteConstants.THIRTY.toDouble(),
            TurfConstants.UNIT_METERS
        )
        val arrowUpcomingSliced = TurfMisc.lineSliceAlong(
            arrowLineUpcoming,
            0.0,
            RouteConstants.THIRTY.toDouble(),
            TurfConstants.UNIT_METERS
        )

        return arrowCurrentSliced.coordinates().reversed().plus(arrowUpcomingSliced.coordinates())
    }

    fun initializeLayers(style: Style, options: RouteArrowOptions) {
        if (!style.fullyLoaded || layersAreInitialized(style)) {
            return
        }

        val aboveLayerIdToUse = if (style.styleLayerExists(options.aboveLayerId)) {
            options.aboveLayerId
        } else {
            null
        }

        if (!style.styleSourceExists(RouteConstants.ARROW_SHAFT_SOURCE_ID)) {
            geoJsonSource(RouteConstants.ARROW_SHAFT_SOURCE_ID) {
                maxzoom(16)
                featureCollection(FeatureCollection.fromFeatures(listOf()))
                tolerance(options.tolerance)
            }.bindTo(style)
        }

        if (!style.styleSourceExists(RouteConstants.ARROW_HEAD_SOURCE_ID)) {
            geoJsonSource(RouteConstants.ARROW_HEAD_SOURCE_ID) {
                maxzoom(16)
                featureCollection(FeatureCollection.fromFeatures(listOf()))
                tolerance(options.tolerance)
            }.bindTo(style)
        }

        if (style.getStyleImage(RouteConstants.ARROW_HEAD_ICON_CASING) != null) {
            style.removeStyleImage(RouteConstants.ARROW_HEAD_ICON_CASING)
        }

        if (
            options.arrowHeadIconBorder.intrinsicHeight > 0 &&
            options.arrowHeadIconBorder.intrinsicWidth > 0
        ) {
            val arrowHeadCasingDrawable = DrawableCompat.wrap(
                options.arrowHeadIconBorder
            )
            DrawableCompat.setTint(
                arrowHeadCasingDrawable.mutate(),
                options.arrowBorderColor
            )
            val arrowHeadCasingBitmap = arrowHeadCasingDrawable.getBitmap()
            style.addImage(RouteConstants.ARROW_HEAD_ICON_CASING, arrowHeadCasingBitmap)
        }

        if (style.getStyleImage(RouteConstants.ARROW_HEAD_ICON) != null) {
            style.removeStyleImage(RouteConstants.ARROW_HEAD_ICON)
        }

        if (options.arrowHeadIcon.intrinsicHeight > 0 && options.arrowHeadIcon.intrinsicWidth > 0) {
            val arrowHeadDrawable = DrawableCompat.wrap(
                options.arrowHeadIcon
            )
            DrawableCompat.setTint(
                arrowHeadDrawable.mutate(),
                options.arrowColor
            )
            val arrowHeadBitmap = arrowHeadDrawable.getBitmap()
            style.addImage(RouteConstants.ARROW_HEAD_ICON, arrowHeadBitmap)
        }

        //val widthExp = Expression.fromRaw("[\"interpolate\",[\"exponential\",1.5],[\"zoom\"],4.0,[\"*\",3.0,0.5],10.0,[\"*\",4.0,0.5],13.0,[\"*\",6.0,0.5],16.0,[\"*\",10.0,0.5],19.0,[\"*\",14.0,0.5],22.0,[\"*\",18.0,0.5]]")

        // arrow shaft casing
        if (style.styleLayerExists(RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID)) {
            style.removeStyleLayer(RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID)
        }
        val arrowShaftCasingLayer = LineLayer(
            RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID,
            RouteConstants.ARROW_SHAFT_SOURCE_ID
        )
            .lineColor(
                Expression.color(
                    options.arrowBorderColor
                )
            )
            .lineWidth(
                Expression.interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(10.0)
                        product {
                            literal(7.0)
                            literal(0.7)
                        }
                    }
                    stop {
                        literal(14.0)
                        product {
                            literal(10.5)
                            literal(0.7)
                        }
                    }
                    stop {
                        literal(16.5)
                        product {
                            literal(15.5)
                            literal(0.7)
                        }
                    }
                    stop {
                        literal(19.0)
                        product {
                            literal(24.0)
                            literal(0.7)
                        }
                    }
                    stop {
                        literal(22.0)
                        product {
                            literal(29.0)
                            literal(0.7)
                        }
                    }
                }
            )
            .lineCap(LineCap.ROUND)
            .lineJoin(LineJoin.ROUND)
            .visibility(Visibility.NONE)
            .lineOpacity(
                Expression.step {
                    zoom()
                    literal(RouteConstants.OPAQUE)
                    stop {
                        literal(RouteConstants.ARROW_HIDDEN_ZOOM_LEVEL)
                        literal(RouteConstants.TRANSPARENT)
                    }
                }
            )

        // arrow head casing
        if (style.styleLayerExists(RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID)) {
            style.removeStyleLayer(RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID)
        }
        val arrowHeadCasingLayer = SymbolLayer(
            RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID,
            RouteConstants.ARROW_HEAD_SOURCE_ID
        )
            .iconImage(RouteConstants.ARROW_HEAD_ICON_CASING)
            .iconAllowOverlap(true)
            .iconIgnorePlacement(true)
            .iconSize(
                Expression.interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(RouteConstants.MIN_ARROW_ZOOM)
                        literal(RouteConstants.MIN_ZOOM_ARROW_HEAD_CASING_SCALE)
                    }
                    stop {
                        literal(RouteConstants.MAX_ARROW_ZOOM)
                        literal(RouteConstants.MAX_ZOOM_ARROW_HEAD_CASING_SCALE)
                    }
                }
            )
            .iconOffset(RouteConstants.ARROW_HEAD_OFFSET.toList())
            .iconRotationAlignment(IconRotationAlignment.MAP)
            .iconRotate(
                com.mapbox.maps.extension.style.expressions.dsl.generated.get {
                    literal(
                        RouteConstants.ARROW_BEARING
                    )
                }
            )
            .visibility(Visibility.NONE)
            .iconOpacity(
                Expression.step {
                    zoom()
                    literal(RouteConstants.OPAQUE)
                    stop {
                        literal(RouteConstants.ARROW_HIDDEN_ZOOM_LEVEL)
                        literal(RouteConstants.TRANSPARENT)
                    }
                }
            )

        // arrow shaft
        if (style.styleLayerExists(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID)) {
            style.removeStyleLayer(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID)
        }
        val arrowShaftLayer = LineLayer(
            RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID,
            RouteConstants.ARROW_SHAFT_SOURCE_ID
        )
            .lineColor(
                Expression.color(options.arrowColor)
            )
            .lineWidth(
                Expression.interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(4.0)
                        product {
                            literal(3.0)
                            literal(0.7)
                        }
                    }
                    stop {
                        literal(10.0)
                        product {
                            literal(4.0)
                            literal(0.7)
                        }
                    }
                    stop {
                        literal(13.0)
                        product {
                            literal(6.0)
                            literal(0.7)
                        }
                    }
                    stop {
                        literal(16.0)
                        product {
                            literal(10.0)
                            literal(0.7)
                        }
                    }
                    stop {
                        literal(19.0)
                        product {
                            literal(14.0)
                            literal(0.7)
                        }
                    }
                    stop {
                        literal(22.0)
                        product {
                            literal(18.0)
                            literal(0.5)
                        }
                    }
                }
            )
            .lineCap(LineCap.ROUND)
            .lineJoin(LineJoin.ROUND)
            .visibility(Visibility.NONE)
            .lineOpacity(
                Expression.step {
                    zoom()
                    literal(RouteConstants.OPAQUE)
                    stop {
                        literal(RouteConstants.ARROW_HIDDEN_ZOOM_LEVEL)
                        literal(RouteConstants.TRANSPARENT)
                    }
                }
            )

        // arrow head
        if (style.styleLayerExists(RouteLayerConstants.ARROW_HEAD_LAYER_ID)) {
            style.removeStyleLayer(RouteLayerConstants.ARROW_HEAD_LAYER_ID)
        }
        val arrowHeadLayer = SymbolLayer(
            RouteLayerConstants.ARROW_HEAD_LAYER_ID,
            RouteConstants.ARROW_HEAD_SOURCE_ID
        )
            .iconImage(RouteConstants.ARROW_HEAD_ICON)
            .iconAllowOverlap(true)
            .iconIgnorePlacement(true)
            .iconSize(
                Expression.interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(RouteConstants.MIN_ARROW_ZOOM)
                        literal(RouteConstants.MIN_ZOOM_ARROW_HEAD_SCALE)
                    }
                    stop {
                        literal(RouteConstants.MAX_ARROW_ZOOM)
                        literal(RouteConstants.MAX_ZOOM_ARROW_HEAD_SCALE)
                    }
                }
            )
            .iconOffset(RouteConstants.ARROW_HEAD_CASING_OFFSET.toList())
            .iconRotationAlignment(IconRotationAlignment.MAP)
            .iconRotate(
                com.mapbox.maps.extension.style.expressions.dsl.generated.get {
                    literal(
                        RouteConstants.ARROW_BEARING
                    )
                }
            )
            .visibility(Visibility.NONE)
            .iconOpacity(
                Expression.step {
                    zoom()
                    literal(RouteConstants.OPAQUE)
                    stop {
                        literal(RouteConstants.ARROW_HIDDEN_ZOOM_LEVEL)
                        literal(RouteConstants.TRANSPARENT)
                    }
                }
            )

        style.addLayerAbove(arrowShaftCasingLayer, aboveLayerIdToUse)
        style.addLayerAbove(arrowHeadCasingLayer, arrowShaftCasingLayer.layerId)
        style.addLayerAbove(arrowShaftLayer, arrowHeadCasingLayer.layerId)
        style.addLayerAbove(arrowHeadLayer, arrowShaftLayer.layerId)
    }

    internal fun layersAreInitialized(style: Style): Boolean {
        return style.fullyLoaded &&
            style.styleSourceExists(RouteConstants.ARROW_SHAFT_SOURCE_ID) &&
            style.styleSourceExists(RouteConstants.ARROW_HEAD_SOURCE_ID) &&
            style.styleLayerExists(RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID) &&
            style.styleLayerExists(RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID) &&
            style.styleLayerExists(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID) &&
            style.styleLayerExists(RouteLayerConstants.ARROW_HEAD_LAYER_ID)
    }
}
