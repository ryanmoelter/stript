import com.diozero.api.SpiDevice
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Suppress("unused")
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
  startColors: List<LedColor>,
  endColors: List<LedColor>
) {
  val colorDistance = startColors.mapIndexed { index, ledColor ->
    ledColor.calculateColorDistanceTo(endColors[index])
  }.average().toFloat()
  val duration = (2000 * colorDistance).roundToInt().toDuration(DurationUnit.MILLISECONDS)
  val lastFrame = maxOf(
    (FRAMES_PER_SECOND * duration.inWholeMilliseconds / 1000).toInt(),
    10
  )

  for (frameNum in 0..lastFrame) {
    val colors = buildList {
      (0 until NUM_LIGHTS).forEach { lightIndex ->
        val linearProgress = frameNum.toFloat() / lastFrame

        val middleOutProgressModifier =
          calculateMiddleOutProgressModifier(linearProgress, lightIndex)

        add(
          startColors[lightIndex].linearlyInterpolateTo(
            end = endColors[lightIndex],
            progress = middleOutProgressModifier
          )
        )
      }
    }
    spiDevice.writeLights(colors)
    delay(FRAME_DELAY_MILLIS)
  }
}

private fun calculateMiddleOutProgressModifier(linearProgress: Float, lightIndex: Int): Float {
  val interpolator = FastOutSlowInInterpolator()

  val interpolatedProgress = interpolator.getScaledProgressValue(linearProgress)
  val halfLightCoordinateSize = 1f / NUM_LIGHTS / 2
  val physicalLightMiddleCoordinate = lightIndex.toFloat() / NUM_LIGHTS + halfLightCoordinateSize
  val mappedLightStartCoordinate =
    ((physicalLightMiddleCoordinate - .5f) * 2f).absoluteValue - (halfLightCoordinateSize / 2)

  return ((interpolatedProgress - mappedLightStartCoordinate) * NUM_LIGHTS)
    .coerceIn(0f, 1f)
}

private fun LedColor.calculateColorDistanceTo(other: LedColor): Float {
  return listOf(
    red.calculateColorDistanceTo(other.red),
    green.calculateColorDistanceTo(other.green),
    blue.calculateColorDistanceTo(other.blue)
  ).average().toFloat()
}

private fun UByte.calculateColorDistanceTo(other: UByte): Float {
  val distance = (this.toInt() - other.toInt()).absoluteValue.toFloat() / UByte.MAX_VALUE.toFloat()
  return distance * distance
}

@Suppress("unused")
private fun calculateRightToLeftProgressModifier(linearProgress: Float, lightIndex: Int): Float {
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

private fun LedColor.linearlyInterpolateTo(end: LedColor, progress: Float) =
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

private fun SpiDevice.writeColor(color: LedColor) {
  write(UByte.MAX_VALUE.toByte(), color.blue.toByte(), color.green.toByte(), color.red.toByte())
}
