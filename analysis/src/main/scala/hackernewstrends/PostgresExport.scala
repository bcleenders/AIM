package hackernewstrends

import com.github.mauricio.async.db.pool.{ConnectionPool, PoolConfiguration}
import com.github.mauricio.async.db.postgresql.pool.PostgreSQLConnectionFactory
import com.github.mauricio.async.db.postgresql.util.URLParser
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.json4s.jackson.JsonMethods._
import org.json4s.{DefaultFormats, _}

import scala.util.Try


object PostgresExport extends App {
  implicit val formats = DefaultFormats // Brings in default date formats etc.

  case class Line(webpage: String, HNItem: HNItem)
  case class HNItem(created_at: java.util.Date, title: String, url: String, author: String, points: Int, story_text: String,
                    num_comments: Int, created_at_i: Int, objectID: String)

  val sc = new SparkContext("local", "Main")

  val configuration = URLParser.parse("jdbc:postgresql://localhost:5432/hackernews?user=postgres&password=bergersg")
  //val connection: Connection = new PostgreSQLConnection(configuration)

  private val factory = new PostgreSQLConnectionFactory( configuration )
  private val pool = new ConnectionPool(factory, PoolConfiguration.Default)

  //Await.result(connection.connect, 5 seconds)


  val corpus: RDD[Try[HNItem]] = sc.wholeTextFiles("articles/HN-stories-2014-*").flatMap { case (_, file) =>
    val lines = file.split("}}").map{ x =>
      val test = x.split("\"HNItem\":")
      if(test.size > 1) {
        test(1) + "}"
      }
    }
    //val lines = file.split("}}").map(_ + "}}")
    for (line <- lines) yield {
      Try {
        parse(line.toString).extract[HNItem]
      }
    }
  }


  corpus.map { line =>
    if(line.isSuccess) {
      val hnItem = line.get
      pool.sendPreparedStatement(Queries.Insert, Array(hnItem.created_at,
        "", hnItem.author, hnItem.title, hnItem.points, hnItem.url, hnItem.objectID))
      println("Async query started!")
    }
  }.collect()


  object Queries {
    val Insert =
      """
        |INSERT INTO articles(
        |            "Date", "Article", "Author", "Title", "Points", "Url",
        |            "ObjectId")
        |    VALUES (?, ?, ?, ?, ?, ?,
        |            ?)
        |    RETURNING "Id"
      """.stripMargin
  }
}
