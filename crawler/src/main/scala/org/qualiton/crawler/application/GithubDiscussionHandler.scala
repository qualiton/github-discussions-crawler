package org.qualiton.crawler.application

import java.time.Instant
import java.time.temporal.ChronoUnit

import cats.data.EitherT
import cats.effect.{Effect, Sync}
import fs2.Stream
import fs2.concurrent.Queue
import org.qualiton.crawler.domain.core.Event
import org.qualiton.crawler.domain.git.{GithubClient, GithubRepository, TeamDiscussionDetails}

class GithubDiscussionHandler[F[_] : Effect] private(eventQueue: Queue[F, Event], githubClient: GithubClient[F], githubRepository: GithubRepository[F]) {

  def synchronizeDiscussions(): Stream[F, Either[Throwable, Unit]] = {
    val program: EitherT[Stream[F, ?], Throwable, Unit] = for {
      maybeLastUpdatedAt <- EitherT.liftF(Stream.eval(githubRepository.findLastUpdatedAt))
      lastUpdatedAt = maybeLastUpdatedAt.getOrElse(Instant.now().minus(1, ChronoUnit.DECADES))
      currentDiscussionDetails <- EitherT(githubClient.getTeamDiscussionsUpdatedAfter(lastUpdatedAt))
      maybePreviousDiscussionDetails <- EitherT(Stream.eval(githubRepository.find(currentDiscussionDetails.team.id, currentDiscussionDetails.discussion.id)))
      event <- EitherT.liftF(Stream.eval(generateEvent(maybePreviousDiscussionDetails, currentDiscussionDetails)))
      _ <- EitherT.liftF(Stream.eval(githubRepository.save(currentDiscussionDetails)))
      _ <- EitherT.liftF(Stream.eval(eventQueue.enqueue1(event)))
    } yield ()

    program.value
  }

  private def generateEvent(previousDiscussionDetails: Option[TeamDiscussionDetails], currentTeamDiscussionDetails: TeamDiscussionDetails): F[Event] = ???

  //    //TODO implement repository
  //
  //    //  private val Addressee = "(@[0-9a-zA-Z]+)".r
  //    //  private val Channel = """(#[a-z_\\-]+)""".r
  //    //
  //    //  import teamDiscussionComments._
  //    //
  //    //  val addressees = Addressee.findAllMatchIn(body).toList.map(_.group(1))
  //    //  val channels = Channel.findAllMatchIn(body).toList.map(_.group(1))
  //      def generateNewDiscussionCreatedEvent(currentTeamDiscussionDetails: TeamDiscussionDetails):NewDiscussionCreatedEvent =
  //        NewDiscussionCreatedEvent(
  //          author = currentTeamDiscussionDetails.discussion.author,
  //          title = currentTeamDiscussionDetails.discussion.title,
  //          url = currentTeamDiscussionDetails.discussion.url,
  //          teamName = currentTeamDiscussionDetails.team.name,
  //          addressee = List(),
  //          createdAt = currentTeamDiscussionDetails.discussion.createdAt)
  //
  //      previousDiscussionDetails.fold(generateNewDiscussionCreatedEvent(currentTeamDiscussionDetails))
  //    }
}

object GithubDiscussionHandler {

  def stream[F[_] : Effect](eventQueue: Queue[F, Event], githubClient: GithubClient[F], githubRepository: GithubRepository[F]): Stream[F, GithubDiscussionHandler[F]] =
    Stream.eval(Sync[F].delay(new GithubDiscussionHandler(eventQueue, githubClient, githubRepository)))

}
