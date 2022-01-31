import kotlinx.coroutines.runBlocking

const val NUM_LIGHTS = 93
const val FRAMES_PER_SECOND = 120
const val FRAME_DELAY_MILLIS = 1000 / FRAMES_PER_SECOND.toLong()

fun main() {
  runBlocking {
    runServer()
  }
}
