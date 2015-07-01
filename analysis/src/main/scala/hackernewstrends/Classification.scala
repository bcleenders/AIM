package hackernewstrends

import java.io.{ObjectInputStream, FileInputStream}

import org.apache.spark.SparkContext
import org.apache.spark.mllib.feature.Word2VecModel
import org.apache.spark.mllib.linalg.{Vector, DenseVector}
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods._

object Classification extends App {
  implicit val formats = DefaultFormats // Brings in default date formats etc.

  val jobQuery = "*"
  val isFirstPart = false

  val sc = new SparkContext("local[8]", "Main")

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

  val languages = sc.parallelize(Seq(
    "python",
    "golang",
    "ruby",
    "javascript",
    "c++",
    "scala",
    "java",
    "clojure",
    ".net",
    "php",
    "node.js",
    "hadoop"
  ))
  val langVectors = languages.map(x =>
    x ->
      new DenseVector(
        x.toVec(model).toArray
      ).asInstanceOf[Vector]
  )

  val test = langVectors.collect()
  val sim = test.map(x => model.findSynonyms(x._2, 20))
  val python = new DenseVector("programming".toVec(model).toArray).asInstanceOf[Vector].toArray
//  val title = "Why is Golang popular in China?"
  val title = "Farewell Node.js"
  val titleToc = title.tokenize().flatten
  val titleVec = titleToc.map(m => m.toVec(model).toArray).reduceLeft(sumArray).div(titleToc.length.toDouble)

  val dist = test.map { case (lang, vec) =>
    lang -> titleVec.cosineSimilarity(vec.toArray)
  }.sortBy(-_._2)
  val top = dist.take(1)(0)

  sc.stop()

  implicit class VectorOperations(val arr: Array[Double]) {
    /*
   * This method takes 2 equal length arrays of integers
   * It returns a double representing similarity of the 2 arrays
   * 0.9925 would be 99.25% similar
   * (x dot y)/||X|| ||Y||
   */
    def cosineSimilarity(other: Array[Double]): Double = {
      require(arr.length == other.length)
      dotProduct(arr, other)/(magnitude(arr) * magnitude(other))
    }

    /*
   * Return the dot product of the 2 arrays
   * e.g. (a[0]*b[0])+(a[1]*a[2])
   */
    private def dotProduct(x: Array[Double], y: Array[Double]): Double = {
      val vec = for((a, b) <- x zip y) yield a * b


      vec.foldLeft(0.0)(_ + _)
    }

    /*
     * Return the magnitude of an array
     * We multiply each element, sum it, then square root the result.
     */
    private def magnitude(x: Array[Double]): Double = {
      math.sqrt(x.map(i => i*i).foldLeft(0.0)(_ + _))
    }

//    /*
//     * Return the dot product of the 2 arrays
//     * e.g. (a[0]*b[0])+(a[1]*a[2])
//     */
//    def dotProduct(other: Array[Double]): Double = {
//      val vec = for ((a, b) <- arr zip other) yield a * b
//
//      vec.sum
//    }
//
//    /*
//     * Return the magnitude of an array
//     * We multiply each element, sum it, then square root the result.
//     */
//    def magnitude(): Double = {
//      math.sqrt(arr map (i => i * i) sum)
//    }
  }

}
