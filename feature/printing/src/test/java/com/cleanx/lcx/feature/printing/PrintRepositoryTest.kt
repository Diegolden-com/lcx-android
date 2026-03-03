package com.cleanx.lcx.feature.printing

import com.cleanx.lcx.feature.printing.data.ConnectionType
import com.cleanx.lcx.feature.printing.data.LabelData
import com.cleanx.lcx.feature.printing.data.PrintRepository
import com.cleanx.lcx.feature.printing.data.PrintResult
import com.cleanx.lcx.feature.printing.data.PrinterInfo
import com.cleanx.lcx.feature.printing.data.PrinterManager
import com.cleanx.lcx.feature.printing.data.PrinterPreferences
import com.cleanx.lcx.feature.printing.data.SavedPrinter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PrintRepositoryTest {

    private lateinit var printerManager: PrinterManager
    private lateinit var printerPreferences: PrinterPreferences
    private lateinit var repository: PrintRepository

    private val fakePrinter = PrinterInfo(
        name = "Test Printer",
        address = "192.168.1.100",
        connectionType = ConnectionType.WIFI,
    )

    private val sampleLabel = LabelData(
        ticketNumber = "T-20260302-0001",
        customerName = "Juan Perez",
        serviceType = "wash-fold",
        date = "2026-03-02",
        dailyFolio = 1,
    )

    @Before
    fun setUp() {
        printerManager = mockk(relaxed = true)
        printerPreferences = mockk(relaxed = true)

        // Default preferences flows
        every { printerPreferences.printCopies } returns flowOf(1)
        every { printerPreferences.autoConnect } returns flowOf(true)
        every { printerPreferences.savedPrinter } returns flowOf(null)

        repository = PrintRepository(printerManager, printerPreferences)
    }

    // =========================================================================
    // Discovery
    // =========================================================================

    @Test
    fun `discoverPrinters delegates to PrinterManager`() = runTest {
        coEvery { printerManager.discoverPrinters() } returns listOf(fakePrinter)

        val printers = repository.discoverPrinters()

        assertEquals(1, printers.size)
        assertEquals(fakePrinter, printers[0])
    }

    // =========================================================================
    // Connection
    // =========================================================================

    @Test
    fun `connectToSelected returns false when no printer selected`() = runTest {
        val result = repository.connectToSelected()

        assertFalse(result)
    }

    @Test
    fun `connectToSelected delegates to PrinterManager when printer is selected`() = runTest {
        coEvery { printerManager.connect(fakePrinter) } returns true
        repository.selectPrinter(fakePrinter)

        val result = repository.connectToSelected()

        assertTrue(result)
        coVerify { printerManager.connect(fakePrinter) }
    }

    @Test
    fun `connectToSelected saves printer on success`() = runTest {
        coEvery { printerManager.connect(fakePrinter) } returns true
        repository.selectPrinter(fakePrinter)

        repository.connectToSelected()

        coVerify { printerPreferences.savePrinter(fakePrinter) }
    }

    @Test
    fun `connectToSelected does not save printer on failure`() = runTest {
        coEvery { printerManager.connect(fakePrinter) } returns false
        repository.selectPrinter(fakePrinter)

        repository.connectToSelected()

        coVerify(exactly = 0) { printerPreferences.savePrinter(any()) }
    }

    @Test
    fun `disconnect delegates to PrinterManager`() {
        repository.disconnect()

        verify { printerManager.disconnect() }
    }

    // =========================================================================
    // Auto-connect
    // =========================================================================

    @Test
    fun `tryAutoConnect returns false when auto-connect disabled`() = runTest {
        every { printerPreferences.autoConnect } returns flowOf(false)

        val result = repository.tryAutoConnect()

        assertFalse(result)
    }

    @Test
    fun `tryAutoConnect returns false when no saved printer`() = runTest {
        every { printerPreferences.autoConnect } returns flowOf(true)
        every { printerPreferences.savedPrinter } returns flowOf(null)

        val result = repository.tryAutoConnect()

        assertFalse(result)
    }

    @Test
    fun `tryAutoConnect connects to saved printer`() = runTest {
        val saved = SavedPrinter(
            name = "Test Printer",
            address = "192.168.1.100",
            connectionType = ConnectionType.WIFI,
        )
        every { printerPreferences.autoConnect } returns flowOf(true)
        every { printerPreferences.savedPrinter } returns flowOf(saved)
        coEvery { printerManager.connect(any()) } returns true

        val result = repository.tryAutoConnect()

        assertTrue(result)
        coVerify { printerManager.connect(saved.toPrinterInfo()) }
    }

    @Test
    fun `tryAutoConnect returns false when connect fails`() = runTest {
        val saved = SavedPrinter(
            name = "Test Printer",
            address = "192.168.1.100",
            connectionType = ConnectionType.WIFI,
        )
        every { printerPreferences.autoConnect } returns flowOf(true)
        every { printerPreferences.savedPrinter } returns flowOf(saved)
        coEvery { printerManager.connect(any()) } returns false

        val result = repository.tryAutoConnect()

        assertFalse(result)
    }

    @Test
    fun `tryAutoConnect returns false when connect throws exception`() = runTest {
        val saved = SavedPrinter(
            name = "Flaky Printer",
            address = "192.168.1.200",
            connectionType = ConnectionType.WIFI,
        )
        every { printerPreferences.autoConnect } returns flowOf(true)
        every { printerPreferences.savedPrinter } returns flowOf(saved)
        coEvery { printerManager.connect(any()) } throws RuntimeException("Connection reset")

        val result = repository.tryAutoConnect()

        assertFalse(result)
    }

    @Test
    fun `tryAutoConnect sets selectedPrinter on success`() = runTest {
        val saved = SavedPrinter(
            name = "Test Printer",
            address = "192.168.1.100",
            connectionType = ConnectionType.WIFI,
        )
        every { printerPreferences.autoConnect } returns flowOf(true)
        every { printerPreferences.savedPrinter } returns flowOf(saved)
        coEvery { printerManager.connect(any()) } returns true

        repository.tryAutoConnect()

        assertEquals(saved.toPrinterInfo(), repository.getSelectedPrinter())
    }

    // =========================================================================
    // Retry logic — printWithRetry
    // =========================================================================

    @Test
    fun `printWithRetry succeeds on first attempt`() = runTest {
        coEvery { printerManager.print(sampleLabel) } returns PrintResult.Success

        val result = repository.printWithRetry(sampleLabel)

        assertTrue(result is PrintResult.Success)
        coVerify(exactly = 1) { printerManager.print(sampleLabel) }
    }

    @Test
    fun `printWithRetry succeeds on second attempt after transient failure`() = runTest {
        coEvery { printerManager.print(sampleLabel) } returnsMany listOf(
            PrintResult.Error("COMMUNICATION_ERROR", "Error de comunicación"),
            PrintResult.Success,
        )

        val result = repository.printWithRetry(sampleLabel, maxAttempts = 3)

        assertTrue("Should succeed on second attempt", result is PrintResult.Success)
        coVerify(exactly = 2) { printerManager.print(sampleLabel) }
    }

    @Test
    fun `printWithRetry retries on failure and succeeds on third attempt`() = runTest {
        coEvery { printerManager.print(sampleLabel) } returnsMany listOf(
            PrintResult.Error("ERR1", "fail 1"),
            PrintResult.Error("ERR2", "fail 2"),
            PrintResult.Success,
        )

        val result = repository.printWithRetry(sampleLabel, maxAttempts = 3)

        assertTrue(result is PrintResult.Success)
        coVerify(exactly = 3) { printerManager.print(sampleLabel) }
    }

    @Test
    fun `printWithRetry returns last error after all attempts exhausted`() = runTest {
        coEvery { printerManager.print(sampleLabel) } returns
            PrintResult.Error("ERR", "always fails")

        val result = repository.printWithRetry(sampleLabel, maxAttempts = 3)

        assertTrue(result is PrintResult.Error)
        assertEquals("always fails", (result as PrintResult.Error).message)
        coVerify(exactly = 3) { printerManager.print(sampleLabel) }
    }

    @Test
    fun `printWithRetry returns last error code not the first`() = runTest {
        coEvery { printerManager.print(sampleLabel) } returnsMany listOf(
            PrintResult.Error("COMMUNICATION_ERROR", "transient"),
            PrintResult.Error("PAPER_JAM", "paper stuck"),
            PrintResult.Error("COVER_OPEN", "final error"),
        )

        val result = repository.printWithRetry(sampleLabel, maxAttempts = 3)

        assertTrue(result is PrintResult.Error)
        assertEquals("COVER_OPEN", (result as PrintResult.Error).code)
        assertEquals("final error", result.message)
    }

    @Test
    fun `printWithRetry fails twice then succeeds`() = runTest {
        coEvery { printerManager.print(sampleLabel) } returnsMany listOf(
            PrintResult.Error("ERR1", "fail 1"),
            PrintResult.Error("ERR2", "fail 2"),
            PrintResult.Success,
        )

        val result = repository.printWithRetry(sampleLabel)

        assertTrue(result is PrintResult.Success)
    }

    @Test
    fun `printWithRetry with maxAttempts 1 does not retry`() = runTest {
        coEvery { printerManager.print(sampleLabel) } returns
            PrintResult.Error("ERR", "fail once")

        val result = repository.printWithRetry(sampleLabel, maxAttempts = 1)

        assertTrue(result is PrintResult.Error)
        coVerify(exactly = 1) { printerManager.print(sampleLabel) }
    }

    @Test
    fun `printWithRetry uses default MAX_RETRY_ATTEMPTS of 3`() = runTest {
        coEvery { printerManager.print(sampleLabel) } returns
            PrintResult.Error("ERR", "always fails")

        repository.printWithRetry(sampleLabel)

        coVerify(exactly = PrintRepository.MAX_RETRY_ATTEMPTS) {
            printerManager.print(sampleLabel)
        }
    }

    // =========================================================================
    // Multi-copy printing
    // =========================================================================

    @Test
    fun `printWithRetry prints multiple copies when configured`() = runTest {
        every { printerPreferences.printCopies } returns flowOf(2)
        repository = PrintRepository(printerManager, printerPreferences)
        coEvery { printerManager.print(sampleLabel) } returns PrintResult.Success

        val result = repository.printWithRetry(sampleLabel)

        assertTrue(result is PrintResult.Success)
        coVerify(exactly = 2) { printerManager.print(sampleLabel) }
    }

    @Test
    fun `printWithRetry stops on error during multi-copy`() = runTest {
        every { printerPreferences.printCopies } returns flowOf(3)
        repository = PrintRepository(printerManager, printerPreferences)
        coEvery { printerManager.print(sampleLabel) } returnsMany listOf(
            PrintResult.Success,
            PrintResult.Error("ERR", "fail"),
            PrintResult.Error("ERR", "fail"),
            PrintResult.Error("ERR", "fail"),
        )

        val result = repository.printWithRetry(sampleLabel)

        assertTrue(result is PrintResult.Error)
    }

    @Test
    fun `printWithRetry with 3 copies prints exactly 3 on all success`() = runTest {
        every { printerPreferences.printCopies } returns flowOf(3)
        repository = PrintRepository(printerManager, printerPreferences)
        coEvery { printerManager.print(sampleLabel) } returns PrintResult.Success

        val result = repository.printWithRetry(sampleLabel)

        assertTrue(result is PrintResult.Success)
        coVerify(exactly = 3) { printerManager.print(sampleLabel) }
    }

    @Test
    fun `printWithRetry multi-copy retries each copy independently`() = runTest {
        every { printerPreferences.printCopies } returns flowOf(2)
        repository = PrintRepository(printerManager, printerPreferences)
        // Copy 1: fail once then succeed (2 calls)
        // Copy 2: succeed immediately (1 call)
        // Total: 3 calls
        coEvery { printerManager.print(sampleLabel) } returnsMany listOf(
            PrintResult.Error("ERR", "transient"),
            PrintResult.Success,
            PrintResult.Success,
        )

        val result = repository.printWithRetry(sampleLabel, maxAttempts = 3)

        assertTrue(result is PrintResult.Success)
        coVerify(exactly = 3) { printerManager.print(sampleLabel) }
    }

    // =========================================================================
    // Cleanup on permanent failure
    // =========================================================================

    @Test
    fun `permanent failure returns error without crashing`() = runTest {
        coEvery { printerManager.print(sampleLabel) } returns
            PrintResult.Error("COVER_OPEN", "Tapa de impresora abierta")

        val result = repository.printWithRetry(sampleLabel, maxAttempts = 3)

        assertTrue(result is PrintResult.Error)
        assertEquals("COVER_OPEN", (result as PrintResult.Error).code)
    }

    @Test
    fun `forgetPrinter disconnects clears preferences and nullifies selected`() = runTest {
        repository.selectPrinter(fakePrinter)
        repository.forgetPrinter()

        verify { printerManager.disconnect() }
        coVerify { printerPreferences.clearPrinter() }
        assertNull(repository.getSelectedPrinter())
    }

    @Test
    fun `disconnect after permanent print failure does not throw`() = runTest {
        coEvery { printerManager.print(sampleLabel) } returns
            PrintResult.Error("OVERHEATING", "Impresora sobrecalentada")

        val result = repository.printWithRetry(sampleLabel, maxAttempts = 1)

        assertTrue(result is PrintResult.Error)

        // Caller decides to disconnect after permanent failure — must not throw
        repository.disconnect()
        verify { printerManager.disconnect() }
    }

    // =========================================================================
    // Rapid-press / no double-action guards
    // =========================================================================

    @Test
    fun `concurrent printWithRetry calls both execute independently`() = runTest {
        coEvery { printerManager.print(sampleLabel) } returns PrintResult.Success

        val result1 = async { repository.printWithRetry(sampleLabel, maxAttempts = 1) }
        val result2 = async { repository.printWithRetry(sampleLabel, maxAttempts = 1) }

        val results = awaitAll(result1, result2)
        assertTrue(results.all { it is PrintResult.Success })
    }

    @Test
    fun `selectPrinter overwrites previous selection`() {
        val printer1 = PrinterInfo("P1", "1.1.1.1", ConnectionType.WIFI)
        val printer2 = PrinterInfo("P2", "2.2.2.2", ConnectionType.BLUETOOTH)

        repository.selectPrinter(printer1)
        assertEquals(printer1, repository.getSelectedPrinter())

        repository.selectPrinter(printer2)
        assertEquals(printer2, repository.getSelectedPrinter())
    }

    // =========================================================================
    // Selection / state
    // =========================================================================

    @Test
    fun `selectedPrinter is null by default`() {
        assertNull(repository.getSelectedPrinter())
    }

    @Test
    fun `selectPrinter stores the printer`() {
        repository.selectPrinter(fakePrinter)

        assertEquals(fakePrinter, repository.getSelectedPrinter())
    }

    @Test
    fun `isConnected delegates to PrinterManager`() {
        every { printerManager.isConnected() } returns true

        assertTrue(repository.isConnected())
    }

    @Test
    fun `isConnected returns false when PrinterManager says false`() {
        every { printerManager.isConnected() } returns false

        assertFalse(repository.isConnected())
    }

    // =========================================================================
    // Forget printer
    // =========================================================================

    @Test
    fun `forgetPrinter disconnects and clears preferences`() = runTest {
        repository.selectPrinter(fakePrinter)
        repository.forgetPrinter()

        verify { printerManager.disconnect() }
        coVerify { printerPreferences.clearPrinter() }
        assertNull(repository.getSelectedPrinter())
    }

    @Test
    fun `forgetPrinter is safe when no printer was selected`() = runTest {
        repository.forgetPrinter()

        verify { printerManager.disconnect() }
        coVerify { printerPreferences.clearPrinter() }
        assertNull(repository.getSelectedPrinter())
    }
}
