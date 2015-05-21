package main

import "log"
import "encoding/json"
import "time"

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
	json := `{  
   "hits":[  
      {  
         "created_at":"2015-05-21T13:59:55.000Z",
         "title":"64-Bit Code in 2015: New in the Diagnostics of Possible Issues",
         "url":"https://medium.com/@CPP_Coder/64-bit-code-in-2015-new-in-the-diagnostics-of-possible-issues-b73c625bf2a2",
         "author":"Coder_CPP",
         "points":1,
         "story_text":"",
         "comment_text":null,
         "num_comments":0,
         "story_id":null,
         "story_title":null,
         "story_url":null,
         "parent_id":null,
         "created_at_i":1432216795,
         "_tags":[  
            "story",
            "author_Coder_CPP",
            "story_9582765"
         ],
         "objectID":"9582765",
         "_highlightResult":{  
            "title":{  
               "value":"64-Bit Code in 2015: New in the Diagnostics of Possible Issues",
               "matchLevel":"none",
               "matchedWords":[  

               ]
            },
            "url":{  
               "value":"https://medium.com/@CPP_Coder/64-bit-code-in-2015-new-in-the-diagnostics-of-possible-issues-b73c625bf2a2",
               "matchLevel":"none",
               "matchedWords":[  

               ]
            },
            "author":{  
               "value":"Coder_CPP",
               "matchLevel":"none",
               "matchedWords":[  

               ]
            },
            "story_text":{  
               "value":"",
               "matchLevel":"none",
               "matchedWords":[  

               ]
            }
         }
      }
   ],
   "nbHits":1298627,
   "page":0,
   "nbPages":1000,
   "hitsPerPage":1,
   "processingTimeMS":4,
   "query":"",
   "params":"advancedSyntax=true\u0026analytics=false\u0026hitsPerPage=1\u0026numericFilters=created_at_i%3C1432216956\u0026tags=story"
}`

	stories, noPages := Parse([]byte(json))

	log.Println(noPages)
	log.Println(stories)
}

func FetchDay(start_time, end_time int) {

}

// func writeToDisc(itemtype string) {
// 	f, err := os.Create(fmt.Sprintf("./output/%v-%06d.json", item.Name, item.Flushes))
// 	if err != nil {
// 		log.Println(err)
// 		log.Println("Dropping content!")
// 		return
// 	}
// 	defer f.Close()

// 	for i := 0; i < item.CurrFillFactor; i++ {
// 		_, err := f.WriteString(item.Content[i] + "\n")

// 		if err != nil {
// 			log.Println(err)
// 			log.Println("Dropping content!")
// 			return
// 		}
// 	}
   
//     f.Sync()
// }
