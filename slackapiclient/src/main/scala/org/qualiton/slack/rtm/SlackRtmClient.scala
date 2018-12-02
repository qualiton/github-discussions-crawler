package org.qualiton.slack.rtm


import cats.effect.Effect
import cats.syntax.functor._

import org.qualiton.slack.SlackApiClient

class SlackRtmClient[F[_] : Effect](slackApiClient: SlackApiClient[F]) {

  for {
    state <- slackApiClient.startRealTimeMessageSession
  } yield RtmState(state)

}
