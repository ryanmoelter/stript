import com.diozero.api.SpiDevice
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope

suspend fun runServer() = coroutineScope {
  var currentColors: List<LedColor> = (0 until NUM_LIGHTS).map { LedColor(0, 0, 0) }
  SpiDevice(0).use { it.writeLights(currentColors) }

  val server = aSocket(ActorSelectorManager(Dispatchers.IO))
    .tcp()
    .bind(hostname = "127.0.0.1", port = 2323)
  println("Server running: ${server.localAddress}")

  while (true) {
    try {
      val socket = server.accept()
      println("Server accepted socket: ${socket.remoteAddress}")
      val input = socket.openReadChannel()

      while (true) {
        val colors = readLedColors(input)
        println("Server received message: ${colors.first()}")

        if (colors != currentColors) {
          SpiDevice(0).use { spiDevice ->
            fadeInFromMiddleOut(
              spiDevice,
              currentColors,
              colors
            )
          }
          currentColors = colors
          println("Server set lights")
        }
      }
    } catch (e: Throwable) {
      e.printStackTrace()
    }
  }
}

private suspend fun ByteReadChannel.readUByte(): UByte = readByte().toUByte()

private suspend fun readLedColors(input: ByteReadChannel): List<LedColor> {
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

  return colors
}
