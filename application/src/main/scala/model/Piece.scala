package model

import sttp.tapir.Schema
import zio.json._
import zio.prelude._

@jsonDiscriminator("$type")
sealed trait Piece {
  def move(from: Position, to: Position): Validation[String, Set[Position]]
}

object Piece {
  case object Rook extends Piece {
    def move(from: Position, to: Position): Validation[String, Set[Position]] = {
      def calcVisitedPositions(from: Int, to: Int, const: Int, isVertical: Boolean): Set[Position] = {
        val range = {
          val min = Math.min(from, to)
          val max = if (min == from) to else from
          min + 1 until max
        }
        val positions = if (isVertical) range.map(y => Position(const, y)) else range.map(x => Position(x, const))
        positions.toSet
      }

      if (from.x == to.x)
        Validation.succeed(calcVisitedPositions(from.y, to.y, from.x, isVertical = true))
      else if (from.y == to.y)
        Validation.succeed(calcVisitedPositions(from.x, to.x, from.y, isVertical = false))
      else
        Validation.fail("Invalid move for Rook")
    }
  }

  case object Bishop extends Piece {
    def move(from: Position, to: Position): Validation[String, Set[Position]] = {
      val deltaX = to.x - from.x
      val deltaY = to.y - from.y
      if (Math.abs(deltaX) == Math.abs(deltaY)) {
        val xStep = if (deltaX > 0) 1 else -1
        val yStep = if (deltaY > 0) 1 else -1
        val path  = (1 until Math.abs(deltaX)).map(i => Position(from.x + i * xStep, from.y + i * yStep))
        Validation.succeed(path.toSet)
      } else
        Validation.fail("Invalid move for Bishop")
    }
  }

  implicit val codec: JsonCodec[Piece] = {
    val encoder: JsonEncoder[Piece] = JsonEncoder[String].contramap {
      case Rook   => "rook"
      case Bishop => "bishop"
    }
    val decoder: JsonDecoder[Piece] = JsonDecoder[String].mapOrFail {
      case "rook"   => Right(Rook)
      case "bishop" => Right(Bishop)
      case other    => Left(s"Unknown piece: $other")
    }
    JsonCodec(encoder, decoder)
  }
  implicit val schema: Schema[Piece] = Schema.derived[Piece]
}
