package hackernewstrends

import java.io.{FileInputStream, ObjectInputStream}

import org.apache.spark.SparkContext
import org.apache.spark.mllib.clustering.KMeans
import org.apache.spark.mllib.feature.Word2VecModel
import org.apache.spark.mllib.linalg._
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods._

object ReadModel extends App {
  implicit val formats = DefaultFormats // Brings in default date formats etc.

  val jobQuery = "*"
  val isFirstPart = false

  val sc = new SparkContext("local[8]", "Main")

  val months = for {
    month <- 1 until 13
    year <- 2007 until 2016
  } yield year + "-" + (if(month < 10) "0" + month.toString else month.toString)

  val table = sc.parallelize(months.sortBy(x => x).drop(1).dropRight(6))



  // Read Word2Vec model from Disk
  val fos = new FileInputStream("output/WordToVec/Model/model.obj")
  val oos = new ObjectInputStream(fos)
  val model = oos.readObject().asInstanceOf[Word2VecModel]


  val corpus = sc.wholeTextFiles("articles/HN-stories-" + jobQuery).flatMap { case (_, file) =>
    file.split("\n").map(parse(_).extract[Item]).filter(x => x.HNItem.title != "")
  }

  val titles = corpus
    .map(x => x.HNItem.objectID -> x.HNItem.title)
    .filter(_._2 != "")
    .map(x => x._1 -> x._2.tokenize().flatten.toSeq)
    .filter(_._2.nonEmpty)

  val titleVectors = titles.map { x =>
    val avgArr = x._2.map(m => m.toVec(model).toArray).reduceLeft(sumArray).div(x._2.length.toDouble)

    new DenseVector(avgArr).asInstanceOf[Vector]
  }
  val titlePairs = titles.map { x =>
    val avgArr = x._2.map(m => m.toVec(model).toArray).reduceLeft(sumArray).div(x._2.length.toDouble)

    (x._1, (x._2, new DenseVector(avgArr).asInstanceOf[Vector]))
  }

  for {
    month <- 1 until 12
    year <- 2007 until 2015
  } {
    val m = if(month < 10) "0" + month.toString else month.toString

    m + "-" + year
  }

  // K-Means
  var numClusters = 1000
  val numIterations = 25
  val clusters = KMeans.train(titleVectors, numClusters, numIterations)
  val articleMembership = titlePairs.mapValues(x => clusters.predict(x._2)).map(x => x._1 -> x._2)
  val clusterCenters = sc.parallelize(clusters.clusterCenters.zipWithIndex.map(e => (e._2, e._1)))
  val clusterTopics = clusterCenters.mapValues(x => model.findSynonyms(x, 10))

  //val top = clusterTopics.collect()

  val node = "node.js".toVec(model).toArray
  val php = "php".toVec(model).toArray
  val dot = node.dot(php) / (node.length * php.length)

//  val firstPart = titles.join(selfTokenized).map { case (objectId, (title, article)) =>
//    //val n = if(article._1.length < 5) article._1.length else 5
//    //val part = article._1.take(n).flatten.toSeq
//    if(article._2 != "") {
//      objectId -> (title ++ article._2.tokenize().flatten.toSeq) // add the the description + title
//    } else {
//      objectId -> title
//    }
//
//  }

//    val languages = sc.parallelize(Seq("python", "golang", "ruby", "javascript", "c++", "scala", "java", "clojure", ".net", "php"))
//    val langVectors = languages.map(x =>
//      new DenseVector(
//       x.toVec(model).toArray
//      ).asInstanceOf[Vector]
//    )
//
//  val syn = langVectors.map(model.findSynonyms(_, 10)).collect()
  //  //val k = KMeans.train(langVectors, 10, 25)


  val test = 0

  articleMembership.saveAsTextFile("output/WordToVec/2015_17/clustering/")

  val topics = for {
    topic <- clusterTopics
    word <- topic._2
  } yield (topic._1, word._1, word._2)
  topics.saveAsTextFile("output/WordToVec/2015_17/topics/")

}
