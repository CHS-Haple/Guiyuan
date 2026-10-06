package com.chaners.guiyuan.xposed

internal object StatusSlotReservation {
    const val STATUS_BAR_ICON_LIST =
        "com.android.systemui.statusbar.phone.ui.StatusBarIconList"

    private const val SLOTS_FIELD = "mSlots"
    private const val VIEW_ONLY_SLOTS_FIELD = "mViewOnlySlots"
    private const val FIND_OR_INSERT_SLOT = "findOrInsertSlot"

    fun reserveTail(
        iconList: Any,
        slot: String,
    ): ReservationResult {
        if (iconList.javaClass.name != STATUS_BAR_ICON_LIST) {
            return ReservationResult.Failure(
                Result.Failure("status-bar-icon-list-type-mismatch"),
            )
        }

        @Suppress("UNCHECKED_CAST")
        val slots =
            readField(iconList, SLOTS_FIELD) as? MutableList<Any?>
                ?: return ReservationResult.Failure(
                    Result.Failure("slot-list-unreadable"),
                )
        val viewOnlySlots =
            readField(iconList, VIEW_ONLY_SLOTS_FIELD) as? List<*>
                ?: return ReservationResult.Failure(
                    Result.Failure("view-only-slot-list-unreadable"),
                )

        if (!sameBackingProjection(slots, viewOnlySlots)) {
            return ReservationResult.Failure(
                Result.Failure("view-only-slot-projection-mismatch"),
            )
        }

        val existingIndices =
            slots.indices.filter { index ->
                slotName(slots[index]) == slot
            }
        if (existingIndices.isNotEmpty()) {
            if (existingIndices.size != 1) {
                return ReservationResult.Failure(
                    Result.Failure("existing-slot-duplicate"),
                )
            }
            val existingIndex = existingIndices.single()
            if (
                existingIndex != slots.lastIndex ||
                viewOnlySlots.getOrNull(existingIndex) !== slots[existingIndex]
            ) {
                return ReservationResult.Failure(
                    Result.Failure("existing-slot-not-tail"),
                )
            }
            return ReservationResult.Ready(
                reservation = Reservation.noOp(),
                result =
                    Result.Ready(
                        created = false,
                        nativeIndex = existingIndex,
                        fromIndex = existingIndex,
                        toIndex = existingIndex,
                        slotCount = slots.size,
                        viewOnlySynced = true,
                        originalOrderPreserved = true,
                    ),
            )
        }

        val findOrInsert =
            iconList.javaClass
                .allMethods()
                .firstOrNull { method ->
                    method.name == FIND_OR_INSERT_SLOT &&
                        method.parameterTypes.contentEquals(
                            arrayOf<Class<*>>(String::class.java),
                        ) &&
                        method.returnType == Integer.TYPE
                }
                ?: return ReservationResult.Failure(
                    Result.Failure("find-or-insert-slot-method-missing"),
                )
        findOrInsert.isAccessible = true

        val original = slots.toList()
        val nativeIndex =
            runCatching {
                (findOrInsert.invoke(iconList, slot) as Number).toInt()
            }.getOrElse { error ->
                val restored = restore(slots, viewOnlySlots, original)
                return ReservationResult.Failure(
                    Result.Failure(
                        if (restored) {
                            "find-or-insert-slot-" +
                                (error.message ?: error.javaClass.simpleName)
                        } else {
                            "find-or-insert-slot-and-rollback-failed"
                        },
                    ),
                )
            }

        val createdIndices =
            slots.indices.filter { index ->
                slotName(slots[index]) == slot
            }
        if (
            createdIndices.size != 1 ||
            slots.size != original.size + 1
        ) {
            val restored = restore(slots, viewOnlySlots, original)
            return ReservationResult.Failure(
                Result.Failure(
                    if (restored) {
                        "slot-create-verification-failed"
                    } else {
                        "slot-create-verification-and-rollback-failed"
                    },
                ),
            )
        }

        val createdIndex = createdIndices.single()
        val createdSlot = slots[createdIndex]
        val mutation =
            runCatching {
                slots.removeAt(createdIndex)
                slots.add(createdSlot)
            }
        if (mutation.isFailure) {
            val restored = restore(slots, viewOnlySlots, original)
            return ReservationResult.Failure(
                Result.Failure(
                    if (restored) {
                        "slot-tail-placement-" +
                            (mutation.exceptionOrNull()?.javaClass?.simpleName ?: "failed")
                    } else {
                        "slot-tail-placement-and-rollback-failed"
                    },
                ),
            )
        }

        val toIndex = slots.lastIndex
        val originalOrderPreserved =
            slots.size == original.size + 1 &&
                original.indices.all { index ->
                    slots[index] === original[index]
                }
        val viewOnlySynced =
            sameBackingProjection(slots, viewOnlySlots) &&
                viewOnlySlots.lastOrNull() === createdSlot
        val tailReady =
            slotName(slots.lastOrNull()) == slot &&
                originalOrderPreserved &&
                viewOnlySynced

        if (!tailReady) {
            val restored = restore(slots, viewOnlySlots, original)
            return ReservationResult.Failure(
                Result.Failure(
                    if (restored) {
                        "slot-tail-verification-failed"
                    } else {
                        "slot-tail-verification-and-rollback-failed"
                    },
                ),
            )
        }

        return ReservationResult.Ready(
            reservation =
                Reservation(
                    slots = slots,
                    viewOnlySlots = viewOnlySlots,
                    original = original,
                ),
            result =
                Result.Ready(
                    created = true,
                    nativeIndex = nativeIndex,
                    fromIndex = createdIndex,
                    toIndex = toIndex,
                    slotCount = slots.size,
                    viewOnlySynced = true,
                    originalOrderPreserved = true,
                ),
        )
    }

    private fun sameBackingProjection(
        slots: List<*>,
        viewOnlySlots: List<*>,
    ): Boolean =
        slots.size == viewOnlySlots.size &&
            slots.indices.all { index ->
                slots[index] === viewOnlySlots[index]
            }

    private fun restore(
        slots: MutableList<Any?>,
        viewOnlySlots: List<*>,
        original: List<Any?>,
    ): Boolean =
        runCatching {
            slots.clear()
            slots.addAll(original)
            slots.size == original.size &&
                original.indices.all { index ->
                    slots[index] === original[index] &&
                        viewOnlySlots.getOrNull(index) === original[index]
                } &&
                viewOnlySlots.size == original.size
        }.getOrDefault(false)

    private fun slotName(value: Any?): String? {
        if (value == null) return null
        if (value is String) return value

        listOf("mName", "name", "slot", "mSlot").forEach { fieldName ->
            val candidate = readField(value, fieldName) as? String
            if (!candidate.isNullOrBlank()) {
                return candidate
            }
        }

        val accessor =
            value.javaClass
                .allMethods()
                .firstOrNull { method ->
                    method.parameterCount == 0 &&
                        method.returnType == String::class.java &&
                        method.name in setOf("getName", "getSlot", "getSlotName")
                }
                ?: return null

        return runCatching {
            accessor.isAccessible = true
            accessor.invoke(value) as? String
        }.getOrNull()
    }

    private fun readField(
        target: Any,
        name: String,
    ): Any? {
        val field =
            generateSequence(target.javaClass) { clazz -> clazz.superclass }
                .mapNotNull { clazz ->
                    clazz.declaredFields.firstOrNull { candidate ->
                        candidate.name == name
                    }
                }
                .firstOrNull()
                ?: return null
        return runCatching {
            field.isAccessible = true
            field.get(target)
        }.getOrNull()
    }

    private fun Class<*>.allMethods() =
        generateSequence(this) { clazz -> clazz.superclass }
            .flatMap { clazz -> clazz.declaredMethods.asSequence() }

    internal class Reservation(
        private val slots: MutableList<Any?>?,
        private val viewOnlySlots: List<*>?,
        private val original: List<Any?>?,
    ) {
        fun rollback(): Boolean {
            val targetSlots = slots ?: return true
            val targetViewOnly = viewOnlySlots ?: return true
            val targetOriginal = original ?: return true
            return restore(
                slots = targetSlots,
                viewOnlySlots = targetViewOnly,
                original = targetOriginal,
            )
        }

        companion object {
            fun noOp(): Reservation =
                Reservation(
                    slots = null,
                    viewOnlySlots = null,
                    original = null,
                )
        }
    }

    internal sealed interface ReservationResult {
        data class Ready(
            val reservation: Reservation,
            val result: Result.Ready,
        ) : ReservationResult

        data class Failure(
            val result: Result.Failure,
        ) : ReservationResult
    }

    internal sealed interface Result {
        val logLine: String

        data class Ready(
            val created: Boolean,
            val nativeIndex: Int,
            val fromIndex: Int,
            val toIndex: Int,
            val slotCount: Int,
            val viewOnlySynced: Boolean,
            val originalOrderPreserved: Boolean,
        ) : Result {
            override val logLine: String
                get() =
                    "nativeSlotOrder reserved slot=" +
                        NativeParticipantUi.SLOT +
                        " created=" + created +
                        " nativeIndex=" + nativeIndex +
                        " from=" + fromIndex +
                        " to=" + toIndex +
                        " slots=" + slotCount +
                        " viewOnlySynced=" + viewOnlySynced +
                        " originalOrderPreserved=" + originalOrderPreserved +
                        " mode=controller-pre-init nativeGeometryWrites=0"
        }

        data class Failure(
            val reason: String,
        ) : Result {
            override val logLine: String
                get() =
                    "nativeSlotOrder unchanged slot=" +
                        NativeParticipantUi.SLOT +
                        " reason=" + reason +
                        " mode=controller-pre-init nativeGeometryWrites=0"
        }
    }
}
