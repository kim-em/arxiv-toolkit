//package net.tqft.dht
//
//// half-baked attempt at writing a Mainline DHT client.
//// c.f. http://bittorrent.org/beps/bep_0005.html
//// some discussion at http://forum.bittorrent.org/viewtopic.php?id=12&p=2
//// and the ipv6 analogue at http://www.pps.univ-paris-diderot.fr/~jch/software/bittorrent/bep-dht-ipv6.html
//
//object BEncode {
//  def apply(values: (String, BEncodable)*): String = (values.toMap: BEncodable).bencode
//}
//
//sealed trait BEncodable {
//  def bencode: String
//}
//
//case class BEncodableString(s: String) extends BEncodable {
//  override def bencode = s.size.toString + ":" + s
//}
//case class BEncodableInt(i: Int) extends BEncodable {
//  override def bencode = "i" + i.toString + "e"
//}
//case class BEncodableMap(m: Map[String, BEncodable]) extends BEncodable {
//  override def bencode = m.keys.toSeq.sorted.map(k => BEncodableString(k).bencode + m(k).bencode).mkString("d", "", "e")
//}
//case class BEncodableList(l: List[BEncodable]) extends BEncodable {
//  override def bencode = l.map(_.bencode).mkString("l", "", "e")
//}
//
//object BEncodable {
//  import language.implicitConversions
//
//  implicit def encodableString(s: String): BEncodable = BEncodableString(s)
//  implicit def encodableInt(i: Int): BEncodable = BEncodableInt(i)
//  implicit def encodableMap(m: Map[String, BEncodable]): BEncodable = BEncodableMap(m)
//  implicit def encodableList(l: List[BEncodable]): BEncodable = BEncodableList(l)
//
// import scala.util.parsing.combinator.RegexParsers
//
// object BDecoder extends RegexParsers {
//    import net.tqft.toolkit.Extractors.Int
//    override val skipWhitespace = false
//
//    private def integer = "-?[0-9]+".r ^? { case Int(i) => i }
//    private def encodedInteger = "i" ~> integer <~ "e" ^^ { i => new BEncodableInt(i) }
//    private def stringOfLength(r: Int) = (".{" + r + "}").r
//    private def encodedString = integer into { r => ":" ~> stringOfLength(r) ^^ { s => new BEncodableString(s) } }
//    private def encodedMap: Parser[BEncodableMap] = "d" ~> (encodedString ~ encoded).* <~ "e" ^^ { case list: List[BEncodableString ~ BEncodable] => new BEncodableMap(list.map({ case k ~ v => k.s -> v }).toMap) }
//    private def encodedList: Parser[BEncodableList] = "l" ~> encoded.* <~ "e" ^^ { case list => new BEncodableList(list) }
//    private def encoded: Parser[BEncodable] = encodedInteger | encodedString | encodedMap | encodedList
//
//    def apply(s: String) = {
//      parseAll(encoded, s) match {
//        case Success(e, _) => Some(e)
//        case _ => None
//      }
//    }
//
//  }
//
//  def unapply(bencoded: String) = BDecoder(bencoded)
//
//}
//
//sealed trait Message extends BEncodable {
//  def y: Char
//  def t: String
//}
//
//object Message {
//  def unapply(bencoded: String): Option[Message] = {
//    bencoded match {
//      case BEncodable(BEncodableMap(m)) => {
//        m.get("y").collect({ case BEncodableString(s) => s }) match {
//          case Some("q") => {
//            m.get("q").collect({ case BEncodableString(s) => s }) match {
//              case Some("ping") => {
//                ???
//              }
//              case Some("find_node") => {
//                ???
//              }
//              case Some("get_peers") => {
//                ???
//              }
//              case Some("peer_announce") => {
//                ???
//              }
//            }
//          }
//          case Some("r") => {
//            ???
//          }
//          case Some("e") => {
//            ???
//          }
//          case _ => None
//        }
//      }
//      case _ => None
//    }
//  }
//}
//
//case class IP(bytes: Array[Byte]) {
//  require(bytes.length == 4)
//  override def toString = bytes.map(_.toInt).mkString(".")
//}
//
//case class Peer(ip: IP, port: Int) extends BEncodable {
//  override def bencode = ???
//}
//case class Node(id: String) extends BEncodable {
//  require(id.size == 20)
//  override def bencode = ???
//}
//
//trait NodeMessage extends Message {
//  def node: Node
//}
//
//trait Query extends NodeMessage {
//  final override def y = 'q'
//  def q: String
//  def a: BEncodable
//
//  final override def bencode = BEncode("y" -> y, "q" -> q, "a" -> a, "t" -> t)
//}
//trait Response extends NodeMessage {
//  final override def y = 'r'
//  def r: BEncodable
//
//  final override def bencode = BEncode("y" -> y, "r" -> r, "t" -> t)
//}
//trait Error extends Message {
//  final override def y = 'e'
//  def errorNumber: Int
//  def errorMessage: String
//
//  final override def bencode = BEncode("y" -> y, "e" -> List[BEncodable](errorNumber, errorMessage), "t" -> t)
//}
//
//case class Ping(node: Node, t: String) extends Query {
//  final override def q = "ping"
//  final override def a = Map[String, BEncodable]("id" -> node)
//}
//
//case class PingResponse(node: Node, t: String) extends Response {
//  final override def r = Map[String, BEncodable]("id" -> node)
//}
//
//case class FindNode(node: Node, target: Node, t: String) extends Query {
//  final override def q = "find_node"
//  final override def a = Map[String, BEncodable]("id" -> node, "target" -> target)
//}
//
//case class FindNodeResponse(node: Node, t: String) extends Response {
//  final override def r = ???
//}
//
//case class GetPeers(node: Node, info_hash: String, t: String) extends Query {
//  final override def q = "get_peers"
//  final override def a = Map[String, BEncodable]("id" -> node, "info_hash" -> info_hash)
//}
//
//case class GetPeersResponse(node: Node, values: List[Peer], nodes: List[Node], token: Option[String], t: String) extends Response {
//  final override def r = Map[String, BEncodable]("id" -> node, "values" -> values, "nodes" -> nodes, "t" -> t) ++ token.map(tt => "token" -> (tt: BEncodable)).toMap
//}
//
//case class AnnouncePeer(node: Node, info_hash: String, port: Int, token: String, t: String) extends Query {
//  final override def q = "announce_peer"
//  final override def a = Map[String, BEncodable]("id" -> node, "info_hash" -> info_hash, "port" -> port, "token" -> token)
//}
//
//case class AnnouncePeerResponse(node: Node, t: String) extends Response {
//  final override def r = ???
//}
//
//trait Responder {
//  def node: Node
//
//  def respond(ping: Ping): PingResponse = PingResponse(node, ping.t)
//  def respond(find_node: FindNode): FindNodeResponse
//  def respond(get_peers: GetPeers): GetPeersResponse
//  def respond(announce_peer: AnnouncePeer): AnnouncePeerResponse
//}
//
//trait Router { router: Responder =>
//  override def respond(get_peers: GetPeers): GetPeersResponse = {
//    val values = ???
//    val nodes = ???
//    GetPeersResponse(node, values, nodes, None, get_peers.t)
//  }
//  override def respond(find_node: FindNode): FindNodeResponse = ???
//  override def respond(announce_peer: AnnouncePeer): AnnouncePeerResponse = ???
//
//}
//
//class UDPNode(val node: Node, port: Int) extends Responder with Router {
//  import java.net.DatagramPacket
//  import java.net.DatagramSocket
//
//  val bufferSize = 16
//  val sock = new DatagramSocket(port)
//  val buf = new Array[Byte](bufferSize)
//  val packet = new DatagramPacket(buf, bufferSize)
//  while (true) {
//    sock.receive(packet)
//    println("received packet from: " + packet.getAddress())
//    sock.send(packet)
//    println("echoed data (first 16 bytes): " + packet.getData().take(16).toString())
//  }
//
//}