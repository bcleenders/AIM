import org.apache.spark.rdd.RDD
import weka.core.stemmers.SnowballStemmer

package object hackernewstrends {
  // Load the stop words
  // List found on: http://jmlr.org/papers/volume5/lewis04a/a11-smart-stop-list/english.stop
  val stream = getClass.getResourceAsStream("/english.stop")
  val stopWords = scala.io.Source.fromInputStream(stream).getLines().toList

  implicit class ArticleCleaning(val article: Array[String]) {
    val snowballStemmer = new SnowballStemmer

    def stem = {
      article.map(_.stem)
    }
  }

  implicit class StringCleaning(val line: String) {
    val snowballStemmer = new SnowballStemmer
    def getArticle = {
      if (line.split("(\"webpage\":\")").size < 2) {
        clean(line)
      }
      else {
        clean(line
          .split("(\"webpage\":\")")(1)
          .split("\",\"HNItem\"")(0)
        )
      }
    }

    def clean(input: String) = {
      val test =
        input
          .replaceAll("\\\\n+", " ")
          //.replaceAll("(\\n+||\\\\n+||\n+)", "")
          .replaceAll("(\\\\r+)", " ")
          .replaceAll("(\\\\t+)", " ")
          .replaceAll("\\s+", " ")
          .replaceAll("\\\\u", "") // weird unicode characters
          .replace("\\\"", "")
          .replaceAll("\"", "")

      test
    }

    def stem = {
      snowballStemmer.stem(line)
    }

  }



  def stem(input: RDD[String]) = {
    input.map( page => page.toLowerCase.split("\\s+")).map { word =>
      word
        .stem
        .filter(_.length > 3) // Only words that are longer than 3 characters
        .filter(_.forall(java.lang.Character.isLetter)) // Only letters
        .filter(!stopWords.contains(_)) // Filter out the stop-words
    }
  }

}