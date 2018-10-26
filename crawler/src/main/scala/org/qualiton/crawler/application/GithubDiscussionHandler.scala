package org.qualiton.crawler.application

import cats.data.EitherT
import cats.effect.{ Effect, Sync }
import cats.instances.option._
import cats.syntax.traverse._
import fs2.Stream
import fs2.concurrent.Queue

import org.qualiton.crawler.domain.core.Event
import org.qualiton.crawler.domain.git.{ EventGenerator, GithubClient, GithubRepository }

class GithubDiscussionHandler[F[_] : Effect] private(
    eventQueue: Queue[F, Event],
    githubClient: GithubClient[F],
    githubRepository: GithubRepository[F]) {

  def synchronizeDiscussions(): Stream[F, Either[Throwable, Unit]] = {
    val program: EitherT[Stream[F, ?], Throwable, Unit] = for {
      lastUpdatedAt <- EitherT.liftF(Stream.eval(githubRepository.findLastUpdatedAt))
      currentDiscussion <- EitherT(githubClient.getTeamDiscussionsUpdatedAfter(lastUpdatedAt))
      maybePreviousDiscussion <- EitherT(Stream.eval(githubRepository.find(currentDiscussion.teamId, currentDiscussion.discussionId)))
      event <- EitherT.liftF(Stream.eval(EventGenerator.generateEvent(maybePreviousDiscussion, currentDiscussion)))
      _ <- EitherT.liftF(Stream.eval(githubRepository.save(currentDiscussion)))
      _ <- EitherT.liftF(Stream.eval(event.traverse(eventQueue.enqueue1)))
    } yield ()

    program.value
  }
}

object GithubDiscussionHandler {

  def stream[F[_] : Effect](
      eventQueue: Queue[F, Event],
      githubClient: GithubClient[F],
      githubRepository: GithubRepository[F]): Stream[F, GithubDiscussionHandler[F]] =
    Stream.eval(Sync[F].delay(new GithubDiscussionHandler(eventQueue, githubClient, githubRepository)))

}
