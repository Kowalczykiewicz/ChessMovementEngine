package api.dto

import sttp.tapir.Schema
import zio.json.DeriveJsonCodec
import zio.json.JsonCodec

import java.util.UUID

case class PlacePieceResponse(id: UUID)
object PlacePieceResponse {
  implicit val codec: JsonCodec[PlacePieceResponse] = DeriveJsonCodec.gen
  implicit val schema: Schema[PlacePieceResponse]   = Schema.derived
}
