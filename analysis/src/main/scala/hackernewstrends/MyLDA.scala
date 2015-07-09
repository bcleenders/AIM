package hackernewstrends

import org.apache.spark.SparkContext
import org.apache.spark.mllib.clustering.LDA
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.rdd.RDD
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization._

import scala.collection.mutable


object MyLDA extends App {
  implicit val formats = DefaultFormats // Brings in default date formats etc.

  //"articles/" "output/" 13 "*" "1000 topics with lemmatization" 1000 50
  val inputPath = "/Users/bramleenders/Studie/AIM3/analysis-lda/lemmatized/"
  val jobId = 200789
  val outputPath = "/Users/bramleenders/Studie/AIM3/analysis-lda/result-Experiment " + jobId + "/"
  val jobQuery = "lemmatized200*"
  val jobDescription = "500 topics with lemmatization"
  // LDA parameters
  val numTopics = 500
  val maxIterations = 25

  val sc = new SparkContext("local[8]", "Main")

  case class TopicStrings(id: Int, words: String)

  val tokenized = sc.wholeTextFiles(inputPath + jobQuery).flatMap { case (_, file) =>
    file.split("\n").map { x =>
      val elements = x.split(",")

      Tokenized(elements(0), elements(1).split(" "))
    }
  }

  //   termCounts: Sorted list of (term, termCount) pairs
  val termCounts: Array[(String, Long)] =
    tokenized
      .flatMap(_.words.map(_ -> 1L))
      .reduceByKey(_ + _)
      .collect()
      .sortBy(-_._2)

  val numStopwords = 100
  val vocabArray: Array[String] =
    termCounts
      .drop(numStopwords)
      .map(_._1)

//  //   vocab: Map term -> term index
  val vocab: Map[String, Int] = vocabArray.zipWithIndex.toMap

  val indexes: RDD[(Long, String)] = tokenized.zipWithIndex().map { case (tokens, id) =>
    (id, tokens.id)
  }

  // Convert documents into term count vectors
  val documents: RDD[(Long, Vector)] =
    tokenized.zipWithIndex().map { case (tokens, id) =>
      val counts = new mutable.HashMap[Int, Double]()
      tokens.words.foreach { term =>
        if (vocab.contains(term)) {
          val idx = vocab(term)
          counts(idx) = counts.getOrElse(idx, 0.0) + 1.0
        }
      }
      (id, Vectors.sparse(vocab.size, counts.toSeq))
    }

  val lda = new LDA().setK(numTopics).setMaxIterations(maxIterations)

  val ldaModel = lda.run(documents)

  // Write topics, showing top-weighted 20 terms for each topic.
  var counter = -1
  val topicIndices = ldaModel.describeTopics(maxTermsPerTopic = 20)
  val topics: Array[(Int, String, Double)] = topicIndices.flatMap { case (terms, termWeights) =>
    counter = counter + 1
    terms.zip(termWeights).map { case (term, weight) =>
      (counter, vocabArray(term.toInt), weight)
    }
  }

  val topicDist = ldaModel.topicDistributions
  val articles = indexes.join(topicDist).map { case (_, (docId, topics)) =>
    val topTopics = topics.toArray.zipWithIndex
      .filter(_._1 >= 1.0 / numTopics)
      .sortBy(-_._1)
      .map { case (probability, topicID) =>
      (topicID, probability)
    }

    (docId, topTopics)
  }

  val articleText = for {
    article <- articles
    topic <- article._2
  } yield (article._1, topic._1, topic._2)

  // Output everything to files
  articleText.saveAsTextFile(outputPath + "clustering")

  val topicsFile = new java.io.FileWriter(outputPath + "topics.txt")
  topics.foreach{case (topicId, word, weight) => topicsFile.write(s"$topicId, $word, $weight \n")}
  topicsFile.close()

  case class JobDistribution(jobId: Int, jobName: String, description: String, numTopics: Int, maxIterations: Int)

  // Add info about the job
  val infoFile = new java.io.FileWriter(outputPath + "info.json")
  infoFile.write(writePretty(JobDistribution(jobId, jobQuery, jobDescription, numTopics, maxIterations)))
  infoFile.close()

}
