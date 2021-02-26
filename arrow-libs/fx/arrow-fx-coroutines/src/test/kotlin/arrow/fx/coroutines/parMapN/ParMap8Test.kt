package arrow.fx.coroutines.parMapN

import arrow.core.Either
import arrow.core.Tuple8
import arrow.fx.coroutines.ArrowFxSpec
import arrow.fx.coroutines.Atomic
import arrow.fx.coroutines.ExitCase
import arrow.fx.coroutines.NamedThreadFactory
import arrow.fx.coroutines.Resource
import arrow.fx.coroutines.guaranteeCase
import arrow.fx.coroutines.leftException
import arrow.fx.coroutines.never
import arrow.fx.coroutines.parMapN
import arrow.fx.coroutines.single
import arrow.fx.coroutines.singleThreadName
import arrow.fx.coroutines.suspend
import arrow.fx.coroutines.threadName
import arrow.fx.coroutines.throwable
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.string
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class ParMap8Test : ArrowFxSpec(
  spec = {
    "parMapN 8 returns to original context" {
      val mapCtxName = "parMap8"
      val mapCtx = Resource.fromExecutor { Executors.newFixedThreadPool(8, NamedThreadFactory { mapCtxName }) }
      checkAll {
        single.zip(mapCtx).use { (_single, _mapCtx) ->
          withContext(_single) {
            threadName() shouldBe singleThreadName

            val (s1, s2, s3, s4, s5, s6, s7, s8) = parMapN(
              _mapCtx, threadName, threadName, threadName, threadName, threadName, threadName, threadName, threadName
            ) { a, b, c, d, e, f, g, h ->
              Tuple8(a, b, c, d, e, f, g, h)
            }

            s1 shouldBe mapCtxName
            s2 shouldBe mapCtxName
            s3 shouldBe mapCtxName
            s4 shouldBe mapCtxName
            s5 shouldBe mapCtxName
            s6 shouldBe mapCtxName
            s7 shouldBe mapCtxName
            s8 shouldBe mapCtxName
            threadName() shouldBe singleThreadName
          }
        }
      }
    }

    "parMapN 8 returns to original context on failure" {
      val mapCtxName = "parMap8"
      val mapCtx = Resource.fromExecutor { Executors.newFixedThreadPool(8, NamedThreadFactory { mapCtxName }) }

      checkAll(Arb.int(1..8), Arb.throwable()) { choose, e ->
        single.zip(mapCtx).use { (_single, _mapCtx) ->
          withContext(_single) {
            threadName() shouldBe singleThreadName

            Either.catch {
              when (choose) {
                1 -> parMapN(
                  _mapCtx,
                  suspend { e.suspend() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() }
                ) { _, _, _, _, _, _, _, _ -> Unit }
                2 -> parMapN(
                  _mapCtx,
                  suspend { never<Nothing>() },
                  suspend { e.suspend() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() }
                ) { _, _, _, _, _, _, _, _ -> Unit }
                3 -> parMapN(
                  _mapCtx,
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { e.suspend() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() }
                ) { _, _, _, _, _, _, _ -> Unit }
                4 -> parMapN(
                  _mapCtx,
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { e.suspend() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() }
                ) { _, _, _, _, _, _, _, _ -> Unit }
                5 -> parMapN(
                  _mapCtx,
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { e.suspend() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() }
                ) { _, _, _, _, _, _, _, _ -> Unit }
                6 -> parMapN(
                  _mapCtx,
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { e.suspend() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() }
                ) { _, _, _, _, _, _, _, _ -> Unit }
                7 -> parMapN(
                  _mapCtx,
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { e.suspend() },
                  suspend { never<Nothing>() }
                ) { _, _, _, _, _, _, _, _ -> Unit }
                else -> parMapN(
                  _mapCtx,
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { never<Nothing>() },
                  suspend { e.suspend() }
                ) { _, _, _, _, _, _, _, _ -> Unit }
              }
            } should leftException(e)
            threadName() shouldBe singleThreadName
          }
        }
      }
    }

    "parMapN 8 runs in parallel" {
      checkAll(Arb.int(), Arb.int(), Arb.int(), Arb.int(), Arb.int(), Arb.int(), Arb.int(), Arb.int()) { a, b, c, d, e, f, g, h ->
        val r = Atomic("")
        val modifyGate1 = CompletableDeferred<Unit>()
        val modifyGate2 = CompletableDeferred<Unit>()
        val modifyGate3 = CompletableDeferred<Unit>()
        val modifyGate4 = CompletableDeferred<Unit>()
        val modifyGate5 = CompletableDeferred<Unit>()
        val modifyGate6 = CompletableDeferred<Unit>()
        val modifyGate7 = CompletableDeferred<Unit>()

        parMapN(
          {
            modifyGate2.await()
            r.update { i -> "$i$a" }
          },
          {
            modifyGate3.await()
            r.update { i -> "$i$b" }
            modifyGate2.complete(Unit)
          },
          {
            modifyGate4.await()
            r.update { i -> "$i$c" }
            modifyGate3.complete(Unit)
          },
          {
            modifyGate5.await()
            r.update { i -> "$i$d" }
            modifyGate4.complete(Unit)
          },
          {
            modifyGate6.await()
            r.update { i -> "$i$e" }
            modifyGate5.complete(Unit)
          },
          {
            modifyGate7.await()
            r.update { i -> "$i$f" }
            modifyGate6.complete(Unit)
          },
          {
            modifyGate1.await()
            r.update { i -> "$i$g" }
            modifyGate7.complete(Unit)
          },
          {
            r.set("$h")
            modifyGate1.complete(Unit)
          }
        ) { _a, _b, _c, _d, _e, _f, _g, _h ->
          Tuple8(_a, _b, _c, _d, _e, _f, _g, _h)
        }

        r.get() shouldBe "$h$g$f$e$d$c$b$a"
      }
    }

    "parMapN 7 finishes on single thread" {
      checkAll(Arb.string()) {
        single.use { ctx ->
          parMapN(ctx, threadName, threadName, threadName, threadName, threadName, threadName, threadName, threadName) { a, b, c, d, e, f, g, h ->
            Tuple8(a, b, c, d, e, f, g, h)
          }
        } shouldBe Tuple8("single", "single", "single", "single", "single", "single", "single", "single")
      }
    }

    "Cancelling parMapN 8 cancels all participants" {
      checkAll(Arb.int(), Arb.int(), Arb.int(), Arb.int(), Arb.int(), Arb.int(), Arb.int(), Arb.int()) { a, b, c, d, e, f, g, h ->
        val s = Channel<Unit>()
        val pa = CompletableDeferred<Pair<Int, ExitCase>>()
        val pb = CompletableDeferred<Pair<Int, ExitCase>>()
        val pc = CompletableDeferred<Pair<Int, ExitCase>>()
        val pd = CompletableDeferred<Pair<Int, ExitCase>>()
        val pe = CompletableDeferred<Pair<Int, ExitCase>>()
        val pf = CompletableDeferred<Pair<Int, ExitCase>>()
        val pg = CompletableDeferred<Pair<Int, ExitCase>>()
        val ph = CompletableDeferred<Pair<Int, ExitCase>>()

        val loserA = suspend { guaranteeCase({ s.receive(); never<Int>() }) { ex -> pa.complete(Pair(a, ex)) } }
        val loserB = suspend { guaranteeCase({ s.receive(); never<Int>() }) { ex -> pb.complete(Pair(b, ex)) } }
        val loserC = suspend { guaranteeCase({ s.receive(); never<Int>() }) { ex -> pc.complete(Pair(c, ex)) } }
        val loserD = suspend { guaranteeCase({ s.receive(); never<Int>() }) { ex -> pd.complete(Pair(d, ex)) } }
        val loserE = suspend { guaranteeCase({ s.receive(); never<Int>() }) { ex -> pe.complete(Pair(e, ex)) } }
        val loserF = suspend { guaranteeCase({ s.receive(); never<Int>() }) { ex -> pf.complete(Pair(f, ex)) } }
        val loserG = suspend { guaranteeCase({ s.receive(); never<Int>() }) { ex -> pg.complete(Pair(g, ex)) } }
        val loserH = suspend { guaranteeCase({ s.receive(); never<Int>() }) { ex -> ph.complete(Pair(h, ex)) } }

        val fork = async {
          parMapN(loserA, loserB, loserC, loserD, loserE, loserF, loserG, loserH, ::Tuple8)
        }

        repeat(8) { s.send(Unit) } // Suspend until all racers started
        fork.cancel()

        pa.await().let { (res, exit) ->
          res shouldBe a
          exit.shouldBeInstanceOf<ExitCase.Cancelled>()
        }
        pb.await().let { (res, exit) ->
          res shouldBe b
          exit.shouldBeInstanceOf<ExitCase.Cancelled>()
        }
        pc.await().let { (res, exit) ->
          res shouldBe c
          exit.shouldBeInstanceOf<ExitCase.Cancelled>()
        }
        pd.await().let { (res, exit) ->
          res shouldBe d
          exit.shouldBeInstanceOf<ExitCase.Cancelled>()
        }
        pe.await().let { (res, exit) ->
          res shouldBe e
          exit.shouldBeInstanceOf<ExitCase.Cancelled>()
        }
        pf.await().let { (res, exit) ->
          res shouldBe f
          exit.shouldBeInstanceOf<ExitCase.Cancelled>()
        }
        pg.await().let { (res, exit) ->
          res shouldBe g
          exit.shouldBeInstanceOf<ExitCase.Cancelled>()
        }
        ph.await().let { (res, exit) ->
          res shouldBe h
          exit.shouldBeInstanceOf<ExitCase.Cancelled>()
        }
      }
    }

    "parMapN 8 cancels losers if a failure occurs in one of the tasks" {
      checkAll(
        Arb.throwable(),
        Arb.element(listOf(1, 2, 3, 4, 5, 6, 7, 8))
      ) { e, winningTask ->

        val intGen = Arb.int()
        val a = intGen.next()
        val b = intGen.next()
        val c = intGen.next()
        val d = intGen.next()
        val f = intGen.next()
        val g = intGen.next()
        val h = intGen.next()

        val s = Channel<Unit>()
        val pa = CompletableDeferred<Pair<Int, ExitCase>>()
        val pb = CompletableDeferred<Pair<Int, ExitCase>>()
        val pc = CompletableDeferred<Pair<Int, ExitCase>>()
        val pd = CompletableDeferred<Pair<Int, ExitCase>>()
        val pf = CompletableDeferred<Pair<Int, ExitCase>>()
        val pg = CompletableDeferred<Pair<Int, ExitCase>>()
        val ph = CompletableDeferred<Pair<Int, ExitCase>>()

        val winner = suspend { repeat(7) { s.send(Unit) }; throw e }
        val loserA = suspend { guaranteeCase({ s.receive(); never<Int>() }) { ex -> pa.complete(Pair(a, ex)) } }
        val loserB = suspend { guaranteeCase({ s.receive(); never<Int>() }) { ex -> pb.complete(Pair(b, ex)) } }
        val loserC = suspend { guaranteeCase({ s.receive(); never<Int>() }) { ex -> pc.complete(Pair(c, ex)) } }
        val loserD = suspend { guaranteeCase({ s.receive(); never<Int>() }) { ex -> pd.complete(Pair(d, ex)) } }
        val loserF = suspend { guaranteeCase({ s.receive(); never<Int>() }) { ex -> pf.complete(Pair(f, ex)) } }
        val loserG = suspend { guaranteeCase({ s.receive(); never<Int>() }) { ex -> pg.complete(Pair(g, ex)) } }
        val loserH = suspend { guaranteeCase({ s.receive(); never<Int>() }) { ex -> ph.complete(Pair(h, ex)) } }

        val r = Either.catch {
          when (winningTask) {
            1 -> parMapN(winner, loserA, loserB, loserC, loserD, loserF, loserG, loserH) { _, _, _, _, _, _, _, _ -> Unit }
            2 -> parMapN(loserA, winner, loserB, loserC, loserD, loserF, loserG, loserH) { _, _, _, _, _, _, _, _ -> Unit }
            3 -> parMapN(loserA, loserB, winner, loserC, loserD, loserF, loserG, loserH) { _, _, _, _, _, _, _, _ -> Unit }
            4 -> parMapN(loserA, loserB, loserC, winner, loserD, loserF, loserG, loserH) { _, _, _, _, _, _, _, _ -> Unit }
            5 -> parMapN(loserA, loserB, loserC, loserD, winner, loserF, loserG, loserH) { _, _, _, _, _, _, _, _ -> Unit }
            6 -> parMapN(loserA, loserB, loserC, loserD, loserF, winner, loserG, loserH) { _, _, _, _, _, _, _, _ -> Unit }
            7 -> parMapN(loserA, loserB, loserC, loserD, loserF, loserG, winner, loserH) { _, _, _, _, _, _, _, _ -> Unit }
            else -> parMapN(loserA, loserB, loserC, loserD, loserF, loserG, loserH, winner) { _, _, _, _, _, _, _, _ -> Unit }
          }
        }

        pa.await().let { (res, exit) ->
          res shouldBe a
          exit.shouldBeInstanceOf<ExitCase.Cancelled>()
        }
        pb.await().let { (res, exit) ->
          res shouldBe b
          exit.shouldBeInstanceOf<ExitCase.Cancelled>()
        }
        pc.await().let { (res, exit) ->
          res shouldBe c
          exit.shouldBeInstanceOf<ExitCase.Cancelled>()
        }
        pd.await().let { (res, exit) ->
          res shouldBe d
          exit.shouldBeInstanceOf<ExitCase.Cancelled>()
        }
        pf.await().let { (res, exit) ->
          res shouldBe f
          exit.shouldBeInstanceOf<ExitCase.Cancelled>()
        }
        pg.await().let { (res, exit) ->
          res shouldBe g
          exit.shouldBeInstanceOf<ExitCase.Cancelled>()
        }
        ph.await().let { (res, exit) ->
          res shouldBe h
          exit.shouldBeInstanceOf<ExitCase.Cancelled>()
        }
        r should leftException(e)
      }
    }
  }
)
