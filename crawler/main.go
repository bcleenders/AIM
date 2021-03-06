package main

import (
   "log"
   "strconv"
   "encoding/json"
   "time"
   "net/http"
   "io/ioutil"
   "fmt"
   "github.com/jinzhu/now"
   "os"
   // "github.com/agonopol/readability"
   "github.com/advancedlogic/GoOse"
   "strings"
   "runtime"
)

type Story struct {
   Created_at time.Time `json:"created_at"`
   Title string `json:"title"`
   Url string `json:"url"`
   Author string `json:"author"`
   Points int `json:"points"`
   Story_text string `json:"story_text"`
   Num_comments int `json:"num_comments"`
   Created_at_i int `json:"created_at_i"`
   ObjectId string `json:"objectID"`
}

type Webpage struct {
   Title string `json:"title"`
   MetaDescription string `json:"metaDescription"`
   MetaKeywords string `json:"metaKeywords"`
   CleanedText string `json:"cleanedText"`
   FinalUrl string `json:"finalUrl"`
   TopImage string `json:"topImage"`
}

type Result struct {
   Webpage Webpage `json:"webpage"`
   Story Story `json:"HNItem"`
}

type Response struct {
   Hits []Story `json:"hits"`
   NbPages int `json:"nbPages"`
}

func Parse(input []byte) ([]Story, int) {
   var parsed Response
   json.Unmarshal(input, &parsed)

   return parsed.Hits, parsed.NbPages
}

func main() {
   runtime.GOMAXPROCS(12)

   // Print some stats
   go func() {
      start := time.Now()
      go func() {
         for {
            log.Println("Running for", time.Since(start), " - successes:", successes, ", failedGets:", failedGets, ", failedReads:", failedReads, ", emptyExtracts:", emptyExtracts, ", skippedUrls:", skippedUrls)
            <-time.After(30 * time.Second)
         }
      }()
   }()

   // According to wikipedia, HN started February 19, 2007 :)
   first_day := time.Date(2007, time.February, 19, 12, 0, 0, 0, time.UTC)
   // So we want to crawl until the day before it started!
   first_day = first_day.Add(-24 * time.Hour)


   // For testing:
   // first_day = time.Now().Add(- 100 * 24 * time.Hour)

   // Rate-limit it! Fetch 3600 pages/hour max!
   ticker := time.Tick(1 * time.Second)

   n := time.Now()
   // For my private testing
   const shortForm = "2006-Jan-02"
   n, _ = time.Parse(shortForm, "2014-Nov-04")

   for day := n; day.After(first_day); day = day.Add(-24 * time.Hour) {
      y, m, d := day.Date()
      log.Println("Retrieving stories for ", fmt.Sprintf("%v-%v-%v", y, m, d))

      stories := FetchDay(day)
      results := FetchContent(stories)
      Save(results, day)

      // Block until we've arrived at the next second
      <-ticker
   }  
}

func FetchDay(day time.Time) ([]Story) {
   start := now.New(day).BeginningOfDay().Unix()
   end := now.New(day).EndOfDay().Unix()

   var stories []Story
   var numPages int

   // Fetch the first page
   stories, numPages = FetchBlock(start, end, 0)

   // Get the rest of the pages
   for i := 1; i < numPages; i++ {
      s, _ := FetchBlock(start, end, i)
      stories = append(stories, s...)
   }

   return stories
}

func FetchBlock(start_time, end_time int64, page int) ([]Story, int) {
   // Fetch the first page
   url := "http://hn.algolia.com/api/v1/search_by_date?tags=story&hitsPerPage=1000"
   url += "&numericFilters=created_at_i>" + strconv.FormatInt(start_time, 10)
   url += ",created_at_i<" + strconv.FormatInt(end_time, 10)
   url += "&page=" + strconv.Itoa(page)

   resp, err := http.Get(url)
   if err != nil {
      return nil, 0
   }
   defer resp.Body.Close()
   body, err := ioutil.ReadAll(resp.Body)
   if err != nil {
      return nil, 0 
   }

   stories, noPages := Parse(body)

   return stories, noPages
}

func Save(stories []Result, date time.Time) {
   year, month, day := date.Date()

   f, err := os.Create(fmt.Sprintf("./output/HN-stories-%v-%v-%v", year, month, day))
   if err != nil {
      log.Println(err)
      log.Println("Dropping content for (1) " + fmt.Sprintf("./output/day-%v-%v-%v", year, month, day))
      return
   }
   defer f.Close()

   for i := 0; i < len(stories); i++ {
      json, _ := json.Marshal(stories[i])
      _, err := f.Write(json)
      f.WriteString("\n")

      if err != nil {
         log.Println(err)
         log.Println("Dropping content for (2) " + fmt.Sprintf("./output/day-%v-%v-%v", year, month, day))
         return
      }
   }
   
    f.Sync()
}

// Use this to preserve order
type NumberedResult struct {
   Result Result
   Id int
}

// Fetch the urls (do it in parallel)
func FetchContent(stories []Story) ([]Result) {
   resultChan := make(chan NumberedResult)

   results := make([]Result, len(stories))

   // We need the buffer (1000) to make sure this thread won't block trying to start a new fetcher
   parallelismLimit := make(chan int, 1000)

   go func() {
      for i := 0; i < len(stories); i++ {
         // Wait untill we're allowed to continue...
         <-parallelismLimit
         go FetchUrl(stories[i], resultChan, i)
      }
   }()

   // Allow 80 parallel fetchers
   for i := 0; i < 240; i++ {
      parallelismLimit<-1
   }

   for i := 0; i < len(stories); i++ {
      numberedResult := <-resultChan
      results[numberedResult.Id] = numberedResult.Result

      // Allow the next fetcher to start
      parallelismLimit <- 1
   }

   return results
}

var failedGets int = 0
var failedReads int = 0
var emptyExtracts int = 0
var skippedUrls int = 0
var successes int = 0

func FetchUrl(story Story, results chan<- NumberedResult, id int) {
    defer func() {
       if r := recover(); r != nil {
           log.Println("Recovered from:", r, " in url:", story.Url)
       }
   }()

   // Return this on error
   emptyResult := NumberedResult{Result: Result{Story: story, Webpage: Webpage{
      Title: "",
      MetaDescription: "",
      MetaKeywords: "",
      CleanedText: "",
      FinalUrl: "",
      TopImage: "",
   }}, Id: id}

   finished := make(chan NumberedResult)

   // Start the fetcher in a new routine
   go func() {
      defer func() {
         if r := recover(); r != nil {
            log.Println("Recovered from:", r, " in url:", story.Url)
            finished <- emptyResult
         }
      }()

      if story.Url != "" && !strings.HasSuffix(story.Url, ".pdf") {
         resp, err := http.Get(story.Url)
         if err != nil {
            finished <- emptyResult
            failedGets++
            return
         }
         defer resp.Body.Close()

         body, err := ioutil.ReadAll(resp.Body)
         if err != nil {
            finished <- emptyResult
            failedReads++
            return
         }

         g := goose.New()
         article := g.ExtractFromRawHtml(story.Url, string(body))

         page := Webpage{
            Title: article.Title,
            MetaDescription: article.MetaDescription,
            MetaKeywords: article.MetaKeywords,
            CleanedText: article.CleanedText,
            FinalUrl: article.FinalUrl,
            TopImage: article.TopImage,
         }

         if page.CleanedText == "" {
            emptyExtracts++
         } else {
            successes++
         }

         finished<- NumberedResult{Result: Result{Story: story, Webpage: page}, Id: id}
      } else {
         skippedUrls++
         finished<- emptyResult
      }
   }()

   // Block untill either a finish or timeout
   select {
      case result := <-finished:
         results<- result
      case <-time.After(20 * time.Second):
         results<- emptyResult
   }
}
