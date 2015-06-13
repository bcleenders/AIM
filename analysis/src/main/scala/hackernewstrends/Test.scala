package hackernewstrends

import java.util.Properties

import edu.stanford.nlp.ling.CoreAnnotations._
import edu.stanford.nlp.pipeline._
import org.apache.spark.SparkContext

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer


object Test extends App {
  val stream = getClass.getResourceAsStream("/english.stop")
  val stopWords = scala.io.Source.fromInputStream(stream).getLines().toList
  val sc = new SparkContext("local", "Main")
  val plainText =  sc.parallelize(List("\"\\r\\nFalling speed is explained in the \\\"Transportation\\\" page on the wiki:\\n\\n\\n  Every tick (1/20 second), non-flying players and mobs have their vertical speed decremented (less upward motion, more downward motion) by 0.08 blocks per tick (1.6 m/s), then multiplied by 0.98. This would produce a terminal velocity of 3.92 blocks per tick, or 78.4 m/s.\\n\\n\\nTerminal Velocity\\n\\nAs stated above, terminal velocity in Minecraft is 78.4 m/s, but this speed cannot be reached in survival mode:\\n\\n\\n  However, the sky isn't quite high enough for that: Falling from layer 256 to bedrock takes about 5.5 seconds, with impact at 3.5 blocks per tick (70 m/s).\\n  \\n  In creative mode, you can fly higher, and could potentially reach terminal velocity falling from \\\"above the sky\\\".\\n\\n\\nAcceleration\\n\\nAcceleration is not talked about on the wiki, however see the change in velocity over time in the following graph. The graph was created using the formula:\\n\\n\\n  v(t) = (0.98t - 1) × 3.92\\n\\n\\n \\n\\nYou will notice that acceleration is not constant, and decreases over time.\\n\\nHow Long?\\n\\nUsing the velocity time graph, we can find how long it would take to reach terminal velocity.\\n\\n\\n  10% Terminal Velocity (0.392 blocks/tick) : ≈ 5 ticks (0.4 seconds)\\n  \\n  50% Terminal Velocity (1.96 blocks/tick) : ≈ 35 ticks (1.75 seconds)\\n  \\n  75% Terminal Velocity (2.94 blocks/tick) : ≈ 69 ticks (3.45 seconds)\\n  \\n  99% Terminal Velocity (3.8808 blocks/tick) : ≈ 228 ticks (11.4 seconds)\\n  \\n  100% Terminal Velocity (3.92 blocks/tick) : ≈ 672 ticks (33.6 seconds)\\n\\n\\nChicken Science!\\n\\nI couldn't find anything on the falling speed of chickens, so I set my own apparatus to test. It involves a 10 block fall with a pressure plate at the bottom, turning on a redstone lamp. I start the timer at the same time that I press the button, and stop the timer when I see the lamp turn on:\\n\\n\\n\\nI timed the fall of 3 different chickens to ensure accurate results:\\n\\n\\n  Test 1: 5.0 seconds\\n  \\n  Test 2: 4.9 seconds\\n  \\n  Test 3: 5.1 seconds\\n\\n\\nThis averages out at 5 seconds for a chicken to fall 10 blocks. Using the formula for speed we can calculate the speed of a falling chicken:\\n\\n\\n  speed = distance / time\\n  \\n  speed = 10 / 5\\n  \\n  speed = 2 m/s\\n\\n\\n∴ The falling speed of a chicken is 2 m/s.\\n\\nHigher?\\n\\nAs per the OP's request, I did the test again at a greater height, 50 blocks to be exact:\\n\\n\\n\\nI timed the test the same as the others, and received another result:\\n\\n\\n  Higher Test 1: 25.0 seconds\\n\\n\\nThe first test I did came back with 25 seconds flat, I didn't feel a need to repeat any more tests:\\n\\n\\n  speed = distance / time\\n  \\n  speed = 50 / 25\\n  \\n  speed = 2 m/s\\n\\n\\nNote: I was using unladen European chickens. I also noticed that chickens reach their maximum velocity as soon as they started falling.\\n\\n\\n\\nNo chicken were harmed in the making of this answer:\\n\\n\\n    \""))

  def plainTextToLemmas(text: String, stopWords: List[String], pipeline: StanfordCoreNLP): Seq[String] = {
    val doc = new Annotation(text)
    pipeline.annotate(doc)
    val lemmas = new ArrayBuffer[String]()
    val sentences = doc.get(classOf[SentencesAnnotation])
    for (sentence <- sentences; token <- sentence.get(classOf[TokensAnnotation])) {
      val lemma = token.get(classOf[LemmaAnnotation])
      if (lemma.length > 2 && !stopWords.contains(lemma)) {
        lemmas += lemma.toLowerCase
      }
    }
    lemmas
  }

//  val test = corpus.collect().reduce(_ + _)
//  val doc = proc.mkDocument(test)
//  proc.tagPartsOfSpeech(doc)
//  proc.lemmatize(doc)
//  val words = doc.sentences.map(x => x.lemmas.get).flatten
//  doc.clear()

  val lemmatized = plainText.mapPartitions(p => {
    val props = new Properties()
    props.put("annotators", "tokenize, ssplit, pos, lemma")
    val pipeline = new StanfordCoreNLP(props)
    p.map(q => plainTextToLemmas(q, stopWords, pipeline))
  })

  lemmatized.foreach(println)

  val ignoreable = ", \t\r\n"
  //val text2 = "{\"webpage\":\"\\nA few days ago a Dutch movie director asked people to upload a copy of one of his older films onto The Pirate Bay. The filmmaker had become fed up with the fact that copyright issues made his work completely unavailable through legal channels. To his surprise, pirates were quick to deliver.\\n\\nDutch movie director Martin Koolhoven sent out an unusual request on Twitter a few days ago. \\nWhile many filmmakers fear The Pirate Bay, Koolhoven asked his followers to upload a copy of his 1999 film “Suzy Q” to the site.\\n“Can someone just upload Suzy Q to The Pirate Bay?” Koolhoven asked.\\nThe director doesn’t own all copyrights to the movie himself, but grew frustrated by the fact that his film is not available through legal channels. \\nThe TV-film, which also features the film debut of Game of Thrones actress Carice Van Houten, was paid for with public money but after the music rights expired nobody was able to see it anymore.\\nThe main problem is with the film’s music, which includes tracks from popular artists such as The Rolling Stones and Jimi Hendrix. This prevented the film from being released in movie theaters and on DVD, and the TV-network also chose not to extend the licenses for the TV rights.\\nSince the music was no longer licensed it couldn’t be shown anymore, not even on the websites of the public broadcasters.\\n“To me, it felt like the movie had died,” Koolhoven tells TorrentFreak. \\nHoping to bring it back to life, Koolhoven tweeted his upload request, and it didn’t take long before the pirates delivered. Within a few hours the first copy of the film was uploaded, and several more were added in the days that followed. \\n“I had no idea the media would pick it up the way they did. That generated more media attention. At first I hesitated because I didn’t want to become the poster boy for the download-movement. All I wanted was for people to be able to see my film,” Koolhoven says.\\nUnfortunately the first upload of the movie that appeared on The Pirate Bay was in very bad quality. So the director decided to go all the way and upload a better version to YouTube himself. \\n“I figured it would probably be thrown off after a few days, due to the music rights issue, but at least people could see a half decent version instead of watching the horrible copy that was available on The Pirate Bay,” Koolhoven tells us.\\nInterestingly, YouTube didn’t remove the film but asked the director whether he had the right to use the songs. Since this is not the case the money made through the advertisements on YouTube will go to the proper rightsholders.\\n“We’re a few days later now and the movie is still on YouTube. And people have started to put higher quality torrents of Suzy Q on Pirate Bay. Even 720p can be found, I’ve heard,” Koolhoven notes.\\nWhile the director is not the exclusive rightsholder, he does see himself as the moral owner of the title. Also, he isn’t shying away from encouraging others to download and share the film.\\nIn essence, he believes that all movies should be available online, as long as it’s commercially viable. It shouldn’t hurt movie theater attendance either, as that remains the main source of income for most films and the best viewing experience.\\n“I know not everybody cares about that, but I do. The cinema is the best place to see movies. If you haven’t seen ‘Once Upon a Time in the West’ on the big screen, you just haven’t seen it,” Koolhoven says.\\nIn the case of Suzy Q, however, people are free to grab a pirated copy. \\n“Everyone can go to The Pirate Bay and grab a copy. People are actually not supposed to, but they have my permission to download Susy Q,” Koolhoven said in an interview with Geenstijl.\\n“If other people download the movie and help with seeding then the download time will be even more reasonable,” Koolhoven adds.\\n\""
  val text3 = "\n\t\t\n\t\t\n\t\t\n\t\tBy David Holmes \n\t\t\t\tOn August 1, 2014\n\n\t\t\nEarlier today, the Times of Israel, an online newspaper claiming a readership of 2 million, published an opinion piece on the Israeli-Gaza conflict called, “When Genocide is Permissible” by blogger Yochanan Gordon. Gordon, who falls squarely on Israel’s side, argues that when fighting an “enemy whose charter calls for the annihilation of our people,” then is not “genocide” an permissible response to this?\n“Nothing, then, can be considered disproportionate when we are fighting for our very right to live.”\nThe post quickly achieved the worst kind of virality, spreading across the web a message that, while not quite pro-genocide, didn’t exactly rule it out.\nThe Times of Israel quickly removed the article, stating that “the contents of this post have been removed for breaching The Times of Israel’s editorial guidelines.”\nSo how did it get published in the first place? What Times of Israel editor approved this post if it so clearly violates the site’s mission which, according to the site’s About page, is “to serve as a platform for constructive debate regarding the challenges facing Israel”?\nAccording to Yair Rosenberg, editor of the Israeli National Archives, the Times of Israel’s “bloggers” are not edited or moderated in any way. The system works more like Buzzfeed’s contributor network or Medium than, say, the New York Times’ array of blogs.\nBut while Buzzfeed’s and Medium’s claim they are “platforms,” not “publications,” the Times of Israel’s presentation of Gordon’s article looks undoubtedly journalism-y. It’s an opinion piece, yes, but opinion pieces still require a measure of accountability:\n\nYou would be forgiven for thinking, at first glance, that the Times of Israel is some decades-old paper of authority. In fact, it’s an online-only outlet established in 2012, funded not unlike many new media organizations in America: By a mega-rich dude. In this case, the billionaire benefactor is Seth Klarman, CEO and President of the Boston-based hedge fund, the Baupost Group. When Klarman first launched the outlet, he stated he would have no editorial control over the publication. But in that same statement, he also wrote, “My own interest in Israel has become even stronger post-9/11, when the threat of terrorism and the danger of radical Islam collided with a global campaign on many fronts to delegitimize the Jewish State.”\nRegardless of Klarman’s politics or how much editorial control he may or may not wield, I’m glad somebody at the Times of Israel realized the total irresponsibility of legitimizing genocide as a warfare tactic. It’s a lesson for any publication willing to throw caution to the wind by hosting unedited contributors. In fact, if the Times of Israel is willing to publish unedited content, what’s the point of having “editorial guidelines” in the first place? As for Gordon, his response in the wake of the post has been mixed — he doubled down on his genocide talk in a series of tweets, but then deleted them.\nAs Rosenberg was quick to note, today was a perfect example of how the Internet’s outrage-meter tends to spike over matters of bad taste as opposed to real-life atrocities.\n\nBut perhaps the best summation came from Jonathan Schanzer, VP for Research at the Foundation for Defense of Democracies:\n“If [you] argue that one blog represents all Israelis, or the Hamas charter represents all Palestinians, please reconsider your presence on twitter.”\nShare on FacebookShare on TwitterShare on Google+Share on LinkedIn\n\t\t\n\t\t\n\t\t\t\t\t\n\n\t\t\n\t\t\t\n\t\t\t\t\n\t\t\t\t\t\n\t\t\t\t\t\t\t\t\t\t\t\tDavid Holmes\n\t\t\t\t\t\tDavid Holmes is Pando's East Coast Editor. He is also the co-founder of Explainer Music, a production company specializing in journalistic music videos. His work has appeared at FastCompany.com, ProPublica, the Guardian, the Daily Dot, NewYorker.com, and Grist.\r\n\r\nYou can follow David on Twitter @holmesdm\t\t\t\t\t\n\t\t\t\t\n\t\t\t\n\t\t\t\t\t\n\t"
    .replaceAll("(\\n+||\\\\n+)", "")
    .replaceAll("(\\r+||\\\\r+)", "")
    .replaceAll("(\\t+||\\\\t+)", "")
    .replaceAll("\\s+", " ")
    .replace("\\\"", "")
  val text2 = "\n    \n\n        \n          \n  \n                   \n  \n\n\n\n          \n        \n    \n\n\n    \n        \n                      for your projects\n\n        \n    \n    \n    \n    \n        \n\n                \n                   \n                \n                \n                  Think Tinder/OKCupid + Linkedin. Hatchr can match you to talents who are great to work with, based on your personality.\n\n\n                    \n\n                    \n                    \n                      \n                    \n                      \n                    \n                      \n                        \n                        \n                          \n                        \n                        \n                    \n                    \n                    \n                \n        \n    \n\n\n\n \n       \n                     Personality matching\n                    \n\n                     \n                     Skills matter, and so does personality. Answer fun questions to get matched to people you'll click with. \n                     \n        \n\n        \n                     \n        \n        \n\n\n\n\n \n        \n                     \n        \n\n        \n\n        \n                     Mutual interests only\n                     \n                     You'll only get connected if you and the other person like each other. No spam, no cold pitch.\n        \n        \n    \n\n\n\n\n\n\n  \n                          \n\n                    Hatchr is launching soonon an invite-only basis.\n                    \n                        \n\n                   \n                \n                        \n                    \n                    \n                    \n                      \n                    \n                      \n                    \n                      \n                        \n                        \n                          \n                        \n                        \n                    \n                        \n                    \n\n                    \n\n                    \n  \n\n  \n  \n                  \n                      \n                      Follow us on\nTwitter \u0026 \nFacebook | \nWebsite made with ❤ by Keira Bui\n                    \n\n    \n\n    \n    \n\n    \n    \n    \n\n    \n\n    \n\n\n\n\n\n  "
    .replaceAll("(\\n+||\\\\n+)", "")
    .replaceAll("(\\r+||\\\\r+)", "")
    .replaceAll("(\\t+||\\\\t+)", "")
    .replaceAll("\\s+", " ")
    .replace("\\\"", "")

  val text = "\"\\r\\nFalling speed is explained in the \\\"Transportation\\\" page on the wiki:\\n\\n\\n  Every tick (1/20 second), non-flying players and mobs have their vertical speed decremented (less upward motion, more downward motion) by 0.08 blocks per tick (1.6 m/s), then multiplied by 0.98. This would produce a terminal velocity of 3.92 blocks per tick, or 78.4 m/s.\\n\\n\\nTerminal Velocity\\n\\nAs stated above, terminal velocity in Minecraft is 78.4 m/s, but this speed cannot be reached in survival mode:\\n\\n\\n  However, the sky isn't quite high enough for that: Falling from layer 256 to bedrock takes about 5.5 seconds, with impact at 3.5 blocks per tick (70 m/s).\\n  \\n  In creative mode, you can fly higher, and could potentially reach terminal velocity falling from \\\"above the sky\\\".\\n\\n\\nAcceleration\\n\\nAcceleration is not talked about on the wiki, however see the change in velocity over time in the following graph. The graph was created using the formula:\\n\\n\\n  v(t) = (0.98t - 1) × 3.92\\n\\n\\n \\n\\nYou will notice that acceleration is not constant, and decreases over time.\\n\\nHow Long?\\n\\nUsing the velocity time graph, we can find how long it would take to reach terminal velocity.\\n\\n\\n  10% Terminal Velocity (0.392 blocks/tick) : ≈ 5 ticks (0.4 seconds)\\n  \\n  50% Terminal Velocity (1.96 blocks/tick) : ≈ 35 ticks (1.75 seconds)\\n  \\n  75% Terminal Velocity (2.94 blocks/tick) : ≈ 69 ticks (3.45 seconds)\\n  \\n  99% Terminal Velocity (3.8808 blocks/tick) : ≈ 228 ticks (11.4 seconds)\\n  \\n  100% Terminal Velocity (3.92 blocks/tick) : ≈ 672 ticks (33.6 seconds)\\n\\n\\nChicken Science!\\n\\nI couldn't find anything on the falling speed of chickens, so I set my own apparatus to test. It involves a 10 block fall with a pressure plate at the bottom, turning on a redstone lamp. I start the timer at the same time that I press the button, and stop the timer when I see the lamp turn on:\\n\\n\\n\\nI timed the fall of 3 different chickens to ensure accurate results:\\n\\n\\n  Test 1: 5.0 seconds\\n  \\n  Test 2: 4.9 seconds\\n  \\n  Test 3: 5.1 seconds\\n\\n\\nThis averages out at 5 seconds for a chicken to fall 10 blocks. Using the formula for speed we can calculate the speed of a falling chicken:\\n\\n\\n  speed = distance / time\\n  \\n  speed = 10 / 5\\n  \\n  speed = 2 m/s\\n\\n\\n∴ The falling speed of a chicken is 2 m/s.\\n\\nHigher?\\n\\nAs per the OP's request, I did the test again at a greater height, 50 blocks to be exact:\\n\\n\\n\\nI timed the test the same as the others, and received another result:\\n\\n\\n  Higher Test 1: 25.0 seconds\\n\\n\\nThe first test I did came back with 25 seconds flat, I didn't feel a need to repeat any more tests:\\n\\n\\n  speed = distance / time\\n  \\n  speed = 50 / 25\\n  \\n  speed = 2 m/s\\n\\n\\nNote: I was using unladen European chickens. I also noticed that chickens reach their maximum velocity as soon as they started falling.\\n\\n\\n\\nNo chicken were harmed in the making of this answer:\\n\\n\\n    \""
    .replaceAll("(\\\\n+||\\\\r+||\\\\t+)", "")
    .replace("\\\"", "")
}
