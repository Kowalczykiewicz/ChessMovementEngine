package repository

import model.Piece
import model.Position
import zio.stm.STM
import zio.stm.TArray
import zio.stm.TMap
import zio.IO
import zio.UIO
import repository.InMemoryBoardRepository.PieceAt

import java.util.UUID

/** Repository for managing a board of pieces. Provides methods to place, move, and remove pieces, as well as retrieve
  * the current state of the board.
  */
trait BoardRepository {

  /** Places a piece at a specific position on the board.
    *
    * @param piece
    *   The piece to place on the board.
    * @param position
    *   The position on the board where the piece should be placed.
    * @return
    *   A unique identifier (UUID) for the placed piece. Returns an error message if the placement fails.
    */
  def place(piece: Piece, position: Position): IO[String, UUID]

  /** Moves a piece to a new position on the board.
    *
    * @param id
    *   The unique identifier (UUID) of the piece to move.
    * @param to
    *   The target position to move the piece to.
    * @return
    *   Unit on success. Returns an error message if the move fails (e.g., invalid move or occupied position).
    */
  def move(id: UUID, to: Position): IO[String, Unit]

  /** Removes a piece from the board.
    *
    * @param id
    *   The unique identifier (UUID) of the piece to remove.
    * @return
    *   Unit on success. Returns an error message if the piece is not found or removal fails.
    */
  def remove(id: UUID): IO[String, Unit]

  /** Retrieves the current state of the board.
    *
    * @return
    *   A map where the key is the UUID of each piece, and the value is a tuple containing the piece and its position.
    */
  def getState: UIO[Map[UUID, PieceAt]]
}

final class InMemoryBoardRepository private (
  pieces: TMap[UUID, PieceAt],
  board: TArray[Option[UUID]],
  removedPieces: TMap[UUID, PieceAt]
) extends BoardRepository {

  @inline
  private def index(pos: Position): Int =
    pos.y * InMemoryBoardRepository.size + pos.x

  private def validatePosition(pos: Position): STM[String, Unit] =
    STM.cond(
      pos.x >= 0 && pos.x < InMemoryBoardRepository.size &&
        pos.y >= 0 && pos.y < InMemoryBoardRepository.size,
      (),
      s"Invalid position $pos"
    )

  private def validatePositionEmpty(pos: Position): STM[String, Unit] =
    for {
      cell <- board.apply(index(pos))
      _    <- STM.fail(s"Position already occupied $pos").when(cell.isDefined)
    } yield ()

  private def markPosition(pos: Position, id: Option[UUID]): STM[Nothing, Unit] =
    board.update(index(pos), _ => id)

  private def findPiece(id: UUID): STM[String, PieceAt] =
    pieces.get(id).flatMap {
      case Some(data) => STM.succeed(data)
      case None       => STM.fail(s"Piece ID ($id) not found")
    }

  override def place(piece: Piece, position: Position): IO[String, UUID] =
    (for {
      _ <- validatePosition(position)
      _ <- validatePositionEmpty(position)
      id = UUID.randomUUID()
      alreadyRemoved <- removedPieces.values.map(_.map(_._1)).map(_.toSet)
      _ <- STM.fail(s"Piece $piece was removed and cannot be placed again").when(alreadyRemoved.contains(piece))
      _ <- pieces.put(id, (piece, position))
      _ <- markPosition(position, Some(id))
    } yield id).commit

  override def move(id: UUID, to: Position): IO[String, Unit] = {
    def validateVisitedPositions(visited: Set[Position]): STM[String, Unit] =
      STM
        .foreach(visited) { pos =>
          for {
            _    <- validatePosition(pos)
            cell <- board.apply(index(pos))
            _    <- STM.fail(s"Path blocked at $pos").when(cell.isDefined)
          } yield ()
        }
        .unit

    (for {
      _             <- validatePosition(to)
      (piece, from) <- findPiece(id)
      _             <- validatePositionEmpty(to)
      visited       <- STM.fromEither(piece.move(from, to).toEither.left.map(_.mkString(", ")))
      _             <- validateVisitedPositions(visited)
      _             <- pieces.put(id, (piece, to))
      _             <- markPosition(from, None)
      _             <- markPosition(to, Some(id))
    } yield ()).commit
  }

  override def remove(id: UUID): IO[String, Unit] =
    (for {
      pieceWithPos <- findPiece(id)
      _            <- pieces.delete(id)
      _            <- removedPieces.put(id, pieceWithPos)
      _            <- markPosition(pieceWithPos._2, None)
    } yield ()).commit

  override def getState: UIO[Map[UUID, PieceAt]] =
    pieces.toMap.commit
}

object InMemoryBoardRepository {
  type PieceAt = (Piece, Position)

  val size: Int = 8 // TODO Consider extracting this to a parameter for a customizable board size
  def make: UIO[BoardRepository] =
    for {
      pieces        <- TMap.empty[UUID, PieceAt].commit
      board         <- TArray.fromIterable(Vector.fill(size * size)(Option.empty[UUID])).commit
      removedPieces <- TMap.empty[UUID, PieceAt].commit
    } yield new InMemoryBoardRepository(pieces, board, removedPieces)
}
