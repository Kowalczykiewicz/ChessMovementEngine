package api.dto

import model.Piece
import model.Position
import sttp.tapir.Schema
import zio.json.DeriveJsonCodec
import zio.json.JsonCodec

case class PlacePieceRequest(piece: Piece, position: Position)
object PlacePieceRequest {
  implicit val codec: JsonCodec[PlacePieceRequest] = DeriveJsonCodec.gen
  implicit val schema: Schema[PlacePieceRequest]   = Schema.derived
}
