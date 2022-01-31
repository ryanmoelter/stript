import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers

@Suppress("unused")
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
