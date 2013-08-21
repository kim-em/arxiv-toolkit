package net.tqft.dht

import java.net.ServerSocket
import org.apache.commons.io.IOUtils
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import scala.collection.mutable.ListBuffer
import java.net.Socket
import java.net.InetAddress
import java.security.SecureRandom
import java.util.Date

sealed trait Message

case class Get(hash: Hash) extends Message {
  def reply(bucket: String, blob: Array[Byte]) = Put(hash, bucket, blob)
  def suggest(nodes: Map[Hash, (Address, Port)]) = Announce(nodes)
}
case class Put(hash: Hash, bucket: String, blob: Array[Byte]) extends Message
case class Announce(nodes: Map[Hash, (Address, Port)]) extends Message

trait WireFormat {
  def toBytes(m: Message): Array[Byte]
  def fromBytes(b: Array[Byte]): Message
}

case object SerializationWireFormat extends WireFormat {
  override def toBytes(m: Message) = {
    val baos = new ByteArrayOutputStream()
    val out = new ObjectOutputStream(baos)
    out.writeObject(m)
    val bytes = baos.toByteArray()
    baos.close
    out.close
    bytes
  }
  override def fromBytes(b: Array[Byte]) = {
    val bais = new ByteArrayInputStream(b)
    val in = new ObjectInputStream(bais)
    val result = in.readObject
    bais.close
    in.close
    result match {
      case result: Message => result
      case _ => ???
    }
  }
}

//case object PackedWireFormat extends WireFormat {
//  private def encodeAddress(a: Address) = {
//    a.length match {
//      case 4 => "ipv4".getBytes ++ a
//      case 16 => "ipv6".getBytes ++ a
//    }
//  }
//
//  override def toBytes(m: Message) = {
//    m match {
//      case m: Get => ("G" + m.hash).getBytes()
//      case m: Put => ("P" + m.hash + m.bucket.size + ":" + m.bucket + m.blob.length + ":").getBytes() ++ m.blob
//      case m: Announce => "A".getBytes ++ m.nodes.map({ case (hash, (address, port)) => hash.getBytes ++ encodeAddress(address) ++ BigInt(port).toByteArray }).flatten
//    }
//  }
//
//  override def fromBytes(b: Array[Byte]) = ???
//}

trait Node {
  def id: Hash
  def address: Address
  def port: Port

  final def response(m: Message): Option[Message] = m match {
    case get @ Get(hash) => {
      if (hash == id) {
        Some(Announce(Map(id -> (address, port))))
      } else {
        responseGet(get) match {
          case Some((bucket, blob)) => Some(get.reply(bucket, blob))
          case None => {
            val nodes = suggestAddressesFor(hash)
            if (nodes.isEmpty) {
              None
            } else {
              Some(Announce(nodes.toMap))
            }
          }
        }
      }
    }
    case put: Put => {
      processPut(put)
      None
    }
    case announce: Announce => {
      processAnnounce(announce)
      None
    }
  }

  def processPut(put: Put)
  def processAnnounce(announce: Announce)
  def responseGet(get: Get): Option[(String, Array[Byte])]

  def suggestNodesFor(hash: Hash): List[Hash]
  def addressFor(hash: Hash): Option[(Address, Port)]
  def addressesFor(hashes: List[Hash]) = (for (h <- hashes; (a, p) <- addressFor(h)) yield (h -> (a, p))).toMap
  def suggestAddressesFor(hash: Hash) = addressesFor(suggestNodesFor(hash))

  def nodeStreamFor(hash: Hash) = {
    lazy val nodes: Stream[Hash] = Stream.from(0).map(k => suggestNodesFor(hash).filterNot(nodes.take(k).contains).headOption).takeWhile(_.nonEmpty).map(_.get);
    nodes
  }

  val K = 8

  def sendGet(get: Get) = {
    val sentTo = ListBuffer[Hash]()
    var result: Option[(String, Array[Byte])] = None
    (for (hash <- nodeStreamFor(get.hash).take(K)) yield {
      sendMessage(get, hash) match {
        case Some(Put(hash, bucket, blob)) if hash == get.hash => Some((bucket, blob))
        case Some(announce: Announce) => {
          processAnnounce(announce)
          None
        }
        case _ => None
      }
    }).flatten.headOption
  }
  def sendPut(put: Put) = for (hash <- suggestNodesFor(put.hash)) sendMessage(put, hash)

  def sendMessage(m: Message, hash: Hash): Option[Message]
  def reportTransportFailure(hash: Hash)
}

trait RoutingTable { node: Node =>
  val bigId = BigInt(node.id)
  
  case class Bucket(val min: BigInt, val max: BigInt, val data: scala.collection.mutable.Map[Hash, (Address, Port)], var lastModified: Long) {
    def split: List[Bucket] = {
      require(min <= bigId && bigId < max)
      ???
    }
  }
  val buckets = scala.collection.mutable.ListBuffer[Bucket](Bucket(BigInt(0), BigInt(1) << 120, scala.collection.mutable.Map.empty, new Date().getTime()))
  
  override def processAnnounce(announce: Announce) {
    ???
  }
  override def reportTransportFailure(hash: Hash) = ???
  override def addressFor(hash: Hash) = ???
  override def suggestNodesFor(hash: Hash) = ???
}

trait DataStore { node: Node =>
  val store = scala.collection.mutable.Map[String, (String, Array[Byte])]()

  override def processPut(put: Put) {
    store += ((put.hash, (put.bucket, put.blob)))
  }
  override def responseGet(get: Get) = {
    store.get(get.hash)
  }
}

trait TCPMessageTransport { node: Node =>
  val wf = SerializationWireFormat

  override def sendMessage(m: Message, hash: Hash): Option[Message] = {
    val (address, port) = addressFor(hash).get
    try {
      val socket = new Socket(InetAddress.getByAddress(address), port)
      IOUtils.write(wf.toBytes(m), socket.getOutputStream())
      val bytes = IOUtils.toByteArray(socket.getInputStream())
      if (bytes.length == 0) {
        None
      } else {
        Some(wf.fromBytes(bytes))
      }
    } catch {
      case e: Exception => {
        println(e)
        reportTransportFailure(hash)
        None
      }
    } finally {
    }
  }

  val serverSocket = new ServerSocket(node.port)

  while (true) {
    val connection = serverSocket.accept()
    val bytes = IOUtils.toByteArray(connection.getInputStream())
    node.response(wf.fromBytes(bytes)) match {
      case Some(reply) => {
        IOUtils.write(wf.toBytes(reply), connection.getOutputStream())
      }
      case None => {}
    }
    connection.close()
  }

}

object LookupIPAddress {
  def apply() = {
    val s = new Socket("google.com", 80);
    val bytes = s.getLocalAddress().getAddress()
    s.close
    bytes
  }
}

object RandomHash {
  val random = new SecureRandom()

  def apply(): Hash = {
    val bytes = Array[Byte](20)
    random.nextBytes(bytes)
    new String(bytes)
  }
}

class DistributedHashTableClient(override val id: Hash = RandomHash(), override val address: Address = LookupIPAddress(), override val port: Int = 6973) extends Node with RoutingTable with DataStore with TCPMessageTransport

class NodeMap(node: Node) extends scala.collection.mutable.Map[String, (String, Array[Byte])] {
  def -=(key: String): this.type = throw new UnsupportedOperationException
  def +=(kv: (String, (String, Array[Byte]))): this.type = {
    node.sendPut(Put(kv._1, kv._2._1, kv._2._2))
    this
  }
  def get(key: String): Option[(String, Array[Byte])] = {
    node.sendGet(Get(key))
  }
  def iterator: Iterator[(String, (String, Array[Byte]))] = throw new UnsupportedOperationException
}
