package com.example.data.catalog

import com.example.data.model.DynoPoint
import com.example.data.model.DynoRunSummary
import com.example.data.model.EcuSpecialFeatures
import com.example.data.model.HondaEcuModel
import com.example.data.model.TuningStrategy
import com.example.data.model.TuningTables
import java.util.zip.CRC32
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

object TuningPresets {

    val TPS_AXIS = listOf(0, 2, 5, 8, 12, 18, 25, 33, 42, 52, 62, 72, 82, 90, 96, 100)

    fun generateTablesForEcu(ecu: HondaEcuModel): TuningTables {
        val fuel = MutableList(16) { row ->
            val tps = TPS_AXIS[row]
            MutableList(16) { col ->
                val rpmRatio = col / 15.0
                val base = 62.0 + (tps / 100.0) * 34.0 + (1.0 - (rpmRatio - 0.65).pow(2)) * 11.0
                val clamped = max(55.0, min(115.0, base))
                (clamped * 10.0).roundToInt() / 10.0
            }
        }

        val ignition = MutableList(16) { row ->
            val tps = TPS_AXIS[row]
            MutableList(16) { col ->
                val rpm = ecu.rpmBreakpoints[col]
                val rpmFactor = min(1.0, max(0.0, (rpm - 1200.0) / (ecu.stockRevLimiter - 1200.0)))
                val loadRetard = (tps / 100.0) * 7.5
                val deg = 11.0 + rpmFactor * 23.0 - loadRetard
                val clamped = max(6.0, min(38.0, deg))
                (clamped * 10.0).roundToInt() / 10.0
            }
        }

        return TuningTables(
            rpmAxis = ecu.rpmBreakpoints,
            tpsAxis = TPS_AXIS,
            fuelVe = fuel,
            ignitionAdvance = ignition,
            revLimiterRpm = ecu.stockRevLimiter,
            launchControlRpm = 4500,
            targetAfr = 13.8,
            activeStrategy = TuningStrategy.STOCK
        )
    }

    fun applyStrategy(ecu: HondaEcuModel, strategy: TuningStrategy): TuningTables {
        val stock = generateTablesForEcu(ecu)
        if (strategy == TuningStrategy.STOCK) return stock

        val newFuel = stock.fuelVe.mapIndexed { row, rowList ->
            val tps = TPS_AXIS[row]
            rowList.mapIndexed { col, valVe ->
                val delta = when (strategy) {
                    TuningStrategy.DRAG_RACE -> {
                        when {
                            tps >= 70 -> 13.0
                            tps >= 30 -> 7.0
                            else -> 2.5
                        }
                    }
                    TuningStrategy.TOURING -> {
                        when {
                            tps >= 70 -> 6.5
                            tps in 18..52 && col in 4..10 -> -3.0
                            else -> 1.5
                        }
                    }
                    TuningStrategy.HARIAN -> {
                        when {
                            tps in 2..25 -> 6.0
                            tps == 0 -> -2.0
                            else -> 3.0
                        }
                    }
                    TuningStrategy.STOCK -> 0.0
                }
                ((max(45.0, min(145.0, valVe + delta))) * 10.0).roundToInt() / 10.0
            }
        }

        val newIgn = stock.ignitionAdvance.mapIndexed { row, rowList ->
            val tps = TPS_AXIS[row]
            rowList.mapIndexed { col, valIgn ->
                val delta = when (strategy) {
                    TuningStrategy.DRAG_RACE -> if (col >= 6) 4.0 else 2.0
                    TuningStrategy.TOURING -> if (tps in 18..62) 2.5 else 1.5
                    TuningStrategy.HARIAN -> if (tps <= 33 && col <= 8) 2.0 else 1.0
                    TuningStrategy.STOCK -> 0.0
                }
                ((max(4.0, min(44.0, valIgn + delta))) * 10.0).roundToInt() / 10.0
            }
        }

        val (limiterDelta, launchRpm, targetAfr) = when (strategy) {
            TuningStrategy.DRAG_RACE -> Triple(1800, 5600, 12.6)
            TuningStrategy.TOURING -> Triple(600, 4200, 13.4)
            TuningStrategy.HARIAN -> Triple(500, 4000, 13.6)
            TuningStrategy.STOCK -> Triple(0, 4500, 13.8)
        }

        return stock.copy(
            fuelVe = newFuel,
            ignitionAdvance = newIgn,
            revLimiterRpm = min(16000, ecu.stockRevLimiter + limiterDelta),
            launchControlRpm = launchRpm,
            targetAfr = targetAfr,
            activeStrategy = strategy
        )
    }

    fun applyGlobalOffset(
        tables: TuningTables,
        fuelDeltaPct: Double,
        ignitionDeltaDeg: Double,
        newLimiterRpm: Int,
        newTargetAfr: Double
    ): TuningTables {
        val updatedFuel = tables.fuelVe.map { row ->
            row.map { v -> ((max(45.0, min(145.0, v + fuelDeltaPct))) * 10.0).roundToInt() / 10.0 }
        }
        val updatedIgn = tables.ignitionAdvance.map { row ->
            row.map { v -> ((max(4.0, min(45.0, v + ignitionDeltaDeg))) * 10.0).roundToInt() / 10.0 }
        }
        return tables.copy(
            fuelVe = updatedFuel,
            ignitionAdvance = updatedIgn,
            revLimiterRpm = max(7500, min(16000, newLimiterRpm)),
            targetAfr = max(11.0, min(16.0, newTargetAfr))
        )
    }

    fun smoothMatrix3x3(matrix: List<List<Double>>): List<List<Double>> {
        val rows = matrix.size
        val cols = matrix.firstOrNull()?.size ?: 0
        return List(rows) { r ->
            List(cols) { c ->
                var sum = 0.0
                var weightSum = 0.0
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        val nr = r + dr
                        val nc = c + dc
                        if (nr in 0 until rows && nc in 0 until cols) {
                            val w = if (dr == 0 && dc == 0) 4.0 else if (dr == 0 || dc == 0) 2.0 else 1.0
                            sum += matrix[nr][nc] * w
                            weightSum += w
                        }
                    }
                }
                ((sum / weightSum) * 10.0).roundToInt() / 10.0
            }
        }
    }

    fun buildRomBytes(ecu: HondaEcuModel, tables: TuningTables, features: EcuSpecialFeatures): ByteArray {
        val totalBytes = ecu.flashSizeKb * 1024
        val rom = ByteArray(totalBytes) { 0xFF.toByte() }

        // Header signature Honda PGM-FI
        val sig = "YOUNK_PRO_${ecu.partNumber}".toByteArray()
        for (i in sig.indices) {
            if (i < totalBytes) rom[i] = sig[i]
        }

        // Write 16x16 Fuel VE at offset
        val fuelOffset = (ecu.fuelTableOffsetHex.removePrefix("0x").toIntOrNull(16) ?: 0x1000) % (totalBytes - 512)
        var idx = fuelOffset
        for (r in 0 until 16) {
            for (c in 0 until 16) {
                val byteVal = max(0, min(255, (tables.fuelVe[r][c] * 1.8).roundToInt()))
                rom[idx++] = byteVal.toByte()
            }
        }

        // Write 16x16 Ignition Advance at offset
        val ignOffset = (ecu.ignitionTableOffsetHex.removePrefix("0x").toIntOrNull(16) ?: 0x1400) % (totalBytes - 512)
        idx = ignOffset
        for (r in 0 until 16) {
            for (c in 0 until 16) {
                val byteVal = max(0, min(255, (tables.ignitionAdvance[r][c] * 4.0).roundToInt()))
                rom[idx++] = byteVal.toByte()
            }
        }

        // Write Rev Limiter
        val limOffset = (ecu.limiterOffsetHex.removePrefix("0x").toIntOrNull(16) ?: 0x1800) % (totalBytes - 16)
        rom[limOffset] = ((tables.revLimiterRpm shr 8) and 0xFF).toByte()
        rom[limOffset + 1] = (tables.revLimiterRpm and 0xFF).toByte()

        // Bypass Patch Flags
        rom[0x20] = if (features.offImmobilizer) 0x90.toByte() else 0x00
        rom[0x21] = if (features.offSideStand) 0x00 else 0x01
        rom[0x22] = if (features.offO2Sensor) 0xFF.toByte() else 0x01
        rom[0x23] = if (features.offEctCut) 0x00 else 0x01

        // Embed CRC32 checksum in last 4 bytes
        val crc = CRC32()
        crc.update(rom, 0, totalBytes - 4)
        val crcVal = crc.value
        rom[totalBytes - 4] = ((crcVal shr 24) and 0xFF).toByte()
        rom[totalBytes - 3] = ((crcVal shr 16) and 0xFF).toByte()
        rom[totalBytes - 2] = ((crcVal shr 8) and 0xFF).toByte()
        rom[totalBytes - 1] = (crcVal and 0xFF).toByte()

        return rom
    }

    fun calculateCrc32Hex(ecu: HondaEcuModel, tables: TuningTables, features: EcuSpecialFeatures): String {
        val bytes = buildRomBytes(ecu, tables, features)
        val crc = CRC32()
        crc.update(bytes, 0, bytes.size - 4)
        return "%08X".format(crc.value)
    }

    fun buildXdfDefinitionXml(ecu: HondaEcuModel): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <XDFFORMAT version="1.70">
          <XDFHEADER>
            <flags>0x1</flags>
            <deftitle>Younk Tuner Pro - ${ecu.name}</deftitle>
            <description>Part Number: ${ecu.partNumber} | MCU: ${ecu.mcuArchitecture}</description>
            <BASEOFFSET offset="0" subtract="0" />
            <DEFAULTS datasizeinbits="8" sigdigits="1" outputtype="1" signed="0" lsbfirst="0" float="0" />
          </XDFHEADER>
          <XDFTABLE uniqueid="0x1001" flags="0x30">
            <title>Fuel VE Map (16x16)</title>
            <XDFAXIS id="x" uniqueid="0x0"><indexcount>16</indexcount><units>RPM</units></XDFAXIS>
            <XDFAXIS id="y" uniqueid="0x0"><indexcount>16</indexcount><units>TPS %</units></XDFAXIS>
            <XDFAXIS id="z"><EMBEDDEDDATA mmedaddress="${ecu.fuelTableOffsetHex}" mmedelementsizebits="8" mmedrowcount="16" mmedcolcount="16" /><MATH equation="X/1.8" /></XDFAXIS>
          </XDFTABLE>
          <XDFTABLE uniqueid="0x1002" flags="0x30">
            <title>Ignition Advance Map (16x16)</title>
            <XDFAXIS id="x" uniqueid="0x0"><indexcount>16</indexcount><units>RPM</units></XDFAXIS>
            <XDFAXIS id="y" uniqueid="0x0"><indexcount>16</indexcount><units>TPS %</units></XDFAXIS>
            <XDFAXIS id="z"><EMBEDDEDDATA mmedaddress="${ecu.ignitionTableOffsetHex}" mmedelementsizebits="8" mmedrowcount="16" mmedcolcount="16" /><MATH equation="X/4.0" /></XDFAXIS>
          </XDFTABLE>
          <XDFCONSTANT uniqueid="0x1003">
            <title>Rev Limiter Cut RPM</title>
            <EMBEDDEDDATA mmedaddress="${ecu.limiterOffsetHex}" mmedelementsizebits="16" />
            <MATH equation="X" />
          </XDFCONSTANT>
        </XDFFORMAT>
    """.trimIndent()

    fun buildDynoComparison(ecu: HondaEcuModel, tables: TuningTables): DynoRunSummary {
        val ccFactor = ecu.displacementCc / 150.0
        val peakRpm = (ecu.stockRevLimiter * 0.85).roundToInt()
        val avgFuelGain = (tables.fuelVe[15].average() - 96.0) * 0.008
        val avgIgnGain = (tables.ignitionAdvance[14].average() - 22.0) * 0.012
        val totalGainMultiplier = max(0.95, min(1.32, 1.0 + avgFuelGain + avgIgnGain))

        val points = ecu.rpmBreakpoints.filter { it >= 2000 }.map { rpm ->
            val norm = (rpm - 2000).toDouble() / max(4000, tables.revLimiterRpm - 2000)
            val torqueCurve = exp(-((norm - 0.58) / 0.42).pow(2))
            val stockTorque = (10.5 * ccFactor) * (0.55 + 0.45 * torqueCurve)
            val tunedTorque = stockTorque * (if (rpm > ecu.stockRevLimiter) totalGainMultiplier * 0.96 else totalGainMultiplier)

            // HP = (Torque_Nm * RPM) / 7127
            val stockHp = if (rpm <= ecu.stockRevLimiter) (stockTorque * rpm) / 7127.0 else 0.0
            val tunedHp = if (rpm <= tables.revLimiterRpm) (tunedTorque * rpm) / 7127.0 else 0.0

            DynoPoint(
                rpm = rpm,
                stockHp = (stockHp * 10.0).roundToInt() / 10.0,
                tunedHp = (tunedHp * 10.0).roundToInt() / 10.0,
                stockTorqueNm = (stockTorque * 10.0).roundToInt() / 10.0,
                tunedTorqueNm = (tunedTorque * 10.0).roundToInt() / 10.0
            )
        }

        val maxStockHpPt = points.maxByOrNull { it.stockHp }
        val maxTunedHpPt = points.maxByOrNull { it.tunedHp }
        val maxStockTqPt = points.maxByOrNull { it.stockTorqueNm }
        val maxTunedTqPt = points.maxByOrNull { it.tunedTorqueNm }

        val peakTunedHp = maxTunedHpPt?.tunedHp ?: 15.0
        val zeroToHundred = max(4.2, min(18.5, (145.0 / peakTunedHp)))
        val drag201 = max(6.8, min(14.5, 6.2 + (68.0 / peakTunedHp)))
        val drag402 = drag201 * 1.59

        return DynoRunSummary(
            points = points,
            peakStockHp = maxStockHpPt?.stockHp ?: 12.0,
            peakTunedHp = peakTunedHp,
            peakHpRpm = maxTunedHpPt?.rpm ?: peakRpm,
            peakStockTorque = maxStockTqPt?.stockTorqueNm ?: 11.0,
            peakTunedTorque = maxTunedTqPt?.tunedTorqueNm ?: 13.0,
            peakTorqueRpm = maxTunedTqPt?.rpm ?: 6500,
            zeroToHundredSec = (zeroToHundred * 10.0).roundToInt() / 10.0,
            drag201mSec = (drag201 * 10.0).roundToInt() / 10.0,
            drag402mSec = (drag402 * 10.0).roundToInt() / 10.0
        )
    }
}
