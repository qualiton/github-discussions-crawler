package org.qualiton.crawler.application

import cats.data.EitherT
import cats.effect.{Effect, Sync}
import cats.instances.option._
import cats.syntax.traverse._
import fs2.Stream
import fs2.concurrent.Queue
import org.qualiton.crawler.domain.core.Event
import org.qualiton.crawler.domain.git.{EventGenerator, GithubClient, GithubRepository}

class GithubDiscussionHandler[F[_] : Effect] private(eventQueue: Queue[F, Event],
                                                     githubClient: GithubClient[F],
                                                     githubRepository: GithubRepository[F]) {

  def synchronizeDiscussions(): Stream[F, Either[Throwable, Unit]] = {
    val program: EitherT[Stream[F, ?], Throwable, Unit] = for {
      lastUpdatedAt <- EitherT.liftF(Stream.eval(githubRepository.findLastUpdatedAt))
      currentDiscussionDetails <- EitherT(githubClient.getTeamDiscussionsUpdatedAfter(lastUpdatedAt))
      maybePreviousDiscussionDetails <- EitherT(Stream.eval(githubRepository.find(currentDiscussionDetails.team.id, currentDiscussionDetails.discussion.id)))
      event <- EitherT.liftF(Stream.eval(EventGenerator.generateEvent(maybePreviousDiscussionDetails, currentDiscussionDetails)))
      _ <- EitherT.liftF(Stream.eval(githubRepository.save(currentDiscussionDetails)))
      _ <- EitherT.liftF(Stream.eval(event.traverse(eventQueue.enqueue1)))
    } yield ()

    program.value
  }
}

object GithubDiscussionHandler {

  def stream[F[_] : Effect](eventQueue: Queue[F, Event],
                            githubClient: GithubClient[F],
                            githubRepository: GithubRepository[F]): Stream[F, GithubDiscussionHandler[F]] =
    Stream.eval(Sync[F].delay(new GithubDiscussionHandler(eventQueue, githubClient, githubRepository)))

}
