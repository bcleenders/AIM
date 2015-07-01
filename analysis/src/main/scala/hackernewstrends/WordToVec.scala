package hackernewstrends

import java.io.{FileOutputStream, ObjectOutputStream}

import org.apache.spark.SparkContext
import org.apache.spark.mllib.feature.Word2Vec
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods._

object WordToVec extends App {
  implicit val formats = DefaultFormats // Brings in default date formats etc.

  val sc = new SparkContext("local[8]", "Main")

  val all = sc.wholeTextFiles("articles/HN-stories-*").flatMap { case (_, file) =>
    file.split("\n").map(parse(_).extract[Item]).filter(x => x.HNItem.title != "")
  }

  val selfTokenized = all
    .filter(_.webpage.cleanedText != "")
    .map(x => x.HNItem.objectID ->(x.webpage.cleanedText.tokenize(), x.webpage.metaDescription))

  val titles = all
    .map(x => x.HNItem.objectID -> x.HNItem.title)
    .filter(_._2 != "")
    .map(x => x._1 -> x._2.tokenize().flatten.toSeq)
    .filter(_._2.nonEmpty)


  // Word2Vec
  val articleWords = selfTokenized.flatMap(_._2._1)
  val vocab = titles.map(_._2) ++ articleWords
  val word2vec = new Word2Vec()
  val model = word2vec.fit(vocab)

  // Write model to disk
  val fos = new FileOutputStream("output/WordToVec/Model/model.obj")
  val oos = new ObjectOutputStream(fos)
  oos.writeObject(model)
  oos.close()

}
