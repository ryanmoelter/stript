import com.diozero.api.SpiDevice
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

const val NUM_LIGHTS = 93

fun main(args: Array<String>) {
  println("Hello World!")

  // Try adding program arguments via Run/Debug configuration.
  // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
  println("Writing bytes...")
  runBlocking {
    SpiDevice(0).use { spiDevice ->
      val lastFrame = 119
      for (frameNum in 0..lastFrame) {
        val colors = buildList {
          (0..NUM_LIGHTS).forEach { lightIndex ->
            add(
              if (lightIndex % 10 == 0) {
                LedColor(0b11111111 * frameNum / lastFrame, 0x0F, 0x00)
              } else {
                LedColor(0, 0, 0)
              }
            )
          }
        }
        spiDevice.writeLights(colors)
        delay(16)
      }
    }
  }
  println("Written")
}

data class LedColor(val red: UByte, val green: UByte, val blue: UByte) {
  constructor(red: Int, green: Int, blue: Int) : this(red.toUByte(), green.toUByte(), blue.toUByte())
}

fun SpiDevice.writeLights(colors: List<LedColor>) {
  assert(colors.size == NUM_LIGHTS) {
    "Colors list is only ${colors.size} long, need $NUM_LIGHTS colors"
  }
  write(0, 0, 0, 0)
  colors.forEach { color ->
    writeColor(color)
  }
  write(0, 0, 0, 0, 0)

}

fun SpiDevice.writeColor(color: LedColor) {
  write(Byte.MAX_VALUE, color.blue.toByte(), color.green.toByte(), color.red.toByte())
}
