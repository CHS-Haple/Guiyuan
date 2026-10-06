package com.chaners.guiyuan.xposed

internal object ParticipantContractProbe {
    fun inspect(host: Any): Snapshot {
        val resolution = ParticipantAccess.resolve(host)
        val handles =
            when (resolution) {
                is ParticipantAccess.ResolveResult.Ready ->
                    resolution.handles

                is ParticipantAccess.ResolveResult.Failure ->
                    return Snapshot.unavailable(resolution.reason)
            }

        val managerClass = handles.manager.javaClass
        val controllerClass = handles.controller.javaClass
        val holderClass = handles.holderClass
        val iconViewClass = handles.iconViewClass
        val displayableClass = handles.displayableClass

        val controllerMatches =
            controllerClass.name.endsWith("StatusBarIconControllerImpl")
        val managerMatches =
            managerClass.name.endsWith("DarkIconManager") ||
                managerClass.name.endsWith("IconManager")
        val groupMatches =
            ParticipantAccess.resourceEntryName(handles.group) ==
                "statusIcons"

        val setIconSignatures =
            ParticipantAccess.methodSignatures(
                clazz = controllerClass,
                names = setOf("setIcon"),
            )
        val setIconHolder =
            ParticipantAccess.setIconHolderAvailable(
                controllerClass,
            )
        val resourceSetter =
            ParticipantAccess.resourceSetter(
                controllerClass,
            )
        val setIconVisibility =
            ParticipantAccess.visibilityMethod(
                controllerClass,
            ) != null
        val removal =
            ParticipantAccess.removal(
                controllerClass,
            )
        val removeSignatures =
            ParticipantAccess.methodSignatures(
                clazz = controllerClass,
                names = setOf("removeIcon", "removeAllIconsForSlot"),
            )

        val addIconGroup =
            ParticipantAccess.hasMethodSignature(
                clazz = controllerClass,
                name = "addIconGroup",
                parameterTypes = listOf(managerClass.name),
            )
        val removeIconGroup =
            ParticipantAccess.hasMethodSignature(
                clazz = controllerClass,
                name = "removeIconGroup",
                parameterTypes = listOf(managerClass.name),
            )
        val addHolder =
            ParticipantAccess.hasMethodSignature(
                clazz = managerClass,
                name = "addHolder",
                parameterTypes =
                    listOf(
                        "int",
                        "java.lang.String",
                        "boolean",
                        ParticipantAccess.ICON_HOLDER,
                    ),
            )

        val holderFactories =
            ParticipantAccess.holderFactories(
                holderClass,
            )
        val holderFactoryReady = holderFactories.isNotEmpty()

        val iconViewDisplayable =
            iconViewClass != null &&
                displayableClass?.isAssignableFrom(iconViewClass) == true

        val iconViewSlotAccessor =
            iconViewClass
                ?.methods
                ?.any { method ->
                    method.name == "getSlot" &&
                        method.parameterCount == 0 &&
                        method.returnType == String::class.java
                } == true

        val systemManagedCreationReady =
            resourceSetter != null ||
                (setIconHolder && holderFactoryReady)

        val registrationContractReady =
            managerMatches &&
                groupMatches &&
                controllerMatches &&
                systemManagedCreationReady &&
                setIconVisibility &&
                removal != null &&
                iconViewDisplayable &&
                iconViewSlotAccessor

        return Snapshot(
            available = true,
            reason = null,
            managerClass = managerClass.name,
            groupClass = handles.group.javaClass.name,
            groupResource =
                ParticipantAccess.resourceEntryName(
                    handles.group,
                ),
            controllerClass = controllerClass.name,
            controllerSource = handles.controllerSource,
            controllerMatches = controllerMatches,
            managerMatches = managerMatches,
            groupMatches = groupMatches,
            setIconHolder = setIconHolder,
            resourceSetIconMode =
                resourceSetter?.mode?.name ?: "NONE",
            setIconVisibility = setIconVisibility,
            removalReady = removal != null,
            removalMode = removal?.mode?.name,
            addIconGroup = addIconGroup,
            removeIconGroup = removeIconGroup,
            addHolder = addHolder,
            holderFactoryReady = holderFactoryReady,
            iconViewDisplayable = iconViewDisplayable,
            iconViewSlotAccessor = iconViewSlotAccessor,
            systemManagedCreationReady = systemManagedCreationReady,
            registrationContractReady = registrationContractReady,
            setIconSignatures = setIconSignatures,
            removeSignatures = removeSignatures,
            holderFactorySignatures =
                holderFactories.map(
                    ParticipantAccess::methodSignature,
                ),
        )
    }

    internal data class Snapshot(
        val available: Boolean,
        val reason: String?,
        val managerClass: String?,
        val groupClass: String?,
        val groupResource: String?,
        val controllerClass: String?,
        val controllerSource: String?,
        val controllerMatches: Boolean,
        val managerMatches: Boolean,
        val groupMatches: Boolean,
        val setIconHolder: Boolean,
        val resourceSetIconMode: String,
        val setIconVisibility: Boolean,
        val removalReady: Boolean,
        val removalMode: String?,
        val addIconGroup: Boolean,
        val removeIconGroup: Boolean,
        val addHolder: Boolean,
        val holderFactoryReady: Boolean,
        val iconViewDisplayable: Boolean,
        val iconViewSlotAccessor: Boolean,
        val systemManagedCreationReady: Boolean,
        val registrationContractReady: Boolean,
        val setIconSignatures: List<String>,
        val removeSignatures: List<String>,
        val holderFactorySignatures: List<String>,
    ) {
        val logLine: String
            get() =
                "nativeParticipantContract available=" + available +
                    " reason=" + (reason ?: "none") +
                    " manager=" + (managerClass ?: "none") +
                    " group=" + (groupClass ?: "none") +
                    " groupRes=" + (groupResource ?: "none") +
                    " controller=" + (controllerClass ?: "none") +
                    " controllerSource=" + (controllerSource ?: "none") +
                    " controllerMatches=" + controllerMatches +
                    " managerMatches=" + managerMatches +
                    " groupMatches=" + groupMatches +
                    " setIconHolder=" + setIconHolder +
                    " resourceSetIconMode=" + resourceSetIconMode +
                    " setIconVisibility=" + setIconVisibility +
                    " removalReady=" + removalReady +
                    " removalMode=" + (removalMode ?: "none") +
                    " addIconGroup=" + addIconGroup +
                    " removeIconGroup=" + removeIconGroup +
                    " addHolder=" + addHolder +
                    " holderFactoryReady=" + holderFactoryReady +
                    " statusIconDisplayable=" + iconViewDisplayable +
                    " slotAccessor=" + iconViewSlotAccessor +
                    " systemManagedCreationReady=" + systemManagedCreationReady +
                    " registrationReady=" + registrationContractReady +
                    " setIconSignatures=" + setIconSignatures.joinToString("|") +
                    " removeSignatures=" + removeSignatures.joinToString("|") +
                    " holderFactories=" + holderFactorySignatures.joinToString("|") +
                    " geometryWrites=0"

        companion object {
            fun unavailable(reason: String): Snapshot =
                Snapshot(
                    available = false,
                    reason = reason,
                    managerClass = null,
                    groupClass = null,
                    groupResource = null,
                    controllerClass = null,
                    controllerSource = null,
                    controllerMatches = false,
                    managerMatches = false,
                    groupMatches = false,
                    setIconHolder = false,
                    resourceSetIconMode = "NONE",
                    setIconVisibility = false,
                    removalReady = false,
                    removalMode = null,
                    addIconGroup = false,
                    removeIconGroup = false,
                    addHolder = false,
                    holderFactoryReady = false,
                    iconViewDisplayable = false,
                    iconViewSlotAccessor = false,
                    systemManagedCreationReady = false,
                    registrationContractReady = false,
                    setIconSignatures = emptyList(),
                    removeSignatures = emptyList(),
                    holderFactorySignatures = emptyList(),
                )
        }
    }
}
