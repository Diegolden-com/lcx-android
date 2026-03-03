package com.cleanx.lcx.feature.printing

import com.cleanx.lcx.feature.printing.data.BrotherErrorMapper
import com.cleanx.lcx.feature.printing.data.PrintResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrotherErrorMapperTest {

    // =========================================================================
    // Legacy SDK (numeric) error codes — mapSdkError
    // =========================================================================

    @Test
    fun `error 0x01 maps to COVER_OPEN`() {
        val result = BrotherErrorMapper.mapSdkError(0x01)
        assertEquals("COVER_OPEN", result.code)
        assertEquals("Tapa de impresora abierta", result.message)
    }

    @Test
    fun `error 0x02 maps to NO_PAPER`() {
        val result = BrotherErrorMapper.mapSdkError(0x02)
        assertEquals("NO_PAPER", result.code)
        assertEquals("Sin papel/etiquetas", result.message)
    }

    @Test
    fun `error 0x04 maps to BATTERY_LOW`() {
        val result = BrotherErrorMapper.mapSdkError(0x04)
        assertEquals("BATTERY_LOW", result.code)
        assertEquals("Batería baja", result.message)
    }

    @Test
    fun `error 0x08 maps to COMMUNICATION_ERROR`() {
        val result = BrotherErrorMapper.mapSdkError(0x08)
        assertEquals("COMMUNICATION_ERROR", result.code)
        assertEquals("Error de comunicación", result.message)
    }

    @Test
    fun `error 0x10 maps to OVERHEATING`() {
        val result = BrotherErrorMapper.mapSdkError(0x10)
        assertEquals("OVERHEATING", result.code)
        assertEquals("Impresora sobrecalentada", result.message)
    }

    @Test
    fun `error 0x20 maps to PAPER_JAM`() {
        val result = BrotherErrorMapper.mapSdkError(0x20)
        assertEquals("PAPER_JAM", result.code)
        assertEquals("Atasco de papel", result.message)
    }

    @Test
    fun `error 0x40 maps to HIGH_VOLTAGE_ADAPTER`() {
        val result = BrotherErrorMapper.mapSdkError(0x40)
        assertEquals("HIGH_VOLTAGE_ADAPTER", result.code)
        assertEquals("Adaptador incorrecto", result.message)
    }

    @Test
    fun `unknown error code maps to UNKNOWN_PRINTER_ERROR`() {
        val result = BrotherErrorMapper.mapSdkError(0xFF)
        assertEquals("UNKNOWN_PRINTER_ERROR", result.code)
        assertEquals("Error desconocido de impresora (255)", result.message)
    }

    @Test
    fun `error code 0 maps to UNKNOWN_PRINTER_ERROR`() {
        val result = BrotherErrorMapper.mapSdkError(0x00)
        assertEquals("UNKNOWN_PRINTER_ERROR", result.code)
    }

    @Test
    fun `negative error code maps to UNKNOWN_PRINTER_ERROR`() {
        val result = BrotherErrorMapper.mapSdkError(-1)
        assertEquals("UNKNOWN_PRINTER_ERROR", result.code)
        assertTrue(result.message.contains("-1"))
    }

    @Test
    fun `Int MAX_VALUE error code maps to UNKNOWN_PRINTER_ERROR`() {
        val result = BrotherErrorMapper.mapSdkError(Int.MAX_VALUE)
        assertEquals("UNKNOWN_PRINTER_ERROR", result.code)
        assertTrue(result.message.contains(Int.MAX_VALUE.toString()))
    }

    @Test
    fun `all known error codes return PrintResult Error`() {
        val knownCodes = listOf(0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40)
        knownCodes.forEach { code ->
            val result = BrotherErrorMapper.mapSdkError(code)
            assertTrue(
                "Expected PrintResult.Error for code $code",
                result is PrintResult.Error,
            )
        }
    }

    @Test
    fun `all known error codes have non-empty code and message`() {
        val knownCodes = listOf(0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40)
        knownCodes.forEach { code ->
            val result = BrotherErrorMapper.mapSdkError(code)
            assertTrue("Code should not be blank for $code", result.code.isNotBlank())
            assertTrue("Message should not be blank for $code", result.message.isNotBlank())
        }
    }

    @Test
    fun `each known error code maps to a unique error code string`() {
        val knownCodes = listOf(0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40)
        val errorCodes = knownCodes.map { BrotherErrorMapper.mapSdkError(it).code }
        assertEquals(
            "Duplicate error code strings found",
            errorCodes.size,
            errorCodes.toSet().size,
        )
    }

    // =========================================================================
    // SDK v4 (string) error codes — mapSdkV4Error
    // =========================================================================

    // -- Cover open -----------------------------------------------------------

    @Test
    fun `v4 PrinterStatusErrorCoverOpen maps to COVER_OPEN`() {
        val result = BrotherErrorMapper.mapSdkV4Error("PrinterStatusErrorCoverOpen")
        assertEquals("COVER_OPEN", result.code)
        assertEquals("Tapa de impresora abierta", result.message)
    }

    // -- Paper / media --------------------------------------------------------

    @Test
    fun `v4 PrinterStatusErrorPaperEmpty maps to NO_PAPER`() {
        val result = BrotherErrorMapper.mapSdkV4Error("PrinterStatusErrorPaperEmpty")
        assertEquals("NO_PAPER", result.code)
        assertEquals("Sin papel/etiquetas", result.message)
    }

    @Test
    fun `v4 PrinterStatusErrorNoMedia maps to NO_PAPER`() {
        val result = BrotherErrorMapper.mapSdkV4Error("PrinterStatusErrorNoMedia")
        assertEquals("NO_PAPER", result.code)
        assertEquals("Sin papel/etiquetas", result.message)
    }

    @Test
    fun `v4 SetLabelSizeError maps to NO_PAPER`() {
        val result = BrotherErrorMapper.mapSdkV4Error("SetLabelSizeError")
        assertEquals("NO_PAPER", result.code)
        assertEquals("Sin papel/etiquetas", result.message)
    }

    // -- Battery --------------------------------------------------------------

    @Test
    fun `v4 PrinterStatusErrorBatteryWeak maps to BATTERY_LOW`() {
        val result = BrotherErrorMapper.mapSdkV4Error("PrinterStatusErrorBatteryWeak")
        assertEquals("BATTERY_LOW", result.code)
        assertEquals("Batería baja", result.message)
    }

    @Test
    fun `v4 PrinterStatusErrorBatteryChargeError maps to BATTERY_LOW`() {
        val result = BrotherErrorMapper.mapSdkV4Error("PrinterStatusErrorBatteryChargeError")
        assertEquals("BATTERY_LOW", result.code)
        assertEquals("Batería baja", result.message)
    }

    // -- Communication errors -------------------------------------------------

    @Test
    fun `v4 ChannelTimeout maps to COMMUNICATION_ERROR`() {
        val result = BrotherErrorMapper.mapSdkV4Error("ChannelTimeout")
        assertEquals("COMMUNICATION_ERROR", result.code)
        assertEquals("Error de comunicación", result.message)
    }

    @Test
    fun `v4 ChannelErrorOpen maps to COMMUNICATION_ERROR`() {
        val result = BrotherErrorMapper.mapSdkV4Error("ChannelErrorOpen")
        assertEquals("COMMUNICATION_ERROR", result.code)
    }

    @Test
    fun `v4 ChannelErrorNotSupported maps to COMMUNICATION_ERROR`() {
        val result = BrotherErrorMapper.mapSdkV4Error("ChannelErrorNotSupported")
        assertEquals("COMMUNICATION_ERROR", result.code)
    }

    @Test
    fun `v4 ChannelErrorGetStatusTimeout maps to COMMUNICATION_ERROR`() {
        val result = BrotherErrorMapper.mapSdkV4Error("ChannelErrorGetStatusTimeout")
        assertEquals("COMMUNICATION_ERROR", result.code)
    }

    @Test
    fun `v4 ChannelErrorBidirectionalOff maps to COMMUNICATION_ERROR`() {
        val result = BrotherErrorMapper.mapSdkV4Error("ChannelErrorBidirectionalOff")
        assertEquals("COMMUNICATION_ERROR", result.code)
    }

    @Test
    fun `v4 ChannelErrorNoSendData maps to COMMUNICATION_ERROR`() {
        val result = BrotherErrorMapper.mapSdkV4Error("ChannelErrorNoSendData")
        assertEquals("COMMUNICATION_ERROR", result.code)
    }

    @Test
    fun `v4 ChannelErrorPjWrongResponse maps to COMMUNICATION_ERROR`() {
        val result = BrotherErrorMapper.mapSdkV4Error("ChannelErrorPjWrongResponse")
        assertEquals("COMMUNICATION_ERROR", result.code)
    }

    @Test
    fun `v4 ChannelErrorCommandError maps to COMMUNICATION_ERROR`() {
        val result = BrotherErrorMapper.mapSdkV4Error("ChannelErrorCommandError")
        assertEquals("COMMUNICATION_ERROR", result.code)
    }

    @Test
    fun `v4 PrinterStatusErrorCommunicationError maps to COMMUNICATION_ERROR`() {
        val result = BrotherErrorMapper.mapSdkV4Error("PrinterStatusErrorCommunicationError")
        assertEquals("COMMUNICATION_ERROR", result.code)
    }

    @Test
    fun `all v4 communication errors share same code and message`() {
        val commErrors = listOf(
            "ChannelTimeout",
            "ChannelErrorOpen",
            "ChannelErrorNotSupported",
            "ChannelErrorGetStatusTimeout",
            "ChannelErrorBidirectionalOff",
            "ChannelErrorNoSendData",
            "ChannelErrorPjWrongResponse",
            "ChannelErrorCommandError",
            "PrinterStatusErrorCommunicationError",
        )
        commErrors.forEach { errorCode ->
            val result = BrotherErrorMapper.mapSdkV4Error(errorCode)
            assertEquals(
                "Expected COMMUNICATION_ERROR for $errorCode",
                "COMMUNICATION_ERROR",
                result.code,
            )
            assertEquals(
                "Expected Spanish message for $errorCode",
                "Error de comunicación",
                result.message,
            )
        }
    }

    // -- Overheating ----------------------------------------------------------

    @Test
    fun `v4 PrinterStatusErrorOverHeat maps to OVERHEATING`() {
        val result = BrotherErrorMapper.mapSdkV4Error("PrinterStatusErrorOverHeat")
        assertEquals("OVERHEATING", result.code)
        assertEquals("Impresora sobrecalentada", result.message)
    }

    // -- Paper jam -------------------------------------------------------------

    @Test
    fun `v4 PrinterStatusErrorPaperJam maps to PAPER_JAM`() {
        val result = BrotherErrorMapper.mapSdkV4Error("PrinterStatusErrorPaperJam")
        assertEquals("PAPER_JAM", result.code)
        assertEquals("Atasco de papel", result.message)
    }

    // -- High voltage adapter -------------------------------------------------

    @Test
    fun `v4 PrinterStatusErrorHighVoltageAdapter maps to HIGH_VOLTAGE_ADAPTER`() {
        val result = BrotherErrorMapper.mapSdkV4Error("PrinterStatusErrorHighVoltageAdapter")
        assertEquals("HIGH_VOLTAGE_ADAPTER", result.code)
        assertEquals("Adaptador incorrecto", result.message)
    }

    // -- Print settings errors ------------------------------------------------

    @Test
    fun `v4 PrintSettingsError maps to PRINT_SETTINGS_ERROR`() {
        val result = BrotherErrorMapper.mapSdkV4Error("PrintSettingsError")
        assertEquals("PRINT_SETTINGS_ERROR", result.code)
        assertEquals("Configuración de impresión inválida", result.message)
    }

    @Test
    fun `v4 InvalidParameterError maps to PRINT_SETTINGS_ERROR`() {
        val result = BrotherErrorMapper.mapSdkV4Error("InvalidParameterError")
        assertEquals("PRINT_SETTINGS_ERROR", result.code)
        assertEquals("Configuración de impresión inválida", result.message)
    }

    @Test
    fun `v4 TemplateFileNotMatchModelError maps to PRINT_SETTINGS_ERROR`() {
        val result = BrotherErrorMapper.mapSdkV4Error("TemplateFileNotMatchModelError")
        assertEquals("PRINT_SETTINGS_ERROR", result.code)
        assertEquals("Configuración de impresión inválida", result.message)
    }

    // -- Unknown / fallback ---------------------------------------------------

    @Test
    fun `v4 unknown error uses description when available`() {
        val result = BrotherErrorMapper.mapSdkV4Error(
            errorCode = "SomethingElse",
            description = "SDK returned unknown status",
        )
        assertEquals("UNKNOWN_PRINTER_ERROR", result.code)
        assertEquals("SDK returned unknown status", result.message)
    }

    @Test
    fun `v4 unknown error with null description falls back to errorCode in message`() {
        val result = BrotherErrorMapper.mapSdkV4Error(
            errorCode = "NeverSeenBefore",
            description = null,
        )
        assertEquals("UNKNOWN_PRINTER_ERROR", result.code)
        assertTrue(
            "Message should contain the error code",
            result.message.contains("NeverSeenBefore"),
        )
    }

    @Test
    fun `v4 unknown error with blank description falls back to errorCode in message`() {
        val result = BrotherErrorMapper.mapSdkV4Error(
            errorCode = "SomeWeirdCode",
            description = "   ",
        )
        assertEquals("UNKNOWN_PRINTER_ERROR", result.code)
        assertTrue(
            "Blank description should be ignored, message should contain error code",
            result.message.contains("SomeWeirdCode"),
        )
    }

    @Test
    fun `v4 unknown error with empty string description falls back to errorCode in message`() {
        val result = BrotherErrorMapper.mapSdkV4Error(
            errorCode = "AnotherCode",
            description = "",
        )
        assertEquals("UNKNOWN_PRINTER_ERROR", result.code)
        assertTrue(
            "Empty description should be ignored",
            result.message.contains("AnotherCode"),
        )
    }

    @Test
    fun `v4 unknown error without description parameter uses default`() {
        val result = BrotherErrorMapper.mapSdkV4Error("TotallyNewError")
        assertEquals("UNKNOWN_PRINTER_ERROR", result.code)
        assertTrue(result.message.contains("TotallyNewError"))
    }

    // -- Exhaustive coverage of all known v4 codes ----------------------------

    @Test
    fun `all known v4 error codes produce non-blank code and message`() {
        val allKnownV4Codes = listOf(
            "PrinterStatusErrorCoverOpen",
            "PrinterStatusErrorPaperEmpty",
            "PrinterStatusErrorNoMedia",
            "SetLabelSizeError",
            "PrinterStatusErrorBatteryWeak",
            "PrinterStatusErrorBatteryChargeError",
            "ChannelTimeout",
            "ChannelErrorOpen",
            "ChannelErrorNotSupported",
            "ChannelErrorGetStatusTimeout",
            "ChannelErrorBidirectionalOff",
            "ChannelErrorNoSendData",
            "ChannelErrorPjWrongResponse",
            "ChannelErrorCommandError",
            "PrinterStatusErrorCommunicationError",
            "PrinterStatusErrorOverHeat",
            "PrinterStatusErrorPaperJam",
            "PrinterStatusErrorHighVoltageAdapter",
            "PrintSettingsError",
            "InvalidParameterError",
            "TemplateFileNotMatchModelError",
        )

        allKnownV4Codes.forEach { errorCode ->
            val result = BrotherErrorMapper.mapSdkV4Error(errorCode)
            assertTrue(
                "Code should not be blank for $errorCode",
                result.code.isNotBlank(),
            )
            assertTrue(
                "Message should not be blank for $errorCode",
                result.message.isNotBlank(),
            )
            assertTrue(
                "Known code $errorCode should not map to UNKNOWN",
                result.code != "UNKNOWN_PRINTER_ERROR",
            )
        }
    }
}
