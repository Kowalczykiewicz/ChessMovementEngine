package api

import api.BoardEndpoints._
import api.dto.PlacePieceResponse
import service.BoardService
import sttp.tapir.server.ServerEndpoint
import zio._

object BoardRoutes {
  def routes(service: BoardService): List[ServerEndpoint[Any, Task]] = List(
    placePiece.serverLogic { req =>
      service.placePiece(req.piece, req.position).map(PlacePieceResponse(_)).either
    },
    movePiece.serverLogic { case (id, to) =>
      service.movePiece(id, to).either
    },
    removePiece.serverLogic { id =>
      service.removePiece(id).either
    }
  )
}
