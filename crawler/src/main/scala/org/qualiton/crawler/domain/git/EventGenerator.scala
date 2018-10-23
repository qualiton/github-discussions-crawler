package org.qualiton.crawler.domain.git

import cats.effect.Sync
import org.qualiton.crawler.domain.core.Event

object EventGenerator {

  def generateEvent[F[_] : Sync](previousDiscussionDetails: Option[TeamDiscussionDetails], currentTeamDiscussionDetails: TeamDiscussionDetails): F[Event] = ???
}

//private def generateEvent(previousDiscussionDetails: Option[TeamDiscussionDetails], currentTeamDiscussionDetails: TeamDiscussionDetails): F[Event] = {
//
//  //  private val Addressee = "(@[0-9a-zA-Z]+)".r
//  //  private val Channel = """(#[a-z_\\-]+)""".r
//  //
//  //  import teamDiscussionComments._
//  //
//  //  val addressees = Addressee.findAllMatchIn(body).toList.map(_.group(1))
//  //  val channels = Channel.findAllMatchIn(body).toList.map(_.group(1))
//  def generateNewDiscussionCreatedEvent(currentTeamDiscussionDetails: TeamDiscussionDetails): NewDiscussionCreatedEvent =
//  NewDiscussionCreatedEvent(
//  author = currentTeamDiscussionDetails.discussion.author,
//  title = currentTeamDiscussionDetails.discussion.title,
//  url = currentTeamDiscussionDetails.discussion.url,
//  teamName = currentTeamDiscussionDetails.team.name,
//  addressee = List(),
//  createdAt = currentTeamDiscussionDetails.discussion.createdAt)
//
//  previousDiscussionDetails.fold(generateNewDiscussionCreatedEvent(currentTeamDiscussionDetails))
//}
