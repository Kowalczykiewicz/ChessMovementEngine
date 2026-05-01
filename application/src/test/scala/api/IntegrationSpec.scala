package api

import api.dto.PlacePieceRequest.codec
import api.dto._
import model.Piece.Rook
import model._
import zio._
import zio.http._
import zio.json.EncoderOps
import zio.json._
import zio.test.Assertion.equalTo
import zio.test._

import java.util.UUID

object IntegrationSpec extends ZIOSpecDefault {

  def spec: Spec[TestEnvironment with Scope, Any] = suite("IntegrationSpec")(
    suite("Place piece")(
      test("should place a piece - happy path") {
        val piece    = Rook
        val position = Position(0, 0)

        for {
          app <- BoardHttpApp.app
          req = Request.post(
            url = URL.root / "pieces",
            body = Body.fromString(PlacePieceRequest(piece, position).toJson)
          )
          res <- app.runZIO(req)
        } yield assertTrue(res.status == Status.Ok)
      },
      test("should fail to place a piece when request body is incorrect") {
        for {
          app <- BoardHttpApp.app
          req = Request.post(URL.root / "pieces", body = Body.fromString("alamakota"))
          res <- app.runZIO(req)
        } yield assertTrue(res.status == Status.BadRequest)
      },
      test("should fail to place a piece that was removed") {
        val piece     = Rook
        val position1 = Position(0, 0)
        val position2 = Position(1, 1)

        for {
          app <- BoardHttpApp.app
          placeReq = Request.post(
            url = URL.root / "pieces",
            body = Body.fromString(PlacePieceRequest(piece, position1).toJson)
          )
          placeRes  <- app.runZIO(placeReq)
          placeBody <- placeRes.body.asString
          placed    <- ZIO.fromEither(placeBody.fromJson[PlacePieceResponse])
          _         <- app.runZIO(Request.delete(URL.root / "pieces" / placed.id.toString))
          secondPlaceReq = Request.post(
            url = URL.root / "pieces",
            body = Body.fromString(PlacePieceRequest(piece, position2).toJson)
          )
          secondPlaceRes <- app.runZIO(secondPlaceReq)
        } yield assertTrue(secondPlaceRes.status == Status.BadRequest)
      }
    ),
    suite("Move piece")(
      test("should move a piece - happy path") {
        val piece           = Rook
        val initialPosition = Position(0, 0)
        val newPosition     = Position(0, 5)

        for {
          app <- BoardHttpApp.app
          placeReq = Request.post(
            url = URL.root / "pieces",
            body = Body.fromString(PlacePieceRequest(piece, initialPosition).toJson)
          )
          placeRes  <- app.runZIO(placeReq)
          placeBody <- placeRes.body.asString
          placed    <- ZIO.fromEither(placeBody.fromJson[PlacePieceResponse])
          moveReq = Request.put(
            url = URL.root / "pieces" / placed.id.toString / "move",
            body = Body.fromString(newPosition.toJson)
          )
          moveRes <- app.runZIO(moveReq)
        } yield assertTrue(moveRes.status == Status.Ok)
      },
      test("should fail to move a piece when request body is incorrect") {
        val fakeId = UUID.randomUUID()

        for {
          app <- BoardHttpApp.app
          req = Request.put(URL.root / "pieces" / fakeId.toString / "move", Body.fromString("alamakota"))
          res <- app.runZIO(req)
        } yield assertTrue(res.status == Status.BadRequest)
      },
      test("should fail to move a piece to an invalid position") {
        val piece           = Rook
        val position        = Position(0, 0)
        val invalidPosition = Position(10, 10)

        for {
          app <- BoardHttpApp.app
          req1 = Request.post(
            url = URL.root / "pieces",
            body = Body.fromString(PlacePieceRequest(piece, position).toJson)
          )
          res1   <- app.runZIO(req1)
          body1  <- res1.body.asString
          placed <- ZIO.fromEither(body1.fromJson[PlacePieceResponse])
          req2 = Request.put(
            url = URL.root / "pieces" / placed.id.toString / "move",
            body = Body.fromString(invalidPosition.toJson)
          )
          res2 <- app.runZIO(req2)
        } yield assertTrue(res2.status == Status.BadRequest)
      },
      test("should fail to move a piece to an occupied position") {
        val piece     = Rook
        val position1 = Position(0, 0)
        val position2 = Position(0, 1)

        for {
          app <- BoardHttpApp.app
          req1 = Request.post(
            url = URL.root / "pieces",
            body = Body.fromString(PlacePieceRequest(piece, position1).toJson)
          )
          res1    <- app.runZIO(req1)
          body1   <- res1.body.asString
          placed1 <- ZIO.fromEither(body1.fromJson[PlacePieceResponse])
          req2 = Request.post(
            url = URL.root / "pieces",
            body = Body.fromString(PlacePieceRequest(Rook, position2).toJson)
          )
          _ <- app.runZIO(req2)
          req3 = Request.put(
            url = URL.root / "pieces" / placed1.id.toString / "move",
            body = Body.fromString(position2.toJson)
          )
          res3 <- app.runZIO(req3)
        } yield assertTrue(res3.status == Status.BadRequest)
      },
      test("should fail to move a non-existing piece") {
        val invalidId   = UUID.randomUUID()
        val newPosition = Position(5, 5)

        for {
          app <- BoardHttpApp.app
          req = Request.put(
            url = URL.root / "pieces" / invalidId.toString / "move",
            body = Body.fromString(newPosition.toJson)
          )
          res <- app.runZIO(req)
        } yield assertTrue(res.status == Status.BadRequest)
      },
      test("should fail to move a piece if path is blocked") {
        val piece             = Rook
        val position1         = Position(0, 0)
        val position2         = Position(0, 5)
        val blockingPosition2 = Position(0, 3)

        for {
          app <- BoardHttpApp.app
          req1 = Request.post(
            url = URL.root / "pieces",
            body = Body.fromString(PlacePieceRequest(piece, position1).toJson)
          )
          res1    <- app.runZIO(req1)
          body1   <- res1.body.asString
          placed1 <- ZIO.fromEither(body1.fromJson[PlacePieceResponse])
          req3 = Request.post(
            url = URL.root / "pieces",
            body = Body.fromString(PlacePieceRequest(Rook, blockingPosition2).toJson)
          )
          _ <- app.runZIO(req3)
          reqMove = Request.put(
            url = URL.root / "pieces" / placed1.id.toString / "move",
            body = Body.fromString(position2.toJson)
          )
          resMove <- app.runZIO(reqMove)
        } yield assertTrue(resMove.status == Status.BadRequest)
      }
    ),
    suite("Remove piece")(
      test("should remove an existing piece - happy path") {
        val piece    = Rook
        val position = Position(0, 0)

        for {
          app <- BoardHttpApp.app
          req1 = Request.post(
            url = URL.root / "pieces",
            body = Body.fromString(PlacePieceRequest(piece, position).toJson)
          )
          res1   <- app.runZIO(req1)
          body1  <- res1.body.asString
          placed <- ZIO.fromEither(body1.fromJson[PlacePieceResponse])
          res2   <- app.runZIO(Request.delete(URL.root / "pieces" / placed.id.toString))
        } yield assertTrue(res2.status == Status.Ok)
      },
      test("should fail to remove a non-existing piece") {
        val id = UUID.randomUUID()

        for {
          httpApp <- BoardHttpApp.app
          req = Request.delete(URL.root / "pieces" / id.toString)
          res <- httpApp.runZIO(req)
        } yield assert(res.status)(equalTo(Status.BadRequest))
      }
    )
  )
}
