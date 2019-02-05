package org.qualiton.slack.models

case class DndStatus(
    dnd_enabled: Boolean,
    next_dnd_start_ts: Long,
    next_dnd_end_ts: Long)
