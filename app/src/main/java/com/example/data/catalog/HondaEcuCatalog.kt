package com.example.data.catalog

import com.example.data.model.DtcRecord
import com.example.data.model.HondaEcuModel

object HondaEcuCatalog {

    private val RPM_SKUTIK_110 = listOf(
        900, 1400, 1900, 2500, 3200, 4000, 4800, 5600,
        6400, 7200, 8000, 8800, 9500, 10200, 10800, 11500
    )

    private val RPM_SKUTIK_125_160 = listOf(
        900, 1500, 2100, 2800, 3500, 4300, 5100, 5900,
        6700, 7500, 8300, 9100, 9900, 10700, 11500, 12500
    )

    private val RPM_SPORT_150 = listOf(
        900, 1500, 2200, 3000, 4000, 5000, 6000, 7000,
        8000, 9000, 10000, 11000, 12000, 12800, 13600, 14500
    )

    private val RPM_TWIN_250 = listOf(
        1000, 1800, 2600, 3500, 4500, 5500, 6500, 7500,
        8500, 9500, 10500, 11500, 12500, 13400, 14100, 14800
    )

    val MODELS: List<HondaEcuModel> = listOf(
        HondaEcuModel(
            id = "vario160-k2s",
            name = "Honda Vario 160 eSP+ (4V)",
            category = "Skutik 160cc",
            partNumber = "30400-K2S-N01",
            mcuArchitecture = "Renesas RH850/F1K",
            manufacturer = "Shindengen",
            displacementCc = 157,
            flashSizeKb = 256,
            protocol = "K-Line 10400bps / Honda Fast-Init",
            stockRevLimiter = 9700,
            fuelTableOffsetHex = "0x012400",
            ignitionTableOffsetHex = "0x012800",
            limiterOffsetHex = "0x0131A0",
            rpmBreakpoints = RPM_SKUTIK_125_160
        ),
        HondaEcuModel(
            id = "pcx160-k1z",
            name = "Honda PCX 160 eSP+",
            category = "Skutik 160cc",
            partNumber = "30400-K1Z-J11",
            mcuArchitecture = "Renesas RH850/F1K",
            manufacturer = "Shindengen",
            displacementCc = 157,
            flashSizeKb = 256,
            protocol = "K-Line 10400bps",
            stockRevLimiter = 9600,
            fuelTableOffsetHex = "0x012200",
            ignitionTableOffsetHex = "0x012600",
            limiterOffsetHex = "0x0130B4",
            rpmBreakpoints = RPM_SKUTIK_125_160
        ),
        HondaEcuModel(
            id = "adv160-k0w",
            name = "Honda ADV 160 HSTC",
            category = "Skutik 160cc",
            partNumber = "30400-K0W-N01",
            mcuArchitecture = "Renesas RH850/F1L",
            manufacturer = "Shindengen",
            displacementCc = 157,
            flashSizeKb = 256,
            protocol = "K-Line 10400bps",
            stockRevLimiter = 9650,
            fuelTableOffsetHex = "0x012480",
            ignitionTableOffsetHex = "0x012880",
            limiterOffsetHex = "0x013220",
            rpmBreakpoints = RPM_SKUTIK_125_160
        ),
        HondaEcuModel(
            id = "stylo160-k3v",
            name = "Honda Stylo 160 ABS",
            category = "Skutik 160cc",
            partNumber = "30400-K3V-N11",
            mcuArchitecture = "Renesas RH850/F1K",
            manufacturer = "Shindengen",
            displacementCc = 157,
            flashSizeKb = 256,
            protocol = "K-Line 10400bps",
            stockRevLimiter = 9700,
            fuelTableOffsetHex = "0x012440",
            ignitionTableOffsetHex = "0x012840",
            limiterOffsetHex = "0x0131C8",
            rpmBreakpoints = RPM_SKUTIK_125_160
        ),
        HondaEcuModel(
            id = "vario150-k59j",
            name = "Honda Vario 150 Keyless",
            category = "Skutik 125-150cc",
            partNumber = "30400-K59-A71",
            mcuArchitecture = "Renesas M32R",
            manufacturer = "Shindengen",
            displacementCc = 149,
            flashSizeKb = 128,
            protocol = "K-Line 10400bps",
            stockRevLimiter = 9500,
            fuelTableOffsetHex = "0x00A100",
            ignitionTableOffsetHex = "0x00A500",
            limiterOffsetHex = "0x00AF20",
            rpmBreakpoints = RPM_SKUTIK_125_160
        ),
        HondaEcuModel(
            id = "vario125-k2v",
            name = "Honda Vario 125 ISS New",
            category = "Skutik 125-150cc",
            partNumber = "30400-K2V-N21",
            mcuArchitecture = "Renesas M32R",
            manufacturer = "Shindengen",
            displacementCc = 125,
            flashSizeKb = 128,
            protocol = "K-Line 10400bps",
            stockRevLimiter = 9400,
            fuelTableOffsetHex = "0x009C00",
            ignitionTableOffsetHex = "0x00A000",
            limiterOffsetHex = "0x00A890",
            rpmBreakpoints = RPM_SKUTIK_125_160
        ),
        HondaEcuModel(
            id = "beat-deluxe-k1a",
            name = "Honda BeAT Deluxe / Street eSP",
            category = "Skutik 110cc",
            partNumber = "30400-K1A-N11",
            mcuArchitecture = "Keihin R8C/SH",
            manufacturer = "Keihin",
            displacementCc = 110,
            flashSizeKb = 64,
            protocol = "K-Line 10400bps",
            stockRevLimiter = 9200,
            fuelTableOffsetHex = "0x006400",
            ignitionTableOffsetHex = "0x006800",
            limiterOffsetHex = "0x007110",
            rpmBreakpoints = RPM_SKUTIK_110
        ),
        HondaEcuModel(
            id = "scoopy-k2f",
            name = "Honda Scoopy Smartkey",
            category = "Skutik 110cc",
            partNumber = "30400-K2F-N11",
            mcuArchitecture = "Keihin R8C/SH",
            manufacturer = "Keihin",
            displacementCc = 110,
            flashSizeKb = 64,
            protocol = "K-Line 10400bps",
            stockRevLimiter = 9200,
            fuelTableOffsetHex = "0x006420",
            ignitionTableOffsetHex = "0x006820",
            limiterOffsetHex = "0x007140",
            rpmBreakpoints = RPM_SKUTIK_110
        ),
        HondaEcuModel(
            id = "genio-k0j",
            name = "Honda Genio eSAF",
            category = "Skutik 110cc",
            partNumber = "30400-K0J-N01",
            mcuArchitecture = "Keihin R8C",
            manufacturer = "Keihin",
            displacementCc = 110,
            flashSizeKb = 64,
            protocol = "K-Line 10400bps",
            stockRevLimiter = 9150,
            fuelTableOffsetHex = "0x006300",
            ignitionTableOffsetHex = "0x006700",
            limiterOffsetHex = "0x007040",
            rpmBreakpoints = RPM_SKUTIK_110
        ),
        HondaEcuModel(
            id = "cbr150r-k45r",
            name = "Honda CBR150R DOHC (K45R)",
            category = "Sport 150cc",
            partNumber = "38770-K45-N61",
            mcuArchitecture = "Keihin SH7058",
            manufacturer = "Keihin",
            displacementCc = 149,
            flashSizeKb = 256,
            protocol = "K-Line 10400bps",
            stockRevLimiter = 10800,
            fuelTableOffsetHex = "0x018200",
            ignitionTableOffsetHex = "0x018600",
            limiterOffsetHex = "0x019410",
            rpmBreakpoints = RPM_SPORT_150
        ),
        HondaEcuModel(
            id = "cb150r-k15p",
            name = "Honda CB150R Streetfire V3",
            category = "Sport 150cc",
            partNumber = "38770-K15-921",
            mcuArchitecture = "Keihin SH7058",
            manufacturer = "Keihin",
            displacementCc = 149,
            flashSizeKb = 256,
            protocol = "K-Line 10400bps",
            stockRevLimiter = 10700,
            fuelTableOffsetHex = "0x018000",
            ignitionTableOffsetHex = "0x018400",
            limiterOffsetHex = "0x019280",
            rpmBreakpoints = RPM_SPORT_150
        ),
        HondaEcuModel(
            id = "sonic150r-k56",
            name = "Honda Sonic 150R DOHC",
            category = "Bebek / Underbone",
            partNumber = "38770-K56-N01",
            mcuArchitecture = "Keihin SH7055",
            manufacturer = "Keihin",
            displacementCc = 149,
            flashSizeKb = 128,
            protocol = "K-Line 10400bps",
            stockRevLimiter = 10900,
            fuelTableOffsetHex = "0x00E100",
            ignitionTableOffsetHex = "0x00E500",
            limiterOffsetHex = "0x00F120",
            rpmBreakpoints = RPM_SPORT_150
        ),
        HondaEcuModel(
            id = "supragtr-k56f",
            name = "Honda Supra GTR 150",
            category = "Bebek / Underbone",
            partNumber = "38770-K56-N11",
            mcuArchitecture = "Keihin SH7055",
            manufacturer = "Keihin",
            displacementCc = 149,
            flashSizeKb = 128,
            protocol = "K-Line 10400bps",
            stockRevLimiter = 10600,
            fuelTableOffsetHex = "0x00E200",
            ignitionTableOffsetHex = "0x00E600",
            limiterOffsetHex = "0x00F240",
            rpmBreakpoints = RPM_SPORT_150
        ),
        HondaEcuModel(
            id = "crf150l-k84",
            name = "Honda CRF150L Trail",
            category = "Sport 150cc",
            partNumber = "38770-K84-901",
            mcuArchitecture = "Keihin M32R",
            manufacturer = "Keihin",
            displacementCc = 149,
            flashSizeKb = 128,
            protocol = "K-Line 10400bps",
            stockRevLimiter = 9800,
            fuelTableOffsetHex = "0x00B400",
            ignitionTableOffsetHex = "0x00B800",
            limiterOffsetHex = "0x00C100",
            rpmBreakpoints = RPM_SPORT_150
        ),
        HondaEcuModel(
            id = "cbr250rr-k64",
            name = "Honda CBR250RR Twin TBW",
            category = "Sport 250cc+",
            partNumber = "38770-K64-N01",
            mcuArchitecture = "Keihin RH850/P1M",
            manufacturer = "Keihin",
            displacementCc = 249,
            flashSizeKb = 512,
            protocol = "CAN-Bus ISO 15765-4 500kbps",
            stockRevLimiter = 14200,
            fuelTableOffsetHex = "0x024000",
            ignitionTableOffsetHex = "0x024800",
            limiterOffsetHex = "0x026400",
            rpmBreakpoints = RPM_TWIN_250
        ),
        HondaEcuModel(
            id = "forza250-k0b",
            name = "Honda Forza 250 Euro5",
            category = "Sport 250cc+",
            partNumber = "38770-K0B-T01",
            mcuArchitecture = "Keihin RH850",
            manufacturer = "Keihin",
            displacementCc = 249,
            flashSizeKb = 512,
            protocol = "CAN-Bus ISO 15765-4 500kbps",
            stockRevLimiter = 9500,
            fuelTableOffsetHex = "0x021200",
            ignitionTableOffsetHex = "0x021600",
            limiterOffsetHex = "0x022900",
            rpmBreakpoints = RPM_SKUTIK_125_160
        )
    )

    val DTC_CATALOG: List<DtcRecord> = listOf(
        DtcRecord(
            code = "P0107",
            blinkCode = 1,
            title = "Sensor MAP Tegangan Rendah",
            description = "Sinyal tekanan udara intake manifold berada di bawah ambang batas 0.2V.",
            remedy = "Periksa soket sensor MAP di throttle body, cek selang vakum bocor atau kabel putus."
        ),
        DtcRecord(
            code = "P0112",
            blinkCode = 9,
            title = "Sensor IAT Tegangan Rendah",
            description = "Sensor suhu udara masuk (Intake Air Temp) mengalami hubung singkat ke massa.",
            remedy = "Cek kabel konektor 3-pin/5-pin sensor unit di throttle body dan bersihkan soket."
        ),
        DtcRecord(
            code = "P0117",
            blinkCode = 7,
            title = "Sensor ECT / EOT Tegangan Rendah",
            description = "Pembacaan sensor suhu air radiator / oli mesin tidak normal.",
            remedy = "Periksa soket sensor ECT di blok silinder kanan dan pastikan air radiator cukup."
        ),
        DtcRecord(
            code = "P0122",
            blinkCode = 8,
            title = "Sensor TPS Tegangan Rendah",
            description = "Sinyal posisi bukaan katup gas di bawah 0.35V saat langsam.",
            remedy = "Lakukan Reset TPS Titik Nol di menu Flash & Fitur Khusus atau cek soket TPS."
        ),
        DtcRecord(
            code = "P0131",
            blinkCode = 21,
            title = "Sensor O2 Tegangan Terlalu Rendah (Lean)",
            description = "Deteksi campuran terlalu kering terus-menerus atau kabel O2 leher knalpot putus.",
            remedy = "Periksa soket sensor O2 knalpot, cek tekanan fuel pump, atau kalibrasi O2."
        ),
        DtcRecord(
            code = "P0201",
            blinkCode = 12,
            title = "Sirkuit Injektor Bahan Bakar Bermasalah",
            description = "Kumparan solenoid injektor putus atau driver injector di ECM tidak mendeteksi beban.",
            remedy = "Ukur tahanan injektor (standar 9-12 Ohm) dan periksa soket 2-pin injektor."
        ),
        DtcRecord(
            code = "P0335",
            blinkCode = 52,
            title = "Sensor CKP (Crankshaft Position) Error",
            description = "Tidak ada pulsa putaran magnet ACG / flywheel saat starter ditekan.",
            remedy = "Periksa soket 6-pin CKP di sebelah kanan mesin, bersihkan kotoran serbuk magnet."
        ),
        DtcRecord(
            code = "P0505",
            blinkCode = 29,
            title = "Motor Stepper IACV Idle Speed Control",
            description = "Katup pengatur langsam (IACV) macet oleh kerak karbon atau pin konektor longgar.",
            remedy = "Bongkar dan bersihkan ujung piston IACV dengan carb cleaner, lalu lakukan reset ECM."
        )
    )
}
