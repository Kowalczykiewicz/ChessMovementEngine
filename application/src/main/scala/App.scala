import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

import api.BoardHttpApp
import zio.http.Server

object App extends ZIOAppDefault {
  override def run =
    BoardHttpApp.app.flatMap { httpApp =>
      Server
        .serve(httpApp)
        .provide(Server.defaultWithPort(8080))
    }
}
