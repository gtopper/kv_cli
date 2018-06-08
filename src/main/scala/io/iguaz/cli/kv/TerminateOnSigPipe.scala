package io.iguaz.cli.kv

import sun.misc.{Signal, SignalHandler}

object TerminateOnSigPipe {
  def registerHandler(): Unit = Signal.handle(new Signal("PIPE"), new SignalHandler() {
    override def handle(sig: Signal): Unit = {
      sys.exit(0)
    }
  })
}
