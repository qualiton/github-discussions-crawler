package org.qualiton.crawler
package server.main

import java.time.Instant

import cats.effect.{ ExitCode, IO, IOApp }
import cats.implicits.toFunctorOps
import cats.instances.option._
import cats.syntax.traverse._
import fs2.Stream

import com.typesafe.scalalogging.LazyLogging
import eu.timepit.refined.auto._
import eu.timepit.refined.types.string.NonEmptyString
import org.http4s.client.blaze.BlazeClientBuilder

import org.qualiton.crawler.common.concurrent.CachedExecutionContext
import org.qualiton.crawler.server.config.DefaultConfigLoader
import org.qualiton.slack.SlackApiHttp4sClient
import org.qualiton.slack.models.ChatMessage
import org.qualiton.slack.rtm.slack.models.SlackEvent
import org.qualiton.slack.rtm.SlackRtmClient.Message

object Main extends IOApp {

  implicit val ec = CachedExecutionContext.default

  override def run(args: List[String]): IO[ExitCode] =
    Server.fromConfig(IO(DefaultConfigLoader.loadOrThrow())).compile.drain.as(ExitCode.Success)

}

//object Main2 extends App with LazyLogging {
//
//  implicit val system = ActorSystem("slack")
//  implicit val ec = system.dispatcher
//
//  val client = SlackRtmClient(Token.token)
//  val selfId = client.state.self.id
//
//  val apiClient = BlockingSlackApiClient(token)
//
//  client.onEvent { event =>
//    logger.info(event.toString)
//  }
//
//  client.onMessage { message =>
//    val mentionedIds = SlackUtil.extractMentionedIds(message.text)
//
//    logger.info(message.toString)
//    if (mentionedIds.contains(selfId)) {
//      val info = apiClient.getUserInfo(message.user)
//
//      val attachments =
//        Some(List(Attachment(
//          color = Some("green"),
//          author_name = Some("@lachatak"),
//          author_icon = Some("https://avatars0.githubusercontent.com/u/5830214?v=4"),
//          title = Some("New test discussion"),
//          title_link = Some("https://github.com/orgs/ovotech/teams/boost/discussions/28"),
//          fields = Some(List(
//            AttachmentField("Team", "VIBE", true),
//            AttachmentField("Comments", "3", true),
//            AttachmentField("Targeted", "@lachatak", false))))))
//
//      logger.warn(s"info ${ info.id }")
//      val directChannelId = apiClient.openIm(info.id)
//      logger.warn(s"directChannelId $directChannelId")
//      apiClient.postChatMessage(
//        channelId = directChannelId,
//        text = "New discussion has been discovered",
//        attachments = attachments)
//      apiClient.closeIm(directChannelId)
//
//      client.state.getChannelIdForName("dg-test")
//        .foreach(channelId =>
//          apiClient.postChatMessage(
//            channelId = channelId,
//            text = "New discussion has been discovered",
//            attachments = attachments))
//      ()
//    }
//  }
//}

object Main3 extends IOApp with LazyLogging {
  override def run(args: List[String]): IO[ExitCode] = {

    import scala.concurrent.duration._

    import cats.effect.ExitCode
    import fs2.Stream
    implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

    val program: Stream[IO, Option[Instant]] = for {
      client <- BlazeClientBuilder[IO](ec).withRequestTimeout(5.seconds).stream
      slackApiClient <- SlackApiHttp4sClient.stream(client, Token.token)
      //      r <- Stream.eval(slackApiClient.findUserById("U9DTBCSBC"))
      maybeUser <- Stream.eval(slackApiClient.findUserByName("krisztian.lachata"))
      //      state <- Stream.eval(slackApiClient.startRealTimeMessageSession)
      //      _ <- Stream.eval(slackApiClient.setUserPresence("active"))
      //      maybeUser <- Stream.eval(slackApiClient.findChannelByName("dg-test"))
      //      maybeChannelId <- Stream.eval(maybeUser.flatTraverse( u => slackApiClient.openIm(u.id)))
      //      _ <- Stream.eval(IO(logger.warn(s"state: $state")))
      ts <- Stream.eval(maybeUser.traverse(c => slackApiClient.postChatMessage(c.id, ChatMessage("Text"))))
      //      ts <- Stream.eval(maybeChannelId.traverse(c => slackApiClient.postChatMessage(ChatMessage(c, "Text", List.empty))))
      //      _ <- Stream.eval(maybeChannelId.traverse(c => slackApiClient.closeIm(c)))
    } yield ts

    program
      .evalMap(l => IO(logger.info(s"result : ${ l }")))
      .handleErrorWith(e => Stream.eval(IO(logger.error(e.toString))))
      .compile.drain.as(ExitCode.Success)
  }
}

object Main4 extends IOApp with LazyLogging {
  override def run(args: List[String]): IO[ExitCode] = {

    implicit val ec = CachedExecutionContext.default

    val pipe: fs2.Pipe[IO, SlackEvent, Message] = _.evalMap(e => IO(logger.info(s"log: ${ e.toString }"))) >> Stream.empty

    val program = for {
      slackRtmClient <- org.qualiton.slack.rtm.SlackRtmClient.stream[IO](Token.token)
      //      event <- slackRtmClient.connect
      event <- slackRtmClient.through(pipe)
    } yield event

    program
      //        .through(pipe)
      //      .evalTap(e => IO(logger.info(s"log: ${ e.toString }")))
      .handleErrorWith(e => Stream.eval(IO(logger.error(e.toString))))
      .compile.drain.map(_ => ExitCode.Success)
  }
}

object Token {
  val token: NonEmptyString = "..."
}
