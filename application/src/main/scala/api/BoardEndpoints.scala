package api

import api.dto.PlacePieceRequest
import api.dto.PlacePieceResponse
import model.Position
import service.BoardService
import service.BoardService.Error.Invalid
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.zio._
import java.util.UUID

object BoardEndpoints {

  private val errorOutput = oneOf[BoardService.Error](
    oneOfVariant(StatusCode.BadRequest, jsonBody[Invalid].description("Invalid input")),
    oneOfVariant(StatusCode.InternalServerError, jsonBody[Invalid].description("Unexpected error"))
  )

  val placePiece: Endpoint[Unit, PlacePieceRequest, BoardService.Error, PlacePieceResponse, Any] =
    endpoint.post
      .in("pieces")
      .in(jsonBody[PlacePieceRequest])
      .errorOut(errorOutput)
      .out(jsonBody[PlacePieceResponse])
      .description("Places a piece at a specific position on the board")

  val movePiece: Endpoint[Unit, (UUID, Position), BoardService.Error, Unit, Any] =
    endpoint.put
      .in("pieces" / path[UUID]("id") / "move")
      .in(jsonBody[Position])
      .errorOut(errorOutput)
      .out(emptyOutput)
      .description("Moves a piece to a new position on the board")

  val removePiece: Endpoint[Unit, UUID, BoardService.Error, Unit, Any] =
    endpoint.delete
      .in("pieces" / path[UUID]("id"))
      .errorOut(errorOutput)
      .out(emptyOutput)
      .description("Removes a piece from the board")

}
