package org.qualiton.crawler.application

import cats.effect.{ Effect, Sync }
import cats.instances.option._
import cats.syntax.traverse._
import fs2.Stream
import fs2.concurrent.Queue

import com.typesafe.scalalogging.LazyLogging

import org.qualiton.crawler.domain.core.Event
import org.qualiton.crawler.domain.git.{ EventGenerator, GithubClient, GithubRepository }

class GithubDiscussionHandler[F[_] : Effect] private(
    eventQueue: Queue[F, Event],
    githubClient: GithubClient[F],
    githubRepository: GithubRepository[F]) extends LazyLogging {

  def synchronizeDiscussions(): Stream[F, Unit] =
    for {
      lastUpdatedAt <- Stream.eval(githubRepository.findLastUpdatedAt)
      _ <- Stream.eval(Sync[F].delay(logger.info(s"Synchronizing discussions updated after $lastUpdatedAt")))
      currentDiscussion <- githubClient.getTeamDiscussionsUpdatedAfter(lastUpdatedAt)
      maybePreviousDiscussion <- Stream.eval(githubRepository.find(currentDiscussion.teamId, currentDiscussion.discussionId))
      maybeEvent <- Stream.eval(EventGenerator.generateEvent(maybePreviousDiscussion, currentDiscussion))
      _ <- Stream.eval(githubRepository.save(currentDiscussion))
      _ <- Stream.eval(maybeEvent.traverse(eventQueue.enqueue1))
    } yield ()
}

object GithubDiscussionHandler {

  def stream[F[_] : Effect](
      eventQueue: Queue[F, Event],
      githubClient: GithubClient[F],
      githubRepository: GithubRepository[F]): Stream[F, GithubDiscussionHandler[F]] =
    Stream.eval(Sync[F].delay(new GithubDiscussionHandler(eventQueue, githubClient, githubRepository)))

}
