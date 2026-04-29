package model

import model.Piece.Bishop
import model.Piece._
import zio.prelude._
import zio.test.Assertion._
import zio.test._

object PieceSpec extends ZIOSpecDefault {

  def spec: Spec[TestEnvironment, Any] = suite("Piece Spec")(
    suite("Rook move")(
      test("Rook can move vertically - happy path") {
        val from                     = Position(4, 4)
        val to                       = Position(4, 7)
        val expectedVisitedPositions = Set(Position(4, 5), Position(4, 6))
        assert(Rook.move(from, to))(equalTo(Validation.succeed(expectedVisitedPositions))) &&
        assert(Rook.move(to, from))(equalTo(Validation.succeed(expectedVisitedPositions)))
      },
      test("Rook can move horizontally - happy path") {
        val from                     = Position(2, 3)
        val to                       = Position(6, 3)
        val expectedVisitedPositions = Set(Position(3, 3), Position(4, 3), Position(5, 3))
        assert(Rook.move(from, to))(equalTo(Validation.succeed(expectedVisitedPositions))) &&
        assert(Rook.move(to, from))(equalTo(Validation.succeed(expectedVisitedPositions)))
      },
      test("Rook can't move diagonally") {
        val from = Position(1, 1)
        val to   = Position(3, 3)
        assert(Rook.move(from, to))(equalTo(Validation.fail("Invalid move for Rook")))
      }
    ),
    suite("Bishop move")(
      test("Bishop can move diagonally - happy path") {
        val from         = Position(2, 2)
        val to           = Position(5, 5)
        val expectedPath = Set(Position(3, 3), Position(4, 4))
        assert(Bishop.move(from, to))(equalTo(Validation.succeed(expectedPath))) &&
        assert(Bishop.move(to, from))(equalTo(Validation.succeed(expectedPath)))
      },
      test("Bishop can't move vertically") {
        val from = Position(1, 1)
        val to   = Position(1, 4)
        assert(Bishop.move(from, to))(equalTo(Validation.fail("Invalid move for Bishop")))
      },
      test("Bishop can't move horizontally") {
        val from = Position(2, 3)
        val to   = Position(5, 3)
        assert(Bishop.move(from, to))(equalTo(Validation.fail("Invalid move for Bishop")))
      },
      test("Bishop can't move in non-diagonal direction") {
        val from = Position(2, 2)
        val to   = Position(5, 3)
        assert(Bishop.move(from, to))(equalTo(Validation.fail("Invalid move for Bishop")))
      }
    )
  )
}
