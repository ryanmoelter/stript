import com.diozero.api.SpiDevice
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt
import kotlin.time.DurationUnit
import kotlin.time.toDuration

const val NUM_LIGHTS = 93
const val FRAMES_PER_SECOND = 120
const val FRAME_DELAY_MILLIS = 1000 / FRAMES_PER_SECOND.toLong()

fun main() {
  SpiDevice(0).use { spiDevice ->
    runBlocking {
      println("Writing bytes...")
      fadeInByProgress(spiDevice)
      println("Written")
    }
  }
}

suspend fun fadeInByProgress(spiDevice: SpiDevice) {
  val duration = 4.toDuration(DurationUnit.SECONDS)
  val lastFrame = (FRAMES_PER_SECOND * duration.inWholeMilliseconds / 1000).toInt()

  for (frameNum in 0..lastFrame) {
    val colors = buildList {
      (0 until NUM_LIGHTS).forEach { lightIndex ->
        val linearProgress = frameNum.toFloat() / lastFrame

        val rightToLeftModifier = calculateRightToLeftProgressModifier(linearProgress, lightIndex)
        val fadeModifier = linearProgress

        val brightness = (0xFF * fadeModifier * rightToLeftModifier).roundToInt()
        add(LedColor(red = brightness, green = brightness, blue = brightness))
      }
    }
    spiDevice.writeLights(colors)
    delay(FRAME_DELAY_MILLIS)
  }
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
  // 0xFF
  // 0b 1111 1111
}
