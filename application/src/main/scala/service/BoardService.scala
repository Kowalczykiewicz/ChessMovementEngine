package service

import model.Piece
import model.Position
import repository.BoardRepository
import service.BoardService.Error
import sttp.tapir.Schema
import zio.IO
import zio.json._

import java.util.UUID

/** Service for managing board-related operations, providing methods to place, move, and remove pieces.
  */
trait BoardService {

  /** Places a piece at a specific position on the board.
    *
    * @param piece
    *   The piece to place.
    * @param position
    *   The position to place the piece at.
    * @return
    *   The unique identifier (UUID) of the placed piece.
    */
  def placePiece(piece: Piece, position: Position): IO[Error, UUID]

  /** Moves a piece to a new position on the board.
    *
    * @param id
    *   The unique identifier (UUID) of the piece to move.
    * @param to
    *   The target position to move the piece to.
    * @return
    *   Unit on success.
    */
  def movePiece(id: UUID, to: Position): IO[Error, Unit]

  /** Removes a piece from the board.
    *
    * @param id
    *   The unique identifier (UUID) of the piece to remove.
    * @return
    *   Unit on success.
    */
  def removePiece(id: UUID): IO[Error, Unit]
}

class BoardServiceImpl(repo: BoardRepository) extends BoardService {
  override def placePiece(piece: Piece, position: Position): IO[Error, UUID] =
    repo.place(piece, position).mapError(error => Error.Invalid(s"Error placing piece: $error"))

  override def movePiece(id: UUID, to: Position): IO[Error, Unit] =
    repo.move(id, to).mapError(error => Error.Invalid(s"Error moving piece: $error"))

  override def removePiece(id: UUID): IO[Error, Unit] =
    repo.remove(id).mapError(error => Error.Invalid(s"Error removing piece: $error"))
}

object BoardService {
  @jsonDiscriminator("$type")
  sealed trait Error extends Product with Serializable

  object Error {
    implicit lazy val codec: JsonCodec[Error] = DeriveJsonCodec.gen

    case class Invalid(error: Seq[String]) extends Error
    object Invalid {
      def apply(error: String): Invalid = Invalid(Seq(error))

      implicit lazy val codec: JsonCodec[Invalid] = DeriveJsonCodec.gen
      implicit lazy val schema: Schema[Invalid]   = Schema.derived
    }
  }

  def make(repo: BoardRepository): BoardService = new BoardServiceImpl(repo)
}
