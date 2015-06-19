package hackernewstrends

import org.apache.spark.SparkContext


object Test extends App {
  case class TopicTest(objectID: String, id: Int, probability: Double)

  val sc = new SparkContext("local[8]", "Main")


  val clustering = sc.textFile("/Users/marcromeyn/Projects/HackerNewsTrends/analysis/output/Experiment 1/clustering")
  val test = clustering.map { row =>
    val items = row.replace("(", "").replace(")", "").split(",")

    TopicTest(items(0), items(1).toInt, items(2).toDouble)
  }.collect()

  sc.stop()
}
