val months = for {
  month <- 1 until 13
  year <- 2007 until 2016
} yield year + "-" + (if(month < 10) "0" + month.toString else month.toString)

val table = months.sortBy(x => x).drop(1).dropRight(6).map(x => x -> 1)