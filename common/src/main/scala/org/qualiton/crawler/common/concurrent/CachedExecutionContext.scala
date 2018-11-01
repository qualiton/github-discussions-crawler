package org.qualiton.crawler.common.concurrent

import java.util.concurrent.{ Executors, ExecutorService }

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutorService }

object CachedExecutionContext {
  val executor: ExecutorService = Executors.newCachedThreadPool()
  implicit val default: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(executor)
}
