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
   // According to wikipedia, HN started February 19, 2007 :)
   first_day := time.Date(2007, time.February, 19, 12, 0, 0, 0, time.UTC)
   // So we want to crawl until the day before it started!
   first_day = first_day.Add(-24 * time.Hour)


   // For testing:
   first_day = time.Now().Add(-10 * 24 * time.Hour)

   // Rate-limit it! Fetch 3600 pages/hour max!
   ticker := time.Tick(1 * time.Second)

   for day := time.Now(); day.After(first_day); day = day.Add(-24 * time.Hour) {
      stories := FetchDay(day)
      Save(stories, day)

      year, month, day := day.Date()
      log.Println("Retrieving stories for ", fmt.Sprintf("%v-%v-%v", year, month, day))

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

func Save(stories []Story, date time.Time) {
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