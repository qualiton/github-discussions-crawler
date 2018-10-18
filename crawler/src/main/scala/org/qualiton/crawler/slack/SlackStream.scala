package org.qualiton.crawler.slack

import cats.effect.ConcurrentEffect
import fs2.Stream
import fs2.concurrent.Queue
import org.http4s.client.blaze.BlazeClientBuilder
import org.qualiton.crawler.common.config.SlackConfig
import org.qualiton.crawler.git.GithubRepository.Result
import org.qualiton.crawler.slack.IncomingWebhookMessageAssembler.toIncomingWebhookMessage

import scala.concurrent.ExecutionContext

object SlackStream {

  def apply[F[_] : ConcurrentEffect](queue: Queue[F, Result],
                                     slackConfig: SlackConfig)
                                    (implicit ec: ExecutionContext): Stream[F, Unit] = {

    for {
      client <- BlazeClientBuilder[F](ec).withRequestTimeout(slackConfig.requestTimeout).stream
      slackClient <- SlackHttp4sClient.stream(client, slackConfig)
      result <- queue.dequeue
      _ <- Stream.eval(slackClient.sendIncomingWebhookMessage(toIncomingWebhookMessage(result)))
    } yield ()
  }
}
