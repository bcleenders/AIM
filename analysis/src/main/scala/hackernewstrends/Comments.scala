package hackernewstrends

import org.apache.spark.SparkContext

object Comments extends App {

  val jobQuery = "2015-*"
  val isFirstPart = false
//  for {
//    line <- lines.next()
//    if counter < 1000
//  } {
//    counter = counter + 1
//    println(line)
//  }

  val sc = new SparkContext("local[8]", "Main")


  case class Text(comment_text: String)
  case class Comment(hits: Array[Text])
  //case class Document(entries:)

  val corpus = sc.textFile("/Users/marcromeyn/Downloads/HNCommentsAll.json").map { file =>
    file.split(",{\"created_at\"")
  }

  val test = corpus.take(10)


  sc.stop()
}
