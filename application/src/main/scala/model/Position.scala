package model

import sttp.tapir.Schema
import zio.json.DeriveJsonCodec
import zio.json.JsonCodec

case class Position(x: Int, y: Int) {
  override def toString = s"($x, $y)"
}

object Position {
  implicit val codec: JsonCodec[Position] = DeriveJsonCodec.gen[Position]
  implicit val schema: Schema[Position]   = Schema.derived[Position]
}
