package to.wim.quicksettings.settings

/** Android's settings in memory, with every write recorded in order. */
class FakeRaw(
    vararg initial: Pair<String, String>,
) : RawSettings {
    val values = mutableMapOf(*initial)
    val writes = mutableListOf<String>()

    override fun get(
        table: Table,
        key: String,
    ): String? = values["${table.name.lowercase()}/$key"]

    override fun put(
        table: Table,
        key: String,
        value: String,
    ) {
        val name = "${table.name.lowercase()}/$key"
        writes += "$name=$value"
        values[name] = value
    }
}

class FakeAudio(
    var ringerMode: RingerMode = RingerMode.Normal,
) : Audio {
    val volumes = mutableMapOf<Stream, Float>()

    override fun ringer() = ringerMode

    override fun setRinger(ringer: RingerMode) {
        ringerMode = ringer
    }

    override fun volume(stream: Stream) = volumes[stream] ?: 0.5f

    override fun setVolume(
        stream: Stream,
        position: Float,
    ) {
        volumes[stream] = position
    }
}

fun device(
    vararg values: Pair<String, String>,
    audio: FakeAudio = FakeAudio(),
): Pair<Device, FakeRaw> {
    val raw = FakeRaw(*values)
    return Device(raw, audio) to raw
}
