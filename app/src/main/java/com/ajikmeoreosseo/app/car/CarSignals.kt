package com.ajikmeoreosseo.app.car

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.constant.control.WiperState
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.DoorPositionListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.SeatBeltBuckledStateListener
import ai.pleos.playground.vehicle.listener.SeatOccupancyListener
import ai.pleos.playground.vehicle.listener.TurnSignalStateListener
import ai.pleos.playground.vehicle.listener.WiperStateListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

/** 이야기가 반응하는 차량 신호. 값이 실제로 바뀔 때만 전달됩니다. */
sealed interface CarEvent {
    data class TurnSignal(val side: Side) : CarEvent {
        enum class Side { OFF, LEFT, RIGHT }
    }

    data class Wiper(val on: Boolean) : CarEvent
    data class Brake(val pressed: Boolean) : CarEvent
    data class Gear(val position: Position) : CarEvent {
        enum class Position { PARK, REVERSE, NEUTRAL, DRIVE, UNKNOWN }
    }

    /** 뒷좌석(2열) 좌·우 중 한 자리라도 앉아 있으면 true. */
    data class RearSeat(val occupied: Boolean) : CarEvent

    /** 앉아 있는 뒷좌석이 모두 벨트를 맸으면 true. */
    data class RearBelt(val buckled: Boolean) : CarEvent
    data class RearDoor(val open: Boolean) : CarEvent
}

/**
 * Pleos Vehicle SDK에서 이야기에 필요한 신호만 구독해 [CarEvent]로 바꿔 줍니다.
 * 모든 콜백은 메인 스레드에서 [onEvent]로 전달됩니다.
 */
class CarSignals(context: Context, private val onEvent: (CarEvent) -> Unit) {

    private val vehicle = Vehicle(context.applicationContext)
    private val main = Handler(Looper.getMainLooper())
    private val unregister = mutableListOf<() -> Unit>()
    private val last = HashMap<String, CarEvent>()

    private val rearSides = listOf(SeatValue.ROW_2_LEFT, SeatValue.ROW_2_RIGHT)
    private val occupied = HashMap<SeatValue, Boolean>()
    private val buckled = HashMap<SeatValue, Boolean>()

    /** 자리마다 벨트 상태를 차가 알려 주는지. 모르는 자리(신호 없음, 실패)는 벨트를 기다리지 않습니다. */
    private val beltKnown = HashMap<SeatValue, Boolean>()
    private val doorOpen = HashMap<DoorValue, Boolean>()

    fun start() {
        vehicle.initialize()
        listenTurnSignal()
        listenWiper()
        listenBrake()
        listenGear()
        listenRearSeats()
        listenRearDoors()
    }

    fun release() {
        unregister.forEach { runCatching(it) }
        unregister.clear()
        runCatching { vehicle.release() }
    }

    // region 신호별 구독

    private fun listenTurnSignal() {
        val api = vehicle.getTurnSignal()
        fun handle(value: TurnSignalValue?) = emit(
            "turn",
            CarEvent.TurnSignal(
                when (value) {
                    TurnSignalValue.LEFT -> CarEvent.TurnSignal.Side.LEFT
                    TurnSignalValue.RIGHT -> CarEvent.TurnSignal.Side.RIGHT
                    else -> CarEvent.TurnSignal.Side.OFF
                }
            ),
        )

        val listener = object : TurnSignalStateListener {
            override fun onTurnSignalStateUpdated(state: TurnSignalValue?) = handle(state)
            override fun onFailed(e: Exception) = warn("turnSignal", e)
        }
        api.registerTurnSignalState(listener)
        unregister += { api.unregisterTurnSignalState(listener) }
        api.getCurrentTurnSignalState({ handle(it) }, { warn("turnSignal.get", it) })
    }

    private fun listenWiper() {
        val api = vehicle.getWiper()
        val front = WindowArea(WindowValue.FRONT_WINDSHIELD)
        fun handle(state: WiperState?) = emit("wiper", CarEvent.Wiper(state == WiperState.ON))

        val listener = object : WiperStateListener {
            override fun onWiperStateUpdated(state: WiperState?) = handle(state)
            override fun onFailed(e: Exception) = warn("wiper", e)
        }
        api.registerWiperState(front, listener)
        unregister += { api.unregisterWiperState(front, listener) }
        api.getCurrentWiperState(front, { handle(it) }, { warn("wiper.get", it) })
    }

    private fun listenBrake() {
        val api = vehicle.getBrake()
        fun handle(pressed: Boolean?) = emit("brake", CarEvent.Brake(pressed == true))

        val listener = object : BrakePedalStatusListener {
            override fun onBrakePedalStatusUpdated(pressed: Boolean?) = handle(pressed)
            override fun onFailed(e: Exception) = warn("brake", e)
        }
        api.registerBrakePedalStatus(listener)
        unregister += { api.unregisterBrakePedalStatus(listener) }
        api.getCurrentBrakePedalStatus({ handle(it) }, { warn("brake.get", it) })
    }

    private fun listenGear() {
        val api = vehicle.getGear()
        fun handle(gear: VehicleGear?) = emit(
            "gear",
            CarEvent.Gear(
                when (gear) {
                    VehicleGear.GEAR_PARK -> CarEvent.Gear.Position.PARK
                    VehicleGear.GEAR_REVERSE -> CarEvent.Gear.Position.REVERSE
                    VehicleGear.GEAR_NEUTRAL -> CarEvent.Gear.Position.NEUTRAL
                    null, VehicleGear.GEAR_UNKNOWN -> CarEvent.Gear.Position.UNKNOWN
                    else -> CarEvent.Gear.Position.DRIVE
                }
            ),
        )

        val listener = object : GearSelectionListener {
            override fun onSelected(gear: VehicleGear?) = handle(gear)
            override fun onFailed(e: Exception) = warn("gear", e)
        }
        api.registerGearSelection(listener)
        unregister += { api.unregisterGearSelection(listener) }
        api.getCurrentGearSelectionState({ handle(it) }, { warn("gear.get", it) })
    }

    private fun listenRearSeats() {
        val api = vehicle.getSeat()
        for (side in rearSides) {
            val area = SeatArea(side)

            val occupancy = object : SeatOccupancyListener {
                override fun onSeatOccupancyUpdated(isOccupied: Boolean?) = onSeat(side, isOccupied)
                override fun onFailed(e: Exception) = warn("seat.$side", e)
            }
            api.registerSeatOccupancy(area, occupancy)
            unregister += { api.unregisterSeatOccupancy(area, occupancy) }
            api.getCurrentSeatOccupancy(area, { onSeat(side, it) }, { warn("seat.get.$side", it) })

            val belt = object : SeatBeltBuckledStateListener {
                override fun onSeatBeltBuckledStateUpdated(isBuckled: Boolean?) = onBelt(side, isBuckled)
                override fun onFailed(e: Exception) {
                    main.post { beltKnown[side] = false; publishRear() }
                    warn("belt.$side", e)
                }
            }
            api.registerSeatBeltBuckledState(area, belt)
            unregister += { api.unregisterSeatBeltBuckledState(area, belt) }
            api.getCurrentSeatBeltBuckledState(area, { onBelt(side, it) }, { warn("belt.get.$side", it) })
        }
    }

    private fun listenRearDoors() {
        val api = vehicle.getDoor()
        for (side in listOf(DoorValue.ROW_2_LEFT, DoorValue.ROW_2_RIGHT)) {
            val area = DoorArea(side)
            fun handle(position: Int?) = main.post {
                doorOpen[side] = (position ?: 0) > 0
                emitNow("rearDoor", CarEvent.RearDoor(doorOpen.values.any { it }))
            }

            val listener = object : DoorPositionListener {
                override fun onPositionUpdated(pos: Int?) { handle(pos) }
                override fun onFailed(e: Exception) = warn("door.$side", e)
            }
            api.registerDoorPosition(area, listener)
            unregister += { api.unregisterDoorPosition(area, listener) }
            api.getCurrentDoorPosition(area, { handle(it) }, { warn("door.get.$side", it) })
        }
    }

    // endregion

    private fun onSeat(side: SeatValue, value: Boolean?) {
        main.post {
            occupied[side] = value != null && value != SDK_REPORTS_VACANT_AS_TRUE
            publishRear()
        }
    }

    private fun onBelt(side: SeatValue, value: Boolean?) {
        main.post {
            buckled[side] = value == true
            beltKnown[side] = value != null
            publishRear()
        }
    }

    private fun publishRear() {
        val seated = rearSides.filter { occupied[it] == true }
        emitNow("rearSeat", CarEvent.RearSeat(seated.isNotEmpty()))
        // 벨트 상태를 알 수 없는 자리는 맨 것으로 봅니다(버튼 없이 차량 신호로만 판단하므로, 모르면 막지 않습니다).
        emitNow("rearBelt", CarEvent.RearBelt(seated.isNotEmpty() && seated.all { buckled[it] == true || beltKnown[it] != true }))
    }

    private fun emit(key: String, event: CarEvent) {
        main.post { emitNow(key, event) }
    }

    /** 메인 스레드에서만 호출합니다. 같은 값이 반복되면 무시합니다. */
    private fun emitNow(key: String, event: CarEvent) {
        if (last[key] == event) return
        last[key] = event
        Log.d(TAG, "event: $event")
        onEvent(event)
    }

    private fun warn(what: String, e: Exception) {
        Log.w(TAG, "$what failed: ${e.message}")
    }

    private companion object {
        const val TAG = "CarSignals"

        /**
         * Vehicle SDK 2.3.2는 SEAT_OCCUPANCY 값이 1일 때 true를 넘깁니다.
         * 그런데 Pleos Connect Emulator v2.1.0의 차량 신호는 1이 빈자리, 2가 착석이어서
         * (Virtual Console로 확인) 결과적으로 true가 '빈자리'를 뜻합니다.
         * SDK나 실차에서 의미가 바로잡히면 이 값만 false로 바꾸면 됩니다.
         */
        const val SDK_REPORTS_VACANT_AS_TRUE = true
    }
}
