package hackernewstrends

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.json4s.jackson.JsonMethods._
import org.json4s.{DefaultFormats, _}

object LemmatizeLocal extends App {
  implicit val formats = DefaultFormats // Brings in default date formats etc.

  val sc = new SparkContext("local[8]", "Main")

  val jobQuery = "2013"
  val path = "articles/"

  case class TokenizedFile(id: String, words: String) {
    override def toString: String = {
      s"$id, $words"
    }
  }

  // Load the stop words
  // List found on: http://jmlr.org/papers/volume5/lewis04a/a11-smart-stop-list/english.stop
  val stream = getClass.getResourceAsStream("/english.stop")
  val stopWords = sc.broadcast(scala.io.Source.fromInputStream(stream).getLines().toList)

  //   Load all the files generated by the crawler and split each line (each line contains one article)
  //   Get rid of weird characters and filter all the empty artciles (the ones that the crawler couldn't fetch)
  val corpus: RDD[Item] = sc.wholeTextFiles(s"$path/HN-stories-" + jobQuery).flatMap { case (_, file) =>
    file.split("\n").map(parse(_).extract[Item]).filter(_.webpage.cleanedText != "")
  }


  val tokenized =
    corpus
      .lemmatizeFile
      .map(item =>
        TokenizedFile(
          item._1,
          item._2
            .filter(_.length > 3) // Only words that are longer than 3 characters
            .filter(_.forall(java.lang.Character.isLetter)) // Only letters
            .filter(!stopWords.value.contains(_)) // Filter out the stop-words
            .map(_.toLowerCase) // convert everything to lowercase
            .mkString(" ")
        )
      )

  tokenized.saveAsTextFile(s"$path/lemmatized2013/")
}
