package api

import repository.InMemoryBoardRepository
import service.BoardService
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zio._
import zio.http._

object BoardHttpApp {

  val app: ZIO[Any, Throwable, HttpApp[Any]] = for {
    repo <- InMemoryBoardRepository.make
    service = BoardService.make(repo)
    routes  = BoardRoutes.routes(service)
    httpApp = ZioHttpInterpreter().toHttp(routes)
  } yield httpApp
}
