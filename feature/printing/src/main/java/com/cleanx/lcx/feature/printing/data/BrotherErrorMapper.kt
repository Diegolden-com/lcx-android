package com.cleanx.lcx.feature.printing.data

/**
 * Maps Brother SDK numeric error codes to human-readable [PrintResult.Error] values.
 *
 * Error codes are taken from the Brother Mobile SDK documentation.
 * All user-facing messages are in Spanish.
 */
object BrotherErrorMapper {

    fun mapSdkError(errorCode: Int): PrintResult.Error {
        return when (errorCode) {
            0x01 -> PrintResult.Error("COVER_OPEN", "Tapa de impresora abierta")
            0x02 -> PrintResult.Error("NO_PAPER", "Sin papel/etiquetas")
            0x04 -> PrintResult.Error("BATTERY_LOW", "Batería baja")
            0x08 -> PrintResult.Error("COMMUNICATION_ERROR", "Error de comunicación")
            0x10 -> PrintResult.Error("OVERHEATING", "Impresora sobrecalentada")
            0x20 -> PrintResult.Error("PAPER_JAM", "Atasco de papel")
            0x40 -> PrintResult.Error("HIGH_VOLTAGE_ADAPTER", "Adaptador incorrecto")
            else -> PrintResult.Error(
                "UNKNOWN_PRINTER_ERROR",
                "Error desconocido de impresora ($errorCode)",
            )
        }
    }

    /**
     * Maps Brother SDK v4 enum error codes into user-facing [PrintResult.Error].
     *
     * Example input values:
     * - `NoError`
     * - `PrinterStatusErrorPaperEmpty`
     * - `PrinterStatusErrorCoverOpen`
     * - `SetLabelSizeError`
     */
    fun mapSdkV4Error(errorCode: String, description: String? = null): PrintResult.Error {
        return when (errorCode) {
            "PrinterStatusErrorCoverOpen" ->
                PrintResult.Error("COVER_OPEN", "Tapa de impresora abierta")

            "PrinterStatusErrorPaperEmpty",
            "PrinterStatusErrorNoMedia",
            "SetLabelSizeError",
            -> PrintResult.Error("NO_PAPER", "Sin papel/etiquetas")

            "PrinterStatusErrorBatteryWeak",
            "PrinterStatusErrorBatteryChargeError",
            -> PrintResult.Error("BATTERY_LOW", "Batería baja")

            "ChannelTimeout",
            "ChannelErrorOpen",
            "ChannelErrorNotSupported",
            "ChannelErrorGetStatusTimeout",
            "ChannelErrorBidirectionalOff",
            "ChannelErrorNoSendData",
            "ChannelErrorPjWrongResponse",
            "ChannelErrorCommandError",
            "PrinterStatusErrorCommunicationError",
            -> PrintResult.Error("COMMUNICATION_ERROR", "Error de comunicación")

            "PrinterStatusErrorOverHeat" ->
                PrintResult.Error("OVERHEATING", "Impresora sobrecalentada")

            "PrinterStatusErrorPaperJam" ->
                PrintResult.Error("PAPER_JAM", "Atasco de papel")

            "PrinterStatusErrorHighVoltageAdapter" ->
                PrintResult.Error("HIGH_VOLTAGE_ADAPTER", "Adaptador incorrecto")

            "PrintSettingsError",
            "InvalidParameterError",
            "TemplateFileNotMatchModelError",
            -> PrintResult.Error("PRINT_SETTINGS_ERROR", "Configuración de impresión inválida")

            else -> PrintResult.Error(
                code = "UNKNOWN_PRINTER_ERROR",
                message = description?.takeIf { it.isNotBlank() }
                    ?: "Error desconocido de impresora ($errorCode)",
            )
        }
    }
}
