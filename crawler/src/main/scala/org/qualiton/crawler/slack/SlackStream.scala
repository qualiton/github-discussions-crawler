package org.qualiton.crawler.slack

import cats.effect.Sync
import fs2.Stream
import fs2.concurrent.Queue
import org.qualiton.crawler.common.config.SlackConfig
import org.qualiton.crawler.git.GithubRepository.Result

object SlackStream {

  def apply[F[_] : Sync](queue: Queue[F, Result],
                         slackConfig: SlackConfig): Stream[F, Unit] = {

    println(slackConfig)

    for {
      result <- queue.dequeue
      _ <- Stream.eval(Sync[F].delay(println(s"result: $result")))
    } yield ()
  }
}
