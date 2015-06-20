import edu.arizona.sista.processors.fastnlp.FastNLPProcessor
import org.apache.spark.rdd.RDD
import weka.core.stemmers.SnowballStemmer


package object hackernewstrends {

  // Input models
  case class WebPage(title: String, metaDescription: String, metaKeywords: String, cleanedText: String, finalUrl: String, topImage: String)

  case class HNItem(created_at: java.util.Date, title: String, url: String, author: String, points: Int, story_text: String,
                    num_comments: Int, created_at_i: Int, objectID: String)

  case class Item(webpage: WebPage, HNItem: HNItem)

  case class Table(title: String, metaDescription: String, metaKeywords: String, cleanedText: String,
                   finalUrl: String, topImage: String, url: String, author: String, points: Int,
                   story_text: String, num_comments: Int, created_at_i: Int, objectID: String)



  // Output models
  case class Topics(jobName: String, description: String, topics: Array[Topic])

  case class Topic(id: Int, words: Array[TopicWord])

  case class TopicWord(word: String, probability: Double)

  case class JobDistribution(jobId: Int, jobName: String, description: String, numTopics: Int, maxIterations: Int)
  case class ArticleTopic(objectID: String, topics: Array[TopTopic])
  case class TopTopic(id: Int, probability: Double)

  case class Tokenized(id: String, words: Array[String])

  // Load the stop words
  // List found on: http://jmlr.org/papers/volume5/lewis04a/a11-smart-stop-list/english.stop
  val stream = getClass.getResourceAsStream("/english.stop")
  val stopWords = scala.io.Source.fromInputStream(stream).getLines().toList

  implicit class ArticleToWords(val corpus: RDD[Item]) {
    def splitWords: RDD[Array[String]] = {
      corpus.map(_.webpage.cleanedText).mapPartitions(partition => {
        partition.map(_.toLowerCase.split("\\s"))
      })
    }

    def lemmatize: RDD[Array[String]] = {
      corpus.map(_.webpage.cleanedText).mapPartitions(partition => {
        // Init the NLPProcessor (other options here: CoreNLPProcessor, BioNLPProcessor)
        val proc = new FastNLPProcessor(withDiscourse = true)
        partition.map { p =>
          val doc = proc.mkDocument(p)
          proc.tagPartsOfSpeech(doc)
          proc.lemmatize(doc)
          val words = doc.sentences.flatMap(x => x.lemmas.get)
          doc.clear()
          val size = words.length
          println(s"Article lemmatized! (number of words = $size)")

          words
        }
      })
    }

    def stem: RDD[(String, Array[String])] = {
      val snowballStemmer = new SnowballStemmer
      corpus.map(x => x.HNItem.objectID -> x.webpage.cleanedText).mapPartitions(partition => {
        partition.map(x => x._1 -> x._2.stem).map(x => x._1 -> x._2.toLowerCase.split("\\s"))
      })
    }
  }

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
    input.map(page => page.toLowerCase.split("\\s+")).map { word =>
      word
        .stem
        .filter(_.length > 3) // Only words that are longer than 3 characters
        .filter(_.forall(java.lang.Character.isLetter)) // Only letters
        .filter(!stopWords.contains(_)) // Filter out the stop-words
    }
  }

}
