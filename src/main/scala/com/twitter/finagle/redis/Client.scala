package com.twitter.finagle.redis

import scala.collection.JavaConversions._
import org.jboss.netty.buffer.ChannelBuffer
import com.twitter.finagle.builder.{ClientBuilder, ClientConfig}
import com.twitter.finagle.Service
import com.twitter.util.{Time, Future}
import com.twitter.finagle.redis.protocol._


// object Client {
//   /**
//    * Construct a client from a single host.
//    *
//    * @param host a String of host:port combination.
//    */
//   def apply(host: String): Client = apply(
//     ClientBuilder()
//       .hosts(host)
//       .hostConnectionLimit(1)
//       .codec(new Redis)
//       .build()
//   )

//   def apply(service: Service[Command, Reply]) = {
//     new ConnectedClient(service)
//   }
// }

// trait Client {
//   def send(name: Command.Name, args: Array[Command.Argument]): Future[Reply]
// }

// protected class ConnectedClient(service: Service[Command, Reply]) extends Client {
//   def send(name: Command.Name, args: Array[Command.Argument]) = {
//     service(Command(name, args))
//   }
// }

