package exercises.sideeffect

import java.time.Instant

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

object IOExercises {

  /////////////////////////
  // 1. Smart constructors
  /////////////////////////

  object IO {
    // 1a. Implement succeed a "smart constructor" that lift a pure value into an IO
    // such as succeed(x).unsafeRun() == x
    // note that succeed is strict, it means that the same value will be return every time it is run
    // it also means it is an unsafe to throw an Exception when you call succeed, e.g. succeed(throw new Exception(""))
    def succeed[A](value: A): IO[A] = ???

    // common alias for succeed
    def pure[A](value: A): IO[A] = succeed(value)

    // 1b. Implement fail a "smart constructor" that creates an IO which always fails
    // note that val error = fail(new Exception("")) does not throw
    // the Exception is only thrown when unsafeRun is called, e.g. error.unsafeRun()
    def fail[A](error: Throwable): IO[A] = ???

    // 1c. Write a test for fail in IOExercisesTest

    // 1d. What is the type of boom? Try to guess without using your IDE
    val boom = fail(new Exception("Boom!"))

    def fromTry[A](fa: Try[A]): IO[A] =
      fa.fold(fail, succeed)

    // 1e. Implement effect which captures a potentially side effecty operation
    // such as effect(4) == succeed(4)
    // and effect(throw new Exception("")) == fail(new Exception(""))
    // use case: effect(impureFunction(4))
    def effect[A](fa: => A): IO[A] = ???

    // common alias for effect
    def apply[A](fa: => A): IO[A] = effect(fa)

    def notImplemented[A]: IO[A] = effect(???)

    // 1f. Implement sleep, see Thread.sleep
    // What the issue with this implementation? How could you fix it?
    def sleep(duration: FiniteDuration): IO[Unit] = ???

    // 2h. Implement an IO that never completes
    // this should be the equivalent of sleep with an Infinite duration
    val forever: IO[Nothing] = notImplemented
  }

  /////////////////////
  // 2. IO API
  /////////////////////

  class IO[A](val unsafeRun: () => A) {

    // 2a. Implement map
    // such as succeed(x).map(f) == succeed(f(x))
    // and     fail(e).map(f) == fail(e)
    // note that f is a pure function, you should NOT use it to do another side effect e.g. succeed(4).map(println)
    def map[B](f: A => B): IO[B] = ???

    // 2b. Implement flatMap
    // such as succeed(x).flatMap(f) == f(x)
    // and     fail(e).flatMap(f) == fail(e)
    def flatMap[B](f: A => IO[B]): IO[B] = ???

    // 2c. Implement attempt which makes the error part of IO explicit
    // such as attempt(pure(x)) == pure(Right(x))
    //         attempt(fail(new Exception(""))) == pure(Left(new Exception("")))
    // note that attempt guarantee that unsafeRun() will not throw an Exception
    def attempt[B]: IO[Either[Throwable, A]] = ???

    // 2d. Implement retryOnce that takes an IO and if it fails, try to run it again
    def retryOnce: IO[A] = ???

    // 2e. Implement retryUntilSuccess
    // similar to retryOnce but it retries until the IO succeeds (potentially indefinitely)
    // sleep `waitBeforeRetry` between each retry
    def retryUntilSuccess(waitBeforeRetry: FiniteDuration): IO[A] = ???
  }

  ////////////////////
  // 3. Programs
  ////////////////////

  def unsafeReadLine: String =
    scala.io.StdIn.readLine()

  def unsafeWriteLine(message: String): Unit =
    println(message)

  // 3a. Implement readLine and writeLine such as they become referentially transparent
  // which smart constructor of IO should you use?
  val readLine: IO[String] = IO.notImplemented

  def writeLine(message: String): IO[Unit] = IO.notImplemented

  def unsafeConsoleProgram: String = {
    println("What's your name?")
    val name = scala.io.StdIn.readLine()
    println(s"Your name is $name")
    name
  }

  // 3b. Implement consoleProgram such as it is a referentially version of unsafeConsoleProgram
  // Try to re-use readLine and writeLine
  val consoleProgram: IO[String] = IO.notImplemented

  // 3c. Implement readInt which reads an Int from the command line
  // such as readInt.unsafeRun() == 32 if user types "32"
  // and readInt.unsafeRun() throws an Exception if user types "hello"
  // use parseInt and readLine
  def parseInt(x: String): Try[Int] = Try(x.toInt)

  val readInt: IO[Int] = IO.notImplemented

  // 3d. Implement userConsoleProgram such as it is a referentially version of unsafeUserConsoleProgram
  case class User(name: String, age: Int, createdAt: Instant)

  val readNow: IO[Instant] = IO.effect(Instant.now())

  val userConsoleProgram: IO[User] = IO.notImplemented

  def unsafeUserConsoleProgram: User = {
    println("What's your name?")
    val name = scala.io.StdIn.readLine()
    println("What's your age?")
    val age = scala.io.StdIn.readLine().toInt
    User(name, age, createdAt = Instant.now())
  }

  // 3e. How would you test userConsoleProgram?
  // what are the issues with the current implementation?

  ////////////////////////
  // 4. Testing
  ////////////////////////

  trait Clock {
    val readNow: IO[Instant]
  }

  val systemClock: Clock = new Clock {
    val readNow: IO[Instant] = IO.effect(Instant.now())
  }

  // 4a. Implement a testClock which facilitates testing
  def testClock: Clock = ???

  trait Console {
    val readLine: IO[String]
    def writeLine(message: String): IO[Unit]

    val readInt: IO[Int] = readLine.map(parseInt).flatMap(IO.fromTry)
  }

  val stdin: Console = new Console {
    val readLine: IO[String]                 = IO.effect(scala.io.StdIn.readLine())
    def writeLine(message: String): IO[Unit] = IO.effect(println(message))
  }

  // 4b. Implement a testConsole which facilitates testing
  // use both testClock and testConsole to write a test for userConsoleProgram2 in IOExercisesTest
  def testConsole(in: List[String], out: ListBuffer[String]): Console = ???

  def userConsoleProgram2(console: Console, clock: Clock): IO[User] =
    for {
      _         <- console.writeLine("What's your name?")
      name      <- console.readLine
      _         <- console.writeLine("What's your age?")
      age       <- console.readInt
      createdAt <- clock.readNow
    } yield User(name, age, createdAt)

  // 4c. Now our production code is "pure" (free of side effect) but not our test code
  // how would you fix this this?
  // try to implement safeTestConsole such as:
  // - it is a pure Console implementation
  // - it makes unit testing convenient
  def safeTestConsole: Console = ???

}
