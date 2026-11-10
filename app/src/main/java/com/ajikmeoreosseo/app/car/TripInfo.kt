package com.ajikmeoreosseo.app.car

import ai.pleos.playground.navi.constants.TurnType
import ai.pleos.playground.navi.data.CameraAlert
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.DrivingInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

/** 내비가 알려 주는 것 가운데 이야기와 운전자 안내에 쓰는 값. 경로가 없으면 전부 비어 있습니다. */
data class NaviSnapshot(
    /** 목적지까지 남은 시간(초). 모르면 null. */
    val remainingSec: Int? = null,
    /** 목적지까지 남은 거리(미터). 모르면 null. */
    val remainingMeters: Int? = null,
    val destinationName: String? = null,
    /** 지금 달리는 길의 제한 속도(km/h). 모르면 null. */
    val speedLimit: Int? = null,
    /** 다음 안내: 어느 쪽으로, 몇 미터 앞에서. */
    val nextTurn: String? = null,
    val nextTurnMeters: Int? = null,
    /** 가장 가까운 과속 카메라: 제한 속도와 남은 거리. */
    val cameraSpeedLimit: Int? = null,
    val cameraMeters: Int? = null,
    val cameraId: String? = null,
) {
    val hasRoute: Boolean get() = remainingSec != null || remainingMeters != null
}

/**
 * Pleos NaviHelper SDK에서 경로 상태를 받아 하나의 [NaviSnapshot]으로 합칩니다.
 *
 * 주의: SDK의 duration 값 단위는 문서에서 확인하지 못해 '초'로 가정했습니다.
 * 실제 경로 안내로 확인한 뒤 필요하면 [toSeconds]만 고치면 됩니다.
 */
class TripInfo(
    context: Context,
    private val onNavi: (NaviSnapshot) -> Unit,
    private val onArrived: () -> Unit,
) {
    private val navi = NaviHelper(context.applicationContext)
    private val main = Handler(Looper.getMainLooper())

    var snapshot = NaviSnapshot()
        private set

    private val listener = object : NaviHelperEventListener {
        override fun onRouteStarted(info: RouteStartInfo) = update {
            Log.d(TAG, "onRouteStarted duration=${info.duration} distance=${info.distance} name=${info.name}")
            copy(remainingSec = toSeconds(info.duration), remainingMeters = info.distance.takeIf { it > 0 }, destinationName = info.name.ifBlank { null })
        }

        override fun onDrivingInfo(info: DrivingInfo) = update {
            val d = info.destination
            Log.d(TAG, "onDrivingInfo duration=${d.duration} distance=${d.distance} name=${d.name}")
            copy(remainingSec = toSeconds(d.duration), remainingMeters = d.distance.takeIf { it > 0 }, destinationName = d.name.ifBlank { destinationName })
        }

        override fun onDestinationInfo(info: DestinationInfo) = update {
            Log.d(TAG, "onDestinationInfo remainTime=${info.remainTime} remainDistance=${info.remainDistance} name=${info.name}")
            copy(
                remainingSec = toSeconds(info.remainTime) ?: remainingSec,
                remainingMeters = info.remainDistance.takeIf { it > 0 } ?: remainingMeters,
                destinationName = info.name.ifBlank { destinationName },
            )
        }

        override fun onCurrentRoadSpeedLimit(limit: Int) = update {
            Log.d(TAG, "onCurrentRoadSpeedLimit $limit")
            copy(speedLimit = limit.takeIf { it > 0 })
        }

        override fun onNavigationStatus(status: ai.pleos.playground.navi.constants.NavigationStatus) {
            Log.d(TAG, "onNavigationStatus $status")
        }

        override fun onError(code: ai.pleos.playground.navi.constants.NaviErrorCode) {
            Log.w(TAG, "navi error $code")
        }

        override fun onTBTInfo(list: List<TBTInfo>) = update {
            val next = list.firstOrNull()
            Log.d(TAG, "onTBTInfo first=${next?.type} ${next?.distance} (${list.size})")
            copy(nextTurn = next?.let { turnWord(it.type) }, nextTurnMeters = next?.distance?.toInt())
        }

        override fun onCameraAlert(list: List<CameraAlert>) = update {
            val nearest = list.filter { it.status }.minByOrNull { it.remainDistance }
            copy(cameraSpeedLimit = nearest?.speedLimit?.takeIf { it > 0 }, cameraMeters = nearest?.remainDistance, cameraId = nearest?.id)
        }

        override fun onDestinationArrived(info: DestinationArrivedInfo) {
            main.post { onArrived() }
        }

        override fun onRouteCancelled() = update { NaviSnapshot() }
        override fun onRouteEnded() = update { NaviSnapshot() }
    }

    fun start() {
        navi.initialize(
            { main.post { navi.addListener(listener) } },
            { Log.w(TAG, "NaviHelper unavailable: ${it.message}") },
        )
    }

    fun release() {
        runCatching {
            navi.removeListener(listener)
            navi.release()
        }
    }

    /** 시험용: 내비에 실제 경로 안내를 요청합니다(값의 단위와 내비 음성 동작 확인). */
    fun requestRouteForDebug(latitude: Double, longitude: Double, name: String) {
        runCatching {
            navi.requestRoute(ai.pleos.playground.navi.data.RouteInfo(latitude = latitude, longitude = longitude, poiName = name, poiId = "", address = "", poiSubId = ""))
        }.onFailure { Log.w(TAG, "requestRoute failed", it) }
    }

    fun cancelRouteForDebug() {
        runCatching { navi.cancelRoute() }
    }

    /** 시연용: 내비 없이 값을 넣어 봅니다. */
    fun inject(value: NaviSnapshot) = update { value }

    private fun update(change: NaviSnapshot.() -> NaviSnapshot) {
        main.post {
            snapshot = snapshot.change()
            onNavi(snapshot)
        }
    }

    private fun toSeconds(duration: Int): Int? = duration.takeIf { it > 0 }

    /** 다음 안내를 아이와 어른이 모두 알아듣는 말로. 모르는 종류는 null. */
    private fun turnWord(type: TurnType): String? = when (type) {
        TurnType.TURN_LEFT, TurnType.TURN_SHARP_L -> "좌회전"
        TurnType.TURN_RIGHT, TurnType.TURN_SHARP_R -> "우회전"
        TurnType.TURN_KEEP_L -> "왼쪽 길"
        TurnType.TURN_KEEP_R -> "오른쪽 길"
        TurnType.TURN_UTURN -> "유턴"
        TurnType.TURN_STRAIGHT -> "직진"
        TurnType.TURN_TUNNEL -> "터널"
        TurnType.TURN_TOLL -> "톨게이트"
        TurnType.TURN_ARRIVE -> "도착"
        TurnType.TURN_ENTER_IC_R, TurnType.TURN_ENTER_IC_L, TurnType.TURN_ENTER_IC_C -> "고속도로 진입"
        TurnType.TURN_EXIT_IC_R, TurnType.TURN_EXIT_IC_L, TurnType.TURN_EXIT_IC_C -> "고속도로 출구"
        TurnType.TURN_ENTER_ROUNDABOUT_02, TurnType.TURN_ENTER_ROUNDABOUT_03, TurnType.TURN_ENTER_ROUNDABOUT_04,
        TurnType.TURN_ENTER_ROUNDABOUT_06, TurnType.TURN_ENTER_ROUNDABOUT_08, TurnType.TURN_ENTER_ROUNDABOUT_09,
        TurnType.TURN_ENTER_ROUNDABOUT_10, TurnType.TURN_ENTER_ROUNDABOUT_12 -> "회전교차로"
        else -> null
    }

    private companion object {
        const val TAG = "TripInfo"
    }
}
