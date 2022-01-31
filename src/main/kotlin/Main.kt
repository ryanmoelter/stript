import com.diozero.api.SpiDevice
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlin.math.absoluteValue
import kotlin.time.DurationUnit
import kotlin.time.toDuration

const val NUM_LIGHTS = 93
const val FRAMES_PER_SECOND = 120
const val FRAME_DELAY_MILLIS = 1000 / FRAMES_PER_SECOND.toLong()

fun main() {
  runBlocking {
    launch { runServer() }
    delay(1000)
    dummyClient(LedColor(0, 0, 0xFF))
    delay(5000)
    dummyClient(LedColor(0xFF, 0xFF, 0x00))
    delay(5000)
    dummyClient(LedColor(0xFF, 0xFF, 0xFF))
  }
}

private suspend fun dummyClient(color: LedColor) {
  val socket = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
    .connect(InetSocketAddress("127.0.0.1", 2323))
  val output = socket.openWriteChannel(autoFlush = true)

  val message = buildList {
    add(0x00.toUByte())  // Channel
    add(0x00.toUByte())  // Command
    val length = (NUM_LIGHTS * 3).toUInt()
    add((length shr Byte.SIZE_BITS).toUByte())  // Length high
    add(length.toUByte())  // Length low
    for (index in 0 until NUM_LIGHTS) {
      add(color.red)  // red
      add(color.green)  // green
      add(color.blue)  // blue
    }
  }

  output.writeFully(message.map { it.toByte() }.toByteArray())
  output.close()
  println("Client sent first message")
}

private suspend fun runServer() {
  SpiDevice(0).use { spiDevice ->
    var currentColors: List<LedColor> = (0 until NUM_LIGHTS).map { LedColor(0, 0, 0) }
    val server = aSocket(ActorSelectorManager(Dispatchers.IO))
      .tcp()
      .bind(hostname = "127.0.0.1", port = 2323)
    println("Server running: ${server.localAddress}")

    while (true) {
      val colors = acceptConnectionAndReadMessage(server)

      fadeInFromMiddleOut(
        spiDevice = spiDevice,
        startColor = currentColors,
        endColor = colors
      )
      println("Server set lights")

      currentColors = colors
      yield()
    }
  }
}

private suspend fun acceptConnectionAndReadMessage(server: ServerSocket): List<LedColor> {
  val socket = server.accept()
  println("Server accepted socket: ${socket.remoteAddress}")
  val input = socket.openReadChannel()

  val colors = readLedColors(input)
  println("Server received message")

  return colors
}

suspend fun ByteReadChannel.readUByte(): UByte = readByte().toUByte()

suspend fun readLedColors(input: ByteReadChannel): List<LedColor> {
  /* From http://openpixelcontrol.org/
   *
   * | channel   | command   | length (n)            | data                    |
   * | 0 to 255  | 0 to 255  | high byte   low byte  | n bytes of message data |
   */
  val channel = input.readUByte()
  val command = input.readUByte()
  val length = (input.readUByte().toUInt() shl Byte.SIZE_BITS) + input.readUByte().toUInt()

  assert(channel == 0.toUByte())
  assert(command == 0.toUByte())

  val colors = buildList {
    for (i in 0U until length step 3) {
      val red = input.readUByte()
      val green = input.readUByte()
      val blue = input.readUByte()
      add(LedColor(red = red, green = green, blue = blue))
    }
  }

  // TODO close buffer?

  return colors
}

suspend fun fadeInFromMiddleOut(
  spiDevice: SpiDevice,
  startColor: LedColor,
  endColor: LedColor
) {
  fadeInFromMiddleOut(
    spiDevice,
    (0 until NUM_LIGHTS).map { startColor },
    (0 until NUM_LIGHTS).map { endColor }
  )
}

suspend fun fadeInFromMiddleOut(
  spiDevice: SpiDevice,
  startColor: List<LedColor>,
  endColor: List<LedColor>
) {
  val duration = 2.toDuration(DurationUnit.SECONDS)
  val lastFrame = (FRAMES_PER_SECOND * duration.inWholeMilliseconds / 1000).toInt()

  for (frameNum in 0..lastFrame) {
    val colors = buildList {
      (0 until NUM_LIGHTS).forEach { lightIndex ->
        val linearProgress = frameNum.toFloat() / lastFrame

        val middleOutProgressModifier =
          calculateMiddleOutProgressModifier(linearProgress, lightIndex)

        add(
          startColor[lightIndex].linearlyInterpolateTo(
            end = endColor[lightIndex],
            progress = middleOutProgressModifier
          )
        )
      }
    }
    spiDevice.writeLights(colors)
    delay(FRAME_DELAY_MILLIS)
  }
}

fun calculateMiddleOutProgressModifier(linearProgress: Float, lightIndex: Int): Float {
  val interpolator = FastOutSlowInInterpolator()

  val interpolatedProgress = interpolator.getScaledProgressValue(linearProgress)
  val halfLightCoordinateSize = 1f / NUM_LIGHTS / 2
  val physicalLightMiddleCoordinate = lightIndex.toFloat() / NUM_LIGHTS + halfLightCoordinateSize
  val mappedLightStartCoordinate =
    ((physicalLightMiddleCoordinate - .5f) * 2f).absoluteValue - (halfLightCoordinateSize / 2)

  return ((interpolatedProgress - mappedLightStartCoordinate) * NUM_LIGHTS)
    .coerceIn(0f, 1f)
}

fun calculateRightToLeftProgressModifier(linearProgress: Float, lightIndex: Int): Float {
  val interpolator = FastOutSlowInInterpolator()

  val interpolatedProgress = interpolator.getScaledProgressValue(linearProgress)
  val lightRelativeCoordinateStart = lightIndex.toFloat() / NUM_LIGHTS

  return ((interpolatedProgress - lightRelativeCoordinateStart) * NUM_LIGHTS)
    .coerceIn(0f, 1f)
}

@Suppress("unused")
fun clear(spiDevice: SpiDevice) {
  spiDevice.writeLights(
    buildList {
      repeat(NUM_LIGHTS) {
        add(LedColor(0, 0, 0))
      }
    }
  )
}

data class LedColor(val red: UByte, val green: UByte, val blue: UByte) {
  constructor(red: Int, green: Int, blue: Int) : this(
    red.toUByte(),
    green.toUByte(),
    blue.toUByte()
  )
}

fun LedColor.linearlyInterpolateTo(end: LedColor, progress: Float) =
  LedColor(
    red = linearlyInterpolate(red.toInt(), end.red.toInt(), progress),
    green = linearlyInterpolate(green.toInt(), end.green.toInt(), progress),
    blue = linearlyInterpolate(blue.toInt(), end.blue.toInt(), progress),
  )

fun SpiDevice.writeLights(colors: List<LedColor>) {
  assert(colors.size == NUM_LIGHTS) {
    "Colors list is only ${colors.size} long, need $NUM_LIGHTS colors"
  }
  write(0, 0, 0, 0)
  colors.forEach { color ->
    writeColor(color)
  }
  write(0, 0, 0, 0, 0, 0)
}

fun SpiDevice.writeColor(color: LedColor) {
  write(UByte.MAX_VALUE.toByte(), color.blue.toByte(), color.green.toByte(), color.red.toByte())
}
