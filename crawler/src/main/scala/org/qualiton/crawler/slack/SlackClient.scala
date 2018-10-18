package org.qualiton.crawler.slack

import enumeratum.{CirceEnum, Enum, EnumEntry}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.string.{MatchesRegex, Uri}
import org.http4s.Status
import org.qualiton.crawler.slack.SlackClient.IncomingWebhookMessage
import shapeless.{Witness => W}

trait SlackClient[F[_]] {

  def sendIncomingWebhookMessage(message: IncomingWebhookMessage): F[Status]

}

object SlackClient {

  type ColorCode = String Refined (MatchesRegex[W.`"#[0-9](6)"`.T])

  sealed abstract class Color(val code: String) extends EnumEntry

  object Color extends Enum[Color] with CirceEnum[Color] {

    case object Good extends Color("good")

    case object Warning extends Color("warning")

    case object Danger extends Color("danger")

    case class Custom(codeCode: ColorCode) extends Color(codeCode)

    val values = findValues
  }

  final case class Field(title: String,
                         value: String,
                         short: Boolean)

  final case class Attachment(color: Color,
                              title: String,
                              title_link: String Refined Uri,
                              text: String,
                              fields: List[Field],
                              ts: Long)

  final case class IncomingWebhookMessage(attachments: List[Attachment])

}


