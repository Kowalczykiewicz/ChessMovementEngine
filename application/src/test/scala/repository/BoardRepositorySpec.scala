package repository

import model.Piece.Bishop
import model.Piece.Rook
import model._
import zio._
import zio.test.Assertion._
import zio.test._

import java.util.UUID

object BoardRepositorySpec extends ZIOSpecDefault {

  val createRepo: UIO[BoardRepository] = InMemoryBoardRepository.make

  def spec: Spec[TestEnvironment, Any] = suite("BoardRepositorySpec")(
    suite("Place piece")(
      test("Place a piece - happy path") {
        val piece    = Rook
        val position = Position(0, 0)

        for {
          repo  <- createRepo
          id    <- repo.place(piece, position)
          state <- repo.getState
        } yield assert(state.size)(equalTo(1)) && assert(state.contains(id))(isTrue)
      },
      test("Fail to place a piece at an incorrect position") {
        val piece    = Rook
        val position = Position(-1, 0)

        for {
          repo   <- createRepo
          result <- repo.place(piece, position).either
        } yield assert(result.isLeft)(isTrue) && assert(result.left.get)(equalTo("Invalid position (-1, 0)"))
      },
      test("Fail to place a piece when position is already occupied") {
        val piece1   = Rook
        val piece2   = Bishop
        val position = Position(1, 1)

        for {
          repo   <- createRepo
          _      <- repo.place(piece1, position)
          result <- repo.place(piece2, position).either
        } yield assert(result.isLeft)(isTrue) && assert(result.left.get)(
          equalTo(s"Position already occupied $position")
        )
      },
      test("Fail to place a piece that was removed") {
        val piece    = Rook
        val position = Position(0, 0)

        for {
          repo   <- createRepo
          id     <- repo.place(piece, position)
          _      <- repo.remove(id)
          result <- repo.place(piece, Position(2, 2)).either
        } yield assert(result)(isLeft(equalTo(s"Piece $piece was removed and cannot be placed again")))
      }
    ),
    suite("Move piece")(
      test("Move a piece - happy path") {
        val piece = Rook
        val from  = Position(0, 0)
        val to    = Position(0, 2)

        for {
          repo  <- createRepo
          id    <- repo.place(piece, from)
          _     <- repo.move(id, to)
          state <- repo.getState
          movedPiece = state.get(id).map(_._2)
        } yield assert(movedPiece)(isSome(equalTo(to))) && assert(state.contains(id))(isTrue)
      },
      test("Fail to move a piece if path is blocked") {
        val piece   = Rook
        val from    = Position(0, 0)
        val to      = Position(0, 2)
        val blocked = Position(0, 1)

        for {
          repo   <- createRepo
          id     <- repo.place(piece, from)
          _      <- repo.place(Rook, blocked)
          result <- repo.move(id, to).either
        } yield assert(result)(isLeft(equalTo("Path blocked at (0, 1)")))
      }
    ),
    suite("Remove piece")(
      test("Remove a piece - happy path") {
        val piece    = Rook
        val position = Position(0, 0)

        for {
          repo  <- createRepo
          id    <- repo.place(piece, position)
          _     <- repo.remove(id)
          state <- repo.getState
        } yield assert(state.contains(id))(isFalse)
      },
      test("Fail to remove a non-existent piece") {
        val invalidId = UUID.randomUUID()

        for {
          repo   <- createRepo
          result <- repo.remove(invalidId).either
        } yield assert(result)(isLeft(equalTo(s"Piece ID ($invalidId) not found")))
      }
    )
  )
}
